// =============================================================================
// JENKINS POST SECTION TEMPLATE - REUSABLE FOR ANY DEPLOYMENT TYPE
// =============================================================================
// Usage: Copy this template and customize the variables at the top
// Supported deployment types: Docker, Enterprise Apps, Chocolatey, etc.

post {
    always {
        script {
            // =================================================================
            // CONFIGURATION - CUSTOMIZE THESE VARIABLES FOR YOUR PROJECT
            // =================================================================
            
            // Deployment identification
            def DEPLOYMENT_TYPE = env.DEPLOYMENT_TYPE ?: 'APPLICATION'  // DOCKER, CHOCOLATEY, APPLICATION, etc.
            def PROJECT_NAME = params.PROJECT_NAME ?: params.PACKAGE_NAME ?: params.APPLICATION_NAME ?: 'Unknown'
            def PROJECT_VERSION = params.PROJECT_VERSION ?: params.PACKAGE_VERSION ?: params.BUILD_VERSION ?: env.BUILD_VERSION ?: 'Latest'
            def OPERATION_TYPE = env.OPERATION_TYPE ?: params.OPERATION_TYPE ?: 'DEPLOY'  // DEPLOY, INSTALL, UNINSTALL, UPDATE, etc.
            def TARGET_ENVIRONMENT = params.TARGET_SERVER ?: params.ENVIRONMENT ?: params.TARGET_ENV ?: 'Unknown'
            
            // Optional fields (set to empty string if not used)
            def DOCKER_IMAGE = params.DOCKER_IMAGE ?: env.DOCKER_IMAGE ?: ''
            def DOCKER_TAG = params.DOCKER_TAG ?: env.DOCKER_TAG ?: ''
            def APPLICATION_URL = params.APPLICATION_URL ?: env.APPLICATION_URL ?: ''
            def DATABASE_VERSION = params.DATABASE_VERSION ?: env.DATABASE_VERSION ?: ''
            def CUSTOM_FIELD_1 = params.CUSTOM_FIELD_1 ?: env.CUSTOM_FIELD_1 ?: ''
            def CUSTOM_FIELD_2 = params.CUSTOM_FIELD_2 ?: env.CUSTOM_FIELD_2 ?: ''
            
            // Email configuration
            def EMAIL_RECIPIENT = 'your-email@company.com'  // CUSTOMIZE THIS
            def EMAIL_SUBJECT_PREFIX = '[Jenkins]'           // CUSTOMIZE THIS
            
            // =================================================================
            // REPORT GENERATION (DO NOT MODIFY UNLESS NEEDED)
            // =================================================================
            
            echo "${DEPLOYMENT_TYPE} deployment completed"
            
            // Build comprehensive report
            def report = """
            === ${DEPLOYMENT_TYPE} DEPLOYMENT REPORT ===
            Project: ${PROJECT_NAME}
            Version: ${PROJECT_VERSION}
            Operation: ${OPERATION_TYPE}
            Target: ${TARGET_ENVIRONMENT}
            Build: #${env.BUILD_NUMBER}
            Date: ${new Date().format('yyyy-MM-dd HH:mm:ss')}
            Status: ${currentBuild.currentResult}
            """ + 
            (DOCKER_IMAGE ? "\nDocker Image: ${DOCKER_IMAGE}" : '') +
            (DOCKER_TAG ? "\nDocker Tag: ${DOCKER_TAG}" : '') +
            (APPLICATION_URL ? "\nApplication URL: ${APPLICATION_URL}" : '') +
            (DATABASE_VERSION ? "\nDatabase Version: ${DATABASE_VERSION}" : '') +
            (CUSTOM_FIELD_1 ? "\nCustom Field 1: ${CUSTOM_FIELD_1}" : '') +
            (CUSTOM_FIELD_2 ? "\nCustom Field 2: ${CUSTOM_FIELD_2}" : '') +
            """

            Build URL: ${env.BUILD_URL}
            Console Logs: ${env.BUILD_URL}console
            ==========================================
            """
            
            writeFile file: "${DEPLOYMENT_TYPE.toLowerCase()}_deployment_report.txt", text: report
            archiveArtifacts artifacts: "${DEPLOYMENT_TYPE.toLowerCase()}_deployment_report.txt", allowEmptyArchive: true
        }
    }
    
    success {
        script {
            // Use the same variables defined in 'always' section
            def DEPLOYMENT_TYPE = env.DEPLOYMENT_TYPE ?: 'APPLICATION'
            def PROJECT_NAME = params.PROJECT_NAME ?: params.PACKAGE_NAME ?: params.APPLICATION_NAME ?: 'Unknown'
            def PROJECT_VERSION = params.PROJECT_VERSION ?: params.PACKAGE_VERSION ?: params.BUILD_VERSION ?: env.BUILD_VERSION ?: 'Latest'
            def OPERATION_TYPE = env.OPERATION_TYPE ?: params.OPERATION_TYPE ?: 'DEPLOY'
            def TARGET_ENVIRONMENT = params.TARGET_SERVER ?: params.ENVIRONMENT ?: params.TARGET_ENV ?: 'Unknown'
            def DOCKER_IMAGE = params.DOCKER_IMAGE ?: env.DOCKER_IMAGE ?: ''
            def DOCKER_TAG = params.DOCKER_TAG ?: env.DOCKER_TAG ?: ''
            def APPLICATION_URL = params.APPLICATION_URL ?: env.APPLICATION_URL ?: ''
            def EMAIL_RECIPIENT = 'your-email@company.com'
            def EMAIL_SUBJECT_PREFIX = '[Jenkins]'
            
            echo "${OPERATION_TYPE} '${PROJECT_NAME}' completed successfully!"
            
            currentBuild.description = "${OPERATION_TYPE} ${PROJECT_NAME} v${PROJECT_VERSION} → ${TARGET_ENVIRONMENT}"
            
            // Build dynamic email body
            def emailBody = """
            🚀 ${DEPLOYMENT_TYPE} DEPLOYMENT - SUCCESS

            Project: ${PROJECT_NAME}
            Version: ${PROJECT_VERSION}
            Operation: ${OPERATION_TYPE}
            Environment: ${TARGET_ENVIRONMENT}
            Build: #${env.BUILD_NUMBER}
            Date: ${new Date().format('yyyy-MM-dd HH:mm:ss')}

            📊 Build details: ${env.BUILD_URL}
            """ + 
            (DOCKER_IMAGE ? "\n🐳 Docker Image: ${DOCKER_IMAGE}" : '') +
            (DOCKER_TAG ? "\n🏷️ Docker Tag: ${DOCKER_TAG}" : '') +
            (APPLICATION_URL ? "\n🌐 Application URL: ${APPLICATION_URL}" : '') +
            """

            ✅ The ${OPERATION_TYPE.toLowerCase()} operation completed successfully.
            """
            
            mail to: EMAIL_RECIPIENT,
                subject: "${EMAIL_SUBJECT_PREFIX} ${DEPLOYMENT_TYPE} ${OPERATION_TYPE} - ${PROJECT_NAME} - Success",
                body: emailBody
        }
    }
    
    failure {
        script {
            // Use the same variables defined in 'always' section
            def DEPLOYMENT_TYPE = env.DEPLOYMENT_TYPE ?: 'APPLICATION'
            def PROJECT_NAME = params.PROJECT_NAME ?: params.PACKAGE_NAME ?: params.APPLICATION_NAME ?: 'Unknown'
            def PROJECT_VERSION = params.PROJECT_VERSION ?: params.PACKAGE_VERSION ?: params.BUILD_VERSION ?: env.BUILD_VERSION ?: 'Latest'
            def OPERATION_TYPE = env.OPERATION_TYPE ?: params.OPERATION_TYPE ?: 'DEPLOY'
            def TARGET_ENVIRONMENT = params.TARGET_SERVER ?: params.ENVIRONMENT ?: params.TARGET_ENV ?: 'Unknown'
            def EMAIL_RECIPIENT = 'your-email@company.com'
            def EMAIL_SUBJECT_PREFIX = '[Jenkins]'
            
            echo "Failed to ${OPERATION_TYPE.toLowerCase()} '${PROJECT_NAME}'"
            
            currentBuild.description = "${OPERATION_TYPE} ${PROJECT_NAME} v${PROJECT_VERSION} → FAILED"
            
            // Build failure email body
            def emailBody = """
            ❌ ${DEPLOYMENT_TYPE} DEPLOYMENT - FAILURE

            Project: ${PROJECT_NAME}
            Version: ${PROJECT_VERSION}
            Operation: ${OPERATION_TYPE}
            Environment: ${TARGET_ENVIRONMENT}
            Build: #${env.BUILD_NUMBER}
            Date: ${new Date().format('yyyy-MM-dd HH:mm:ss')}

            🔍 Error logs: ${env.BUILD_URL}console

            ⚠️ Please check the logs for more details.
            """
            
            mail to: EMAIL_RECIPIENT,
                subject: "${EMAIL_SUBJECT_PREFIX} ${DEPLOYMENT_TYPE} ${OPERATION_TYPE} - ${PROJECT_NAME} - Failure",
                body: emailBody
        }
    }
    
    cleanup {
        cleanWs()
    }
}