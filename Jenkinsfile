// Jenkins declarative pipeline for the iStyle mobile automation.
// Jenkins needs: Maven tool named 'Maven3', JDK tool named 'JDK17' (Manage Jenkins > Tools),
// the "HTML Publisher" plugin, and Google Chrome installed on the agent.

// Runs a Maven command on Linux/Mac agents (sh) and on Windows agents (bat)
def mvn(String args) {
    if (isUnix()) {
        sh "mvn ${args}"
    } else {
        bat "mvn ${args}"
    }
}

// Runs the tests on one emulated phone and publishes the result of that phone
def runOnDevice(String device) {
    mvn("clean test -Ddevice=\"${device}\" -Dheadless=${params.HEADLESS} -Dcucumber.filter.tags=\"${params.TAGS}\" -DemailStartNumber=${env.BUILD_NUMBER}00")
    // a failed test marks the build UNSTABLE (yellow) and the numbers show up in Jenkins
    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
    publishHTML(target: [
        reportDir: 'target/cucumber-report',
        reportFiles: 'cucumber.html',
        reportName: "Cucumber Report - ${device}",
        keepAll: true,
        allowMissing: true,
        alwaysLinkToLastBuild: true
    ])
}

pipeline {
    agent any

    tools {
        maven 'Maven3'
        jdk 'JDK17'
    }

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
        // two runs at the same time would use the same email numbers
        disableConcurrentBuilds()
    }

    parameters {
        choice(name: 'DEVICE', choices: ['Both', 'Pixel 7', 'iPhone 16 Pro Max'], description: 'Phone to imitate (Pixel 7 = Android, iPhone = iOS)')
        booleanParam(name: 'HEADLESS', defaultValue: true, description: 'Run Chrome without a window (needed on a server without a screen)')
        string(name: 'TAGS', defaultValue: '@checkout', description: 'Cucumber tags to run, e.g. @checkout, @negative, @smoke, @checkout or @negative')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Compile') {
            steps {
                // fails fast on a code error, before any browser is started
                script { mvn('clean test-compile') }
            }
        }

        stage('Prepare test data') {
            steps {
                // every build starts the catchmail.io email numbers from a new range (BUILD_NUMBER00, BUILD_NUMBER01, ...)
                script {
                    if (isUnix()) {
                        sh 'rm -f email-counter.txt'
                    } else {
                        bat 'if exist email-counter.txt del email-counter.txt'
                    }
                }
            }
        }

        stage('Android - Pixel 7') {
            when { expression { params.DEVICE == 'Both' || params.DEVICE == 'Pixel 7' } }
            steps {
                script { runOnDevice('Pixel 7') }
            }
        }

        stage('iOS - iPhone 16 Pro Max') {
            when { expression { params.DEVICE == 'Both' || params.DEVICE == 'iPhone 16 Pro Max' } }
            steps {
                script { runOnDevice('iPhone 16 Pro Max') }
            }
        }
    }

    post {
        always {
            echo "Finished with result: ${currentBuild.currentResult}"
        }
        unstable {
            echo 'Some scenarios failed. Open the Cucumber Report of the phone (failure screenshots are inside it).'
        }
    }
}