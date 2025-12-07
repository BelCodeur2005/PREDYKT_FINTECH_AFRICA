# 🤖 ML MATCHING SYSTEM - ÉTAT D'IMPLÉMENTATION

**Date:** 2025-01-07
**Session:** 1
**Statut global:** 30% complété

---

## ✅ FICHIERS CRÉÉS (6/12)

### 1. Migrations SQL
- ✅ `V16__add_ml_matching_system.sql` - Tables ML complètes
- ✅ `V17__add_internal_notes_to_invoices_bills.sql` - Notes internes

### 2. Documentation
- ✅ `ML_MATCHING_SYSTEM_README.md` - Guide complet (architecture, installation, utilisation)
- ✅ `ML_IMPLEMENTATION_STATUS.md` - Ce fichier

### 3. Entités ML (`src/main/java/com/predykt/accounting/domain/entity/ml/`)
- ✅ `MLTrainingData.java` - Données d'apprentissage
- ✅ `MLModel.java` - Registry des modèles ML
- ✅ `MLPredictionLog.java` - Log des prédictions

### 4. Enums
- ✅ `MLModelStatus.java` - TRAINING, TRAINED, DEPLOYED, DEPRECATED

---

## ⏳ FICHIERS À CRÉER (18 fichiers restants)

### A. Repositories (6 fichiers)

**1. `MLTrainingDataRepository.java`**
```java
package com.predykt.accounting.repository.ml;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.ml.MLTrainingData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MLTrainingDataRepository extends JpaRepository<MLTrainingData, Long> {

    List<MLTrainingData> findByCompanyOrderByCreatedAtDesc(Company company);

    @Query("SELECT COUNT(t) FROM MLTrainingData t WHERE t.company = :company")
    long countByCompany(@Param("company") Company company);

    @Query("SELECT t FROM MLTrainingData t WHERE t.company = :company " +
           "AND t.createdAt >= :since " +
           "ORDER BY t.createdAt DESC")
    List<MLTrainingData> findRecentByCompany(
        @Param("company") Company company,
        @Param("since") LocalDateTime since
    );

    @Query("SELECT t FROM MLTrainingData t WHERE t.company = :company " +
           "ORDER BY t.createdAt DESC")
    Page<MLTrainingData> findByCompanyPaginated(
        @Param("company") Company company,
        Pageable pageable
    );

    // Récupérer données d'entraînement filtrées
    @Query("SELECT t FROM MLTrainingData t WHERE t.company = :company " +
           "AND t.wasAccepted IS NOT NULL " +
           "ORDER BY t.createdAt DESC")
    List<MLTrainingData> findUsableTrainingData(@Param("company") Company company);
}
```

**2. `MLModelRepository.java`**
```java
package com.predykt.accounting.repository.ml;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.ml.MLModel;
import com.predykt.accounting.domain.enums.MLModelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MLModelRepository extends JpaRepository<MLModel, Long> {

    Optional<MLModel> findByCompanyAndIsActiveTrue(Company company);

    List<MLModel> findByCompanyOrderByCreatedAtDesc(Company company);

    List<MLModel> findByCompanyAndStatus(Company company, MLModelStatus status);

    @Query("SELECT m FROM MLModel m WHERE m.company = :company " +
           "AND m.modelName = :modelName " +
           "ORDER BY m.createdAt DESC")
    List<MLModel> findVersionHistory(
        @Param("company") Company company,
        @Param("modelName") String modelName
    );

    @Query("SELECT m FROM MLModel m WHERE m.isActive = true " +
           "AND m.status = 'DEPLOYED' " +
           "ORDER BY m.accuracy DESC")
    List<MLModel> findAllActiveModels();
}
```

**3. `MLPredictionLogRepository.java`**
```java
package com.predykt.accounting.repository.ml;

import com.predykt.accounting.domain.entity.BankReconciliation;
import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.ml.MLPredictionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MLPredictionLogRepository extends JpaRepository<MLPredictionLog, Long> {

    List<MLPredictionLog> findByReconciliationOrderByPredictedAtDesc(
        BankReconciliation reconciliation
    );

    @Query("SELECT COUNT(p) FROM MLPredictionLog p " +
           "WHERE p.company = :company " +
           "AND p.wasCorrect = true " +
           "AND p.predictedAt >= :since")
    long countCorrectPredictions(
        @Param("company") Company company,
        @Param("since") LocalDateTime since
    );

    @Query("SELECT COUNT(p) FROM MLPredictionLog p " +
           "WHERE p.company = :company " +
           "AND p.actualOutcome IS NOT NULL " +
           "AND p.predictedAt >= :since")
    long countValidatedPredictions(
        @Param("company") Company company,
        @Param("since") LocalDateTime since
    );

    @Query("SELECT AVG(p.predictionTimeMs) FROM MLPredictionLog p " +
           "WHERE p.company = :company " +
           "AND p.predictedAt >= :since")
    Double calculateAverageLatency(
        @Param("company") Company company,
        @Param("since") LocalDateTime since
    );
}
```

**4-6.** Créer aussi `MLFeatureImportanceRepository`, `MLMonitoringMetricsRepository` si nécessaire (optionnel pour MVP)

---

### B. DTOs (4 fichiers)

**1. `MatchFeatures.java`** (DTO pour features ML)
```java
package com.predykt.accounting.dto.ml;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchFeatures {
    // Features numériques
    private Double amountDifference;
    private Long dateDiffDays;
    private Double textSimilarity;
    private Double amountRatio;

    // Features binaires (0/1)
    private Double sameSense;
    private Double referenceMatch;
    private Double isRoundNumber;
    private Double isMonthEnd;

    // Features catégorielles
    private Double dayOfWeekBT;
    private Double dayOfWeekGL;

    // Features historiques
    private Double historicalMatchRate;
    private Double avgDaysHistorical;

    // Conversion vers array pour Smile ML
    public double[] toArray() {
        return new double[] {
            amountDifference,
            dateDiffDays.doubleValue(),
            textSimilarity,
            amountRatio,
            sameSense,
            referenceMatch,
            isRoundNumber,
            isMonthEnd,
            dayOfWeekBT,
            dayOfWeekGL,
            historicalMatchRate,
            avgDaysHistorical
        };
    }

    // Noms des features
    public static String[] getFeatureNames() {
        return new String[] {
            "amount_difference",
            "date_diff_days",
            "text_similarity",
            "amount_ratio",
            "same_sense",
            "reference_match",
            "is_round_number",
            "is_month_end",
            "day_of_week_bt",
            "day_of_week_gl",
            "historical_match_rate",
            "avg_days_historical"
        };
    }

    // Conversion vers Map<String, Object> pour JSON
    public java.util.Map<String, Object> toMap() {
        String[] names = getFeatureNames();
        double[] values = toArray();
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        for (int i = 0; i < names.length; i++) {
            map.put(names[i], values[i]);
        }
        return map;
    }
}
```

**2. `MLPredictionResult.java`**
```java
package com.predykt.accounting.dto.ml;

import com.predykt.accounting.domain.entity.BankTransaction;
import com.predykt.accounting.domain.entity.GeneralLedger;
import lombok.*;

@Data
@Builder
public class MLPredictionResult {
    private BankTransaction bankTransaction;
    private GeneralLedger glEntry;
    private Double confidenceScore;  // 0-100
    private MatchFeatures features;
    private String modelVersion;
    private Long predictionTimeMs;
    private String explanation;  // Pourquoi ce match ?
}
```

**3-4.** `MLModelMetricsDTO.java`, `MLTrainingRequestDTO.java`

---

### C. Services ML (8 fichiers critiques)

#### **1. MLFeatureExtractor.java** ⭐ PRIORITAIRE
```java
package com.predykt.accounting.service.ml;

import com.predykt.accounting.domain.entity.BankTransaction;
import com.predykt.accounting.domain.entity.GeneralLedger;
import com.predykt.accounting.dto.ml.MatchFeatures;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

/**
 * Extracteur de features ML pour matching bancaire
 * Convertit (BankTransaction, GeneralLedger) → Features numériques
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MLFeatureExtractor {

    public MatchFeatures extract(BankTransaction bt, GeneralLedger gl) {
        return MatchFeatures.builder()
            // 1. Différence de montant (normalisée)
            .amountDifference(calculateAmountDifference(bt, gl))

            // 2. Différence de jours
            .dateDiffDays(ChronoUnit.DAYS.between(bt.getTransactionDate(), gl.getEntryDate()))

            // 3. Similarité textuelle (simple)
            .textSimilarity(calculateTextSimilarity(bt.getDescription(), gl.getDescription()))

            // 4. Ratio de montant
            .amountRatio(calculateAmountRatio(bt, gl))

            // 5. Même sens débit/crédit
            .sameSense(sameSense(bt, gl) ? 1.0 : 0.0)

            // 6. Référence identique
            .referenceMatch(referenceMatch(bt, gl) ? 1.0 : 0.0)

            // 7. Montant rond
            .isRoundNumber(isRoundNumber(bt.getAmount()) ? 1.0 : 0.0)

            // 8. Fin de mois
            .isMonthEnd(isMonthEnd(bt.getTransactionDate()) ? 1.0 : 0.0)

            // 9-10. Jour de la semaine
            .dayOfWeekBT((double) bt.getTransactionDate().getDayOfWeek().getValue())
            .dayOfWeekGL((double) gl.getEntryDate().getDayOfWeek().getValue())

            // 11-12. Historique (à implémenter)
            .historicalMatchRate(0.5)  // TODO: calculer historique
            .avgDaysHistorical(30.0)   // TODO: calculer délai moyen

            .build();
    }

    private Double calculateAmountDifference(BankTransaction bt, GeneralLedger gl) {
        BigDecimal btAmount = bt.getAmount().abs();
        BigDecimal glAmount = gl.getDebitAmount().subtract(gl.getCreditAmount()).abs();
        return btAmount.subtract(glAmount).abs().doubleValue();
    }

    private Double calculateAmountRatio(BankTransaction bt, GeneralLedger gl) {
        BigDecimal btAmount = bt.getAmount().abs();
        BigDecimal glAmount = gl.getDebitAmount().subtract(gl.getCreditAmount()).abs();
        if (glAmount.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return btAmount.divide(glAmount, 4, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    private Double calculateTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0;
        text1 = text1.toLowerCase();
        text2 = text2.toLowerCase();

        // Simple contains pour MVP
        if (text1.contains(text2) || text2.contains(text1)) return 0.8;
        if (text1.equals(text2)) return 1.0;

        // Jaccard similarity basique
        String[] words1 = text1.split("\\s+");
        String[] words2 = text2.split("\\s+");
        java.util.Set<String> set1 = new java.util.HashSet<>(java.util.Arrays.asList(words1));
        java.util.Set<String> set2 = new java.util.HashSet<>(java.util.Arrays.asList(words2));

        java.util.Set<String> intersection = new java.util.HashSet<>(set1);
        intersection.retainAll(set2);

        java.util.Set<String> union = new java.util.HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) return 0.0;
        return (double) intersection.size() / union.size();
    }

    private boolean sameSense(BankTransaction bt, GeneralLedger gl) {
        boolean btIsDebit = bt.getAmount().compareTo(BigDecimal.ZERO) > 0;
        boolean glIsDebit = gl.getDebitAmount().compareTo(BigDecimal.ZERO) > 0;
        return btIsDebit == glIsDebit;
    }

    private boolean referenceMatch(BankTransaction bt, GeneralLedger gl) {
        if (bt.getReference() == null || gl.getReference() == null) return false;
        return bt.getReference().equals(gl.getReference());
    }

    private boolean isRoundNumber(BigDecimal amount) {
        BigDecimal abs = amount.abs();
        BigDecimal[] divideAndRemainder = abs.divideAndRemainder(new BigDecimal("1000"));
        return divideAndRemainder[1].compareTo(BigDecimal.ZERO) == 0;
    }

    private boolean isMonthEnd(java.time.LocalDate date) {
        return date.getDayOfMonth() >= 28;  // Fin de mois approximatif
    }
}
```

#### **2. MLMatchingService.java** ⭐ PRIORITAIRE
```java
// Voir README ML section "Fichiers à créer" pour code complet
// Service de prédiction utilisant le modèle Random Forest
```

#### **3. MLTrainingService.java** ⭐ PRIORITAIRE
```java
// Voir README ML pour code complet
// Service d'entraînement du modèle
```

#### **4-8.** Autres services (voir README ML)

---

## 🎯 PRIORITÉS POUR CONTINUER

### Phase 1 (Session suivante) - CŒUR DU SYSTÈME
1. ✅ Créer les 3 repositories ML
2. ✅ Créer MLFeatureExtractor.java (extraction features)
3. ✅ Créer MLMatchingService.java (prédictions)
4. ✅ Créer MLTrainingService.java (entraînement)

### Phase 2 - INFRASTRUCTURE
5. ✅ Créer MLModelStorageService.java (sérialisation modèles)
6. ✅ Créer MLTrainingScheduler.java (auto-entraînement nocturne)
7. ✅ Créer MLConfiguration.java (config beans)

### Phase 3 - INTÉGRATION
8. ✅ Modifier BankReconciliationMatchingService (Phase 2.5 ML)
9. ✅ Ajouter dépendances Maven (Smile ML 3.0.2)
10. ✅ Tester end-to-end

---

## 📦 DÉPENDANCES MAVEN À AJOUTER

Ajouter dans `pom.xml` :

```xml
<!-- Smile ML - Machine Learning pour Java -->
<dependency>
    <groupId>com.github.haifengl</groupId>
    <artifactId>smile-core</artifactId>
    <version>3.0.2</version>
</dependency>

<!-- Hypersistence Utils - JSON Hibernate -->
<dependency>
    <groupId>io.hypersistence</groupId>
    <artifactId>hypersistence-utils-hibernate-63</artifactId>
    <version>3.7.0</version>
</dependency>

<!-- Commons Math (statistiques) -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-math3</artifactId>
    <version>3.6.1</version>
</dependency>
```

---

## 🧪 TESTS À CRÉER

```
src/test/java/com/predykt/accounting/service/ml/
├── MLFeatureExtractorTest.java
├── MLMatchingServiceTest.java
├── MLTrainingServiceTest.java
└── MLIntegrationTest.java
```

---

## 📝 COMMANDES UTILES

### Lancer migrations
```bash
./mvnw flyway:migrate
```

### Vérifier tables ML créées
```sql
SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename LIKE 'ml_%';
```

### Compter données d'entraînement
```sql
SELECT company_id, COUNT(*)
FROM ml_training_data
GROUP BY company_id;
```

---

## ⚠️ POINTS D'ATTENTION

1. **Hypersistence Utils**: Nécessaire pour `@Type(JsonBinaryType.class)` sur PostgreSQL
2. **Smile ML**: Version 3.0.2 compatible Java 17
3. **Migration V16**: Exécuter AVANT de créer les entités JPA
4. **Multi-tenant**: Tous les services ML doivent filtrer par `company_id`

---

## 🔗 LIENS UTILES

- README ML complet: `ML_MATCHING_SYSTEM_README.md`
- Migration SQL: `V16__add_ml_matching_system.sql`
- Smile ML docs: https://haifengl.github.io/

---

**Prochaine étape**: Créer les repositories et services ML (voir Phase 1 ci-dessus)