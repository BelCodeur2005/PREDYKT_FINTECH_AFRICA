# PREDYKT Core Accounting API (MVP 1.0)

API REST professionnelle pour la gestion comptable et financière des entreprises africaines (OHADA).

## 🚀 Démarrage Rapide

### Prérequis
- Java 17+
- Docker & Docker Compose
- Maven 3.8+
- PostgreSQL 15+ (via Docker)

### Installation

1. **Cloner le projet**
```bash
git clone https://github.com/predykt/accounting-api.git
cd accounting-api
```

2. **Configurer les variables d'environnement**
```bash
cp .env.example .env
# Éditer .env avec vos valeurs
```

3. **Démarrer les services Docker**
```bash
docker-compose up -d
```

4. **Lancer l'application**
```bash
./mvnw spring-boot:run
```

5. **Vérifier le déploiement**
```bash
curl http://localhost:8080/api/v1/health
```

## 📚 Documentation

- **API Documentation (Swagger)**: http://localhost:8080/api/v1/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/api/v1/api-docs

## 🧪 Tests
```bash
# Tests unitaires
./mvnw test

# Tests d'intégration
./mvnw verify

# Couverture de code
./mvnw jacoco:report
```

## 📦 Build Production
```bash
./mvnw clean package -DskipTests
docker build -t predykt/accounting:1.0.0 .
```

## 🔐 Sécurité

- Les endpoints sont protégés par JWT (Phase 2)
- Audit trail automatique sur toutes les modifications
- Conformité ISO 27001 en cours

## 📞 Support

- Email: tech@predykt.com
- Documentation: https://docs.predykt.com