#!/bin/bash

################################################################################
# Script de Restore d'un Tenant
# Usage: ./restore-tenant.sh <mode> <tenant_id> <timestamp>
# Exemples:
#   ./restore-tenant.sh shared "" 20241126_143000
#   ./restore-tenant.sh dedicated companyA 20241126_143000
#   ./restore-tenant.sh cabinet cabinet-douala 20241126_143000
################################################################################

set -euo pipefail

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step() { echo -e "${BLUE}[STEP]${NC} $1"; }

# Vérification des arguments
if [ "$#" -lt 2 ]; then
    log_error "Usage: $0 <mode> <tenant_id> [timestamp]"
    log_error ""
    log_error "Exemples:"
    log_error "  $0 shared \"\" latest"
    log_error "  $0 shared \"\" 20241126_143000"
    log_error "  $0 dedicated companyA latest"
    log_error "  $0 dedicated companyA 20241126_143000"
    log_error "  $0 cabinet cabinet-douala latest"
    exit 1
fi

MODE="$1"
TENANT_ID="$2"
TIMESTAMP="${3:-latest}"

# Validation du mode
if [[ ! "$MODE" =~ ^(shared|dedicated|cabinet)$ ]]; then
    log_error "Mode invalide: $MODE"
    log_error "Modes valides: shared, dedicated, cabinet"
    exit 1
fi

# Configuration
BACKUP_DIR="./backups"

# Déterminer les noms selon le mode
case "$MODE" in
    shared)
        POSTGRES_CONTAINER="predykt-postgres-shared"
        REDIS_CONTAINER="predykt-redis-shared"
        DB_NAME="predykt_db"
        DB_USER="predykt"
        BACKUP_SUBDIR="shared"
        ;;
    dedicated)
        if [ -z "$TENANT_ID" ]; then
            log_error "Le tenant_id est requis pour le mode dedicated"
            exit 1
        fi
        POSTGRES_CONTAINER="postgres-${TENANT_ID}"
        REDIS_CONTAINER="redis-${TENANT_ID}"
        DB_NAME="predykt_${TENANT_ID}"
        DB_USER="predykt_${TENANT_ID}"
        BACKUP_SUBDIR="dedicated/${TENANT_ID}"
        ;;
    cabinet)
        if [ -z "$TENANT_ID" ]; then
            log_error "Le tenant_id est requis pour le mode cabinet"
            exit 1
        fi
        POSTGRES_CONTAINER="predykt-postgres-cabinet-${TENANT_ID}"
        REDIS_CONTAINER="predykt-redis-cabinet-${TENANT_ID}"
        DB_NAME="predykt_cabinet_${TENANT_ID}"
        DB_USER="predykt_cabinet_${TENANT_ID}"
        BACKUP_SUBDIR="cabinet/${TENANT_ID}"
        ;;
esac

POSTGRES_BACKUP_DIR="${BACKUP_DIR}/${BACKUP_SUBDIR}/postgres"
REDIS_BACKUP_DIR="${BACKUP_DIR}/${BACKUP_SUBDIR}/redis"

# Déterminer le fichier de backup
if [ "$TIMESTAMP" = "latest" ]; then
    POSTGRES_BACKUP_FILE="${POSTGRES_BACKUP_DIR}/latest.sql.gz"
    REDIS_BACKUP_FILE="${REDIS_BACKUP_DIR}/latest.rdb"
else
    POSTGRES_BACKUP_FILE="${POSTGRES_BACKUP_DIR}/backup_${TIMESTAMP}.sql.gz"
    REDIS_BACKUP_FILE="${REDIS_BACKUP_DIR}/dump_${TIMESTAMP}.rdb"
fi

# Vérifier que le fichier existe
if [ ! -f "$POSTGRES_BACKUP_FILE" ]; then
    log_error "Fichier de backup introuvable: ${POSTGRES_BACKUP_FILE}"
    log_error ""
    log_error "Backups disponibles:"
    ls -lh "${POSTGRES_BACKUP_DIR}/" 2>/dev/null || log_error "  Aucun backup trouvé"
    exit 1
fi

log_info "TPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPW"
log_info "Q          RESTORE PREDYKT - Mode: ${MODE^^}                      "
log_info "ZPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP]"
log_info ""
log_info "Mode             : ${MODE}"
[[ -n "$TENANT_ID" ]] && log_info "Tenant ID        : ${TENANT_ID}"
log_info "Timestamp        : ${TIMESTAMP}"
log_info "Fichier backup   : ${POSTGRES_BACKUP_FILE}"
log_info ""

# Confirmation
log_warn "   ATTENTION: Cette opération va ÉCRASER les données actuelles!"
log_warn ""
read -p "Voulez-vous continuer? (yes/no) " -r
echo
if [[ ! $REPLY =~ ^(yes|YES|y|Y)$ ]]; then
    log_warn "Restore annulé"
    exit 0
fi

################################################################################
# ÉTAPE 1: Vérifier que les conteneurs existent
################################################################################

log_step "[1/5] Vérification des conteneurs..."

if ! docker ps --format '{{.Names}}' | grep -q "^${POSTGRES_CONTAINER}$"; then
    log_error "Conteneur PostgreSQL non actif: ${POSTGRES_CONTAINER}"
    log_error "Démarrez d'abord les services avec docker-compose up -d"
    exit 1
fi

log_info " Conteneurs actifs"

################################################################################
# ÉTAPE 2: Arrêter l'application (pour éviter les écritures pendant le restore)
################################################################################

log_step "[2/5] Arrêt de l'application..."

case "$MODE" in
    shared)
        APP_CONTAINER="predykt-app-shared"
        ;;
    dedicated)
        APP_CONTAINER="predykt-app-${TENANT_ID}"
        ;;
    cabinet)
        APP_CONTAINER="predykt-app-cabinet-${TENANT_ID}"
        ;;
esac

if docker ps --format '{{.Names}}' | grep -q "^${APP_CONTAINER}$"; then
    docker stop "${APP_CONTAINER}"
    log_info " Application arrêtée"
else
    log_warn "  Application déjà arrêtée"
fi

################################################################################
# ÉTAPE 3: Restore PostgreSQL
################################################################################

log_step "[3/5] Restore PostgreSQL..."

# Supprimer la base de données existante et la recréer
docker exec -i "${POSTGRES_CONTAINER}" psql -U "${DB_USER}" -c "SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pg_stat_activity.datname = '${DB_NAME}' AND pid <> pg_backend_pid();" 2>/dev/null || true

docker exec -i "${POSTGRES_CONTAINER}" dropdb -U "${DB_USER}" --if-exists "${DB_NAME}"
docker exec -i "${POSTGRES_CONTAINER}" createdb -U "${DB_USER}" "${DB_NAME}"

# Restaurer le dump
gunzip -c "${POSTGRES_BACKUP_FILE}" | docker exec -i "${POSTGRES_CONTAINER}" pg_restore \
    -U "${DB_USER}" \
    -d "${DB_NAME}" \
    --verbose \
    --no-owner \
    --no-acl \
    2>&1 | grep -v "^pg_restore:" || true

log_info " Base de données restaurée"

################################################################################
# ÉTAPE 4: Restore Redis (optionnel)
################################################################################

log_step "[4/5] Restore Redis..."

if [ -f "$REDIS_BACKUP_FILE" ] && docker ps --format '{{.Names}}' | grep -q "^${REDIS_CONTAINER}$"; then
    # Arrêter Redis
    docker stop "${REDIS_CONTAINER}"

    # Copier le fichier dump.rdb
    docker cp "${REDIS_BACKUP_FILE}" "${REDIS_CONTAINER}:/data/dump.rdb"

    # Redémarrer Redis
    docker start "${REDIS_CONTAINER}"

    log_info " Redis restauré"
else
    log_warn "  Restore Redis ignoré (fichier ou conteneur introuvable)"
fi

################################################################################
# ÉTAPE 5: Redémarrer l'application
################################################################################

log_step "[5/5] Redémarrage de l'application..."

docker start "${APP_CONTAINER}"

# Attendre que l'application soit prête
log_info "Attente du démarrage de l'application..."
sleep 10

# Vérifier le health check
MAX_RETRIES=30
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if docker exec "${APP_CONTAINER}" curl -f -s http://localhost:8080/api/v1/health > /dev/null 2>&1; then
        log_info " Application démarrée et opérationnelle"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo -n "."
    sleep 2
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    log_warn "  L'application met du temps à démarrer. Vérifiez les logs:"
    log_warn "  docker logs ${APP_CONTAINER}"
fi

################################################################################
# RÉSUMÉ
################################################################################

log_info ""
log_info "TPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPW"
log_info "Q               RESTORE TERMINÉ AVEC SUCCÈS                  Q"
log_info "ZPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP]"
log_info ""
log_info " Base de données restaurée depuis: ${POSTGRES_BACKUP_FILE}"
log_info " Application redémarrée"
log_info ""
log_info "Vérifications recommandées:"
log_info "  1. Tester l'accès: curl http://localhost:${APP_PORT:-8080}/api/v1/health"
log_info "  2. Vérifier les logs: docker logs ${APP_CONTAINER}"
log_info "  3. Tester les fonctionnalités critiques"
log_info ""
