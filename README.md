# PREDYKT Core Accounting API - Documentation Complète

[![Java](https://img.shields.io/badge/Java-17-red.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-Proprietary-yellow.svg)](LICENSE)

**API REST professionnelle pour la gestion comptable et financière des entreprises africaines (OHADA)**

---

## 📋 Table des Matières

1. [Vue d'ensemble](#vue-densemble)
2. [Installation](#installation)
3. [Configuration](#configuration)
4. [Utilisation](#utilisation)
5. [Import de Données](#import-de-données)
6. [API Endpoints](#api-endpoints)
7. [Architecture](#architecture)
8. [Tests](#tests)
9. [Déploiement](#déploiement)

---

## 🎯 Vue d'ensemble

PREDYKT est la première plateforme panafricaine d'analyse et de prédiction financière basée sur une IA nativement conçue pour le contexte africain.

### Fonctionnalités Principales (MVP Phase I)

✅ **Comptabilité Générale OHADA**
- Plan comptable OHADA pré-configuré
- Gestion des écritures comptables (respect de la partie double)
- Grand Livre et journaux
- Clôture et verrouillage des périodes

✅ **Import de Données**
- Import CSV des activités comptables
- Parsing intelligent avec détection automatique du format
- Mapping automatique vers les comptes OHADA

✅ **Ratios Financiers**
- Calcul automatique de 20+ ratios financiers
- Ratios de rentabilité (ROA, ROE, marges)
- Ratios de liquidité et solvabilité
- Ratios d'activité (DSO, DIO, DPO)

✅ **Rapports Financiers**
- Bilan (Balance Sheet)
- Compte de Résultat (Income Statement)
- Balance de vérification (Trial Balance)

✅ **Prévisions de Trésorerie** *(Phase II)*
- Projection J+30 (MVP)
- Projection J+60/J+90 (Phase II)
- Modèles ARIMA/Prophet

---

## 🚀 Installation

### Prérequis

```bash
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- PostgreSQL 15+
- Redis 7+
- Git
```

### 1. Cloner le Repository

```bash
git clone https://github.com/predykt/predykt-backend-java.git
cd predykt-backend-java
```

### 2. Configurer les Variables d'Environnement

```bash
cp .env.example .env
```

Éditez `.env` avec vos valeurs :

```env
# Base de données
DB_HOST=localhost
DB_PORT=5432
DB_NAME=predykt_db
DB_USER=predykt
DB_PASSWORD=VotreMot DePasseSecurisé

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=VotreMotDePasseRedis

# JWT (Phase II)
JWT_SECRET=VotreCléSecrèteJWT256Bits
JWT_EXPIRATION=86400000

# Python ML API (Phase II)
PYTHON_API_URL=http://localhost:8000
```

### 3. Démarrer les Services Docker

```bash
docker-compose up -d
```

Cela démarre :
- PostgreSQL (port 5432)
- Redis (port 6379)
- PgAdmin (port 5050) - Interface web pour PostgreSQL

### 4. Compiler et Lancer l'Application

```bash
# Compiler
./mvnw clean package -DskipTests

# Lancer
./mvnw spring-boot:run
```

L'API sera accessible sur : **http://localhost:8080/api/v1**

### 5. Vérifier le Démarrage

```bash
curl http://localhost:8080/api/v1/health
```

Réponse attendue :
```json
{
  "status": "UP",
  "timestamp": "2024-01-15T10:30:00",
  "application": "PREDYKT Core Accounting API",
  "version": "1.0.0",
  "database": "UP"
}
```

---

## ⚙️ Configuration

### Application Profiles

L'application supporte plusieurs profils :

- **dev** (par défaut) : Développement local
- **prod** : Production

Changer de profil :

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

### Base de Données

Les migrations Flyway s'exécutent automatiquement au démarrage :

```
src/main/resources/db/migration/
├── V1__initial_schema.sql
├── V2__add_ratios_and_projections.sql
└── V3__add_indexes.sql
```

Pour exécuter manuellement les migrations :

```bash
./mvnw flyway:migrate
```

---

## 📊 Utilisation

### 1. Créer une Entreprise

```bash
curl -X POST http://localhost:8080/api/v1/companies \
  -H "Content-Type: application/json" \
  -d '{
    "name": "SARL EXEMPLE CAMEROUN",
    "taxId": "M012345678901",
    "email": "contact@exemple.cm",
    "phone": "+237690000000",
    "address": "123 Avenue de la Réunification",
    "city": "Douala",
    "country": "CM",
    "currency": "XAF",
    "accountingStandard": "OHADA"
  }'
```

### 2. Initialiser le Plan Comptable OHADA

Le plan comptable OHADA est **automatiquement initialisé** lors de la création d'une entreprise.

Pour vérifier les comptes :

```bash
curl http://localhost:8080/api/v1/companies/{companyId}/charts
```

### 3. Enregistrer une Écriture Comptable

```bash
curl -X POST http://localhost:8080/api/v1/companies/1/journal-entries \
  -H "Content-Type: application/json" \
  -d '{
    "entryDate": "2024-01-15",
    "reference": "FACT-2024-001",
    "journalCode": "VE",
    "lines": [
      {
        "accountNumber": "411",
        "debitAmount": 118000,
        "creditAmount": 0,
        "description": "Vente marchandises - Client A"
      },
      {
        "accountNumber": "701",
        "debitAmount": 0,
        "creditAmount": 100000,
        "description": "Vente marchandises"
      },
      {
        "accountNumber": "4431",
        "debitAmount": 0,
        "creditAmount": 18000,
        "description": "TVA collectée 18%"
      }
    ]
  }'
```

---

## 📁 Import de Données

### Format CSV Attendu

Le fichier CSV doit avoir la structure suivante (séparateur `;` ou `,`) :

```csv
date de saisie;Activitées;description;Montant Brut;Type;Années
14/04/2021;Wholesale Sales;Vente - Wholesale Sales - client 9850;1606982;Revenu;2021
26/09/2021;Maintenance;Charge - Maintenance - fournisseur 428;257025;Dépenses;2021
```

**Colonnes :**
1. **date de saisie** : Date au format `DD/MM/YYYY` ou `YYYY-MM-DD`
2. **Activitées** : Type d'activité (Sales, Purchase, Rent, etc.)
3. **description** : Description détaillée
4. **Montant Brut** : Montant (avec ou sans espaces/virgules)
5. **Type** : `Revenu`, `Dépenses`, `Capex`, ou `Financing`
6. **Années** : Année fiscale

### Importer le Fichier

```bash
curl -X POST http://localhost:8080/api/v1/companies/1/import/activities-csv \
  -H "Content-Type: multipart/form-data" \
  -F "file=@activités.csv"
```

**Réponse :**

```json
{
  "success": true,
  "data": {
    "totalRows": 1500,
    "successCount": 1487,
    "errorCount": 13,
    "message": "Import terminé: 1487/1500 lignes importées",
    "errors": [
      "Ligne 2024-03-15 - Transaction invalide: Montant manquant"
    ]
  }
}
```

### Mapping Automatique

Le service effectue automatiquement :

✅ Détection du séparateur (`;` ou `,`)
✅ Parsing intelligent des dates (plusieurs formats)
✅ Nettoyage des montants (espaces, virgules)
✅ Mapping vers les comptes OHADA selon l'activité
✅ Création d'écritures équilibrées (débit = crédit)

**Exemples de mapping :**

| Activité | Type | Compte OHADA |
|----------|------|--------------|
| Wholesale Sales | Revenu | 701 (Ventes de marchandises) |
| Administrative Salaries | Dépenses | 661 (Rémunérations) |
| Rent | Dépenses | 622 (Loyers) |
| Raw Materials Purchases | Dépenses | 601 (Achats MP) |
| Maintenance | Dépenses | 625 (Entretien) |
| Capex - Equipment | Capex | 24 (Matériel) |
| Loan draw | Financing | 16 (Emprunts) |

---

## 🔌 API Endpoints

### Swagger UI (Documentation Interactive)

Accédez à la documentation complète sur :

**http://localhost:8080/api/v1/swagger-ui.html**

### Endpoints Principaux

#### 🏢 Entreprises

```
POST   /api/v1/companies                    # Créer une entreprise
GET    /api/v1/companies/{id}               # Obtenir une entreprise
GET    /api/v1/companies                    # Lister toutes les entreprises
PUT    /api/v1/companies/{id}               # Mettre à jour
DELETE /api/v1/companies/{id}               # Désactiver
```

#### 📒 Écritures Comptables

```
POST   /api/v1/companies/{id}/journal-entries           # Créer une écriture
GET    /api/v1/companies/{id}/journal-entries/...       # Grand livre
GET    /api/v1/companies/{id}/journal-entries/trial-balance  # Balance
POST   /api/v1/companies/{id}/journal-entries/lock-period    # Verrouiller
```

#### 💰 Transactions Bancaires

```
POST   /api/v1/companies/{id}/bank-transactions/import       # Importer CSV
GET    /api/v1/companies/{id}/bank-transactions              # Lister
GET    /api/v1/companies/{id}/bank-transactions/unreconciled # Non réconciliées
POST   /api/v1/companies/{id}/bank-transactions/{tid}/reconcile  # Réconcilier
```

#### 📈 Ratios Financiers

```
POST   /api/v1/companies/{id}/ratios/calculate    # Calculer les ratios
GET    /api/v1/companies/{id}/ratios/year/{year}  # Ratios d'une année
GET    /api/v1/companies/{id}/ratios/history      # Historique
GET    /api/v1/companies/{id}/ratios/compare      # Comparer 2 années
```

#### 📊 Rapports Financiers

```
GET    /api/v1/companies/{id}/reports/balance-sheet      # Bilan
GET    /api/v1/companies/{id}/reports/income-statement   # Compte de résultat
```

#### 📥 Import de Données

```
POST   /api/v1/companies/{id}/import/activities-csv   # Import CSV
```

---

## 🏗️ Architecture

### Structure du Projet

```
src/main/java/com/predykt/accounting/
├── config/              # Configuration (Security, Redis, Swagger)
├── controller/          # REST Controllers
├── service/             # Logique métier
├── repository/          # Accès base de données (JPA)
├── domain/
│   ├── entity/          # Entités JPA
│   └── enums/           # Énumérations
├── dto/
│   ├── request/         # DTOs de requête
│   └── response/        # DTOs de réponse
├── mapper/              # MapStruct mappers
├── exception/           # Gestion des erreurs
└── util/                # Utilitaires

src/main/resources/
├── application.yaml     # Configuration principale
├── db/migration/        # Scripts Flyway
└── ohada/
    └── chart-of-accounts-ohada.json  # Plan comptable OHADA
```

### Technologies

- **Backend**: Java 17, Spring Boot 3.4
- **Base de données**: PostgreSQL 15 + Flyway
- **Cache**: Redis 7
- **Documentation**: SpringDoc OpenAPI 3
- **Mapping**: MapStruct + Lombok
- **CSV**: OpenCSV
- **Tests**: JUnit 5, Spring Test

---

## 🧪 Tests

### Lancer Tous les Tests

```bash
./mvnw test
```

### Tests Unitaires

```bash
./mvnw test -Dtest=*ServiceTest
```

### Tests d'Intégration

```bash
./mvnw verify
```

### Couverture de Code

```bash
./mvnw jacoco:report
```

Rapport généré dans : `target/site/jacoco/index.html`

---

## 🚢 Déploiement

### Build Production

```bash
./mvnw clean package -DskipTests
```

Le JAR est généré dans : `target/predykt-backend-java-1.0.0-SNAPSHOT.jar`

### Docker Build

```bash
docker build -t predykt/accounting-api:1.0.0 .
```

### Docker Compose (Production)

```bash
docker-compose -f docker-compose.prod.yml up -d
```

### Variables d'Environnement Production

```env
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db:5432/predykt_db
SPRING_DATASOURCE_USERNAME=predykt_prod
SPRING_DATASOURCE_PASSWORD=***
REDIS_HOST=prod-redis
REDIS_PASSWORD=***
```

---

## 📝 Exemples Complets

### Scénario : Import et Analyse Complète

```bash
# 1. Créer l'entreprise
COMPANY_ID=$(curl -X POST http://localhost:8080/api/v1/companies \
  -H "Content-Type: application/json" \
  -d '{"name":"Test SARL","email":"test@test.cm","country":"CM"}' | jq -r '.data.id')

# 2. Importer les activités CSV
curl -X POST http://localhost:8080/api/v1/companies/$COMPANY_ID/import/activities-csv \
  -F "file=@activités.csv"

# 3. Calculer les ratios pour 2021
curl -X POST "http://localhost:8080/api/v1/companies/$COMPANY_ID/ratios/calculate?startDate=2021-01-01&endDate=2021-12-31"

# 4. Obtenir le bilan au 31/12/2021
curl "http://localhost:8080/api/v1/companies/$COMPANY_ID/reports/balance-sheet?asOfDate=2021-12-31"

# 5. Obtenir le compte de résultat 2021
curl "http://localhost:8080/api/v1/companies/$COMPANY_ID/reports/income-statement?startDate=2021-01-01&endDate=2021-12-31"

# 6. Comparer les ratios 2021 vs 2022
curl "http://localhost:8080/api/v1/companies/$COMPANY_ID/ratios/compare?year1=2021&year2=2022"
```

---

## 🤝 Support et Contribution

### Signaler un Bug

Ouvrez une issue sur GitHub avec :
- Description du problème
- Étapes pour reproduire
- Logs d'erreur
- Version de l'API

### Documentation Complète

- **API Docs**: http://localhost:8080/api/v1/swagger-ui.html
- **Cahier des charges**: Voir `docs/cahier-des-charges.pdf`
- **Plan OHADA**: `src/main/resources/ohada/chart-of-accounts-ohada.json`

### Contact

- **Email**: tech@predykt.com
- **Site Web**: https://predykt.com
- **GitHub**: https://github.com/predykt

---

## 📜 Licence

Copyright © 2024 PREDYKT. Tous droits réservés.

Ce logiciel est protégé par le droit d'auteur et ne peut être utilisé, copié, modifié ou distribué sans autorisation écrite préalable.

---

**Made with ❤️ in Cameroon for African businesses**