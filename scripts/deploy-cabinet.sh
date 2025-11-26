#!/bin/bash

################################################################################
# Script de Déploiement Mode CABINET (Cabinet Comptable)
# Usage: ./deploy-cabinet.sh <cabinet_id> [start|stop|restart|status|logs]
# Exemple: ./deploy-cabinet.sh cabinet-douala start
################################################################################

set -euo pipefail

# Couleurs
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step() { echo -e "${BLUE}[STEP]${NC} $1"; }

# Vérification des arguments
if [ "$#" -lt 1 ]; then
    log_error "Usage: $0 <cabinet_id> [action]"
    log_error ""
    log_error "Exemples:"
    log_error "  $0 cabinet-douala start"
    log_error "  $0 cabinet-douala stop"
    log_error "  $0 cabinet-douala logs"
    log_error "  $0 cabinet-douala backup"
    exit 1
fi

CABINET_ID="$1"
ACTION="${2:-start}"

ENV_FILE=".env.cabinet-${CABINET_ID}"
COMPOSE_FILE="docker-compose.cabinet.yml"

# Vérifier que le fichier .env existe
if [ ! -f "$ENV_FILE" ]; then
    log_error "Fichier $ENV_FILE introuvable!"
    log_error ""
    log_error "Actions requises:"
    log_error "  1. Copier l'exemple: cp .env.cabinet.example $ENV_FILE"
    log_error "  2. Éditer $ENV_FILE et configurer:"
    log_error "     - CABINET_ID=${CABINET_ID}"
    log_error "     - Remplacer tous les CHANGEME"
    log_error "     - Configurer des ports uniques"
    log_error "  3. Relancer ce script"
    exit 1
fi

log_info "╔════════════════════════════════════════════════════════════╗"
log_info "║     PREDYKT DEPLOYMENT - Mode CABINET (Comptable)         ║"
log_info "╚════════════════════════════════════════════════════════════╝"
log_info ""
log_info "Cabinet ID: ${CABINET_ID}"
log_info "Action: ${ACTION}"
log_info "Compose: ${COMPOSE_FILE}"
log_info "Env: ${ENV_FILE}"
log_info ""

case "$ACTION" in
    start)
        log_step "Démarrage des services pour le cabinet ${CABINET_ID}..."

        # Vérification de sécurité
        if grep -q "CHANGEME" "$ENV_FILE"; then
            log_error "❌ Le fichier $ENV_FILE contient encore des valeurs CHANGEME!"
            log_error "   Éditez le fichier et remplacez tous les secrets avant le déploiement."
            exit 1
        fi

        # Vérifier que CABINET_ID correspond
        ENV_CABINET_ID=$(grep "^CABINET_ID=" "$ENV_FILE" | cut -d= -f2)
        if [ "$ENV_CABINET_ID" != "$CABINET_ID" ]; then
            log_error "❌ Incohérence CABINET_ID:"
            log_error "   Argument: $CABINET_ID"
            log_error "   Fichier: $ENV_CABINET_ID"
            exit 1
        fi

        # Build et démarrage
        docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" pull
        docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d

        log_info ""
        log_info "✓ Services démarrés avec succès pour ${CABINET_ID}!"
        log_info ""
        log_info "Vérification de la santé des services..."
        sleep 10

        docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps

        APP_PORT=$(grep "^APP_PORT=" "$ENV_FILE" | cut -d= -f2)
        CABINET_DOMAIN=$(grep "^CABINET_DOMAIN=" "$ENV_FILE" | cut -d= -f2)

        log_info ""
        log_info "Accès aux services:"
        log_info "  - API:     http://localhost:${APP_PORT}"
        log_info "  - Domaine: https://${CABINET_DOMAIN}"
        log_info ""
        ;;

    stop)
        log_step "Arrêt des services du cabinet ${CABINET_ID}..."
        docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" stop
        log_info "✓ Services arrêtés"
        ;;

    restart)
        log_step "Redémarrage des services du cabinet ${CABINET_ID}..."
        docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" restart
        log_info "✓ Services redémarrés"
        ;;

    down)
        log_warn "⚠️  ATTENTION: Cette action va supprimer les conteneurs du cabinet ${CABINET_ID}"
        log_warn "   (Les données seront conservées dans les volumes)"
        read -p "Continuer? (yes/no) " -r
        if [[ $REPLY =~ ^(yes|YES)$ ]]; then
            docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down
            log_info "✓ Conteneurs supprimés"
        else
            log_info "Annulé"
        fi
        ;;

    status)
        log_step "État des services du cabinet ${CABINET_ID}:"
        docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps
        ;;

    logs)
        CONTAINER="predykt-app-cabinet-${CABINET_ID}"
        log_step "Logs du cabinet ${CABINET_ID}:"
        docker logs -f --tail=100 "$CONTAINER"
        ;;

    backup)
        log_step "Lancement du backup pour le cabinet ${CABINET_ID}..."
        ./scripts/backup-tenant.sh cabinet "$CABINET_ID"
        ;;

    restore)
        TIMESTAMP="${3:-latest}"
        log_step "Restore du cabinet ${CABINET_ID} (timestamp: ${TIMESTAMP})..."
        ./scripts/restore-tenant.sh cabinet "$CABINET_ID" "$TIMESTAMP"
        ;;

    update)
        log_step "Mise à jour de l'application pour le cabinet ${CABINET_ID}..."

        # Backup avant mise à jour
        log_info "[1/4] Backup de sécurité..."
        ./scripts/backup-tenant.sh cabinet "$CABINET_ID"

        # Pull nouvelle image
        log_info "[2/4] Téléchargement de la nouvelle version..."
        docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" pull predykt-app-cabinet-${CABINET_ID}

        # Restart
        log_info "[3/4] Redémarrage avec la nouvelle version..."
        docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d predykt-app-cabinet-${CABINET_ID}

        # Health check
        log_info "[4/4] Vérification de la santé..."
        sleep 15

        APP_PORT=$(grep "^APP_PORT=" "$ENV_FILE" | cut -d= -f2)
        if curl -f -s "http://localhost:${APP_PORT}/api/v1/health" > /dev/null; then
            log_info "✓ Mise à jour réussie pour ${CABINET_ID}!"
        else
            log_error "❌ La santé de l'application est KO"
            log_error "   Vérifiez les logs: $0 $CABINET_ID logs"
            exit 1
        fi
        ;;

    companies)
        log_step "Liste des dossiers clients du cabinet ${CABINET_ID}:"
        APP_PORT=$(grep "^APP_PORT=" "$ENV_FILE" | cut -d= -f2)
        curl -s "http://localhost:${APP_PORT}/api/v1/companies" | jq '.'
        ;;

    *)
        log_error "Action inconnue: $ACTION"
        log_error ""
        log_error "Actions disponibles:"
        log_error "  start     - Démarrer les services"
        log_error "  stop      - Arrêter les services"
        log_error "  restart   - Redémarrer les services"
        log_error "  down      - Supprimer les conteneurs"
        log_error "  status    - Afficher l'état des services"
        log_error "  logs      - Afficher les logs"
        log_error "  backup    - Créer un backup"
        log_error "  restore   - Restaurer un backup"
        log_error "  update    - Mettre à jour l'application"
        log_error "  companies - Lister les dossiers clients"
        exit 1
        ;;
esac
