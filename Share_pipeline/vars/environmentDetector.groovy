def detect(String ecosystem) {
    def ecosystemLower = ecosystem.toLowerCase()
    def jobUrl = env.JOB_URL ?: ""
    def jenkinsUrl = env.JENKINS_URL ?: ""
    
    echo "=== DÉTECTION ENVIRONNEMENT ==="
    echo "Écosystème: ${ecosystem} (${ecosystemLower})"
    echo "Job URL: ${jobUrl}"
    echo "Jenkins URL: ${jenkinsUrl}"
    
    // TON ALGORITHME EXISTANT
    if (jobUrl.contains("${ecosystemLower}-dev-${ecosystemLower}")) {
        echo "✅ Pattern DEV détecté"
        return ["DEV"]
    }
    if (jobUrl.contains("${ecosystemLower}-aps-${ecosystemLower}")) {
        echo "✅ Pattern STG/PRD détecté"
        return ["STG", "PRD"]
    }
    
    echo "⚠️ Aucun pattern spécifique détecté, retour par défaut"
    return ["DEV", "STG", "PRD"]
}

return this