#!/usr/bin/env groovy

// ============================================================================
// BuildPipeline.groovy - Main Jenkins Pipeline Script
// ============================================================================

def environment = 'production'  // Change based on deployment target

pipeline {
    agent any
    
    options {
        timeout(time: 1, unit: 'HOURS')
        disableCrumbBuilder(true)
        buildDiscarder(logRotator(numToKeepStr: '30'))
        timestamps('true')
    }
    
    environment {
        // Use environment variables for secrets - never hardcode credentials
        APP_ENV = environment
        DB_HOST = env('DB_HOST', 'localhost')
        DB_PORT = env('DB_PORT', '5432')
        DB_NAME = env('DB_NAME', 'app_db')
        API_KEY = env('API_KEY')  // Should be set via Jenkins Credentials Binding
        
        // Feature flags for environment-specific behavior
        FEATURE_NEW_UI = true
        FEATURE_OLD_API = false
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                cleanWs()
            }
        }
        
        stage('Build') {
            steps {
                script {
                    // Build configuration - can be overridden via parameters
                    def buildNumber = env('BUILD_NUMBER', '1.0.0')
                    def version = env('VERSION', '1.0.0-SNAPSHOT')
                    
                    echo "Starting build ${buildNumber} (version: ${version})"
                    
                    // Example: Run compilation and packaging
                    sh '''
                        cd /var/jenkins/workspace/${env.BUILD_NAME}
                        ./gradlew clean build -x test --no-daemon
                    '''
                }
            }
        }
        
        stage('Test') {
            steps {
                script {
                    def testResults = []
                    
                    // Unit tests
                    sh '''
                        cd /var/jenkins/workspace/${env.BUILD_NAME}
                        ./gradlew test --no-daemon --stacktrace
                    '''
                    
                    // Integration tests (run against local DB)
                    sh '''
                        cd /var/jenkins/workspace/${env.BUILD_NAME}
                        ./gradlew integrationTest --no-daemon
                    '''
                }
            }
        }
        
        stage('Quality Gates') {
            steps {
                script {
                    // Quality metrics from build output
                    def codeCoverage = env('CODE_COVERAGE', '80%')
                    def testFailures = env('TEST_FAILURES', 0)
                    
                    echo "Code Coverage: ${codeCoverage}"
                    echo "Test Failures: ${testFailures}"
                    
                    // Fail pipeline if quality gates not met
                    if (testFailures > 0 || codeCoverage < '80%') {
                        error "Quality gate failed - coverage below threshold or test failures detected"
                    }
                }
            }
        }
        
        stage('Security Scan') {
            steps {
                script {
                    // SAST/SCA scanning using environment variables for credentials
                    def scanner = env('SCANNER_TYPE', 'trivy')
                    
                    sh '''
                        cd /var/jenkins/workspace/${env.BUILD_NAME}
                        ${scanner} --config=${env('SCANNER_CONFIG')} --output=${env('SCAN_OUTPUT')} .
                    '''
                }
            }
        }
        
        stage('Deploy to Staging') {
            steps {
                script {
                    def stagingUrl = env('STAGING_URL', 'https://staging.example.com')
                    
                    sh '''
                        cd /var/jenkins/workspace/${env.BUILD_NAME}
                        ./gradlew deployStaging --no-daemon
                    '''
                }
            }
        }
        
        stage('Deploy to Production') {
            when {
                expression { env('DEPLOYMENT_ENV') == 'production' }
            }
            steps {
                script {
                    def prodUrl = env('PRODUCTION_URL', 'https://example.com')
                    
                    sh '''
                        cd /var/jenkins/workspace/${env.BUILD_NAME}
                        ./gradlew deployProduction --no-daemon
                    '''
                }
                
                post {
                    success {
                        echo "Successfully deployed to ${prodUrl}"
                        publishHTML(target: [
                            allowMissing: true,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: 'build/reports',
                            reportFiles: 'index.html',
                            locations: ''
                        ])
                    }
                    
                    failure {
                        echo "Deployment failed - manual intervention required"
                        mail to: env('NOTIFICATION_EMAIL'), 
                           subject: "Pipeline Failed: ${env.BUILD_NAME}",
                           body: """
                           Pipeline build failed for job: ${env.BUILD_NAME}
                           
                           Build URL: ${env.JOB_URL}
                           Timestamp: ${env.BUILD_TIMESTAMP}
                           
                           Please investigate the failure logs.
                           """
                    }
                }
            }
        }
    }
    
    post {
        always {
            // Cleanup resources
            cleanWs()
            
            // Archive build artifacts
            archiveArtifacts artifacts: '**/build/libs/*.jar', fingerprint: true
            
            // Release notes generation
            script {
                def releaseNotes = generateReleaseNotes()
                echo "Generated release notes:"
                echo releaseNotes
            }
        }
        
        success {
            // Slack notification (requires webhook URL in environment)
            sh '''
                curl -X POST "${env('SLACK_WEBHOOK_URL')}" \
                    -H 'Content-type: application/json' \
                    -d "{\"text\":\"✅ Build ${env.BUILD_NUMBER} succeeded\"}"
            '''
        }
        
        failure {
            // Slack notification on failure
            sh '''
                curl -X POST "${env('SLACK_WEBHOOK_URL')}" \
                    -H 'Content-type: application/json' \
                    -d "{\"text\":\"❌ Build ${env.BUILD_NUMBER} failed\"}"
            '''
        }
    }
    
    post {
        always {
            // Final cleanup and reporting
            script {
                def summary = generateBuildSummary()
                echo "=== BUILD SUMMARY ==="
                echo summary
            }
        }
    }
}

// ============================================================================
// Helper Functions
// ============================================================================

def generateReleaseNotes() {
    def notes = """
# Release Notes for Build ${env.BUILD_NUMBER}

## Summary
- Version: ${env.VERSION}
- Commit: ${env.GIT_COMMIT}
- Author: ${env.GIT_AUTHOR_NAME}

## Changes
${generateChangelog()}

## Testing
- Unit tests: PASSED (${env.TEST_COUNT})
- Integration tests: PASSED (${env.INTEGRATION_TEST_COUNT})
"""
    return notes
}

def generateBuildSummary() {
    def summary = """
| Metric | Value |
|--------|-------|
| Build Number | ${env.BUILD_NUMBER} |
| Version | ${env.VERSION} |
| Duration | ${env.DURATION} |
| Status | ${env.BUILD_STATUS} |
"""
    return summary
}

def generateChangelog() {
    def commits = []
    
    // Fetch recent commits for changelog
    try {
        sh '''
            cd /var/jenkins/workspace/${env.BUILD_NAME}
            git log --oneline -20
        '''
    } catch (Exception e) {
        echo "Failed to fetch changelog: ${e.message}"
        return "# No commits available"
    }
    
    return "# Recent changes\n${commits.join('\n')}"
}

// ============================================================================
// Shared Library Usage Example
// ============================================================================

@Library('shared-lib@1.0') _ import

// This pipeline can use shared functions from the library above
// For example: buildWithRetry(), validateEnvironment(), etc.
