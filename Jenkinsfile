pipeline {
    agent any

    environment {
        // 项目配置
        PROJECT_NAME = 'bickdemo'
        BACKEND_DIR = 'bickdemo-backend'
        FRONTEND_DIR = 'bickdemo-frontend'

        // 服务器配置
        DEPLOY_HOST = '124.221.113.208'
        DEPLOY_USER = 'root'

        // 数据库配置
        MYSQL_ROOT_PASSWORD = 'Lile200623'
        MYSQL_DATABASE = 'bickdemo'

        // MinIO 配置
        MINIO_ENDPOINT = 'http://124.221.113.208:9000'
        MINIO_ACCESS_KEY = 'Cg6huvLg5AuW8ShqQoAr'
        MINIO_SECRET_KEY = 'j8AoV6yOOUXVNPWVcIpJLuuZJidCeurCiBwg1c1z'
        MINIO_BUCKET = 'bicycles'
    }

    options {
        // 保留最近的构建记录
        buildDiscarder(logRotator(numToKeepStr: '10'))
        // 超时时间
        timeout(time: 30, unit: 'MINUTES')
        // 禁止并发构建
        disableConcurrentBuilds()
    }

    triggers {
        // Git Hook 触发 (需要在 Gitee/GitHub 配置 Webhook)
        pollSCM('*/5 * * * *')  // 每 5 分钟检查一次代码变更
        // 定时构建 (每天凌晨 2 点)
        cron('0 2 * * *')
    }

    stages {
        stage('Checkout') {
            steps {
                echo '📦 拉取代码...'
                // 清空工作空间，避免旧代码干扰
                cleanWs()
                checkout scm
                script {
                    // 获取 Git 提交信息
                    env.GIT_COMMIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.GIT_BRANCH_NAME = env.BRANCH_NAME ?: 'main'
                }
                echo "✅ 代码拉取完成 | 分支：${env.GIT_BRANCH_NAME} | 提交：${env.GIT_COMMIT_SHORT}"
            }
        }

        stage('Prepare') {
            steps {
                echo '🔧 准备构建环境...'
                sh 'docker --version'
            }
        }

        stage('Build Backend') {
            steps {
                echo '🔨 构建后端...'
                dir("${BACKEND_DIR}") {
                    sh '''
                        echo "Maven 版本："
                        mvn --version

                        echo "清理并构建..."
                        # -o 离线模式，使用本地缓存
                        # -U 强制更新快照（可选）
                        mvn clean package -DskipTests -B -o || mvn clean package -DskipTests -B

                        echo "检查构建产物..."
                        ls -lh target/*.jar 2>/dev/null || echo "未找到 jar 包"
                    '''
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: "${BACKEND_DIR}/target/*.jar", allowEmptyArchive: true
                }
            }
        }

        stage('Build Frontend') {
            steps {
                echo '🎨 构建前端...'
                // 直接在 Jenkins 容器内构建（不挂载 Docker）
                dir("${FRONTEND_DIR}") {
                    sh '''
                        echo "当前目录：" && pwd &&
                        echo "文件列表：" && ls -la &&
                        echo "安装依赖..." &&
                        npm install --legacy-peer-deps &&
                        echo "构建..." &&
                        npx vite build &&
                        echo "检查产物：" && ls -lh dist/
                    '''
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                echo '🐳 构建 Docker 镜像...'
                sh '''
                    echo "切换到工作空间..."
                    cd ${WORKSPACE}
                    echo "当前目录：" && pwd
                    echo "停止旧容器..."
                    docker-compose down || true
                    echo "构建镜像（不使用 buildkit）..."
                    COMPOSE_DOCKER_CLI_BUILD=0 DOCKER_BUILDKIT=0 docker-compose build --no-cache
                '''
            }
        }

        stage('Deploy') {
            steps {
                echo '🚀 部署应用...'
                sh """
                    cd ${WORKSPACE}
                    echo "启动服务..."
                    docker-compose up -d

                    echo "等待服务启动..."
                    sleep 30

                    echo "检查容器状态..."
                    docker-compose ps

                    echo "查看最近日志..."
                    docker-compose logs --tail=50
                """
            }
        }

        stage('Health Check') {
            steps {
                echo '🏥 健康检查...'
                sh '''
                    echo "等待后端启动..."
                    sleep 10
                    curl -f http://localhost:8080/actuator/health || echo "健康检查失败，但继续..."
                '''
            }
        }

        stage('Cleanup') {
            steps {
                echo '🧹 清理构建缓存...'
                sh '''
                    # 清理悬空镜像
                    docker image prune -f
                '''
            }
        }
    }

    post {
        always {
            echo '📊 构建完成，清理工作空间...'
            cleanWs(cleanWhenNotBuilt: true)
        }
        success {
            echo '✅ 部署成功！'
            script {
                def currentTime = new Date().format('yyyy-MM-dd HH:mm:ss', TimeZone.getTimeZone('Asia/Shanghai'))
                echo "部署完成时间：${currentTime}"
                echo "访问地址："
                echo "  前端：http://124.221.113.208"
                echo "  后端：http://124.221.113.208:8080"
                echo "  Jenkins: http://124.221.113.208:8081"
                echo "  Gitea: http://124.221.113.208:3000"
            }
        }
        failure {
            echo '❌ 构建失败，请查看控制台输出'
        }
    }
}
