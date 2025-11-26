#!/bin/bash

################################################################################
# Script de Déploiement Mode SHARED (Production Multi-PME)
# Usage: ./deploy-shared.sh [start|stop|restart|status|logs]
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

ACTION="${1:-start}"

# Vérifier que le fichier .env.shared existe
if [ ! -f ".env.shared" ]; then
    log_error "Fichier .env.shared introuvable!"
    log_error ""
    log_error "Actions requises:"
    log_error "  1. Copier l'exemple: cp .env.shared.example .env.shared"
    log_error "  2. Éditer .env.shared et remplacer tous les CHANGEME"
    log_error "  3. Relancer ce script"
    exit 1
fi

COMPOSE_FILE="docker-compose.shared.yml"
ENV_FILE=".env.shared"

log_info "╔════════════════════════════════════════════════════════════╗"
log_info "║      PREDYKT DEPLOYMENT - Mode SHARED (Multi-PME)         ║"
log_info "╚════════════════════════════════════════════════════════════╝"
log_info ""
log_info "Action: ${ACTION}"
log_info "Compose: ${COMPOSE_FILE}"
log_info "Env: ${ENV_FILE}"
log_info ""

case "$ACTION" in
    start)
        log_step "Démarrage des services en mode SHARED..."

        # Vérification de sécurité: s'assurer que les mots de passe ont été changés
        if grep -q "CHANGEME" "$ENV_FILE"; then
            log_error "❌ Le fichier $ENV_FILE contient encore des valeurs CHANGEME!"
            log_error "   Éditez le fichier et remplacez tous les secrets avant le déploiement."
            exit 1
        fi

        # Build et démarrage
        docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" pull
        docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d

        log_info ""
        log_info "✓ Services démarrés avec succès!"
        log_info ""
        log_info "Vérification de la santé des services..."
        sleep 10

        # Health check
        docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps

        log_info ""
        log_info "Accès aux services:"
        log_info "  - API:     http://localhost:$(grep APP_PORT $ENV_FILE | cut -d= -f2)"
        log_info "  - PgAdmin: http://localhost:$(grep PGADMIN_PORT $ENV_FILE | cut -d= -f2)"
        log_info ""
        log_info "Pour voir les logs: ./scripts/deploy-shared.sh logs"
        ;;

    stop)
        log_step "Arrêt des services..."
        docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" stop
        log_info "✓ Services arrêtés"
        ;;

    restart)
        log_step "Redémarrage des services..."
        docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" restart
        log_info "✓ Services redémarrés"
        ;;

    down)
        log_warn "⚠️  ATTENTION: Cette action va supprimer les conteneurs (mais pas les volumes/données)"
        read -p "Continuer? (yes/no) " -r
        if [[ $REPLY =~ ^(yes|YES)$ ]]; then
            docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down
            log_info "✓ Conteneurs supprimés"
        else
            log_info "Annulé"
        fi
        ;;

    status)
        log_step "État des services:"
        docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps
        ;;

    logs)
        CONTAINER="${2:-predykt-app-shared}"
        log_step "Logs du conteneur: $CONTAINER"
        docker logs -f --tail=100 "$CONTAINER"
        ;;

    backup)
        log_step "Lancement du backup..."
        ./scripts/backup-tenant.sh shared
        ;;

    update)
        log_step "Mise à jour de l'application..."

        # Backup avant mise à jour
        log_info "[1/4] Backup de sécurité..."
        ./scripts/backup-tenant.sh shared

        # Pull de la nouvelle image
        log_info "[2/4] Téléchargement de la nouvelle version..."
        docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" pull predykt-app

        # Restart avec la nouvelle image
        log_info "[3/4] Redémarrage avec la nouvelle version..."
        docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d predykt-app

        # Health check
        log_info "[4/4] Vérification de la santé..."
        sleep 15

        APP_PORT=$(grep APP_PORT $ENV_FILE | cut -d= -f2)
        if curl -f -s "http://localhost:${APP_PORT}/api/v1/health" > /dev/null; then
            log_info "✓ Mise à jour réussie!"
        else
            log_error "❌ La santé de l'application est KO"
            log_error "   Vérifiez les logs: ./scripts/deploy-shared.sh logs"
            exit 1
        fi
        ;;

    *)
        log_error "Action inconnue: $ACTION"
        log_error ""
        log_error "Actions disponibles:"
        log_error "  start   - Démarrer les services"
        log_error "  stop    - Arrêter les services"
        log_error "  restart - Redémarrer les services"
        log_error "  down    - Supprimer les conteneurs"
        log_error "  status  - Afficher l'état des services"
        log_error "  logs    - Afficher les logs (optionnel: nom du conteneur)"
        log_error "  backup  - Créer un backup"
        log_error "  update  - Mettre à jour l'application"
        exit 1
        ;;
esac
