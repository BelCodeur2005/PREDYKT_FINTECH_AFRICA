# Scripts de Déploiement et Maintenance PREDYKT

Ce dossier contient tous les scripts pour déployer, gérer et maintenir PREDYKT Core Accounting API.

## 📋 Liste des scripts

### 🚀 Déploiement

| Script | Description | Modes supportés |
|--------|-------------|-----------------|
| `deploy-shared.sh` | Déployer/gérer le mode SHARED | SHARED |
| `deploy-cabinet.sh` | Déployer/gérer le mode CABINET | CABINET |
| `provision-new-tenant.sh` | Provisionner un nouveau tenant DEDICATED | DEDICATED |

### 💾 Backup & Restore

| Script | Description | Modes supportés |
|--------|-------------|-----------------|
| `backup-tenant.sh` | Créer un backup (PostgreSQL + Redis) | ALL |
| `restore-tenant.sh` | Restaurer depuis un backup | ALL |

### 🗄️ Initialisation Base de données

| Fichier | Usage |
|---------|-------|
| `init-db.sql` | Script d'init pour mode SHARED |
| `init-tenant-db.sql` | Script d'init pour modes DEDICATED/CABINET |

---

## 🚀 Scripts de Déploiement

### deploy-shared.sh

**Usage**: `./deploy-shared.sh [action]`

**Actions disponibles:**
- `start` - Démarrer les services
- `stop` - Arrêter les services
- `restart` - Redémarrer les services
- `down` - Supprimer les conteneurs (garde les données)
- `status` - Afficher l'état des services
- `logs` - Afficher les logs (optionnel: nom du conteneur)
- `backup` - Créer un backup
- `update` - Mettre à jour l'application (avec backup auto)

**Exemples:**
```bash
# Démarrer le mode SHARED
./deploy-shared.sh start

# Voir les logs de l'application
./deploy-shared.sh logs

# Mettre à jour (backup auto + pull + restart + health check)
./deploy-shared.sh update

# Créer un backup manuel
./deploy-shared.sh backup
```

**Prérequis:**
- Fichier `.env.shared` configuré
- Docker et Docker Compose installés

---

### deploy-cabinet.sh

**Usage**: `./deploy-cabinet.sh <cabinet_id> [action]`

**Actions disponibles:**
- `start` - Démarrer les services du cabinet
- `stop` - Arrêter les services
- `restart` - Redémarrer les services
- `down` - Supprimer les conteneurs
- `status` - Afficher l'état
- `logs` - Afficher les logs
- `backup` - Créer un backup
- `restore` - Restaurer un backup
- `update` - Mettre à jour
- `companies` - Lister les dossiers clients

**Exemples:**
```bash
# Démarrer le cabinet "cabinet-douala"
./deploy-cabinet.sh cabinet-douala start

# Voir les logs
./deploy-cabinet.sh cabinet-douala logs

# Lister les dossiers clients
./deploy-cabinet.sh cabinet-douala companies

# Backup
./deploy-cabinet.sh cabinet-douala backup

# Restore
./deploy-cabinet.sh cabinet-douala restore 20241126_143000
```

**Prérequis:**
- Fichier `.env.cabinet-{CABINET_ID}` configuré
- Ports uniques configurés

---

### provision-new-tenant.sh

**Usage**: `./provision-new-tenant.sh <tenant_id> <tenant_domain> <company_name>`

**Description**: Script automatisé pour provisionner un nouveau tenant DEDICATED. Effectue toutes les étapes de configuration.

**Exemples:**
```bash
./provision-new-tenant.sh companyA erp.companyA.com "Company A Ltd"
./provision-new-tenant.sh eti-douala erp.eti-douala.predykt.com "ETI Douala SARL"
```

**Ce que le script fait:**
1. ✅ Génère les secrets sécurisés (JWT, DB password, Redis password)
2. ✅ Crée la structure de répertoires
3. ✅ Génère le fichier `.env` avec secrets
4. ✅ Génère le docker-compose.yml depuis le template
5. ✅ Configure le DNS (si CloudFlare API key fourni)
6. ✅ Démarre les services Docker
7. ✅ Attend que la base soit prête
8. ✅ Initialise l'entreprise via l'API

**Output:**
```
╔════════════════════════════════════════════════════════════╗
║              PROVISIONING TERMINÉ AVEC SUCCÈS              ║
╚════════════════════════════════════════════════════════════╝

🌐 URL d'accès       : https://erp.companyA.com
🔐 Répertoire config : /opt/predykt/tenants/companyA
📊 Monitoring        : http://localhost:8001/actuator/health
```

**Prérequis:**
- Base directory: `/opt/predykt` (modifiable dans le script)
- Template: `infrastructure/docker/docker-compose-tenant-template.yml`
- Optionnel: Variable `CLOUDFLARE_API_KEY` pour DNS auto

---

## 💾 Scripts de Backup/Restore

### backup-tenant.sh

**Usage**: `./backup-tenant.sh <mode> [tenant_id]`

**Description**: Crée un backup compressé de PostgreSQL et Redis avec timestamp.

**Exemples:**
```bash
# Mode SHARED
./backup-tenant.sh shared

# Mode DEDICATED
./backup-tenant.sh dedicated companyA

# Mode CABINET
./backup-tenant.sh cabinet cabinet-douala
```

**Ce que le script fait:**
1. ✅ Vérifie que les conteneurs existent et sont actifs
2. ✅ Dump PostgreSQL avec compression gzip
3. ✅ Copie dump.rdb de Redis
4. ✅ Crée un lien symbolique vers le dernier backup
5. ✅ Nettoie les backups >30 jours

**Output:**
```
╔════════════════════════════════════════════════════════════╗
║                 BACKUP TERMINÉ AVEC SUCCÈS                 ║
╚════════════════════════════════════════════════════════════╝

📁 Répertoire: ./backups/shared
💾 PostgreSQL: ./backups/shared/postgres/backup_20241126_143000.sql.gz
💾 Redis     : ./backups/shared/redis/dump_20241126_143000.rdb

Pour restaurer ce backup:
  ./scripts/restore-tenant.sh shared  20241126_143000
```

**Stockage:**
```
backups/
├── shared/
│   ├── postgres/
│   │   ├── backup_20241126_143000.sql.gz
│   │   └── latest.sql.gz -> backup_20241126_143000.sql.gz
│   └── redis/
│       ├── dump_20241126_143000.rdb
│       └── latest.rdb -> dump_20241126_143000.rdb
├── dedicated/
│   └── companyA/
│       └── ...
└── cabinet/
    └── cabinet-douala/
        └── ...
```

---

### restore-tenant.sh

**Usage**: `./restore-tenant.sh <mode> <tenant_id> [timestamp]`

**Description**: Restaure un backup. ⚠️ ÉCRASE les données actuelles!

**Exemples:**
```bash
# Restaurer le dernier backup (latest)
./restore-tenant.sh shared "" latest
./restore-tenant.sh dedicated companyA latest

# Restaurer un backup spécifique
./restore-tenant.sh shared "" 20241126_143000
./restore-tenant.sh dedicated companyA 20241126_143000
./restore-tenant.sh cabinet cabinet-douala 20241126_143000
```

**Ce que le script fait:**
1. ⚠️ Demande confirmation (données seront écrasées!)
2. ✅ Arrête l'application
3. ✅ Drop la base de données existante
4. ✅ Crée une nouvelle base vide
5. ✅ Restore le dump PostgreSQL
6. ✅ Restore Redis (optionnel)
7. ✅ Redémarre l'application
8. ✅ Vérifie le health check

**⚠️ Warnings:**
- Les données actuelles seront **PERDUES**
- Créer un backup avant restore en cas de doute
- Vérifier le timestamp du backup avant restore

---

## 🗄️ Scripts d'initialisation DB

### init-db.sql

**Usage**: Automatique lors du `docker-compose up` (mode SHARED)

**Contenu:**
- Extensions PostgreSQL (`uuid-ossp`, `pg_stat_statements`)
- Configuration performance (shared_buffers, work_mem, etc.)
- Messages de confirmation

**Montage Docker:**
```yaml
volumes:
  - ./scripts/init-db.sql:/docker-entrypoint-initdb.d/init.sql
```

---

### init-tenant-db.sql

**Usage**: Automatique lors du `docker-compose up` (modes DEDICATED/CABINET)

**Contenu:**
- Extensions PostgreSQL
- Configuration performance optimisée pour tenant dédié
- Logging des requêtes lentes (>1 seconde)
- Messages de confirmation

**Différences avec `init-db.sql`:**
- ✅ Logging activé pour debug
- ✅ Configuration adaptée pour charge moindre
- ✅ Utilisé par les templates tenant

---

## 🔧 Permissions

Tous les scripts doivent être exécutables:

```bash
chmod +x scripts/*.sh
```

Si vous rencontrez `Permission denied`:
```bash
# Windows (Git Bash)
git update-index --chmod=+x scripts/*.sh

# Linux/Mac
chmod +x scripts/*.sh
```

---

## 📝 Notes importantes

### Modes de tenant

- **SHARED**: Tous les scripts utilisent `shared` comme identifiant
- **DEDICATED**: Chaque tenant a un `TENANT_ID` unique (ex: companyA, companyB)
- **CABINET**: Chaque cabinet a un `CABINET_ID` unique (ex: cabinet-douala)

### Ports uniques

Pour DEDICATED et CABINET, **TOUJOURS** utiliser des ports uniques:

| Tenant | APP_PORT | DB_PORT | REDIS_PORT | SUBNET |
|--------|----------|---------|------------|--------|
| companyA | 8001 | 5401 | 6401 | 21 |
| companyB | 8002 | 5402 | 6402 | 22 |
| cabinet-douala | 8100 | 5500 | 6500 | 30 |

### Backups

- Rétention: 30 jours (automatique)
- Format: `backup_YYYYMMDD_HHMMSS.sql.gz`
- Compression: gzip
- Lien `latest.*` vers le dernier backup

### Logs

```bash
# Logs temps réel
docker logs -f <container_name>

# 100 dernières lignes
docker logs --tail 100 <container_name>

# Depuis timestamp
docker logs --since 2024-11-26T14:30:00 <container_name>
```

---

## 🆘 Troubleshooting

### Script bash: command not found

**Windows (Git Bash):**
```bash
# Vérifier les fins de ligne (CRLF vs LF)
dos2unix scripts/*.sh

# Ou dans Git Bash:
sed -i 's/\r$//' scripts/*.sh
```

### Permission denied

```bash
chmod +x scripts/*.sh
```

### Variable not found

Vérifier que le fichier `.env.*` existe et est bien configuré:
```bash
ls -la .env.*
cat .env.shared  # Vérifier le contenu
```

### Docker not found

```bash
# Vérifier que Docker est installé
docker --version
docker-compose --version

# Windows: redémarrer Docker Desktop
```

---

## 📚 Documentation

- **Guide de déploiement complet**: `../DEPLOYMENT.md`
- **Résumé de configuration**: `../CONFIGURATION_SUMMARY.md`
- **Architecture**: `../CLAUDE.md`
- **Documentation principale**: `../README.md`
