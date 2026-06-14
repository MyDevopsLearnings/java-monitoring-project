pipeline {
    agent any

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package'
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker build -t java-monitoring:latest .'
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                docker stop java-app || true
                docker rm java-app || true

                docker run -d \
                  -p 8080:8080 \
                  --name java-app \
                  java-monitoring:latest
                '''
            }
        }
    }

    post {
        success {
            echo 'Deployment Successful!'
        }

        failure {
            echo 'Build Failed!'
        }
    }
}
