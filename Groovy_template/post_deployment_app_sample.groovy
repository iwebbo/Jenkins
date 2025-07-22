post {
    always {
        echo "Gestion des packages Chocolatey terminée"
        
        // Créer un rapport détaillé
        script {
            def report = """
            === RAPPORT CHOCOLATEY PACKAGE MANAGER ===
            Package: ${params.PACKAGE_NAME}
            Version: ${params.PACKAGE_VERSION ?: 'Latest'}
            État Final: ${env.FINAL_PACKAGE_STATE ?: 'Non défini'}
            Serveur Cible: ${params.TARGET_SERVER}
            Force Installation: ${params.FORCE_INSTALL}
            Source Chocolatey: ${params.CHOCOLATEY_SOURCE}
            Timeout: ${params.CHOCOLATEY_TIMEOUT} secondes
            Paramètres Package: ${params.PACKAGE_PARAMS ?: 'Aucun'}
            Variables Extra: ${params.EXTRA_VARS ?: 'Aucune'}
            Variables Ansible: ${env.ANSIBLE_EXTRA_VARS ?: 'Non générées'}
            Playbook: ${env.SELECTED_PLAYBOOK ?: 'Non défini'}
            Build: #${env.BUILD_NUMBER}
            Date: ${new Date().format('yyyy-MM-dd HH:mm:ss')}
            Statut: ${currentBuild.currentResult}
            ==========================================
            """
            writeFile file: 'chocolatey_package_report.txt', text: report
            archiveArtifacts artifacts: 'chocolatey_package_report.txt', allowEmptyArchive: true
        }
    }
    success {
        script {
            echo "Package '${params.PACKAGE_NAME}' ${params.PACKAGE_STATE} avec succès!"
            
            currentBuild.description = "${params.PACKAGE_STATE} ${params.PACKAGE_NAME} → ${params.TARGET_SERVER}"
            
            // Créer le corps de l'email en texte avec formatage
            def emailBody = """
                🍫 CHOCOLATEY PACKAGE - SUCCÈS

                Package: ${params.PACKAGE_NAME}
                Version: ${params.PACKAGE_VERSION ?: 'Latest'}
                Action: ${params.PACKAGE_STATE}
                Serveur: ${params.TARGET_SERVER}
                État Final: ${env.FINAL_PACKAGE_STATE ?: 'Non défini'}
                Build: #${env.BUILD_NUMBER}
                Date: ${new Date().format('yyyy-MM-dd HH:mm:ss')}

                📊 Détails du build: ${env.BUILD_URL}

                Configuration utilisée:
                - Force Installation: ${params.FORCE_INSTALL}
                - Source Chocolatey: ${params.CHOCOLATEY_SOURCE}
                - Timeout: ${params.CHOCOLATEY_TIMEOUT} secondes
                - Paramètres Package: ${params.PACKAGE_PARAMS ?: 'Aucun'}
                - Variables Extra: ${params.EXTRA_VARS ?: 'Aucune'}

                ✅ L'opération s'est terminée avec succès.
            """
            
            mail to: 'l.kieran95@gmail.com',
                subject: "[Jenkins] Chocolatey ${params.PACKAGE_STATE} - ${params.PACKAGE_NAME} - Succès",
                body: emailBody
        }
    }
    failure {
        script {
            echo "Échec de la gestion du package '${params.PACKAGE_NAME}'"
            
            currentBuild.description = "${params.PACKAGE_STATE} ${params.PACKAGE_NAME} → ÉCHEC"
            
            // Créer le corps de l'email d'échec
            def emailBody = """
                ❌ CHOCOLATEY PACKAGE - ÉCHEC

                Package: ${params.PACKAGE_NAME}
                Version: ${params.PACKAGE_VERSION ?: 'Latest'}
                Action: ${params.PACKAGE_STATE}
                Serveur: ${params.TARGET_SERVER}
                Build: #${env.BUILD_NUMBER}
                Date: ${new Date().format('yyyy-MM-dd HH:mm:ss')}

                🔍 Logs d'erreur: ${env.BUILD_URL}console

                Configuration utilisée:
                - Force Installation: ${params.FORCE_INSTALL}
                - Source Chocolatey: ${params.CHOCOLATEY_SOURCE}
                - Timeout: ${params.CHOCOLATEY_TIMEOUT} secondes
                - Paramètres Package: ${params.PACKAGE_PARAMS ?: 'Aucun'}
                - Variables Extra: ${params.EXTRA_VARS ?: 'Aucune'}

                Variables Ansible générées:
                ${env.ANSIBLE_EXTRA_VARS ?: 'Non générées'}

                ⚠️ Veuillez vérifier les logs pour plus de détails.
            """
            
            mail to: 'l.kieran95@gmail.com',
                subject: "[Jenkins] Chocolatey - Échec - ${params.PACKAGE_NAME}",
                body: emailBody
        }
    }
    cleanup {
        cleanWs()
    }
}