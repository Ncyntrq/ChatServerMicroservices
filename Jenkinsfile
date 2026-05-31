pipeline {
    agent any

    environment {
        DOCKER_REGISTRY    = "hoangnguyen1007"
        IMAGE_TAG          = "${env.BUILD_NUMBER}"
        SONAR_PROJECT_KEY  = 'chat-server-microservices'
        COMPOSE_FILE_APP   = 'docker-compose.yml'
        COMPOSE_FILE_DEV   = 'docker-compose.devops.yml'
        NEWMAN_TEST_DIR    = 'tnguyen/test'
        SERVICES           = 'gateway-service auth-service server-service channel-service messaging-service presence-service log-service notification-service file-service role-service user-profile-service friend-service'
    }

    tools {
        maven 'Maven-3.9'
        nodejs 'NodeJS-20'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 60, unit: 'MINUTES')
        disableConcurrentBuilds()
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
                }
                echo "Building commit: ${env.GIT_COMMIT_SHORT} | Tag: ${env.IMAGE_TAG}"
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests -q --batch-mode'
            }
            post {
                success {
                    echo 'Maven build succeeded.'
                }
                failure {
                    echo 'Maven build failed. Aborting pipeline.'
                }
            }
        }

        stage('Static Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh """
                        mvn sonar:sonar \
                            -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                            -Dsonar.projectName='Chat Server Microservices' \
                            -Dsonar.host.url=${SONAR_HOST_URL} \
                            -Dsonar.token=${SONAR_AUTH_TOKEN} \
                            --batch-mode -q
                    """
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Dockerization & Push') {
            steps {
                script {
                    def services = env.SERVICES.trim().split('\\s+')
                    def portMap = [
                        'gateway-service'      : '8080',
                        'auth-service'         : '8081',
                        'messaging-service'    : '8082',
                        'presence-service'     : '8083',
                        'log-service'          : '8084',
                        'server-service'       : '8085',
                        'channel-service'      : '8086',
                        'notification-service' : '8088',
                        'file-service'         : '8089',
                        'user-profile-service' : '8090',
                        'role-service'         : '8091',
                        'friend-service'       : '8092'
                    ]

                    def buildTasks = [:]

                    services.each { svc ->
                        def port = portMap[svc] ?: '8080'
                        buildTasks[svc] = {
                            stage("Build & Push ${svc}") {
                                sh """
                                    docker build \
                                        --build-arg SERVICE_NAME="${svc}" \
                                        --build-arg EXPOSED_PORT="${port}" \
                                        -f Dockerfile.template \
                                        -t "${DOCKER_REGISTRY}/${svc}:${IMAGE_TAG}" \
                                        -t "${DOCKER_REGISTRY}/${svc}:latest" \
                                        .
                                """
                                withCredentials([usernamePassword(
                                    credentialsId: 'dockerhub-credentials',
                                    usernameVariable: 'DOCKER_USER',
                                    passwordVariable: 'DOCKER_PASS'
                                )]) {
                                    sh 'echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin'
                                    sh "docker push \"${DOCKER_REGISTRY}/${svc}:${IMAGE_TAG}\""
                                    sh "docker push \"${DOCKER_REGISTRY}/${svc}:latest\""
                                }
                            }
                        }
                    }

                    parallel buildTasks
                }
            }
            post {
                always {
                    sh 'docker logout || true'
                }
            }
        }

        stage('Deploy') {
            steps {
                script {
                    withCredentials([file(credentialsId: 'dotenv-file', variable: 'ENV_FILE')]) {
                        sh "cp \"${ENV_FILE}\" .env"
                    }
                    sh """
                        docker network create chat-net || true
                        IMAGE_TAG=${IMAGE_TAG} docker compose -f "${COMPOSE_FILE_APP}" pull --quiet || true
                        IMAGE_TAG=${IMAGE_TAG} docker compose -f "${COMPOSE_FILE_APP}" up -d --remove-orphans
                    """
                }
            }
            post {
                success {
                    echo "Deployment completed successfully. Image tag: ${IMAGE_TAG}"
                }
                failure {
                    echo "Deployment failed. Rolling back is required manually."
                }
                always {
                    sh 'rm -f .env || true'
                }
            }
        }
    }

    post {
        always {
            cleanWs(
                cleanWhenAborted: true,
                cleanWhenFailure: true,
                cleanWhenSuccess: true,
                patterns: [[pattern: '.env', type: 'INCLUDE']]
            )
        }
        success {
            echo "Pipeline SUCCESS | Build: ${env.BUILD_NUMBER} | Commit: ${env.GIT_COMMIT_SHORT}"
        }
        failure {
            echo "Pipeline FAILED | Build: ${env.BUILD_NUMBER} | Commit: ${env.GIT_COMMIT_SHORT}"
        }
    }
}
