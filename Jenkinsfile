pipeline {
	agent any

    parameters {
        choice(name: 'BUILD_TYPE', choices: [ 'TEST', 'STAGE', 'RELEASE', 'SITE', 'TAG_RELEASE' ], description: 'Indicates what type of build')
        string(name: 'LATEST_JDK', defaultValue: 'jdk11', description: 'Tool name for the latest JDK to support')
		string(name: 'OLDEST_JDK', defaultValue: 'jdk8', description: 'Tool name for the oldest JDK to support')
    }
    
    triggers {
        cron('0 1 * * *')
    }
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
		disableConcurrentBuilds()
		timeout(time: 6, unit: 'HOURS')
    }

	tools {
	    maven 'maven-3.6.0'
	    jdk "${params.LATEST_JDK}"
	}

	
	stages {
	
		stage('Backwards compatible') {
			when {
				allOf {
					expression { params.BUILD_TYPE == 'TEST' }
				}
			}
			tools {
            	jdk "${params.OLDEST_JDK}"
            }
			steps {
	        	dir('officefloor/bom') {
					sh 'mvn -Dmaven.test.failure.ignore=true -Dofficefloor.skip.stress.tests=true clean install'
	        	}
				dir('officefloor/eclipse') {
				    sh 'mvn clean install -P OXYGEN.target'
				}
			}
		}
	
		stage('Build') {
		    when {
		        expression { params.BUILD_TYPE == 'TEST' }
		    }
	        steps {
	        	dir('officefloor/bom') {
	        	    sh 'mvn clean'
	        	}
	        	sh './.travis-install.sh'
	        }
		}
	
	    stage('Test') {
			when {
				allOf {
					expression { params.BUILD_TYPE == 'TEST' }
					not { branch 'master' }
				}
			}
	        steps {
	        	dir('officefloor/bom') {
					sh 'mvn -Dofficefloor.skip.stress.tests=true verify'
	        	}
	        }
	    }
	    
	    stage('Test and Stress') {
			when {
				allOf {
					expression { params.BUILD_TYPE == 'TEST' }
					branch 'master'
				}
			}
	        steps {
	        	dir('officefloor/bom') {
					sh 'mvn -Dmaven.test.failure.ignore=true verify'
	        	}
	        }
	    }
	    	    
	    stage('Stage') {
	        when {
	        	allOf {
	        	    branch 'master'
					expression { params.BUILD_TYPE == 'STAGE' }
	        	}
	        }
	        steps {
		        echo "JAVA_HOME = ${env.JAVA_HOME}"
	        	sh 'java -version'
	        	sh 'mvn -version'
	        }
	    }
	    
	    stage('Deploy Site') {
			when {
				allOf {
				    branch 'master'
					expression { params.BUILD_TYPE == 'SITE' }
				}
			}
			steps {
				echo "Running site deploy"
			}         
	    }
	}
	
    post {
   		always {
	    	junit 'officefloor/**/target/surefire-reports/TEST-*.xml'
	    	junit 'officefloor/**/target/failsafe-reports/TEST-*.xml'
	    }
	    success {
			emailext to: 'daniel@officefloor.net', replyTo: 'daniel@officefloor.net', subject: 'OF ${params.BUILD_TYPE} $BUILD_STATUS! ($BRANCH_NAME $BUILD_NUMBER)', body: '''
$PROJECT_NAME - $BRANCH_NAME - $BUILD_NUMBER - $BUILD_STATUS

Passed: ${TEST_COUNTS,var="pass"}
Failed: ${TEST_COUNTS,var="fail"}
Skipped: ${TEST_COUNTS,var="skip"}
Total: ${TEST_COUNTS,var="total"}

${FAILED_TESTS}'''
	    }
		failure {
	        emailext to: 'daniel@officefloor.net', replyTo: 'daniel@officefloor.net', subject: "OF ${params.BUILD_TYPE} $BUILD_STATUS! ($BRANCH_NAME $BUILD_NUMBER)", body: '''
$PROJECT_NAME - $BRANCH_NAME - $BUILD_NUMBER - $BUILD_STATUS

Changes (since last successful build):
${CHANGES_SINCE_LAST_SUCCESS}

Log (last lines):
...
${BUILD_LOG}
'''
		}
	}

}
