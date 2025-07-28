def generate(Map config, String role = 'default') {
    echo "🔧 Génération des variables Ansible pour le rôle: ${role}"
    
    // Variables de base - PAS DE TARGET_SERVER (géré par inventory/group_vars)
    def baseVars = [
        "ecosystem='${config.ecosystem}'",
        "environment='${config.environment}'",
        "deployment_user='ansible'"
    ]
    
    // Variables spécifiques par rôle
    def roleVars = []
    switch(role.toLowerCase()) {
        case 'webserver':
            roleVars << "web_server_port='${config.webPort ?: '80'}'"
            roleVars << "ssl_enabled=${config.sslEnabled ?: false}"
            roleVars << "document_root='${config.documentRoot ?: '/var/www/html'}'"
            break
            
        case 'database':
            roleVars << "db_type='${config.dbType ?: 'mysql'}'"
            roleVars << "db_port='${config.dbPort ?: '3306'}'"
            roleVars << "db_name='${config.dbName ?: config.ecosystem.toLowerCase()}'"
            break
            
        case 'homeassistant':
            roleVars << "ha_config_path='${config.haConfigPath ?: '/config'}'"
            roleVars << "ha_port='${config.haPort ?: '8123'}'"
            roleVars << "ha_backup_enabled=${config.haBackupEnabled ?: true}"
            break
            
        case 'monitoring':
            roleVars << "metrics_port='${config.metricsPort ?: '9090'}'"
            roleVars << "grafana_enabled=${config.grafanaEnabled ?: false}"
            roleVars << "alerting_enabled=${config.alertingEnabled ?: true}"
            break
            
        case 'application':
        default:
            roleVars << "app_name='${config.ecosystem.toLowerCase()}'"
            roleVars << "app_port='${config.appPort ?: '8080'}'"
            roleVars << "service_name='${config.ecosystem.toLowerCase()}'"
    }
    
    // Variables optionnelles communes
    def optionalVars = []
    if (config.containsKey('backupEnabled')) {
        optionalVars << "backup_enabled=${config.backupEnabled}"
    }
    if (config.containsKey('dryRun')) {
        optionalVars << "dry_run=${config.dryRun}"
    }
    
    def allVars = (baseVars + roleVars + optionalVars).join(' ')
    
    echo "Variables générées: ${allVars}"
    return allVars
}

return this
