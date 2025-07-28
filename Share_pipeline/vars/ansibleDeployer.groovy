def deploy(Map config) {
    echo "🚀 Déploiement Ansible en cours..."
    echo "Écosystème: ${config.ecosystem}"
    echo "Environnement: ${config.environment}"
    echo "Playbook: ${config.playbook}"
    echo "Mode: ${config.mode}"
    
    // Chemins basés sur l'environnement (group_vars automatique)
    def inventoryPath = "inventories/${config.environment.toLowerCase()}/hosts"
    def playbookPath = "playbooks/${config.playbook}"
    
    echo "Inventaire: ${inventoryPath}"
    echo "Group vars: group_vars/${config.environment.toLowerCase()}"
    echo "Host vars: host_vars/ (si configuré)"
    
    // Commande Ansible - PAS DE --limit car géré par inventory
    def ansibleCommand = """
        ansible-playbook -i ${inventoryPath} \\
            ${playbookPath} \\
            --extra-vars "${config.ansibleVars}"
    """
    
    // Options selon le mode
    switch(config.mode) {
        case 'DRYRUN':
            ansibleCommand += " --check --diff"
            echo "🔍 Mode DRY-RUN activé"
            break
        case 'ROLLBACK':
            ansibleCommand += " --extra-vars \"rollback_mode=true\""
            echo "🔄 Mode ROLLBACK activé"
            break
        default:
            echo "📋 Mode STANDARD"
    }
    
    echo "Commande Ansible:"
    echo ansibleCommand
    
    // Exécution
    sh ansibleCommand
    
    echo "✅ Déploiement Ansible terminé"
}

return this