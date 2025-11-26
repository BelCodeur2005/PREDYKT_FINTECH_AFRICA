# 🚀 Guide de Déploiement PREDYKT

Ce guide explique comment déployer PREDYKT Core Accounting API dans les 3 modes multi-tenant.

## 📋 Table des Matières

1. [Vue d'ensemble des modes](#vue-densemble-des-modes)
2. [Prérequis](#prérequis)
3. [Mode SHARED (Multi-PME)](#mode-shared-multi-pme)
4. [Mode DEDICATED (ETI Isolée)](#mode-dedicated-eti-isolée)
5. [Mode CABINET (Cabinet Comptable)](#mode-cabinet-cabinet-comptable)
6. [Opérations courantes](#opérations-courantes)
7. [Troubleshooting](#troubleshooting)

---

## Vue d'ensemble des modes

| Mode | Use Case | Base de données | Isolation | Scalabilité |
|------|----------|-----------------|-----------|-------------|
| **SHARED** | Plusieurs PME | Unique partagée | Ligne (company_id) | Horizontale +++|
| **DEDICATED** | Une grande entreprise (ETI) | Dédiée par tenant | Base de données | Verticale + |
| **CABINET** | Cabinet comptable | Dédiée par cabinet | Ligne (company_id) | Mixte ++ |

---

## Prérequis

### Logiciels requis
- Docker 20.10+
- Docker Compose 2.0+
- Git
- curl (pour les health checks)

### Ressources minimales
- **SHARED**: 4 CPU, 8 GB RAM, 100 GB disque
- **DEDICATED**: 2 CPU, 4 GB RAM, 50 GB disque (par tenant)
- **CABINET**: 2 CPU, 4 GB RAM, 50 GB disque (par cabinet)

### Sécurité
- Générer des secrets forts (JWT, DB passwords)
- Ne JAMAIS commiter les fichiers `.env` dans Git
- Utiliser HTTPS en production (Traefik + Let's Encrypt configuré)

---

## Mode SHARED (Multi-PME)

**Cas d'usage**: Plateforme SaaS pour plusieurs petites entreprises partageant la même base de données.

### 1. Configuration initiale

```bash
# Copier le fichier d'environnement
cp .env.shared.example .env.shared

# Éditer et configurer
nano .env.shared
```

**Paramètres à configurer dans `.env.shared`:**
```bash
# Remplacer TOUS les CHANGEME par des valeurs sécurisées

# Générer un JWT secret (512 bits):
openssl rand -base64 64 | tr -d "=+/" | cut -c1-64

# Générer des mots de passe forts:
openssl rand -base64 32 | tr -d "=+/" | cut -c1-32
```

### 2. Déploiement

```bash
# Démarrer les services
./scripts/deploy-shared.sh start

# Vérifier l'état
./scripts/deploy-shared.sh status

# Voir les logs
./scripts/deploy-shared.sh logs
```

### 3. Vérification

```bash
# Health check
curl http://localhost:8080/api/v1/health

# Créer une première entreprise
curl -X POST http://localhost:8080/api/v1/companies \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test PME",
    "email": "test@pme.cm",
    "country": "CM",
    "currency": "XAF",
    "accountingStandard": "OHADA"
  }'
```

### 4. Accès aux services

- **API**: http://localhost:8080/api/v1
- **Swagger**: http://localhost:8080/api/v1/swagger-ui.html
- **PgAdmin**: http://localhost:5050 (admin@predykt.com / voir .env.shared)

---

## Mode DEDICATED (ETI Isolée)

**Cas d'usage**: Une grande entreprise avec sa propre base de données isolée.

### Méthode 1: Script automatisé (recommandé)

```bash
# Provisionner un nouveau tenant
./scripts/provision-new-tenant.sh companyA erp.companyA.com "Company A Ltd"

# Le script va automatiquement:
# ✓ Générer les secrets sécurisés
# ✓ Créer la structure de répertoires
# ✓ Configurer le docker-compose
# ✓ Démarrer les services
# ✓ Initialiser la base de données
# ✓ Créer l'entreprise
```

### Méthode 2: Configuration manuelle

```bash
# 1. Copier le fichier d'environnement
cp .env.dedicated.example .env.dedicated-companyA

# 2. Éditer la configuration
nano .env.dedicated-companyA
```

**Paramètres importants:**
```bash
TENANT_ID=companyA
TENANT_DOMAIN=erp.companyA.predykt.com
COMPANY_NAME=Company A Ltd

# Ports UNIQUES pour ce tenant (éviter les conflits)
APP_PORT=8001
DB_PORT=5401
REDIS_PORT=6401
PGADMIN_PORT=5051

# Subnet unique (20-254)
TENANT_SUBNET_OCTET=21
```

```bash
# 3. Démarrer les services
docker-compose -f docker-compose.dedicated.yml \
  --env-file .env.dedicated-companyA \
  up -d

# 4. Vérifier
docker-compose -f docker-compose.dedicated.yml \
  --env-file .env.dedicated-companyA \
  ps
```

### Gestion de plusieurs tenants DEDICATED

Chaque tenant doit avoir:
- ✅ Un `TENANT_ID` unique
- ✅ Des ports uniques (APP_PORT, DB_PORT, REDIS_PORT)
- ✅ Un subnet unique (TENANT_SUBNET_OCTET)
- ✅ Son propre fichier `.env.dedicated-{TENANT_ID}`

**Exemple avec 3 tenants:**
```bash
# Tenant 1
TENANT_ID=companyA, APP_PORT=8001, DB_PORT=5401, SUBNET=21

# Tenant 2
TENANT_ID=companyB, APP_PORT=8002, DB_PORT=5402, SUBNET=22

# Tenant 3
TENANT_ID=companyC, APP_PORT=8003, DB_PORT=5403, SUBNET=23
```

---

## Mode CABINET (Cabinet Comptable)

**Cas d'usage**: Un cabinet comptable gérant plusieurs dossiers clients dans une base dédiée.

### 1. Configuration

```bash
# Copier le fichier d'environnement
cp .env.cabinet.example .env.cabinet-douala

# Éditer
nano .env.cabinet-douala
```

**Paramètres spécifiques:**
```bash
CABINET_ID=cabinet-douala
CABINET_NAME=Cabinet Expert Comptable Douala
CABINET_DOMAIN=cabinet-douala.predykt.com

# Ports uniques
APP_PORT=8100
DB_PORT=5500
REDIS_PORT=6500
PGADMIN_PORT=5150

CABINET_SUBNET_OCTET=30
```

### 2. Déploiement

```bash
# Démarrer le cabinet
./scripts/deploy-cabinet.sh cabinet-douala start

# Vérifier l'état
./scripts/deploy-cabinet.sh cabinet-douala status

# Voir les logs
./scripts/deploy-cabinet.sh cabinet-douala logs
```

### 3. Gestion des dossiers clients

```bash
# Lister les dossiers clients du cabinet
./scripts/deploy-cabinet.sh cabinet-douala companies

# Créer un nouveau dossier client
curl -X POST http://localhost:8100/api/v1/companies \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Dossier SARL EXEMPLE",
    "email": "contact@exemple.cm",
    "country": "CM",
    "currency": "XAF",
    "accountingStandard": "OHADA"
  }'
```

---

## Opérations courantes

### Backup

```bash
# Mode SHARED
./scripts/backup-tenant.sh shared

# Mode DEDICATED
./scripts/backup-tenant.sh dedicated companyA

# Mode CABINET
./scripts/backup-tenant.sh cabinet cabinet-douala
```

Les backups sont stockés dans `./backups/{mode}/{tenant_id}/`

### Restore

```bash
# Restaurer le dernier backup
./scripts/restore-tenant.sh shared "" latest
./scripts/restore-tenant.sh dedicated companyA latest
./scripts/restore-tenant.sh cabinet cabinet-douala latest

# Restaurer un backup spécifique
./scripts/restore-tenant.sh shared "" 20241126_143000
```

### Mise à jour

```bash
# Mode SHARED
./scripts/deploy-shared.sh update

# Mode CABINET
./scripts/deploy-cabinet.sh cabinet-douala update
```

La commande `update` effectue automatiquement:
1. ✅ Backup de sécurité
2. ✅ Pull de la nouvelle image Docker
3. ✅ Redémarrage avec la nouvelle version
4. ✅ Health check

### Monitoring des logs

```bash
# Mode SHARED
./scripts/deploy-shared.sh logs

# Mode DEDICATED
docker logs -f predykt-app-companyA

# Mode CABINET
./scripts/deploy-cabinet.sh cabinet-douala logs
```

### Arrêt et redémarrage

```bash
# Arrêter (sans supprimer les données)
./scripts/deploy-shared.sh stop
./scripts/deploy-cabinet.sh cabinet-douala stop

# Redémarrer
./scripts/deploy-shared.sh restart
./scripts/deploy-cabinet.sh cabinet-douala restart
```

---

## Troubleshooting

### La base de données ne démarre pas

```bash
# Vérifier les logs PostgreSQL
docker logs predykt-postgres-shared

# Vérifier les permissions du volume
docker volume inspect predykt-backend-java_postgres-data-shared

# Recréer les volumes (⚠️ PERTE DE DONNÉES)
docker-compose -f docker-compose.shared.yml down -v
docker-compose -f docker-compose.shared.yml up -d
```

### L'application ne démarre pas

```bash
# Vérifier les logs
docker logs predykt-app-shared

# Erreurs communes:
# 1. JWT secret trop court (min 512 bits)
# 2. Base de données non accessible
# 3. Variables d'environnement manquantes
```

### Conflit de ports

```bash
# Vérifier les ports utilisés
docker ps --format "table {{.Names}}\t{{.Ports}}"

# Si conflit, modifier les ports dans le fichier .env
# APP_PORT, DB_PORT, REDIS_PORT doivent être uniques
```

### Health check échoue

```bash
# Vérifier manuellement
curl -v http://localhost:8080/api/v1/health

# Vérifier les migrations Flyway
docker exec predykt-postgres-shared psql -U predykt -d predykt_db -c "SELECT * FROM flyway_schema_history;"

# Redémarrer l'application
docker restart predykt-app-shared
```

### Erreur "tenant not found"

```bash
# Mode DEDICATED/CABINET: Vérifier que les variables sont bien définies
docker exec predykt-app-companyA env | grep PREDYKT

# Doit afficher:
# PREDYKT_TENANT_MODE=DEDICATED
# PREDYKT_TENANT_ID=companyA
```

### Manque d'espace disque

```bash
# Nettoyer les images Docker non utilisées
docker system prune -a

# Nettoyer les anciens backups (>30 jours)
find ./backups -type f -mtime +30 -delete

# Vérifier l'espace disque des volumes
docker system df -v
```

---

## Sécurité en production

### Checklist avant mise en production

- [ ] Tous les mots de passe changés (pas de CHANGEME)
- [ ] JWT secret de 512 bits minimum
- [ ] HTTPS configuré (Traefik + Let's Encrypt)
- [ ] Firewall configuré (ports internes non exposés)
- [ ] Backups automatiques activés
- [ ] Monitoring configuré (Prometheus/Grafana)
- [ ] Logs centralisés (ELK Stack / Loki)
- [ ] Variables d'environnement dans secrets (pas dans .env)

### Recommandations

1. **Secrets Management**: Utiliser Docker Secrets ou Vault en production
2. **Backups**: Tester régulièrement la procédure de restore
3. **Monitoring**: Configurer des alertes sur Prometheus
4. **Updates**: Planifier des fenêtres de maintenance
5. **Logs**: Activer la rotation des logs (logrotate)

---

## Support

Pour toute question ou problème:
- **Documentation**: https://docs.predykt.com
- **Issues**: https://github.com/predykt/predykt-backend-java/issues
- **Email**: tech@predykt.com
