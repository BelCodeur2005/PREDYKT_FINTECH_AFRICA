# Dockerfile
# Build multi-stage pour optimiser la taille de l'image finale

# ============================================
# STAGE 1 : BUILD (Compilation de l'application)
# ============================================
FROM eclipse-temurin:17-jdk-alpine AS build
LABEL maintainer="tech@predykt.com"
LABEL description="PREDYKT Core Accounting API - Build Stage"

WORKDIR /app

# Copier les fichiers Maven (pour cache des dépendances)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Télécharger les dépendances (layer cacheable)
# Si pom.xml ne change pas, cette étape est réutilisée
RUN ./mvnw dependency:go-offline -B

# Copier le code source
COPY src ./src

# Build de l'application (skip tests pour accélérer)
RUN ./mvnw clean package -DskipTests

# Vérifier que le JAR a été créé
RUN ls -lh /app/target/*.jar

# ============================================
# STAGE 2 : RUNTIME (Image finale légère)
# ============================================
FROM eclipse-temurin:17-jre-alpine
LABEL maintainer="tech@predykt.com"
LABEL description="PREDYKT Core Accounting API - Production"

WORKDIR /app

# Créer un utilisateur non-root pour la sécurité
RUN addgroup -S predykt && adduser -S predykt -G predykt

# Installer des outils utiles (optionnel)
RUN apk add --no-cache curl wget

# Copier le JAR depuis le stage de build
COPY --from=build /app/target/*.jar app.jar

# Copier le script de healthcheck
COPY scripts/healthcheck.sh /app/healthcheck.sh
RUN chmod +x /app/healthcheck.sh

# Créer le répertoire des logs
RUN mkdir -p /app/logs && chown -R predykt:predykt /app

# Changer les permissions
USER predykt

# Variables d'environnement par défaut (peuvent être surchargées)
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV SPRING_PROFILES_ACTIVE=prod
ENV TZ=Africa/Douala

# Exposer le port de l'application
EXPOSE 8080

# Health check (Docker vérifie automatiquement la santé)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD /app/healthcheck.sh

# Point d'entrée de l'application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]