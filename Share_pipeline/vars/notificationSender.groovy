def send(Map config) {
    def status = config.result == 'SUCCESS' ? '✅ SUCCÈS' : '❌ ÉCHEC'
    def emoji = config.result == 'SUCCESS' ? '🚀' : '❌'
    
    def emailBody = """
        ${emoji} ${config.ecosystem.toUpperCase()} DEPLOYMENT - ${status}

        Environnement: ${config.environment}
        Build: #${config.buildNumber}
        Date: ${new Date().format('yyyy-MM-dd HH:mm:ss')}
        
        📊 Détails: ${config.buildUrl}
        
        ${config.result == 'SUCCESS' ? '✅ Déploiement réussi' : '⚠️ Vérifiez les logs'}
        
        🗂️ Inventaire utilisé: inventories/${config.environment.toLowerCase()}/hosts
        📋 Group vars: group_vars/${config.environment.toLowerCase()}/
    """
    
    mail to: 'l.kieran95@gmail.com',
         subject: "[Jenkins] ${config.ecosystem} - ${config.environment} - ${status}",
         body: emailBody
}

return this
