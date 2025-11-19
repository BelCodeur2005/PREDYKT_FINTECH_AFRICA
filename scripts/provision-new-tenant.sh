#!/bin/bash

################################################################################
# Script de Provisioning Automatisé d'un Nouveau Tenant (Mono-Tenant Dédié)
# Usage: ./provision-new-tenant.sh <tenant_id> <tenant_domain> <company_name>
# Exemple: ./provision-new-tenant.sh companyA erp.companyA.com "Company A Ltd"
################################################################################

set -euo pipefail

# Couleurs pour output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Fonction d'affichage
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Vérification des arguments
if [ "$#" -ne 3 ]; then
    log_error "Usage: $0 <tenant_id> <tenant_domain> <company_name>"
    log_error "Exemple: $0 companyA erp.companyA.com 'Company A Ltd'"
    exit 1
fi

TENANT_ID="$1"
TENANT_DOMAIN="$2"
COMPANY_NAME="$3"

# Configuration
BASE_DIR="/opt/predykt"
TENANT_DIR="${BASE_DIR}/tenants/${TENANT_ID}"
TEMPLATE_DIR="${BASE_DIR}/templates"
BACKUPS_DIR="${BASE_DIR}/backups/${TENANT_ID}"

# Génération de ports uniques basés sur hash du tenant_id
TENANT_HASH=$(echo -n "$TENANT_ID" | md5sum | cut -c1-4)
APP_PORT=$((8000 + 0x${TENANT_HASH:0:2} % 1000))
DB_PORT=$((5400 + 0x${TENANT_HASH:2:2} % 100))
REDIS_PORT=$((6400 + 0x${TENANT_HASH:0:2} % 100))
TENANT_SUBNET_OCTET=$((20 + 0x${TENANT_HASH:2:2} % 235))

log_info "╔════════════════════════════════════════════════════════════╗"
log_info "║  PREDYKT - Provisioning Nouveau Tenant (Mono-Tenant)      ║"
log_info "╚════════════════════════════════════════════════════════════╝"
log_info ""
log_info "Tenant ID       : ${TENANT_ID}"
log_info "Domaine         : ${TENANT_DOMAIN}"
log_info "Société         : ${COMPANY_NAME}"
log_info "App Port        : ${APP_PORT}"
log_info "DB Port         : ${DB_PORT}"
log_info "Redis Port      : ${REDIS_PORT}"
log_info "Subnet          : 172.${TENANT_SUBNET_OCTET}.0.0/16"
log_info ""

# Confirmation
read -p "Continuer le provisioning ? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    log_warn "Provisioning annulé"
    exit 0
fi

################################################################################
# ÉTAPE 1: Génération des secrets sécurisés
################################################################################

log_info "[1/8] Génération des secrets sécurisés..."

DB_PASSWORD=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-32)
REDIS_PASSWORD=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-32)
JWT_SECRET=$(openssl rand -base64 64 | tr -d "=+/" | cut -c1-64)

log_info "✓ Secrets générés"

################################################################################
# ÉTAPE 2: Création de la structure de répertoires
################################################################################

log_info "[2/8] Création de la structure de répertoires..."

mkdir -p "${TENANT_DIR}"/{config,logs,data/uploads}
mkdir -p "${BACKUPS_DIR}"/{postgres,redis}
mkdir -p "${TENANT_DIR}"/docker

log_info "✓ Répertoires créés"

################################################################################
# ÉTAPE 3: Génération du fichier .env tenant
################################################################################

log_info "[3/8] Génération du fichier de configuration..."

cat > "${TENANT_DIR}/.env" <<EOF
# Configuration Tenant: ${TENANT_ID}
# Généré automatiquement le $(date)
# NE PAS COMMITER DANS GIT - Contient des secrets sensibles

# Identifiant Tenant
TENANT_ID=${TENANT_ID}
TENANT_DOMAIN=${TENANT_DOMAIN}
COMPANY_NAME=${COMPANY_NAME}

# Ports
APP_PORT=${APP_PORT}
DB_PORT=${DB_PORT}
REDIS_PORT=${REDIS_PORT}

# Network
TENANT_SUBNET_OCTET=${TENANT_SUBNET_OCTET}

# Secrets (NE JAMAIS PARTAGER)
DB_PASSWORD=${DB_PASSWORD}
REDIS_PASSWORD=${REDIS_PASSWORD}
JWT_SECRET=${JWT_SECRET}

# Version de l'application
VERSION=latest

# Environnement
SPRING_PROFILES_ACTIVE=prod-tenant

# Monitoring
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,metrics,prometheus
EOF

chmod 600 "${TENANT_DIR}/.env"

log_info "✓ Configuration générée: ${TENANT_DIR}/.env"

################################################################################
# ÉTAPE 4: Génération du docker-compose.yml tenant
################################################################################

log_info "[4/8] Génération du docker-compose tenant..."

envsubst < "${TEMPLATE_DIR}/docker-compose-tenant-template.yml" > "${TENANT_DIR}/docker/docker-compose.yml"

log_info "✓ Docker Compose généré"

################################################################################
# ÉTAPE 5: Configuration DNS (via API CloudFlare ou Route53)
################################################################################

log_info "[5/8] Configuration DNS..."

# Exemple avec CloudFlare API (à adapter selon votre provider)
if [ -n "${CLOUDFLARE_API_KEY:-}" ]; then
    ZONE_ID=$(curl -s -X GET "https://api.cloudflare.com/client/v4/zones?name=predykt.com" \
        -H "Authorization: Bearer ${CLOUDFLARE_API_KEY}" \
        -H "Content-Type: application/json" | jq -r '.result[0].id')
    
    if [ "$ZONE_ID" != "null" ]; then
        # Récupération de l'IP publique du serveur
        SERVER_IP=$(curl -s ifconfig.me)
        
        # Création de l'enregistrement DNS A
        DNS_RESULT=$(curl -s -X POST "https://api.cloudflare.com/client/v4/zones/${ZONE_ID}/dns_records" \
            -H "Authorization: Bearer ${CLOUDFLARE_API_KEY}" \
            -H "Content-Type: application/json" \
            --data "{\"type\":\"A\",\"name\":\"${TENANT_DOMAIN}\",\"content\":\"${SERVER_IP}\",\"ttl\":120,\"proxied\":true}")
        
        if echo "$DNS_RESULT" | jq -e '.success' > /dev/null; then
            log_info "✓ DNS configuré: ${TENANT_DOMAIN} → ${SERVER_IP}"
        else
            log_warn "⚠ Erreur DNS - Configuration manuelle requise"
        fi
    else
        log_warn "⚠ Zone DNS non trouvée - Configuration manuelle requise"
    fi
else
    log_warn "⚠ CLOUDFLARE_API_KEY non défini - Configuration DNS manuelle requise"
fi

################################################################################
# ÉTAPE 6: Démarrage des services Docker
################################################################################

log_info "[6/8] Démarrage des services Docker..."

cd "${TENANT_DIR}/docker"

# Export des variables d'environnement
set -a
source "${TENANT_DIR}/.env"
set +a

# Pull des images
docker-compose pull

# Démarrage des services
docker-compose up -d

log_info "✓ Services Docker démarrés"

################################################################################
# ÉTAPE 7: Attente de la disponibilité de la base de données
################################################################################

log_info "[7/8] Attente de l'initialisation de la base de données..."

MAX_RETRIES=30
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if docker-compose exec -T "postgres-${TENANT_ID}" pg_isready -U "predykt_${TENANT_ID}" > /dev/null 2>&1; then
        log_info "✓ Base de données prête"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo -n "."
    sleep 2
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    log_error "✗ Timeout: Base de données non disponible après ${MAX_RETRIES} tentatives"
    exit 1
fi

################################################################################
# ÉTAPE 8: Initialisation de l'entreprise dans la base de données
################################################################################

log_info "[8/8] Initialisation de l'entreprise..."

# Attente que l'application soit prête
sleep 10

# Création de l'entreprise via API REST
COMPANY_RESPONSE=$(curl -s -X POST "http://localhost:${APP_PORT}/api/v1/companies" \
    -H "Content-Type: application/json" \
    -d "{
        \"name\": \"${COMPANY_NAME}\",
        \"email\": \"admin@${TENANT_DOMAIN}\",
        \"country\": \"CM\",
        \"currency\": \"XAF\",
        \"accountingStandard\": \"OHADA\"
    }")

if echo "$COMPANY_RESPONSE" | jq -e '.success' > /dev/null 2>&1; then
    COMPANY_ID=$(echo "$COMPANY_RESPONSE" | jq -r '.data.id')
    log_info "✓ Entreprise créée avec ID: ${COMPANY_ID}"
    
    # Sauvegarde de l'ID dans le fichier de config
    echo "COMPANY_ID=${COMPANY_ID}" >> "${TENANT_DIR}/.env"
else
    log_warn "⚠ Création entreprise échouée - À faire manuellement"
fi

################################################################################
# RÉSUMÉ FINAL
################################################################################

log_info ""
log_info "╔════════════════════════════════════════════════════════════╗"
log_info "║              PROVISIONING TERMINÉ AVEC SUCCÈS              ║"
log_info "╚════════════════════════════════════════════════════════════╝"
log_info ""
log_info "🌐 URL d'accès       : https://${TENANT_DOMAIN}"
log_info "🔐 Répertoire config : ${TENANT_DIR}"
log_info "📊 Monitoring        : http://localhost:${APP_PORT}/actuator/health"
log_info "📝 Logs              : ${TENANT_DIR}/logs"
log_info "💾 Backups           : ${BACKUPS_DIR}"
log_info ""
log_info "Prochaines étapes:"
log_info "1. Vérifier l'accès: curl https://${TENANT_DOMAIN}/api/v1/health"
log_info "2. Créer le premier utilisateur administrateur"
log_info "3. Configurer les backups automatiques"
log_info "4. Activer le monitoring (Prometheus/Grafana)"
log_info ""

# Sauvegarde des informations de provisioning dans un registre centralisé
echo "$(date)|${TENANT_ID}|${TENANT_DOMAIN}|${COMPANY_NAME}|${APP_PORT}|${DB_PORT}|${REDIS_PORT}" >> "${BASE_DIR}/tenants-registry.log"

log_info "✅ Tenant ${TENANT_ID} prêt à l'emploi!"