pipeline {
    agent any

    environment {
        // 项目配置
        PROJECT_NAME = 'bickdemo'
        BACKEND_DIR = 'bickdemo-backend'
        FRONTEND_DIR = 'bickdemo-frontend'

        // Docker 配置
        DOCKER_REGISTRY = ''  // 留空表示使用本地 Docker
        IMAGE_TAG = "${BUILD_NUMBER}"

        // 服务器配置 (如果是远程部署)
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
                script {
                    // 检查 Docker 是否可用
                    sh 'docker --version'
                    sh 'docker-compose --version || docker compose version'
                }
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
                        mvn clean package -DskipTests -B

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
                dir("${FRONTEND_DIR}") {
                    sh '''
                        echo "Node.js 版本："
                        node --version
                        npm --version

                        echo "安装依赖..."
                        npm ci --legacy-peer-deps || npm install --legacy-peer-deps

                        echo "构建生产版本..."
                        npm run build

                        echo "检查构建产物..."
                        ls -lh dist/ 2>/dev/null || echo "未找到 dist 目录"
                    '''
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: "${FRONTEND_DIR}/dist/**", allowEmptyArchive: true
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                echo '🐳 构建 Docker 镜像...'
                script {
                    def dockerComposeCmd = sh(script: 'docker-compose --version 2>/dev/null', returnStatus: true) == 0 ? 'docker-compose' : 'docker compose'
                    env.DOCKER_COMPOSE_CMD = dockerComposeCmd
                }
                sh '''
                    echo "使用命令：${DOCKER_COMPOSE_CMD}"

                    # 停止旧容器
                    ${DOCKER_COMPOSE_CMD} down || true

                    # 清理悬空镜像
                    docker image prune -f

                    # 构建新镜像
                    ${DOCKER_COMPOSE_CMD} build --no-cache
                '''
            }
        }

        stage('Deploy') {
            steps {
                echo '🚀 部署应用...'
                script {
                    def dockerComposeCmd = env.DOCKER_COMPOSE_CMD ?: 'docker-compose'
                    sh """
                        echo "启动服务..."
                        ${dockerComposeCmd} up -d

                        echo "等待服务启动..."
                        sleep 30

                        echo "检查容器状态..."
                        ${dockerComposeCmd} ps

                        echo "查看最近日志..."
                        ${dockerComposeCmd} logs --tail=50
                    """
                }
            }
        }

        stage('Health Check') {
            steps {
                echo '🏥 健康检查...'
                script {
                    // 等待后端启动
                    timeout(time: 2, unit: 'MINUTES') {
                        waitForURL(
                            url: 'http://localhost:8080/actuator/health',
                            timeout: 120000,
                            retryInterval: 5000
                        )
                    }
                    echo '✅ 后端服务健康检查通过'
                }
            }
            post {
                failure {
                    echo '⚠️ 健康检查失败，查看日志...'
                    sh 'docker-compose logs app'
                }
            }
        }

        stage('Cleanup') {
            steps {
                echo '🧹 清理构建缓存...'
                sh '''
                    # 清理悬空镜像
                    docker image prune -f

                    # 清理工作目录
                    cleanWs()
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
            script {
                // 可选：发送失败通知
                // emailext subject: "构建失败：${env.JOB_NAME}", body: "请查看：${env.BUILD_URL}", to: 'your-email@example.com'
            }
        }
    }
}
