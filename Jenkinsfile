pipeline {
    agent any

    environment {
        // 项目配置
        PROJECT_NAME = 'bickdemo'
        BACKEND_DIR = 'bickdemo-backend'
        FRONTEND_DIR = 'bickdemo-frontend'
        ADMIN_DIR = 'bickdemo-admin'
    }

    options {
        // 保留最近的构建记录
        buildDiscarder(logRotator(numToKeepStr: '10'))
        // 超时时间
        timeout(time: 30, unit: 'MINUTES')
        // 禁止并发构建
        disableConcurrentBuilds()
        // 避免 Declarative 自动 checkout 一次 + 我们自己再 checkout 一次
        skipDefaultCheckout(true)
    }

    parameters {
        // 仅用于“快速重启部署已构建版本”，正常推送上线请保持 false
        booleanParam(name: 'SKIP_BUILD', defaultValue: false, description: '跳过前后端构建与镜像构建，仅执行 docker-compose up -d 进行部署')
        // 仅在你怀疑工作区脏了/依赖坏了时才打开
        booleanParam(name: 'CLEAN_WORKSPACE', defaultValue: false, description: '构建前清空 Jenkins 工作区（会导致每次重新 npm install）')
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
                script {
                    if (params.CLEAN_WORKSPACE) {
                        echo '🧹 CLEAN_WORKSPACE=true，清空工作区...'
                        cleanWs()
                    }
                }
                checkout scm
                script {
                    String envFilePath = null
                    List<String> workspaceEnvCandidates = [
                        '.env',
                        'script/prod/deploy/.env.jenkins.current',
                        'script/prod/deploy/jenkins.env'
                    ]
                    List<String> externalEnvCandidates = [
                        env.BICKDEMO_ENV_FILE,
                        '/opt/bickdemo/.env',
                        "${env.JENKINS_HOME ?: ''}/.bickdemo.env",
                        '/var/jenkins_home/.bickdemo.env',
                        "${env.HOME ?: ''}/.bickdemo.env"
                    ].findAll { it?.trim() }

                    for (String candidate : workspaceEnvCandidates) {
                        if (fileExists(candidate)) {
                            envFilePath = candidate
                            break
                        }
                    }

                    if (!envFilePath) {
                        for (String candidate : externalEnvCandidates) {
                            int existsStatus = sh(script: "[ -f '${candidate}' ]", returnStatus: true)
                            if (existsStatus == 0) {
                                envFilePath = candidate
                                break
                            }
                        }
                    }

                    if (envFilePath) {
                        String envFileContent = fileExists(envFilePath)
                            ? readFile(envFilePath)
                            : sh(script: "cat '${envFilePath}'", returnStdout: true)

                        envFileContent
                            .split('\n')
                            .collect { it.trim() }
                            .findAll { it && !it.startsWith('#') && it.contains('=') }
                            .each { line ->
                                int separatorIndex = line.indexOf('=')
                                String key = line.substring(0, separatorIndex).trim()
                                String value = line.substring(separatorIndex + 1).trim()
                                env."${key}" = value
                            }
                        if (!env.MYSQL_PASSWORD?.trim()) {
                            env.MYSQL_PASSWORD = env.MYSQL_ROOT_PASSWORD
                        }
                        echo "已从环境文件加载部署变量: ${envFilePath}"
                    } else {
                        echo "未找到可用环境文件，已检查工作区路径: ${workspaceEnvCandidates}"
                        echo "未找到可用环境文件，已检查节点路径: ${externalEnvCandidates}"
                        echo '提示：script/prod/deploy/.env.jenkins.current 被 .gitignore 忽略，不会随 checkout 出现在 Jenkins 工作区'
                        echo '继续使用 Jenkins 环境变量/凭据'
                    }

                    if (!env.MYSQL_PASSWORD?.trim()) {
                        env.MYSQL_PASSWORD = env.MYSQL_ROOT_PASSWORD
                    }

                    env.DEPLOY_HOST = env.DEPLOY_HOST?.trim() ? env.DEPLOY_HOST.trim() : 'your-server-host'
                    env.DEPLOY_USER = env.DEPLOY_USER?.trim() ? env.DEPLOY_USER.trim() : 'root'
                    env.MYSQL_USERNAME = env.MYSQL_USERNAME?.trim() ? env.MYSQL_USERNAME.trim() : 'root'
                    env.MYSQL_DATABASE = env.MYSQL_DATABASE?.trim() ? env.MYSQL_DATABASE.trim() : 'bickdemo'
                    env.MINIO_ENDPOINT = env.MINIO_ENDPOINT?.trim() ? env.MINIO_ENDPOINT.trim() : 'http://localhost:9000'
                    env.MINIO_ACCESS_KEY = env.MINIO_ACCESS_KEY?.trim() ? env.MINIO_ACCESS_KEY.trim() : 'change-me-minio-access-key'
                    env.MINIO_SECRET_KEY = env.MINIO_SECRET_KEY?.trim() ? env.MINIO_SECRET_KEY.trim() : 'change-me-minio-secret-key'
                    env.MINIO_BUCKET = env.MINIO_BUCKET?.trim() ? env.MINIO_BUCKET.trim() : 'bicycles'
                    env.COMPOSE_PROJECT_NAME = env.COMPOSE_PROJECT_NAME?.trim() ? env.COMPOSE_PROJECT_NAME.trim() : 'bike-deploy'
                    env.APP_PUBLIC_HOST = env.APP_PUBLIC_HOST?.trim() ? env.APP_PUBLIC_HOST.trim() : 'http://localhost'
                    env.ADMIN_PUBLIC_HOST = env.ADMIN_PUBLIC_HOST?.trim() ? env.ADMIN_PUBLIC_HOST.trim() : 'http://localhost:3001'
                    env.BACKEND_PUBLIC_HOST = env.BACKEND_PUBLIC_HOST?.trim() ? env.BACKEND_PUBLIC_HOST.trim() : 'http://localhost:8080'
                    env.JENKINS_PUBLIC_HOST = env.JENKINS_PUBLIC_HOST?.trim() ? env.JENKINS_PUBLIC_HOST.trim() : 'http://localhost:8081'
                    env.GITEA_PUBLIC_HOST = env.GITEA_PUBLIC_HOST?.trim() ? env.GITEA_PUBLIC_HOST.trim() : 'http://localhost:3000'

                    def invalidSecrets = []
                    ['MYSQL_ROOT_PASSWORD', 'MYSQL_PASSWORD', 'JWT_SECRET'].each { key ->
                        String value = env."${key}"
                        if (!value?.trim() || value.contains('change-me')) {
                            invalidSecrets << key
                        }
                    }
                    if (invalidSecrets) {
                        error("部署环境变量未正确配置：${invalidSecrets.join(', ')}")
                    }

                    echo "部署变量检查完成：MYSQL_USERNAME=${env.MYSQL_USERNAME ?: 'root'}，MYSQL_PASSWORD=${env.MYSQL_PASSWORD == env.MYSQL_ROOT_PASSWORD ? '已复用 root 密码' : '使用独立数据库密码'}，COMPOSE_PROJECT_NAME=${env.COMPOSE_PROJECT_NAME}"

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
            when { expression { !params.SKIP_BUILD } }
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
            when { expression { !params.SKIP_BUILD } }
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

        stage('Build Admin') {
            when { expression { !params.SKIP_BUILD } }
            steps {
                echo '🛠️ 构建管理端...'
                dir("${ADMIN_DIR}") {
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

        stage('Stop Deployment Containers') {
            steps {
                echo '🛑 停止部署容器，先释放服务器资源再继续构建/部署...'
                sh '''
                    set +e

                    CONTAINERS="${COMPOSE_PROJECT_NAME}-frontend-1 ${COMPOSE_PROJECT_NAME}-admin-1 ${COMPOSE_PROJECT_NAME}-app-1 ${COMPOSE_PROJECT_NAME}-mysql-1"

                    echo "目标容器：${CONTAINERS}"
                    for container in ${CONTAINERS}; do
                        if docker ps -a --format '{{.Names}}' | grep -Fxq "${container}"; then
                            echo "停止容器：${container}"
                            docker stop "${container}" || true
                        else
                            echo "容器不存在，跳过：${container}"
                        fi
                    done

                    echo "当前相关容器状态："
                    docker ps -a --format 'table {{.Names}}\t{{.Status}}' | grep "${COMPOSE_PROJECT_NAME}" || true
                '''
            }
        }

        stage('Build Docker Image') {
            when { expression { !params.SKIP_BUILD } }
            steps {
                echo '🐳 构建 Docker 镜像...'
                sh '''
                    echo "切换到工作空间..."
                    cd ${WORKSPACE}
                    echo "当前目录：" && pwd
                    echo "构建镜像（使用缓存加速）..."
                    docker-compose build
                '''
            }
        }

        stage('Deploy') {
            steps {
                echo '🚀 部署应用...'
                sh """
                    cd ${WORKSPACE}
                    echo "启动服务..."
                    docker-compose up -d --remove-orphans

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
                    curl -f http://localhost:3001/health || echo "管理端健康检查失败，但继续..."
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
            echo '📊 构建完成'
        }
        success {
            echo '✅ 部署成功！'
            script {
                def currentTime = new Date().format('yyyy-MM-dd HH:mm:ss', TimeZone.getTimeZone('Asia/Shanghai'))
                echo "部署完成时间：${currentTime}"
                echo "访问地址："
                echo "  前端：${APP_PUBLIC_HOST}"
                echo "  管理端：${ADMIN_PUBLIC_HOST}"
                echo "  后端：${BACKEND_PUBLIC_HOST}"
                echo "  Jenkins: ${JENKINS_PUBLIC_HOST}"
                echo "  Gitea: ${GITEA_PUBLIC_HOST}"
            }
        }
        failure {
            echo '❌ 构建失败，请查看控制台输出'
        }
    }
}
