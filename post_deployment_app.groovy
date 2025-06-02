// =============================================================================
// JENKINS POST SECTION TEMPLATE - REUSABLE FOR ANY DEPLOYMENT TYPE
// =============================================================================
// Usage: Copy this template and customize the variables at the top
// Supported deployment types: Docker, Enterprise Apps, Chocolatey, etc.

post {
    always {
        script {
            // =================================================================
            // GLOBAL VARIABLES DEFINITION - USED ACROSS ALL POST SECTIONS
            // =================================================================
            
            // Store variables in environment for access in other post sections
            env.POST_DEPLOYMENT_TYPE = env.DEPLOYMENT_TYPE ?: 'APPLICATION'
            env.POST_PROJECT_NAME = params.PROJECT_NAME ?: params.PACKAGE_NAME ?: params.APPLICATION_NAME ?: 'Unknown'
            env.POST_PROJECT_VERSION = params.PROJECT_VERSION ?: params.PACKAGE_VERSION ?: params.BUILD_VERSION ?: env.BUILD_VERSION ?: 'Latest'
            env.POST_OPERATION_TYPE = env.OPERATION_TYPE ?: params.OPERATION_TYPE ?: 'DEPLOY'
            env.POST_TARGET_ENVIRONMENT = params.TARGET_SERVER ?: params.ENVIRONMENT ?: params.TARGET_ENV ?: 'Unknown'
            
            // Optional fields
            env.POST_DOCKER_IMAGE = params.DOCKER_IMAGE ?: env.DOCKER_IMAGE ?: ''
            env.POST_DOCKER_TAG = params.DOCKER_TAG ?: env.DOCKER_TAG ?: ''
            env.POST_APPLICATION_URL = params.APPLICATION_URL ?: env.APPLICATION_URL ?: ''
            env.POST_DATABASE_VERSION = params.DATABASE_VERSION ?: env.DATABASE_VERSION ?: ''
            env.POST_CUSTOM_FIELD_1 = params.CUSTOM_FIELD_1 ?: env.CUSTOM_FIELD_1 ?: ''
            env.POST_CUSTOM_FIELD_2 = params.CUSTOM_FIELD_2 ?: env.CUSTOM_FIELD_2 ?: ''
            
            // Email configuration
            env.POST_EMAIL_RECIPIENT = 'xxx@gmail.com'
            env.POST_EMAIL_SUBJECT_PREFIX = '[SUBJECT]'
            
            // Build timestamp
            env.POST_BUILD_TIMESTAMP = new Date().format('yyyy-MM-dd HH:mm:ss')
            
            // =================================================================
            // HELPER FUNCTIONS FOR EMAIL CONTENT
            // =================================================================
            
            // Function to build optional fields for emails
            env.POST_OPTIONAL_FIELDS = [
                (env.POST_DOCKER_IMAGE ? "🐳 Docker Image: ${env.POST_DOCKER_IMAGE}" : ''),
                (env.POST_DOCKER_TAG ? "🏷️ Docker Tag: ${env.POST_DOCKER_TAG}" : ''),
                (env.POST_APPLICATION_URL ? "🌐 Application URL: ${env.POST_APPLICATION_URL}" : ''),
                (env.POST_DATABASE_VERSION ? "💾 Database Version: ${env.POST_DATABASE_VERSION}" : ''),
                (env.POST_CUSTOM_FIELD_1 ? "📝 Custom Field 1: ${env.POST_CUSTOM_FIELD_1}" : ''),
                (env.POST_CUSTOM_FIELD_2 ? "📝 Custom Field 2: ${env.POST_CUSTOM_FIELD_2}" : '')
            ].findAll { it != '' }.join('\n')
            
            // =================================================================
            // REPORT GENERATION
            // =================================================================
            
            echo "${env.POST_DEPLOYMENT_TYPE} deployment completed"
            
            // Build comprehensive report using global variables
            def report = """
=== ${env.POST_DEPLOYMENT_TYPE} DEPLOYMENT REPORT ===
Project: ${env.POST_PROJECT_NAME}
Version: ${env.POST_PROJECT_VERSION}
Operation: ${env.POST_OPERATION_TYPE}
Target: ${env.POST_TARGET_ENVIRONMENT}
Build: #${env.BUILD_NUMBER}
Date: ${env.POST_BUILD_TIMESTAMP}
Status: ${currentBuild.currentResult}
""" + 
(env.POST_DOCKER_IMAGE ? "\nDocker Image: ${env.POST_DOCKER_IMAGE}" : '') +
(env.POST_DOCKER_TAG ? "\nDocker Tag: ${env.POST_DOCKER_TAG}" : '') +
(env.POST_APPLICATION_URL ? "\nApplication URL: ${env.POST_APPLICATION_URL}" : '') +
(env.POST_DATABASE_VERSION ? "\nDatabase Version: ${env.POST_DATABASE_VERSION}" : '') +
(env.POST_CUSTOM_FIELD_1 ? "\nCustom Field 1: ${env.POST_CUSTOM_FIELD_1}" : '') +
(env.POST_CUSTOM_FIELD_2 ? "\nCustom Field 2: ${env.POST_CUSTOM_FIELD_2}" : '') +
"""

Build URL: ${env.BUILD_URL}
Console Logs: ${env.BUILD_URL}console
==========================================
"""
            
            writeFile file: "${env.POST_DEPLOYMENT_TYPE.toLowerCase()}_deployment_report.txt", text: report
            archiveArtifacts artifacts: "${env.POST_DEPLOYMENT_TYPE.toLowerCase()}_deployment_report.txt", allowEmptyArchive: true
        }
    }
    
    success {
        script {
            echo "${env.POST_OPERATION_TYPE} '${env.POST_PROJECT_NAME}' completed successfully!"
            
            // Set build description using global variables
            currentBuild.description = "${env.POST_OPERATION_TYPE} ${env.POST_PROJECT_NAME} v${env.POST_PROJECT_VERSION} → ${env.POST_TARGET_ENVIRONMENT}"
            
            // Build email body using global variables and helper
            def emailBody = """
🚀 ${env.POST_DEPLOYMENT_TYPE} DEPLOYMENT - SUCCESS

Project: ${env.POST_PROJECT_NAME}
Version: ${env.POST_PROJECT_VERSION}
Operation: ${env.POST_OPERATION_TYPE}
Environment: ${env.POST_TARGET_ENVIRONMENT}
Build: #${env.BUILD_NUMBER}
Date: ${env.POST_BUILD_TIMESTAMP}

📊 Build details: ${env.BUILD_URL}

${env.POST_OPTIONAL_FIELDS}

✅ The ${env.POST_OPERATION_TYPE.toLowerCase()} operation completed successfully.
"""
            
            mail to: env.POST_EMAIL_RECIPIENT,
                subject: "${env.POST_EMAIL_SUBJECT_PREFIX} ${env.POST_DEPLOYMENT_TYPE} ${env.POST_OPERATION_TYPE} - ${env.POST_PROJECT_NAME} - Success",
                body: emailBody
        }
    }
    
    failure {
        script {
            echo "Failed to ${env.POST_OPERATION_TYPE.toLowerCase()} '${env.POST_PROJECT_NAME}'"
            
            // Set build description using global variables
            currentBuild.description = "${env.POST_OPERATION_TYPE} ${env.POST_PROJECT_NAME} v${env.POST_PROJECT_VERSION} → FAILED"
            
            // Build failure email body using global variables
            def emailBody = """
❌ ${env.POST_DEPLOYMENT_TYPE} DEPLOYMENT - FAILURE

Project: ${env.POST_PROJECT_NAME}
Version: ${env.POST_PROJECT_VERSION}
Operation: ${env.POST_OPERATION_TYPE}
Environment: ${env.POST_TARGET_ENVIRONMENT}
Build: #${env.BUILD_NUMBER}
Date: ${env.POST_BUILD_TIMESTAMP}

🔍 Error logs: ${env.BUILD_URL}console

${env.POST_OPTIONAL_FIELDS}

⚠️ Please check the logs for more details.
"""
            
            mail to: env.POST_EMAIL_RECIPIENT,
                subject: "${env.POST_EMAIL_SUBJECT_PREFIX} ${env.POST_DEPLOYMENT_TYPE} ${env.POST_OPERATION_TYPE} - ${env.POST_PROJECT_NAME} - Failure",
                body: emailBody
        }
    }
    
    cleanup {
        cleanWs()
    }
}