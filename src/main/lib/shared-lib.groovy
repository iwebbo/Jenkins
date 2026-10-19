#!/usr/bin/env groovy

// ============================================================================
// Shared Library - Reusable Jenkins Functions
// ============================================================================

@Library('shared-lib@1.0') _ import

// ============================================================================
// Build Utilities
// ============================================================================

def buildWithRetry(maxAttempts = 3, delayMs = 5000) {
    def attempt = 0
    
    while (attempt < maxAttempts) {
        attempt++
        try {
            echo "Attempt ${attempt}/${maxAttempts} - Starting build..."
            // Build logic here
            return true
        } catch (Exception e) {
            echo "Attempt ${attempt}/${maxAttempts} failed: ${e.message}"
            if (attempt < maxAttempts) {
                sleep(delayMs / 1000)
            } else {
                throw new Exception("Build exceeded maximum retry attempts")
            }
        }
    }
    
    return false
}

def validateEnvironment() {
    def requiredEnvVars = ['APP_ENV', 'DB_HOST', 'API_KEY']
    
    for (envVar in requiredEnvVars) {
        if (!env(envVar)) {
            error "Required environment variable not set: ${envVar}"
        }
    }
    
    echo "Environment validation passed"
}

// ============================================================================
// Deployment Utilities
// ============================================================================

def deployToEnvironment(environmentName, deploymentScript) {
    def endpoint = env("DEPLOY_ENDPOINT_${environmentName}") ?: 'http://localhost:8080'
    
    sh '''
        curl -X POST "${endpoint}/deploy" \
            -H "Content-Type: application/json" \
            -d '{"environment": "${environmentName}", "script": "${deploymentScript}"}'
    '''
}

def rollbackToPreviousVersion() {
    echo "Rolling back to previous version..."
    
    sh '''
        cd /var/jenkins/workspace/${env.BUILD_NAME}
        ./gradlew deployRollback --no-daemon
    '''
}

// ============================================================================
// Health Check Utilities
// ============================================================================

def healthCheck(url, timeout = 10) {
    def response = sh(
        script: "curl -s -o /dev/null -w '%{http_code}' '${url}'",
        returnStdout: true,
        timeout: timeout * 1000
    )
    
    def statusCode = Integer.parseInt(response.trim())
    if (statusCode >= 200 && statusCode < 300) {
        echo "Health check passed for ${url} (${statusCode})"
        return true
    } else {
        error "Health check failed for ${url}: HTTP ${statusCode}"
        return false
    }
}

def performHealthCheck() {
    def checks = [
        url: 'http://localhost:8080/actuator/health',
        timeout: 5
    ]
    
    echo "Running health checks..."
    for (check in checks) {
        if (!healthCheck(check.url, check.timeout)) {
            error "Health check suite failed"
            return false
        }
    }
    
    echo "All health checks passed"
    return true
}

// ============================================================================
// Logging Utilities
// ============================================================================

def logBuildMetrics() {
    def metrics = [
        buildDuration: env('BUILD_DURATION'),
        memoryUsage: env('MEMORY_USAGE'),
        cpuUsage: env('CPU_USAGE')
    ]
    
    echo "=== Build Metrics ==="
    for (metric in metrics) {
        echo "${metric.key}: ${metric.value}"
    }
}

def logDeploymentStatus() {
    def status = env('DEPLOYMENT_STATUS', 'unknown')
    
    echo "=== Deployment Status ==="
    switch(status) {
        case 'success':
            echo "✓ Deployment completed successfully"
            break
        case 'partial':
            echo "⚠ Partial deployment - some services may be down"
            break
        case 'failed':
            echo "✗ Deployment failed - manual intervention required"
            break
    }
}

// ============================================================================
// Configuration Utilities
// ============================================================================

def getConfigurationValue(key, defaultValue = null) {
    def value = env("CONFIG_${key}") ?: defaultValue
    
    if (value == 'null' || value == '') {
        return defaultValue
    }
    
    try {
        // Try to parse as JSON first
        def jsonValue = new JsonSlurper().parseText(value)
        return jsonValue
    } catch (Exception e) {
        // Return string if not valid JSON
        return value
    }
}

def loadConfiguration() {
    def config = getConfigurationValue('app', {})
    
    echo "Loaded configuration:"
    for (key in config.keySet()) {
        echo "  ${key}: ${config[key]}"
    }
    
    return config
}

// ============================================================================
// Database Utilities
// ============================================================================

def executeDatabaseQuery(query) {
    def result = sh(
        script: """
            cd /var/jenkins/workspace/${env.BUILD_NAME}
            psql -h ${env('DB_HOST')} -p ${env('DB_PORT')} \
                -U ${env('DB_USER')} -d ${env('DB_NAME')} \
                -c "${query}"
        """,
        returnStdout: true,
        timeout: 30 * 1000
    )
    
    def output = result.trim()
    if (output.isEmpty()) {
        echo "Query returned no results"
        return []
    }
    
    // Parse query results into list of maps
    def lines = output.split('\n')
    def headers = lines[0].split('|').collect { it.trim().replaceAll(/['"]+/g, '').replace(/\s+/, ' ') }
    def rows = lines[1..-1].findAll { line -> line.trim() != '' }.each { 
        it.split('|').collect { it.trim().replaceAll(/['"]+/g, '') }
    }
    
    return headers.collectAsList() { h -> rows.collectAsList() { r -> h.zip(r).collect { [h[0], r[0]] } } }
}

def checkDatabaseConnection() {
    def query = "SELECT 1"
    try {
        executeDatabaseQuery(query)
        echo "✓ Database connection successful"
        return true
    } catch (Exception e) {
        error "✗ Database connection failed: ${e.message}"
        return false
    }
}

// ============================================================================
// API Utilities
// ============================================================================

def callExternalApi(endpoint, method = 'GET', data = null) {
    def headers = [
        'Content-Type': 'application/json'
    ]
    
    if (env('API_KEY')) {
        headers['Authorization'] = "Bearer ${env('API_KEY')}"
    }
    
    def url = "${endpoint}${data ? '?${data}' : ''}"
    
    try {
        def response = sh(
            script: """
                curl -X ${method} \
                    --header '${headers.join('\\n')}\\n' \
                    "${url}"
            """,
            returnStdout: true,
            timeout: 30 * 1000
        )
        
        def body = sh(
            script: """
                curl -X ${method} \
                    --header '${headers.join('\\n')}\\n' \
                    "${url}"
            """,
            returnStdout: true,
            timeout: 30 * 1000
        )
        
        def result = body.trim()
        if (result.isEmpty()) {
            echo "API call returned empty response"
            return null
        }
        
        try {
            def jsonResult = new JsonSlurper().parseText(result)
            return jsonResult
        } catch (Exception e) {
            echo "Failed to parse API response: ${e.message}"
            return result
        }
    } catch (Exception e) {
        error "API call failed for ${endpoint}: ${e.message}"
        throw new Exception("External API call failed")
    }
}

def syncWithExternalService() {
    def config = loadConfiguration()
    
    echo "Syncing with external service..."
    
    try {
        def response = callExternalApi(
            endpoint: "${config.external_service_url}",
            method: 'POST',
            data: config.sync_payload ?: '{}'
        )
        
        if (response.status == 200) {
            echo "✓ External service sync successful"
            return true
        } else {
            error "✗ External service sync failed with status ${response.status}"
            return false
        }
    } catch (Exception e) {
        error "✗ Sync exception: ${e.message}"
        throw e
    }
}

// ============================================================================
// Notification Utilities
// ============================================================================

def sendNotification(type, message) {
    def channels = [
        slack: env('SLACK_WEBHOOK_URL'),
        email: env('NOTIFICATION_EMAIL')
    ]
    
    echo "Sending notification: ${type}"
    
    for (channel in channels.keySet()) {
        try {
            if (channels[channel]) {
                def payload = [
                    type: type,
                    message: message,
                    timestamp: new Date().format('yyyy-MM-dd HH:mm:ss')
                ]
                
                sh """
                    curl -X POST "${channels[${channel}]}" \
                        -H 'Content-Type: application/json' \
                        -d '${payload}'
                """
            }
        } catch (Exception e) {
            echo "Failed to send ${channel} notification: ${e.message}"
        }
    }
    
    echo "✓ Notification sent"
}

def notifyBuildFailure(buildNumber, failureMessage) {
    sendNotification('FAILURE', """
    Build #${buildNumber} failed!
    
    Failure message: ${failureMessage}
    
    Please investigate and take corrective action.
    """)
}

// ============================================================================
// Utility Functions
// ============================================================================

def sleep(seconds = 1) {
    echo "Sleeping for ${seconds}s..."
    Thread.sleep(seconds * 1000)
}

def waitForCondition(condition, timeoutMs = 60000) {
    def startTime = new Date()
    
    while (new Date().getTime() - startTime < timeoutMs) {
        if (condition()) {
            echo "✓ Condition met"
            return true
        }
        sleep(1)
    }
    
    error "✗ Timeout waiting for condition"
    return false
}

def getTimestamp() {
    def timestamp = new Date().format('yyyy-MM-dd HH:mm:ss.SSS')
    echo "Current timestamp: ${timestamp}"
    return timestamp
}

// ============================================================================
// Example Usage in Pipeline Context
// ============================================================================

/*
// This code block demonstrates how to use shared library functions
// within a pipeline stage

stage('Example Stage') {
    steps {
        script {
            // Validate environment before proceeding
            validateEnvironment()
            
            // Perform health checks
            performHealthCheck()
            
            // Execute deployment with retry logic
            def deployed = buildWithRetry(5, 3000) {
                deployToEnvironment('production', 'deployScript')
            }
            
            if (!deployed) {
                error "Deployment failed after retries"
            }
            
            // Send notification on success
            sendNotification('SUCCESS', "Build #${env.BUILD_NUMBER} deployed successfully")
        }
    }
}
*/

// ============================================================================
// Library Metadata
// ============================================================================

@LibraryMetadata(
    name: 'shared-lib',
    version: '1.0',
    description: 'Reusable Jenkins pipeline utilities and helper functions'
)
