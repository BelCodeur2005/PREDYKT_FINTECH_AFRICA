#!/bin/bash

################################################################################
# Script de Backup Manuel d'un Tenant
# Usage: ./backup-tenant.sh <mode> <tenant_id>
# Exemples:
#   ./backup-tenant.sh shared
#   ./backup-tenant.sh dedicated companyA
#   ./backup-tenant.sh cabinet cabinet-douala
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
if [ "$#" -lt 1 ]; then
    log_error "Usage: $0 <mode> [tenant_id]"
    log_error ""
    log_error "Exemples:"
    log_error "  $0 shared"
    log_error "  $0 dedicated companyA"
    log_error "  $0 cabinet cabinet-douala"
    exit 1
fi

MODE="$1"
TENANT_ID="${2:-}"

# Validation du mode
if [[ ! "$MODE" =~ ^(shared|dedicated|cabinet)$ ]]; then
    log_error "Mode invalide: $MODE"
    log_error "Modes valides: shared, dedicated, cabinet"
    exit 1
fi

# Pour dedicated et cabinet, le tenant_id est obligatoire
if [[ "$MODE" != "shared" && -z "$TENANT_ID" ]]; then
    log_error "Le tenant_id est requis pour le mode $MODE"
    exit 1
fi

# Configuration
BACKUP_DIR="./backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Déterminer les noms de conteneurs selon le mode
case "$MODE" in
    shared)
        POSTGRES_CONTAINER="predykt-postgres-shared"
        REDIS_CONTAINER="predykt-redis-shared"
        DB_NAME="predykt_db"
        DB_USER="predykt"
        BACKUP_SUBDIR="shared"
        ;;
    dedicated)
        POSTGRES_CONTAINER="postgres-${TENANT_ID}"
        REDIS_CONTAINER="redis-${TENANT_ID}"
        DB_NAME="predykt_${TENANT_ID}"
        DB_USER="predykt_${TENANT_ID}"
        BACKUP_SUBDIR="dedicated/${TENANT_ID}"
        ;;
    cabinet)
        POSTGRES_CONTAINER="predykt-postgres-cabinet-${TENANT_ID}"
        REDIS_CONTAINER="predykt-redis-cabinet-${TENANT_ID}"
        DB_NAME="predykt_cabinet_${TENANT_ID}"
        DB_USER="predykt_cabinet_${TENANT_ID}"
        BACKUP_SUBDIR="cabinet/${TENANT_ID}"
        ;;
esac

# Créer les répertoires de backup
POSTGRES_BACKUP_DIR="${BACKUP_DIR}/${BACKUP_SUBDIR}/postgres"
REDIS_BACKUP_DIR="${BACKUP_DIR}/${BACKUP_SUBDIR}/redis"
mkdir -p "${POSTGRES_BACKUP_DIR}"
mkdir -p "${REDIS_BACKUP_DIR}"

log_info "TPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPW"
log_info "Q           BACKUP PREDYKT - Mode: ${MODE^^}                      "
log_info "ZPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP]"
log_info ""
log_info "Mode             : ${MODE}"
[[ -n "$TENANT_ID" ]] && log_info "Tenant ID        : ${TENANT_ID}"
log_info "Timestamp        : ${TIMESTAMP}"
log_info "Backup Directory : ${BACKUP_DIR}/${BACKUP_SUBDIR}"
log_info ""

################################################################################
# ÉTAPE 1: Vérifier que les conteneurs existent
################################################################################

log_step "[1/4] Vérification des conteneurs..."

if ! docker ps -a --format '{{.Names}}' | grep -q "^${POSTGRES_CONTAINER}$"; then
    log_error "Conteneur PostgreSQL introuvable: ${POSTGRES_CONTAINER}"
    exit 1
fi

if ! docker ps -a --format '{{.Names}}' | grep -q "^${REDIS_CONTAINER}$"; then
    log_warn "Conteneur Redis introuvable: ${REDIS_CONTAINER} (backup Redis ignoré)"
    REDIS_CONTAINER=""
fi

log_info " Conteneurs trouvés"

################################################################################
# ÉTAPE 2: Backup PostgreSQL
################################################################################

log_step "[2/4] Backup PostgreSQL..."

POSTGRES_BACKUP_FILE="${POSTGRES_BACKUP_DIR}/backup_${TIMESTAMP}.sql.gz"

# Dump de la base de données avec compression
docker exec -t "${POSTGRES_CONTAINER}" pg_dump \
    -U "${DB_USER}" \
    -d "${DB_NAME}" \
    --format=custom \
    --verbose \
    --no-owner \
    --no-acl \
    | gzip > "${POSTGRES_BACKUP_FILE}"

if [ -f "${POSTGRES_BACKUP_FILE}" ]; then
    BACKUP_SIZE=$(du -h "${POSTGRES_BACKUP_FILE}" | cut -f1)
    log_info " Backup PostgreSQL créé: ${POSTGRES_BACKUP_FILE} (${BACKUP_SIZE})"
else
    log_error " Échec de la création du backup PostgreSQL"
    exit 1
fi

# Créer un lien symbolique vers le dernier backup
ln -sf "$(basename ${POSTGRES_BACKUP_FILE})" "${POSTGRES_BACKUP_DIR}/latest.sql.gz"

################################################################################
# ÉTAPE 3: Backup Redis
################################################################################

log_step "[3/4] Backup Redis..."

if [ -n "$REDIS_CONTAINER" ]; then
    REDIS_BACKUP_FILE="${REDIS_BACKUP_DIR}/dump_${TIMESTAMP}.rdb"

    # Forcer une sauvegarde Redis
    docker exec -t "${REDIS_CONTAINER}" redis-cli SAVE 2>/dev/null || \
        docker exec -t "${REDIS_CONTAINER}" redis-cli --no-auth-warning SAVE

    # Copier le fichier dump.rdb
    docker cp "${REDIS_CONTAINER}:/data/dump.rdb" "${REDIS_BACKUP_FILE}" 2>/dev/null

    if [ -f "${REDIS_BACKUP_FILE}" ]; then
        REDIS_SIZE=$(du -h "${REDIS_BACKUP_FILE}" | cut -f1)
        log_info " Backup Redis créé: ${REDIS_BACKUP_FILE} (${REDIS_SIZE})"

        # Lien symbolique vers le dernier backup
        ln -sf "$(basename ${REDIS_BACKUP_FILE})" "${REDIS_BACKUP_DIR}/latest.rdb"
    else
        log_warn "  Backup Redis échoué (non critique)"
    fi
else
    log_warn "  Backup Redis ignoré (conteneur introuvable)"
fi

################################################################################
# ÉTAPE 4: Nettoyage des anciens backups (garder 30 derniers jours)
################################################################################

log_step "[4/4] Nettoyage des anciens backups..."

# Supprimer les backups PostgreSQL de plus de 30 jours
find "${POSTGRES_BACKUP_DIR}" -name "backup_*.sql.gz" -type f -mtime +30 -delete
POSTGRES_COUNT=$(find "${POSTGRES_BACKUP_DIR}" -name "backup_*.sql.gz" -type f | wc -l)

# Supprimer les backups Redis de plus de 30 jours
if [ -n "$REDIS_CONTAINER" ]; then
    find "${REDIS_BACKUP_DIR}" -name "dump_*.rdb" -type f -mtime +30 -delete 2>/dev/null || true
    REDIS_COUNT=$(find "${REDIS_BACKUP_DIR}" -name "dump_*.rdb" -type f 2>/dev/null | wc -l || echo 0)
fi

log_info " Nettoyage effectué"
log_info "  - Backups PostgreSQL conservés: ${POSTGRES_COUNT}"
[[ -n "${REDIS_COUNT:-}" ]] && log_info "  - Backups Redis conservés: ${REDIS_COUNT}"

################################################################################
# RÉSUMÉ
################################################################################

log_info ""
log_info "TPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPW"
log_info "Q                 BACKUP TERMINÉ AVEC SUCCÈS                 Q"
log_info "ZPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP]"
log_info ""
log_info "=Á Répertoire: ${BACKUP_DIR}/${BACKUP_SUBDIR}"
log_info "=¾ PostgreSQL: ${POSTGRES_BACKUP_FILE}"
[[ -n "$REDIS_CONTAINER" && -f "${REDIS_BACKUP_FILE:-}" ]] && log_info "=¾ Redis    : ${REDIS_BACKUP_FILE}"
log_info ""
log_info "Pour restaurer ce backup:"
log_info "  ./scripts/restore-tenant.sh $MODE ${TENANT_ID:-} ${TIMESTAMP}"
log_info ""
