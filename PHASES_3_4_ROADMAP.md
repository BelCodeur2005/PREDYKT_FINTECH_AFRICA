# 🗺️ Phases 3 & 4 - Roadmap d'Implémentation

**Document** : Guide d'implémentation Phases 3 & 4
**Date de création** : 11 Décembre 2025
**Status** : 📋 **À IMPLÉMENTER**
**Prérequis** : ✅ Phase 2 complète (Imputation partielle + PDF)

---

## 📋 Table des Matières

1. [Vue d'Ensemble](#vue-densemble)
2. [Phase 3 : Reporting et Alertes](#phase-3--reporting-et-alertes)
3. [Phase 4 : Automatisation et Suggestions](#phase-4--automatisation-et-suggestions)
4. [Ordre d'Implémentation](#ordre-dimplémentation)
5. [Estimation Temps & Ressources](#estimation-temps--ressources)
6. [Bénéfices Attendus](#bénéfices-attendus)
7. [Prérequis Techniques](#prérequis-techniques)

---

## 🎯 Vue d'Ensemble

### Objectif Global

Transformer le système d'acomptes en **plateforme intelligente de pilotage financier** avec :
- **Visibilité temps réel** (dashboards, KPIs)
- **Détection proactive** (alertes automatiques)
- **Automatisation** (suggestions, notifications)
- **Aide à la décision** (recommandations IA)

### Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    UTILISATEURS                          │
│  Comptables • Direction Financière • Commerciaux        │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│                  PHASE 4: AUTOMATION                     │
│  ┌──────────────┬──────────────┬─────────────────────┐ │
│  │  Suggestions │ Notifications │ Recommandations IA  │ │
│  │  Automatiques│     Email     │   Facturations      │ │
│  └──────────────┴──────────────┴─────────────────────┘ │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│                PHASE 3: REPORTING & ALERTES             │
│  ┌──────────────┬──────────────┬─────────────────────┐ │
│  │  Dashboard   │  Export      │    Alertes          │ │
│  │  Statistiques│   Excel      │  Automatiques       │ │
│  └──────────────┴──────────────┴─────────────────────┘ │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│           PHASE 2: IMPUTATION PARTIELLE & PDF           │
│                    (✅ TERMINÉE)                         │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│              PHASE 1: GESTION DE BASE                    │
│                    (✅ TERMINÉE)                         │
└─────────────────────────────────────────────────────────┘
```

---

## 📊 Phase 3 : Reporting et Alertes

### 🎯 Objectifs

1. **Dashboard en temps réel** avec indicateurs clés de performance
2. **Export Excel professionnel** multi-feuilles avec graphiques
3. **Système d'alertes intelligent** avec détection automatique
4. **Scheduler automatique** pour rapports et notifications périodiques

---

### 📦 Fichiers à Créer (Phase 3)

#### 1. Service Dashboard

**📄 `DashboardService.java`** (~400 lignes)

**Responsabilités** :
- Calcul des statistiques globales
- Agrégations par période (jour, mois, trimestre, année)
- Top clients par volume d'acomptes
- Calcul des KPIs (rotation, taux d'imputation, délais moyens)
- Analyse de tendances (comparaison périodes)

**Méthodes principales** :
```java
public class DashboardService {

    // Vue d'ensemble
    DashboardOverviewResponse getOverview(Long companyId, LocalDate startDate, LocalDate endDate);

    // Statistiques mensuelles
    List<MonthlyStatistics> getMonthlyStatistics(Long companyId, int year);

    // Top clients
    List<CustomerDepositStatistics> getTopCustomers(Long companyId, int limit);

    // KPIs
    DepositKPIs calculateKPIs(Long companyId);

    // Tendances
    TrendAnalysis analyzeTrends(Long companyId, LocalDate startDate, LocalDate endDate);

    // Alertes dashboard
    List<DashboardAlert> getActiveAlerts(Long companyId);

    // Prévisions
    ForecastResponse forecastNextMonth(Long companyId);
}
```

**DTOs à créer** :
```java
// DashboardOverviewResponse.java
@Data
public class DashboardOverviewResponse {
    private BigDecimal totalReceived;           // Total reçu
    private BigDecimal totalApplied;            // Total imputé
    private BigDecimal totalAvailable;          // Total disponible
    private BigDecimal averageAmount;           // Montant moyen
    private Integer activeDepositsCount;        // Nb acomptes actifs
    private Double applicationRate;             // Taux imputation (%)
    private Double averageDelayDays;            // Délai moyen (jours)
    private String monthlyTrend;                // Tendance (+15%)
    private Integer alertsCount;                // Nb alertes actives
    private List<CustomerDepositStatistics> topCustomers;
}

// MonthlyStatistics.java
@Data
public class MonthlyStatistics {
    private Integer year;
    private Integer month;
    private String monthName;
    private BigDecimal totalReceived;
    private BigDecimal totalApplied;
    private Integer depositsCount;
    private Integer applicationsCount;
    private Double averageAmount;
}

// DepositKPIs.java
@Data
public class DepositKPIs {
    private Double rotationRate;                // Vitesse d'utilisation
    private BigDecimal cashTiedUp;              // Trésorerie bloquée
    private BigDecimal vatImpact;               // Impact TVA
    private Double healthScore;                 // Score santé (0-100)
    private Integer oldDepositsCount;           // Nb acomptes anciens
    private BigDecimal oldDepositsAmount;       // Montant anciens
}
```

#### 2. Service Export Excel

**📄 `ExcelExportService.java`** (~500 lignes)

**Responsabilités** :
- Génération fichiers Excel multi-feuilles
- Formatage professionnel (couleurs, bordures, fonts)
- Création de graphiques Excel natifs
- Export filtré par critères

**Méthodes principales** :
```java
public class ExcelExportService {

    // Export complet
    byte[] exportDeposits(Long companyId, LocalDate startDate, LocalDate endDate);

    // Export par client
    byte[] exportByCustomer(Long companyId, Long customerId);

    // Export alertes
    byte[] exportAlerts(Long companyId);

    // Méthodes privées pour chaque feuille
    private void createDepositsSheet(Workbook workbook, List<Deposit> deposits);
    private void createStatisticsSheet(Workbook workbook, Statistics stats);
    private void createFinancialAnalysisSheet(Workbook workbook, FinancialAnalysis analysis);
    private void createAlertsSheet(Workbook workbook, List<Alert> alerts);
    private void addChart(Sheet sheet, int startRow, int endRow);
}
```

**Structure du fichier Excel** :
```
📊 acomptes-2025.xlsx
│
├─ 📄 Feuille 1: Liste Acomptes (colonnes: 12)
│  ├─ Numéro (RA-YYYY-NNNNNN)
│  ├─ Date réception
│  ├─ Client
│  ├─ Montant HT
│  ├─ Taux TVA
│  ├─ Montant TVA
│  ├─ Montant TTC
│  ├─ Statut (Disponible, Partiellement imputé, Complètement imputé)
│  ├─ Montant appliqué
│  ├─ Montant restant
│  ├─ Nb imputations
│  └─ Factures liées
│  └─ Totaux automatiques (ligne finale)
│
├─ 📈 Feuille 2: Statistiques Mensuelles
│  ├─ Tableau pivot mois par mois
│  ├─ Graphique en barres (reçu vs imputé)
│  └─ Graphique linéaire (évolution)
│
├─ 💰 Feuille 3: Analyse Financière
│  ├─ Trésorerie bloquée par période
│  ├─ Impact TVA mensuel
│  ├─ Rotation moyenne
│  ├─ Top 10 clients
│  └─ Prévisions (si applicable)
│
└─ ⚠️ Feuille 4: Alertes & Recommandations
   ├─ Acomptes anciens (>90 jours)
   ├─ Montants importants non utilisés
   ├─ Anomalies détectées
   └─ Actions recommandées
```

#### 3. Service Alertes

**📄 `AlertService.java`** (~350 lignes)

**Responsabilités** :
- Détection automatique des situations problématiques
- Génération d'alertes avec niveaux de priorité
- Historique des alertes
- Résolution et archivage

**Types d'alertes** :
```java
public enum AlertType {
    OLD_DEPOSIT,                    // Acompte ancien (>90 jours)
    LARGE_AMOUNT_UNUSED,            // Montant important non utilisé
    CUSTOMER_MISMATCH,              // Client incohérent
    AMOUNT_EXCEEDED,                // Montant dépassé
    VAT_RATE_MISMATCH,              // Taux TVA incohérent
    DUPLICATE_SUSPECTED,            // Doublon suspecté
    ORPHAN_DEPOSIT                  // Acompte orphelin
}

public enum AlertSeverity {
    INFO,       // Bleu - Information
    WARNING,    // Orange - Attention
    ERROR,      // Rouge - Urgent
    CRITICAL    // Rouge foncé - Critique
}
```

**Méthodes principales** :
```java
public class AlertService {

    // Scan et détection
    List<Alert> scanOldDeposits(Long companyId, int thresholdDays);
    List<Alert> scanLargeAmounts(Long companyId, BigDecimal threshold);
    List<Alert> scanAnomalies(Long companyId);

    // Gestion alertes
    Alert createAlert(Long companyId, AlertType type, AlertSeverity severity, String message, Long depositId);
    void resolveAlert(Long alertId, String resolution);
    void dismissAlert(Long alertId);

    // Consultation
    List<Alert> getActiveAlerts(Long companyId);
    List<Alert> getAlertsByType(Long companyId, AlertType type);
    List<Alert> getAlertsBySeverity(Long companyId, AlertSeverity severity);

    // Statistiques
    AlertStatistics getAlertStatistics(Long companyId);
}
```

**Entité Alert** :
```java
@Entity
@Table(name = "deposit_alerts")
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Company company;

    @ManyToOne
    private Deposit deposit;

    @Enumerated(EnumType.STRING)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    private AlertSeverity severity;

    private String message;
    private String details;

    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String resolvedBy;
    private String resolution;

    private Boolean isActive;
    private Boolean isDismissed;
}
```

#### 4. Scheduler Automatique

**📄 `DepositScheduler.java`** (~250 lignes)

**Responsabilités** :
- Exécution tâches périodiques
- Génération rapports automatiques
- Envoi notifications
- Mise à jour statistiques

**Tâches programmées** :
```java
@Component
@EnableScheduling
public class DepositScheduler {

    // Tous les jours à 8h00: Scan acomptes anciens
    @Scheduled(cron = "0 0 8 * * MON-FRI")
    public void scanOldDepositsDaily() {
        log.info("🔍 Scan quotidien des acomptes anciens...");
        // Détecte acomptes >90 jours
        // Génère alertes si nécessaire
    }

    // Tous les jours à 9h00: Rapport quotidien
    @Scheduled(cron = "0 0 9 * * MON-FRI")
    public void generateDailyReport() {
        log.info("📧 Génération rapport quotidien...");
        // Envoie email au comptable
        // Résumé des nouveaux acomptes et imputations
    }

    // Tous les lundis à 9h00: Rapport hebdomadaire
    @Scheduled(cron = "0 0 9 * * MON")
    public void generateWeeklyReport() {
        log.info("📊 Génération rapport hebdomadaire...");
        // Envoie rapport complet avec Excel joint
    }

    // Premier jour du mois à 9h00: Rapport mensuel
    @Scheduled(cron = "0 0 9 1 * ?")
    public void generateMonthlyReport() {
        log.info("📈 Génération rapport mensuel...");
        // Bilan mois écoulé
        // Statistiques complètes
        // Fichier Excel joint
    }

    // Toutes les heures: Rafraîchir vue matérialisée
    @Scheduled(cron = "0 0 * * * ?")
    public void refreshMaterializedView() {
        log.info("♻️ Rafraîchissement vue matérialisée...");
        // REFRESH MATERIALIZED VIEW CONCURRENTLY mv_deposit_application_summary
    }

    // Toutes les 6 heures: Calcul KPIs
    @Scheduled(cron = "0 0 */6 * * ?")
    public void calculateKPIs() {
        log.info("🎯 Calcul KPIs...");
        // Mise à jour indicateurs de performance
    }
}
```

#### 5. Contrôleurs REST

**📄 `DepositDashboardController.java`** (~200 lignes)

**Endpoints** :
```java
@RestController
@RequestMapping("/api/v1/companies/{companyId}/deposits/dashboard")
public class DepositDashboardController {

    @GetMapping("/overview")
    DashboardOverviewResponse getOverview(
        @PathVariable Long companyId,
        @RequestParam(required = false) LocalDate startDate,
        @RequestParam(required = false) LocalDate endDate
    );

    @GetMapping("/monthly-stats")
    List<MonthlyStatistics> getMonthlyStatistics(
        @PathVariable Long companyId,
        @RequestParam int year
    );

    @GetMapping("/top-customers")
    List<CustomerDepositStatistics> getTopCustomers(
        @PathVariable Long companyId,
        @RequestParam(defaultValue = "10") int limit
    );

    @GetMapping("/kpi")
    DepositKPIs getKPIs(@PathVariable Long companyId);

    @GetMapping("/trends")
    TrendAnalysis getTrends(
        @PathVariable Long companyId,
        @RequestParam LocalDate startDate,
        @RequestParam LocalDate endDate
    );

    @GetMapping("/alerts")
    List<DashboardAlert> getActiveAlerts(@PathVariable Long companyId);

    @GetMapping("/forecast")
    ForecastResponse getForecast(@PathVariable Long companyId);
}
```

**📄 `DepositExportController.java`** (~150 lignes)

**Endpoints** :
```java
@RestController
@RequestMapping("/api/v1/companies/{companyId}/deposits/export")
public class DepositExportController {

    @GetMapping("/excel")
    ResponseEntity<byte[]> exportToExcel(
        @PathVariable Long companyId,
        @RequestParam(required = false) LocalDate startDate,
        @RequestParam(required = false) LocalDate endDate,
        @RequestParam(required = false) Long customerId,
        @RequestParam(required = false) String status
    );

    @GetMapping("/excel/customer/{customerId}")
    ResponseEntity<byte[]> exportCustomerDeposits(
        @PathVariable Long companyId,
        @PathVariable Long customerId
    );

    @GetMapping("/excel/alerts")
    ResponseEntity<byte[]> exportAlerts(@PathVariable Long companyId);
}
```

**📄 `DepositAlertController.java`** (~180 lignes)

**Endpoints** :
```java
@RestController
@RequestMapping("/api/v1/companies/{companyId}/deposits/alerts")
public class DepositAlertController {

    @GetMapping
    List<Alert> getActiveAlerts(@PathVariable Long companyId);

    @GetMapping("/{alertId}")
    Alert getAlert(@PathVariable Long companyId, @PathVariable Long alertId);

    @PostMapping("/{alertId}/resolve")
    Alert resolveAlert(
        @PathVariable Long companyId,
        @PathVariable Long alertId,
        @RequestBody ResolveAlertRequest request
    );

    @PostMapping("/{alertId}/dismiss")
    void dismissAlert(@PathVariable Long companyId, @PathVariable Long alertId);

    @GetMapping("/statistics")
    AlertStatistics getStatistics(@PathVariable Long companyId);

    @PostMapping("/scan")
    ScanResult triggerScan(@PathVariable Long companyId);
}
```

#### 6. Migration Base de Données

**📄 `V22__add_alerts_table.sql`** (~80 lignes)

```sql
-- Table des alertes
CREATE TABLE deposit_alerts (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    deposit_id BIGINT,

    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,

    message VARCHAR(500) NOT NULL,
    details TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(255),
    resolution TEXT,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_dismissed BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_alert_company FOREIGN KEY (company_id)
        REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_deposit FOREIGN KEY (deposit_id)
        REFERENCES deposits(id) ON DELETE CASCADE
);

-- Index
CREATE INDEX idx_alerts_company ON deposit_alerts(company_id);
CREATE INDEX idx_alerts_deposit ON deposit_alerts(deposit_id);
CREATE INDEX idx_alerts_active ON deposit_alerts(company_id, is_active)
    WHERE is_active = TRUE;
CREATE INDEX idx_alerts_severity ON deposit_alerts(company_id, severity, created_at DESC);
CREATE INDEX idx_alerts_type ON deposit_alerts(company_id, alert_type);
```

---

### 📊 Cas d'Usage Phase 3

#### Cas 1 : Le Rapport Mensuel Express

**Situation** :
Direction demande le 1er du mois : "Rapport des acomptes du mois dernier"

**Avant Phase 3** :
1. Comptable fait des requêtes SQL manuelles (30 min)
2. Copie dans Excel, formatte (45 min)
3. Crée graphiques manuellement (30 min)
4. Vérifie totaux (15 min)
**Total** : 2 heures

**Avec Phase 3** :
1. Clic sur "Export Excel" avec dates
2. Téléchargement immédiat du fichier
3. Fichier complet avec 4 feuilles + graphiques
**Total** : 5 secondes

**Gain** : 99.93% de temps économisé

#### Cas 2 : L'Acompte Oublié

**Situation** :
Acompte de 5M XAF reçu en janvier, toujours non imputé en juin

**Avant Phase 3** :
- Découverte lors de l'inventaire semestriel
- Client mécontent (pas facturé depuis 5 mois)
- Image ternie

**Avec Phase 3** :
- Jour 90 : Email automatique "Alerte acompte ancien"
- Comptable voit alerte rouge sur dashboard
- Action immédiate : facturation dans la semaine
- Client satisfait (proactivité)

**Gain** : Zéro acompte oublié, relation client préservée

#### Cas 3 : Le Pilotage Financier

**Situation** :
Réunion direction : "Quelle est notre position acomptes ?"

**Avant Phase 3** :
- "Je vais vérifier et je reviens..."
- Réponse 2 jours plus tard
- Décisions retardées

**Avec Phase 3** :
- Ouvre dashboard sur mobile
- Lecture instantanée :
  - 3.5M XAF disponibles
  - 15% de hausse ce mois
  - 2 alertes à traiter
- Décision immédiate

**Gain** : Agilité décisionnelle

---

## 🤖 Phase 4 : Automatisation et Suggestions

### 🎯 Objectifs

1. **Suggestions automatiques** lors de la facturation
2. **Notifications email intelligentes** (clients + comptables)
3. **Recommandations basées sur l'historique**
4. **Automatisation complète du workflow**

---

### 📦 Fichiers à Créer (Phase 4)

#### 1. Service de Suggestions

**📄 `DepositSuggestionService.java`** (~400 lignes)

**Responsabilités** :
- Suggérer automatiquement les acomptes applicables lors de la facturation
- Analyser l'historique client pour recommandations
- Optimiser l'ordre d'imputation (FIFO, montant, date)
- Prévenir les erreurs d'imputation

**Méthodes principales** :
```java
public class DepositSuggestionService {

    /**
     * Suggère les acomptes applicables pour une nouvelle facture.
     * Retourne les acomptes triés par pertinence.
     */
    List<DepositSuggestion> suggestDepositsForInvoice(
        Long companyId,
        Long customerId,
        BigDecimal invoiceAmount
    );

    /**
     * Calcule le plan d'imputation optimal.
     * Exemple: Facture 500k, acomptes 300k + 200k = suggestion complète
     */
    ApplicationPlan calculateOptimalPlan(
        Long companyId,
        Long customerId,
        BigDecimal invoiceAmount
    );

    /**
     * Recommande une stratégie d'imputation basée sur l'historique.
     */
    ImpactationStrategy recommendStrategy(
        Long companyId,
        Long customerId
    );

    /**
     * Alerte si facturation sans utiliser acomptes disponibles.
     */
    SuggestionAlert alertUnusedDeposits(
        Long companyId,
        Long customerId,
        BigDecimal invoiceAmount
    );

    /**
     * Prédictions basées sur patterns.
     */
    List<PredictiveInsight> predictNextDeposits(
        Long companyId,
        Long customerId
    );
}
```

**DTOs** :
```java
// DepositSuggestion.java
@Data
public class DepositSuggestion {
    private Long depositId;
    private String depositNumber;
    private BigDecimal availableAmount;
    private LocalDate depositDate;
    private Integer ageDays;

    private Integer relevanceScore;        // 0-100
    private String reason;                 // "Acompte le plus ancien"
    private Boolean isFullyCovering;       // Couvre toute la facture ?
    private BigDecimal suggestedAmount;    // Montant suggéré à imputer

    private List<String> warnings;         // Alertes éventuelles
}

// ApplicationPlan.java
@Data
public class ApplicationPlan {
    private BigDecimal invoiceAmount;
    private BigDecimal totalAvailable;
    private Boolean isFullyCovered;

    private List<PlannedApplication> applications;
    private BigDecimal remainingDue;       // Après application du plan

    private String recommendation;         // Description du plan
    private Integer confidence;            // 0-100
}

// PlannedApplication.java
@Data
public class PlannedApplication {
    private Long depositId;
    private String depositNumber;
    private BigDecimal amount;
    private Integer order;                 // Ordre d'application (1, 2, 3...)
}
```

**Algorithmes de suggestion** :

```java
// Algorithme 1: FIFO (First In, First Out)
// Impute les acomptes les plus anciens en premier
public ApplicationPlan calculateFIFOPlan() {
    List<Deposit> deposits = getSortedByDateAsc();
    // Applique les acomptes par ordre chronologique
}

// Algorithme 2: Montant optimal
// Minimise le nombre d'imputations
public ApplicationPlan calculateOptimalAmountPlan() {
    List<Deposit> deposits = getSortedByAmountDesc();
    // Cherche la combinaison avec le moins d'imputations
}

// Algorithme 3: Équilibré
// Balance ancienneté et montant
public ApplicationPlan calculateBalancedPlan() {
    List<Deposit> deposits = getSortedByScoreDesc();
    // Score = (ancienneté * 0.6) + (montant * 0.4)
}

// Algorithme 4: Machine Learning (optionnel - Phase 5)
// Apprend des patterns historiques du client
public ApplicationPlan calculateMLPlan() {
    // Utilise l'historique pour prédire la meilleure stratégie
}
```

#### 2. Service de Notifications Email

**📄 `EmailNotificationService.java`** (~500 lignes)

**Responsabilités** :
- Envoi emails transactionnels (reçu d'acompte, imputation)
- Envoi emails d'alerte (acomptes anciens)
- Emails périodiques (rapports quotidiens/hebdomadaires)
- Templates HTML professionnels

**Configuration** :
```yaml
# application.yml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${SMTP_USERNAME}
    password: ${SMTP_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

predykt:
  email:
    from: noreply@predykt.com
    accountant: comptable@votre-entreprise.com
    enabled: true
    templates:
      path: classpath:/templates/emails/
```

**Méthodes principales** :
```java
public class EmailNotificationService {

    // Emails clients
    void sendDepositReceivedEmail(Deposit deposit);
    void sendDepositAppliedEmail(Deposit deposit, Invoice invoice);
    void sendDepositReminderEmail(Deposit deposit);

    // Emails comptables
    void sendDailyReportEmail(Long companyId, DailyReport report);
    void sendWeeklyReportEmail(Long companyId, WeeklyReport report);
    void sendMonthlyReportEmail(Long companyId, MonthlyReport report);
    void sendAlertEmail(Alert alert);

    // Emails direction
    void sendExecutiveSummaryEmail(Long companyId, ExecutiveSummary summary);

    // Méthodes utilitaires
    void sendEmailWithAttachment(String to, String subject, String htmlBody, byte[] attachment, String filename);
    String renderTemplate(String templateName, Map<String, Object> variables);
}
```

**Templates Email (HTML)** :

```html
<!-- templates/emails/deposit-received.html (Thymeleaf) -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Reçu d'Acompte</title>
    <style>
        body { font-family: Arial, sans-serif; }
        .header { background-color: #2962FF; color: white; padding: 20px; }
        .amount { font-size: 24px; font-weight: bold; color: #22C55E; }
        .footer { background-color: #F8FAFC; padding: 20px; font-size: 12px; }
    </style>
</head>
<body>
    <div class="header">
        <h1>Reçu d'Acompte</h1>
        <p th:text="${depositNumber}">RA-2025-000001</p>
    </div>

    <div style="padding: 20px;">
        <p>Bonjour <span th:text="${customerName}">Client</span>,</p>

        <p>Nous avons bien reçu votre acompte d'un montant de :</p>

        <p class="amount" th:text="${amountTtc} + ' XAF'">119 250 XAF</p>

        <table style="width: 100%; margin: 20px 0;">
            <tr>
                <td><strong>Montant HT:</strong></td>
                <td th:text="${amountHt} + ' XAF'">100 000 XAF</td>
            </tr>
            <tr>
                <td><strong>TVA (19.25%):</strong></td>
                <td th:text="${vatAmount} + ' XAF'">19 250 XAF</td>
            </tr>
            <tr>
                <td><strong>Date de réception:</strong></td>
                <td th:text="${depositDate}">11/12/2025</td>
            </tr>
        </table>

        <p>Cet acompte sera imputé sur vos prochaines factures.</p>

        <p>Pour toute question, contactez-nous à <a th:href="'mailto:' + ${companyEmail}" th:text="${companyEmail}">contact@entreprise.com</a></p>

        <p>Cordialement,<br>
        <span th:text="${companyName}">Votre Entreprise</span></p>
    </div>

    <div class="footer">
        <p>Cet email a été généré automatiquement par PREDYKT Accounting System</p>
        <p>Conforme OHADA SYSCOHADA - CGI Cameroun</p>
    </div>
</body>
</html>
```

**Autres templates** :
- `deposit-applied.html` : Notification imputation sur facture
- `deposit-reminder.html` : Rappel acompte ancien
- `daily-report.html` : Rapport quotidien comptable
- `alert-notification.html` : Notification d'alerte
- `executive-summary.html` : Résumé direction

#### 3. Service de Recommandations

**📄 `RecommendationService.java`** (~300 lignes)

**Responsabilités** :
- Analyser l'historique du client
- Identifier des patterns de comportement
- Recommander des actions
- Prévoir les besoins futurs

**Méthodes principales** :
```java
public class RecommendationService {

    /**
     * Recommande de demander un acompte pour une commande.
     */
    DepositRecommendation recommendDepositRequest(
        Long companyId,
        Long customerId,
        BigDecimal orderAmount
    );

    /**
     * Analyse le comportement du client.
     */
    CustomerBehaviorAnalysis analyzeCustomerBehavior(
        Long companyId,
        Long customerId
    );

    /**
     * Recommande le montant d'acompte à demander.
     */
    BigDecimal recommendDepositAmount(
        Long companyId,
        Long customerId,
        BigDecimal orderAmount
    );

    /**
     * Identifie les clients à risque (beaucoup d'acomptes non utilisés).
     */
    List<RiskCustomer> identifyRiskCustomers(Long companyId);

    /**
     * Recommandations pour optimiser la trésorerie.
     */
    List<CashOptimizationTip> getCashOptimizationTips(Long companyId);
}
```

**DTOs** :
```java
// DepositRecommendation.java
@Data
public class DepositRecommendation {
    private Boolean shouldRequestDeposit;
    private BigDecimal recommendedAmount;
    private Double recommendedPercentage;

    private String reason;
    private Integer confidence;           // 0-100

    private CustomerRiskLevel riskLevel;
    private List<String> factors;         // Facteurs de décision
}

// CustomerBehaviorAnalysis.java
@Data
public class CustomerBehaviorAnalysis {
    private Long customerId;
    private String customerName;

    private Integer totalOrders;
    private BigDecimal averageOrderAmount;

    private Integer depositsReceived;
    private BigDecimal averageDepositAmount;
    private Double averageDepositPercentage;

    private Double averagePaymentDelayDays;
    private Double depositUtilizationRate;

    private CustomerRiskLevel riskLevel;
    private String behaviorPattern;       // "Ponctuel", "Lent", "Risqué"

    private List<String> insights;
    private List<String> recommendations;
}
```

**Exemples de recommandations** :
```
✅ Recommandation 1: Demander acompte
├─ Client : SARL BELTEC
├─ Commande : 5 000 000 XAF
├─ Acompte suggéré : 30% (1 500 000 XAF)
├─ Raison : Client historiquement ponctuel, montant important
└─ Confiance : 85%

⚠️ Recommandation 2: Surveiller client
├─ Client : CONSTRUCTION SA
├─ Problème : 3 acomptes non utilisés depuis >120 jours
├─ Montant bloqué : 4 500 000 XAF
├─ Action : Contacter pour facturer ou rembourser
└─ Confiance : 95%

💡 Recommandation 3: Optimisation trésorerie
├─ Observation : 8 clients avec acomptes anciens
├─ Total bloqué : 12 000 000 XAF
├─ Action : Campagne de facturation groupée
└─ Gain potentiel : Libérer 12M XAF de trésorerie
```

#### 4. Workflow d'Automatisation

**📄 `DepositAutomationService.java`** (~350 lignes)

**Responsabilités** :
- Orchestrer le workflow complet
- Déclencher automatiquement les actions
- Suivre l'exécution des tâches
- Gérer les erreurs et retry

**Workflow automatique lors de la création de facture** :
```java
@Service
public class DepositAutomationService {

    @EventListener
    public void onInvoiceCreated(InvoiceCreatedEvent event) {
        Long companyId = event.getCompanyId();
        Invoice invoice = event.getInvoice();

        // 1. Vérifier si le client a des acomptes disponibles
        List<Deposit> availableDeposits = depositRepository
            .findByCompanyAndCustomerAndIsAppliedFalse(
                invoice.getCompany(),
                invoice.getCustomer()
            );

        if (availableDeposits.isEmpty()) {
            return; // Pas d'acomptes, rien à faire
        }

        // 2. Calculer le plan d'imputation optimal
        ApplicationPlan plan = suggestionService
            .calculateOptimalPlan(
                companyId,
                invoice.getCustomer().getId(),
                invoice.getTotalTtc()
            );

        // 3. Créer une notification pour le comptable
        Notification notification = Notification.builder()
            .type(NotificationType.DEPOSIT_SUGGESTION)
            .title("Acomptes disponibles pour facturation")
            .message(String.format(
                "La facture %s peut être payée avec %d acompte(s) disponibles",
                invoice.getInvoiceNumber(),
                plan.getApplications().size()
            ))
            .data(plan)
            .build();

        notificationService.send(notification);

        // 4. Envoyer email si configuré
        if (emailEnabled) {
            emailService.sendDepositSuggestionEmail(
                invoice,
                availableDeposits,
                plan
            );
        }

        // 5. Si auto-apply activé, appliquer automatiquement
        if (autoApplyEnabled && plan.getConfidence() > 90) {
            log.info("🤖 Application automatique des acomptes...");
            for (PlannedApplication application : plan.getApplications()) {
                depositApplicationService.applyPartially(
                    companyId,
                    application.getDepositId(),
                    invoice.getId(),
                    application.getAmount(),
                    "AUTO-SYSTEM",
                    "Application automatique selon plan optimal"
                );
            }

            // Envoyer confirmation
            emailService.sendAutoApplicationConfirmation(invoice, plan);
        }
    }
}
```

**Workflow automatique de rappel** :
```java
@Scheduled(cron = "0 0 10 * * MON") // Tous les lundis à 10h
public void sendWeeklyReminders() {
    log.info("📧 Envoi rappels hebdomadaires...");

    List<Company> companies = companyRepository.findAll();

    for (Company company : companies) {
        // Trouver acomptes anciens (>60 jours)
        List<Deposit> oldDeposits = depositRepository
            .findOldDeposits(company, 60);

        if (oldDeposits.isEmpty()) {
            continue;
        }

        // Grouper par client
        Map<Customer, List<Deposit>> byCustomer = oldDeposits.stream()
            .collect(Collectors.groupingBy(Deposit::getCustomer));

        // Envoyer un email par client
        for (Map.Entry<Customer, List<Deposit>> entry : byCustomer.entrySet()) {
            Customer customer = entry.getKey();
            List<Deposit> deposits = entry.getValue();

            emailService.sendCustomerReminderEmail(
                customer,
                deposits,
                "Rappel : acomptes en attente d'imputation"
            );
        }

        // Notifier le comptable
        emailService.sendAccountantSummaryEmail(
            company,
            oldDeposits,
            "Résumé hebdomadaire des acomptes anciens"
        );
    }
}
```

#### 5. Contrôleurs REST

**📄 `DepositSuggestionController.java`** (~150 lignes)

**Endpoints** :
```java
@RestController
@RequestMapping("/api/v1/companies/{companyId}/deposits/suggestions")
public class DepositSuggestionController {

    @GetMapping("/for-invoice")
    List<DepositSuggestion> getSuggestionsForInvoice(
        @PathVariable Long companyId,
        @RequestParam Long customerId,
        @RequestParam BigDecimal invoiceAmount
    );

    @GetMapping("/plan")
    ApplicationPlan getOptimalPlan(
        @PathVariable Long companyId,
        @RequestParam Long customerId,
        @RequestParam BigDecimal invoiceAmount
    );

    @PostMapping("/auto-apply")
    List<DepositApplicationResponse> autoApplyPlan(
        @PathVariable Long companyId,
        @RequestBody ApplicationPlan plan
    );
}
```

**📄 `DepositRecommendationController.java`** (~120 lignes)

**Endpoints** :
```java
@RestController
@RequestMapping("/api/v1/companies/{companyId}/deposits/recommendations")
public class DepositRecommendationController {

    @GetMapping("/customer/{customerId}")
    CustomerBehaviorAnalysis getCustomerAnalysis(
        @PathVariable Long companyId,
        @PathVariable Long customerId
    );

    @GetMapping("/request-deposit")
    DepositRecommendation getDepositRecommendation(
        @PathVariable Long companyId,
        @RequestParam Long customerId,
        @RequestParam BigDecimal orderAmount
    );

    @GetMapping("/risk-customers")
    List<RiskCustomer> getRiskCustomers(@PathVariable Long companyId);

    @GetMapping("/cash-optimization")
    List<CashOptimizationTip> getCashOptimizationTips(@PathVariable Long companyId);
}
```

#### 6. Configuration

**📄 `application-automation.yml`** (~50 lignes)

```yaml
predykt:
  deposit:
    automation:
      enabled: true

      # Suggestions automatiques
      suggestions:
        enabled: true
        confidence-threshold: 75        # Seuil de confiance (0-100)

      # Application automatique
      auto-apply:
        enabled: false                   # Désactivé par défaut (sécurité)
        confidence-threshold: 95         # Très haute confiance requise
        max-amount: 1000000              # Limite max auto (1M XAF)

      # Notifications
      notifications:
        email:
          enabled: true
          accountant: comptable@entreprise.com
          cc-management: false

        # Fréquence des rapports
        reports:
          daily: true
          weekly: true
          monthly: true

      # Alertes
      alerts:
        old-deposits:
          enabled: true
          thresholds:
            info: 30                     # Jours
            warning: 60
            error: 90
            critical: 120

        large-amounts:
          enabled: true
          threshold: 1000000             # XAF

      # Rappels clients
      reminders:
        enabled: true
        frequency: weekly                # weekly, biweekly, monthly
        day: MONDAY
        time: "10:00"
```

---

### 📊 Cas d'Usage Phase 4

#### Cas 1 : La Facturation Intelligente

**Situation** :
Comptable crée une facture de 500 000 XAF pour client SARL BELTEC

**Avant Phase 4** :
1. Comptable crée la facture
2. Se souvient (ou pas) qu'il y a des acomptes
3. Cherche manuellement les acomptes disponibles
4. Impute manuellement
**Temps** : 10-15 minutes

**Avec Phase 4** :
1. Comptable crée la facture
2. **🤖 Notification automatique** : "3 acomptes disponibles pour ce client (total 600k XAF)"
3. **Plan suggéré** : Imputer 500k XAF (2 acomptes)
4. Clic sur "Appliquer le plan suggéré"
**Temps** : 30 secondes

**Gain** : 95% de temps économisé, zéro oubli

#### Cas 2 : Le Client Proactif

**Situation** :
Client CONSTRUCTION SA a versé un acompte il y a 3 mois

**Avant Phase 4** :
- Client se demande : "Pourquoi pas encore facturé ?"
- Appelle l'entreprise
- Impression de désorganisation

**Avec Phase 4** :
- **Jour 60** : Email automatique au client
  ```
  Bonjour,

  Votre acompte de 2 500 000 XAF (RA-2025-000015)
  est toujours en attente d'imputation.

  Nous vous contacterons prochainement pour finaliser
  votre facturation.

  Merci de votre confiance.
  ```
- Client rassuré (suivi proactif)
- Image professionnelle renforcée

#### Cas 3 : L'Optimisation de Trésorerie

**Situation** :
Fin de trimestre, besoin de libérer de la trésorerie

**Avant Phase 4** :
- Direction ne sait pas combien est "bloqué"
- Pas de visibilité sur les actions possibles

**Avec Phase 4** :
- Dashboard affiche : **12M XAF en acomptes anciens**
- Recommandations automatiques :
  ```
  💡 8 clients ont des acomptes >90 jours
  💡 Action : Campagne de facturation groupée
  💡 Gain potentiel : Libérer 12M XAF

  [Générer les factures suggérées]
  ```
- Clic sur le bouton → 8 factures créées automatiquement
- Emails envoyés aux clients
- Trésorerie libérée en 2 semaines

**Gain** : 12M XAF libérés

#### Cas 4 : Le Commercial Éclairé

**Situation** :
Commercial négocie une commande de 10M XAF avec nouveau client

**Avant Phase 4** :
- Commercial ne sait pas s'il doit demander un acompte
- Décision au feeling

**Avec Phase 4** :
- Commercial ouvre le CRM
- **Recommandation IA** affichée :
  ```
  ⚠️ Nouveau client, montant important (10M XAF)

  Recommandation : Demander acompte 40% (4M XAF)

  Raisons :
  - Client inconnu (risque élevé)
  - Montant > moyenne entreprise (2.5M)
  - Secteur construction (paiements souvent lents)

  Confiance : 92%
  ```
- Commercial demande 40% d'acompte
- Risque mitigé

**Gain** : Sécurisation des ventes

---

## 📅 Ordre d'Implémentation Recommandé

### Approche Progressive (12 étapes)

#### Semaine 1 : Phase 3 - Fondations

**Jour 1-2 : Dashboard & Statistiques**
1. Créer `DashboardService` avec calculs de base
2. Créer DTOs (`DashboardOverviewResponse`, etc.)
3. Créer `DepositDashboardController`
4. Tester avec Postman

**Jour 3-4 : Export Excel**
5. Créer `ExcelExportService`
6. Implémenter les 4 feuilles Excel
7. Créer `DepositExportController`
8. Tester téléchargement

**Jour 5-6 : Alertes**
9. Migration V22 (table `deposit_alerts`)
10. Créer entité `Alert`
11. Créer `AlertService` avec détection
12. Créer `DepositAlertController`

**Jour 7 : Scheduler**
13. Créer `DepositScheduler`
14. Implémenter tâches périodiques
15. Tester exécutions

#### Semaine 2 : Phase 4 - Intelligence

**Jour 1-2 : Suggestions**
16. Créer `DepositSuggestionService`
17. Implémenter algorithmes (FIFO, optimal, équilibré)
18. Créer DTOs de suggestion
19. Créer `DepositSuggestionController`

**Jour 3-4 : Notifications Email**
20. Configurer Spring Mail
21. Créer `EmailNotificationService`
22. Créer templates HTML (Thymeleaf)
23. Tester envois email

**Jour 5-6 : Recommandations**
24. Créer `RecommendationService`
25. Implémenter analyses comportementales
26. Créer `DepositRecommendationController`
27. Tester recommandations

**Jour 7 : Automatisation**
28. Créer `DepositAutomationService`
29. Implémenter workflow auto
30. Configuration finale
31. Tests end-to-end

---

## ⏱️ Estimation Temps & Ressources

### Phase 3 : Reporting et Alertes

| Tâche | Complexité | Temps Estimé |
|-------|------------|--------------|
| DashboardService | Moyenne | 4-6 heures |
| ExcelExportService | Moyenne | 6-8 heures |
| AlertService | Faible | 3-4 heures |
| Scheduler | Faible | 2-3 heures |
| Controllers | Faible | 2-3 heures |
| Migration BDD | Faible | 1 heure |
| Tests | Moyenne | 3-4 heures |
| **TOTAL Phase 3** | - | **21-29 heures** |

### Phase 4 : Automatisation et Suggestions

| Tâche | Complexité | Temps Estimé |
|-------|------------|--------------|
| SuggestionService | Moyenne | 5-7 heures |
| EmailNotificationService | Moyenne | 6-8 heures |
| Templates Email | Faible | 3-4 heures |
| RecommendationService | Moyenne | 4-6 heures |
| AutomationService | Moyenne | 4-5 heures |
| Controllers | Faible | 2-3 heures |
| Configuration | Faible | 2 heures |
| Tests | Moyenne | 4-5 heures |
| **TOTAL Phase 4** | - | **30-40 heures** |

### **TOTAL PHASES 3 & 4** : **51-69 heures** (~1.5-2 semaines)

---

## 💰 Bénéfices Attendus

### Gains Quantifiables

| Bénéfice | Avant | Après | Gain |
|----------|-------|-------|------|
| **Temps génération rapport** | 2 heures | 5 secondes | 99.93% |
| **Acomptes oubliés/an** | 5-10 | 0 | 100% |
| **Temps facturation (avec acomptes)** | 15 min | 30 sec | 96.67% |
| **Délai détection problème** | 6 mois | 1 jour | 99.45% |
| **Trésorerie optimisée** | - | +15-20% | - |
| **Temps administratif/semaine** | 8 heures | 3 heures | 62.5% |

### ROI Estimé

**Hypothèses** :
- Comptable : 25€/heure
- 5 heures/semaine économisées
- Éviter 3 acomptes oubliés/an (moyenne 2M XAF chacun)

**Calcul ROI annuel** :
```
Économies temps : 5h/semaine × 52 semaines × 25€ = 6 500€/an
Éviter oublis : 3 × 2M XAF × impact financier (5%) = 300 000 XAF ≈ 450€
Optimisation trésorerie : ~2 000€/an

TOTAL : ~9 000€/an

Investissement développement : ~60 heures × 50€ = 3 000€

ROI = (9 000 - 3 000) / 3 000 = 200%
Retour sur investissement : 4 mois
```

---

## 🔧 Prérequis Techniques

### Dépendances Maven

```xml
<!-- Email -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>

<!-- Thymeleaf (templates email) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>

<!-- Apache POI (déjà présent) -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>

<!-- Scheduler (déjà présent dans Spring Boot) -->
<!-- Spring @EnableScheduling built-in -->
```

### Configuration Serveur

**Serveur SMTP requis** :
- Gmail : `smtp.gmail.com:587` (TLS)
- SendGrid : `smtp.sendgrid.net:587`
- AWS SES : `email-smtp.region.amazonaws.com:587`
- Mailgun : `smtp.mailgun.org:587`

**Variables d'environnement** :
```bash
SMTP_USERNAME=noreply@predykt.com
SMTP_PASSWORD=xxxxxxxx
EMAIL_FROM=noreply@predykt.com
EMAIL_ACCOUNTANT=comptable@entreprise.com
ENABLE_EMAIL_NOTIFICATIONS=true
ENABLE_AUTO_APPLY=false  # Sécurité
```

### Base de Données

**Migration V22 requise** :
```bash
mvn flyway:migrate
```

**Vérifications** :
```sql
-- Vérifier table alertes créée
SELECT COUNT(*) FROM deposit_alerts;

-- Vérifier vue matérialisée existe
SELECT * FROM mv_deposit_application_summary LIMIT 1;
```

---

## 📚 Documentation Additionnelle

### Guides à Créer Après Implémentation

1. **GUIDE_DASHBOARD.md**
   - Comment lire le dashboard
   - Interprétation des KPIs
   - Actions recommandées

2. **GUIDE_ALERTES.md**
   - Types d'alertes
   - Seuils configurables
   - Procédures de résolution

3. **GUIDE_AUTOMATISATION.md**
   - Configuration des règles
   - Activation/désactivation
   - Logs et monitoring

4. **GUIDE_EMAILS.md**
   - Configuration SMTP
   - Personnalisation templates
   - Dépannage

---

## ✅ Checklist de Déploiement

### Avant le Déploiement

- [ ] Phase 2 complète et testée
- [ ] Serveur SMTP configuré
- [ ] Variables d'environnement définies
- [ ] Backup base de données
- [ ] Tests unitaires passent (>80% couverture)

### Déploiement Phase 3

- [ ] Migration V22 exécutée
- [ ] Table `deposit_alerts` créée
- [ ] DashboardService testé
- [ ] Export Excel fonctionnel
- [ ] Alertes détectées
- [ ] Scheduler actif (vérifier logs)

### Déploiement Phase 4

- [ ] Configuration email validée
- [ ] Templates email testés
- [ ] Suggestions fonctionnelles
- [ ] Notifications reçues
- [ ] Workflow automatique testé
- [ ] Auto-apply DÉSACTIVÉ par défaut

### Après Déploiement

- [ ] Surveiller logs pendant 48h
- [ ] Vérifier exécution schedulers
- [ ] Valider emails reçus
- [ ] Tester dashboard avec vrais utilisateurs
- [ ] Former comptables
- [ ] Documenter procédures

---

## 🆘 Support & Maintenance

### Monitoring

**Logs à surveiller** :
```
✅ [DepositScheduler] Scan quotidien exécuté : 15 acomptes analysés
✅ [EmailService] Email envoyé à comptable@entreprise.com
✅ [AlertService] 2 nouvelles alertes créées (WARNING)
✅ [DashboardService] KPIs calculés en 234ms
```

**Erreurs courantes** :
```
❌ [EmailService] Échec envoi email : SMTP timeout
→ Vérifier configuration SMTP, credentials

❌ [DepositScheduler] Erreur lors du scan : NullPointerException
→ Vérifier données intégrité base

❌ [ExcelExportService] OutOfMemoryError
→ Augmenter heap JVM : -Xmx2g
```

### Performance

**Optimisations** :
- Vue matérialisée rafraîchie toutes les heures (ajustable)
- Requêtes dashboard avec index appropriés
- Export Excel limité à 10 000 lignes (pagination si plus)
- Cache Redis pour KPIs (TTL 15 min)

---

## 🎓 Formation Utilisateurs

### Comptables

**Session 1 : Dashboard** (30 min)
- Navigation dans le dashboard
- Lecture des KPIs
- Interprétation des alertes

**Session 2 : Alertes & Actions** (30 min)
- Répondre aux alertes
- Résoudre les problèmes
- Marquer comme traité

**Session 3 : Exports & Rapports** (20 min)
- Générer exports Excel
- Lire les rapports
- Partager avec direction

### Direction

**Session : Pilotage Financier** (45 min)
- Vue d'ensemble trésorerie acomptes
- Indicateurs de performance
- Prise de décision basée sur données

---

## 🔮 Phase 5 (Future - Optionnelle)

### Machine Learning Avancé

**Prévisions basées sur IA** :
- Prédiction des acomptes futurs par client
- Détection d'anomalies avec algorithmes ML
- Optimisation des stratégies d'imputation
- Scoring de risque client

**Technologies** :
- TensorFlow ou PyTorch
- Python service externe (API REST)
- Intégration via microservice

**Temps estimé** : 40-60 heures

---

## 📞 Contacts

**Questions techniques** : Consulter le code source avec commentaires détaillés
**Questions OHADA** : Voir `CONSULTATION_COMPTABLE_ACOMPTES.md`
**Support implémentation** : Issues GitHub

---

**Version** : 1.0.0
**Date** : 11/12/2025
**Status** : 📋 **GUIDE COMPLET - PRÊT À IMPLÉMENTER**
**Prérequis** : ✅ Phase 2 terminée

🚀 **Prêt à transformer votre système d'acomptes en plateforme intelligente !**
