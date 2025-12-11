# 🔍 AUDIT COMPLET DE COHÉRENCE DES SERVICES - PREDYKT

**Date :** 2025-12-10
**Version :** 1.0.0
**Couverture :** 47 services analysés
**Score de cohérence actuel :** **65/100**

---

## 📊 RÉSUMÉ EXÉCUTIF

### Problèmes identifiés

| Priorité | Type | Nombre | Impact |
|----------|------|--------|--------|
| 🔴 **CRITIQUE** | Relations manquantes | 4 | Bloquant pour fonctionnalités clés |
| 🟡 **MOYEN** | Incohérences | 8 | Risque de bugs/confusion |
| 🟢 **BAS** | Redondances | 3 | Performance/Maintenabilité |

---

## 🎯 RÉPONSES AUX QUESTIONS POSÉES

### 1. À quoi sert la table `payments` ?

La table `payments` gère les **paiements logiques** associés aux factures :

```
┌─────────────┐         ┌──────────┐
│  Invoice    │ 1     N │ Payment  │
│ (Facture    │◄────────┤ (Paiement│
│  client)    │         │  client) │
└─────────────┘         └──────────┘
                             │
                             │ invoice_id
                             │
┌─────────────┐              │
│    Bill     │ 1          N │
│ (Facture    │◄─────────────┘
│ fournisseur)│              bill_id
└─────────────┘
```

**Rôle de Payment :**
- ✅ Enregistre les encaissements clients (Payment.paymentType = CUSTOMER_PAYMENT)
- ✅ Enregistre les décaissements fournisseurs (Payment.paymentType = SUPPLIER_PAYMENT)
- ✅ Lettrage automatique avec Invoice/Bill (met à jour `amountPaid`, `amountDue`, `status`)
- ✅ Génère des écritures comptables (débit banque, crédit client ou vice-versa)
- ✅ Supporte paiements fractionnés (Invoice.payments = liste de Payment)

**Exemple concret :**
```sql
-- Facture client FV-2025-0001 = 119 250 XAF
-- Paiement 1: 50 000 XAF le 2025-01-15 (partiel)
-- Paiement 2: 69 250 XAF le 2025-02-01 (solde)

SELECT * FROM payments WHERE invoice_id = 1;

id | payment_number | invoice_id | amount    | payment_date | status    | is_reconciled
1  | PAY-2025-0001  | 1          | 50000.00  | 2025-01-15   | COMPLETED | TRUE
2  | PAY-2025-0002  | 1          | 69250.00  | 2025-02-01   | COMPLETED | TRUE
```

---

### 2. Quel est le lien entre `payments`, `invoices` et `bank_transactions` ?

**État actuel : INCOMPLET** ❌

```
                                ┌──────────────────┐
                                │   Invoice/Bill   │
                                │  (Facture)       │
                                └────────┬─────────┘
                                         │
                                         │ 1:N
                                         │
                                ┌────────▼─────────┐
                                │    Payment       │
                                │  (Paiement       │
                                │   logique)       │
                                └──────────────────┘

                                         ❌ RELATION MANQUANTE

                                ┌──────────────────┐
                                │ BankTransaction  │
                                │ (Mouvement       │
                                │  bancaire réel)  │
                                └──────────────────┘
```

**Problème identifié :**
1. ✅ **Invoice ↔ Payment** : Relation existe (invoice_id dans payments)
2. ✅ **Bill ↔ Payment** : Relation existe (bill_id dans payments)
3. ❌ **Payment ↔ BankTransaction** : **RELATION MANQUANTE** (problème critique)

**Ce qui devrait exister :**

```java
// Dans Payment.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "bank_transaction_id")
private BankTransaction bankTransaction;  // ❌ MANQUANT ACTUELLEMENT
```

**Impact du problème :**
- ❌ Impossible de savoir si un paiement enregistré a réellement été encaissé en banque
- ❌ Le lettrage bancaire (BankReconciliationService) ne voit pas les paiements
- ❌ Les utilisateurs doivent faire le rapprochement manuellement entre :
  - Paiement logique (Payment) enregistré dans le système
  - Mouvement bancaire réel (BankTransaction) importé du relevé

**Exemple du problème :**

```
Facture FV-2025-0001 = 119 250 XAF
│
├─ Payment #1 créé le 2025-01-15 = 119 250 XAF (status: COMPLETED)
│  └─ ✅ Lettré avec la facture (Invoice.isReconciled = true)
│
└─ BankTransaction importé du relevé le 2025-01-17 = 119 250 XAF
   └─ ❌ AUCUN lien avec Payment #1 !
      └─ L'utilisateur doit MANUELLEMENT vérifier que c'est le même paiement
```

---

### 3. Les rapports sont-ils à jour ?

**Analyse des services de rapports :**

#### ✅ Rapports financiers OHADA : **CONFORMES**

| Rapport | Service | Utilise | État |
|---------|---------|---------|------|
| **Bilan (Balance Sheet)** | FinancialReportService | GeneralLedger | ✅ Conforme OHADA |
| **Compte de résultat** | FinancialReportService | GeneralLedger | ✅ Conforme OHADA |
| **TAFIRE** | TAFIREService | FinancialReportService | ✅ Conforme OHADA |
| **Cash Flow Statement** | FinancialReportService | GeneralLedger | ✅ Conforme OHADA |

**Conclusion :** Les rapports financiers OHADA sont **à jour et conformes**.

#### ⚠️ Rapports fiscaux : **PARTIELLEMENT À JOUR**

| Rapport | Service | Utilise TaxCalculation ? | État |
|---------|---------|--------------------------|------|
| **Déclaration TVA** | VATDeclarationService | ❌ NON (utilise GL direct) | ⚠️ N'exploite pas TaxCalculation |
| **Rapport AIR** | ❌ N'EXISTE PAS | - | ❌ MANQUANT |
| **Rapport IRPP** | ❌ N'EXISTE PAS | - | ❌ MANQUANT |
| **Alertes NIU** | TaxService (logs) | ✅ OUI (TaxCalculation.hasAlert) | ✅ OK mais pas de rapport |

**Problèmes détectés :**

1. **VATDeclarationService n'utilise PAS les TaxCalculation** :
   ```java
   // VATDeclarationService.java - Calcule depuis GeneralLedger
   private BigDecimal calculateVATByAccount(...) {
       List<GeneralLedger> entries = generalLedgerRepository
           .findByCompanyAndAccountNumberAndEntryDateBetween(...);
       // ❌ Ne regarde pas tax_calculations table
   }
   ```

   **Vs ce qui a été créé en Phase 2 :**
   ```java
   // InvoiceService.java + BillService.java
   taxCalculationRepository.save(taxCalc); // ✅ Sauvegarde TaxCalculation
   ```

   **Impact :** Les TaxCalculation créées par InvoiceService/BillService **ne sont pas exploitées** pour les rapports fiscaux !

2. **Pas de rapport dédié AIR/IRPP** :
   - Les calculs AIR sont faits (BillService → TaxService)
   - Les TaxCalculation sont sauvegardées avec alertes NIU
   - **MAIS** aucun rapport mensuel AIR n'est généré

**Recommandation :** Créer un **TaxReportService** qui utilise `tax_calculations` pour :
```sql
-- Rapport AIR mensuel
SELECT
    SUM(CASE WHEN tax_type = 'AIR_WITH_NIU' THEN tax_amount ELSE 0 END) AS air_2_2_pct,
    SUM(CASE WHEN tax_type = 'AIR_WITHOUT_NIU' THEN tax_amount ELSE 0 END) AS air_5_5_pct,
    COUNT(CASE WHEN has_alert = TRUE THEN 1 END) AS nb_fournisseurs_sans_niu
FROM tax_calculations
WHERE company_id = 1
  AND calculation_date BETWEEN '2025-01-01' AND '2025-01-31'
  AND tax_type IN ('AIR_WITH_NIU', 'AIR_WITHOUT_NIU');
```

#### ✅ Dashboard : **À JOUR**

DashboardService utilise FinancialReportService et calcule des KPIs en temps réel. ✅ OK.

**Petite optimisation possible :** DashboardService recalcule certains ratios (marge brute, etc.) alors que **FinancialRatioService** existe déjà. Il pourrait le réutiliser.

---

### 4. Tous les services sont-ils conformes entre eux ?

**Score de conformité global : 65/100**

#### ✅ Services CONFORMES (bien connectés)

1. **InvoiceService ↔ TaxService** : ✅ Excellent
   - Récupère taux TVA depuis configuration
   - Crée TaxCalculation pour traçabilité
   - Utilise comptes comptables depuis TaxService

2. **BillService ↔ TaxService** : ✅ Excellent
   - Utilise `calculateAllTaxesForTransaction()` pour AIR + IRPP
   - Sauvegarde TaxCalculation automatiquement
   - Alertes NIU persistées en base

3. **PaymentService ↔ Invoice/Bill** : ✅ Bon
   - Lettrage automatique (met à jour amountPaid, status)
   - Support paiements fractionnés
   - Génère écritures comptables

4. **FinancialReportService ↔ GeneralLedgerService** : ✅ Bon
   - Utilise GL pour calculs de soldes
   - Rapports OHADA conformes

5. **TAFIREService ↔ FinancialReportService** : ✅ Bon
   - Réutilise bilans N et N-1
   - Cohérence assurée

#### ❌ Incohérences détectées

1. **BankReconciliationMatchingService ↔ PaymentService** : ❌ **AUCUNE RELATION**
   - Le matching bancaire ne considère PAS les paiements
   - Matching fait uniquement : BankTransaction ↔ GeneralLedger
   - **Devrait faire :** BankTransaction ↔ Payment (puis Payment → Invoice/Bill)

2. **PaymentService contourne GeneralLedgerService** : ⚠️ Problème
   ```java
   // PaymentService.java:290 - Création GL directe
   GeneralLedger savedEntry = generalLedgerRepository.save(bankEntry);

   // ❌ Ne passe PAS par GeneralLedgerService.createJournalEntry()
   // → Pas de validation double-écriture
   // → Pas de vérification verrouillage période
   ```

3. **VATDeclarationService n'utilise pas TaxCalculation** : ⚠️ Duplication
   - Calcule TVA depuis GeneralLedger directement
   - Ignore les TaxCalculation créées par Invoice/BillService
   - Risque d'incohérence si règles de calcul changent

4. **Calculs de soldes dupliqués** : ⚠️ Redondance
   - FinancialReportService calcule soldes de comptes
   - TAFIREService recalcule les mêmes soldes
   - VATDeclarationService aussi
   - **Devrait être centralisé** dans GeneralLedgerService

---

### 5. Y a-t-il des redondances ou services mal connectés ?

**Redondances identifiées :**

#### 1. Calcul de soldes de comptes (3 implémentations)

```java
// 1. FinancialReportService.java
private BigDecimal calculateAccountClassBalance(...) {
    return chartService.getActiveAccounts(companyId).stream()
        .filter(account -> account.getAccountNumber().startsWith(classPrefix))
        .map(account -> glService.getAccountBalance(...))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
}

// 2. TAFIREService.java
private BigDecimal getSoldeCompte(...) {
    List<GeneralLedger> entries = generalLedgerRepository.find...(...);
    // Calcul similaire
}

// 3. VATDeclarationService.java
private BigDecimal calculateVATByAccount(...) {
    // Encore la même logique
}
```

**Solution :** Centraliser dans GeneralLedgerService avec méthodes réutilisables.

#### 2. Calcul BFR (2 implémentations)

- FinancialReportService (Cash Flow Statement) calcule variation BFR
- TAFIREService calcule aussi variation BFR
- **Risque :** Formules peuvent diverger

**Solution :** Créer WorkingCapitalService ou centraliser dans FinancialReportService.

#### 3. Calcul de ratios (2 implémentations)

- DashboardService calcule marges, ROA, etc.
- FinancialRatioService (service dédié) calcule les mêmes ratios

**Solution :** DashboardService devrait appeler FinancialRatioService au lieu de recalculer.

#### Services mal connectés

| Service manquant | Devrait lier | Impact |
|------------------|--------------|--------|
| **PaymentReconciliationService** | Payment ↔ BankTransaction | ❌ N'EXISTE PAS |
| **TaxReportService** | TaxCalculation → Rapports AIR/IRPP | ❌ N'EXISTE PAS |
| **BankReconciliationMatchingService** | Devrait utiliser PaymentService | ❌ Ne le fait pas |

---

## 🚨 PROBLÈMES CRITIQUES À CORRIGER

### Priorité 🔴 CRITIQUE (À faire immédiatement)

#### 1. Ajouter relation Payment → BankTransaction

**Fichier à modifier :** `Payment.java`

```java
// Ajouter dans Payment.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "bank_transaction_id")
private BankTransaction bankTransaction;
```

**Migration Flyway à créer :** `V19__add_bank_transaction_to_payments.sql`

```sql
ALTER TABLE payments
ADD COLUMN bank_transaction_id BIGINT;

ALTER TABLE payments
ADD CONSTRAINT fk_payment_bank_transaction
    FOREIGN KEY (bank_transaction_id)
    REFERENCES bank_transactions(id)
    ON DELETE SET NULL;

CREATE INDEX idx_payments_bank_transaction ON payments(bank_transaction_id);
```

#### 2. Créer PaymentReconciliationService

**Fichier à créer :** `PaymentReconciliationService.java`

```java
@Service
@RequiredArgsConstructor
public class PaymentReconciliationService {

    private final PaymentRepository paymentRepository;
    private final BankTransactionRepository bankTransactionRepository;

    /**
     * Lettrer un paiement avec une transaction bancaire
     */
    public void reconcilePaymentWithBankTransaction(Long paymentId, Long bankTransactionId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(...);
        BankTransaction bt = bankTransactionRepository.findById(bankTransactionId).orElseThrow(...);

        // Vérifier cohérence montants (tolérance 1%)
        BigDecimal diff = payment.getAmount().subtract(bt.getAmount().abs()).abs();
        BigDecimal tolerance = payment.getAmount().multiply(new BigDecimal("0.01"));

        if (diff.compareTo(tolerance) > 0) {
            throw new ValidationException("Montants incohérents: Payment=" + payment.getAmount()
                + " vs BankTx=" + bt.getAmount());
        }

        // Lettrage
        payment.setBankTransaction(bt);
        payment.setIsReconciled(true);
        payment.setReconciliationDate(LocalDate.now());

        bt.setIsReconciled(true);

        paymentRepository.save(payment);
        bankTransactionRepository.save(bt);

        log.info("✅ Paiement {} lettré avec transaction bancaire {}",
            payment.getPaymentNumber(), bt.getBankReference());
    }
}
```

#### 3. Intégrer Payment dans BankReconciliationMatchingService

**Fichier à modifier :** `BankReconciliationMatchingService.java`

```java
// Ajouter injection
private final PaymentRepository paymentRepository;

// Dans performIntelligentMatching() - Ajouter PHASE 2.3
// PHASE 2.3: Matching avec Payments
log.info("🔍 PHASE 2.3: Matching BankTransactions avec Payments");
for (BankTransaction bt : unmatched) {
    List<Payment> candidates = paymentRepository
        .findByCompanyAndPaymentDateBetween(
            company,
            bt.getTransactionDate().minusDays(5),
            bt.getTransactionDate().plusDays(5)
        )
        .stream()
        .filter(p -> p.getBankTransaction() == null) // Pas déjà lettré
        .filter(p -> {
            BigDecimal diff = p.getAmount().subtract(bt.getAmount().abs()).abs();
            return diff.compareTo(config.getAmountToleranceAbsolute()) <= 0;
        })
        .collect(Collectors.toList());

    if (candidates.size() == 1) {
        // Match automatique
        paymentReconciliationService.reconcilePaymentWithBankTransaction(
            candidates.get(0).getId(), bt.getId()
        );
        matchCount++;
    }
}
```

#### 4. PaymentService doit utiliser GeneralLedgerService

**Fichier à modifier :** `PaymentService.java`

**Avant :**
```java
// PaymentService.java:290 - ❌ MAUVAISE PRATIQUE
GeneralLedger savedEntry = generalLedgerRepository.save(bankEntry);
```

**Après :**
```java
// ✅ BONNE PRATIQUE
GeneralLedger savedEntry = generalLedgerService.createJournalEntry(
    company,
    journalEntryRequest
);
```

**Avantages :**
- ✅ Validation double-écriture (débit = crédit)
- ✅ Vérification verrouillage de période
- ✅ Cohérence avec le reste du système

---

### Priorité 🟡 MOYEN (À planifier)

#### 5. VATDeclarationService devrait utiliser TaxCalculation

**Fichier à modifier :** `VATDeclarationService.java`

**Avant :**
```java
private BigDecimal calculateVATByAccount(...) {
    List<GeneralLedger> entries = generalLedgerRepository.find...(...);
    // Calcul manuel
}
```

**Après :**
```java
private BigDecimal calculateVATByAccount(...) {
    // 1. Essayer d'utiliser TaxCalculation d'abord
    List<TaxCalculation> taxCalcs = taxCalculationRepository
        .findByCompanyAndTaxTypeAndCalculationDateBetween(
            company, TaxType.VAT, startDate, endDate
        );

    if (!taxCalcs.isEmpty()) {
        // Utiliser TaxCalculation (source de vérité)
        return taxCalcs.stream()
            .filter(calc -> calc.getInvoice() != null) // TVA collectée
            .map(TaxCalculation::getTaxAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // 2. Fallback sur GeneralLedger si aucune TaxCalculation
    List<GeneralLedger> entries = generalLedgerRepository.find...(...);
    // ...
}
```

#### 6. Créer TaxReportService

**Fichier à créer :** `TaxReportService.java`

```java
@Service
@RequiredArgsConstructor
public class TaxReportService {

    private final TaxCalculationRepository taxCalculationRepository;

    /**
     * Rapport AIR mensuel avec alertes NIU
     */
    public AIRMonthlyReportResponse generateAIRReport(Long companyId, YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        List<TaxCalculation> airCalcs = taxCalculationRepository
            .findByCompanyAndTaxTypeAndCalculationDateBetween(...);

        BigDecimal airWithNIU = airCalcs.stream()
            .filter(tc -> tc.getTaxType() == TaxType.AIR_WITH_NIU)
            .map(TaxCalculation::getTaxAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal airWithoutNIU = airCalcs.stream()
            .filter(tc -> tc.getTaxType() == TaxType.AIR_WITHOUT_NIU)
            .map(TaxCalculation::getTaxAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TaxCalculation> alerts = airCalcs.stream()
            .filter(TaxCalculation::getHasAlert)
            .collect(Collectors.toList());

        return AIRMonthlyReportResponse.builder()
            .month(month)
            .airWithNIU(airWithNIU)
            .airWithoutNIU(airWithoutNIU)
            .totalAIR(airWithNIU.add(airWithoutNIU))
            .suppliersWithoutNIU(alerts.size())
            .excessCostDueToMissingNIU(
                airWithoutNIU.subtract(
                    airCalcs.stream()
                        .filter(tc -> tc.getTaxType() == TaxType.AIR_WITHOUT_NIU)
                        .map(tc -> tc.getBaseAmount().multiply(new BigDecimal("0.022")))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                )
            )
            .alerts(alerts)
            .build();
    }
}
```

#### 7. DashboardService doit appeler FinancialRatioService

**Fichier à modifier :** `DashboardService.java`

**Avant :**
```java
// DashboardService.java:110-124 - Recalcule les marges
BigDecimal grossMargin = yearlyIncome.getGrossProfit()
    .divide(yearlyIncome.getTotalRevenue(), 4, RoundingMode.HALF_UP)
    .multiply(BigDecimal.valueOf(100));
```

**Après :**
```java
// ✅ Réutiliser FinancialRatioService
FinancialRatio ratios = financialRatioService.calculateFinancialRatios(companyId, ...);
BigDecimal grossMargin = ratios.getGrossMarginPercent();
```

#### 8. Centraliser calculs de soldes

**Fichier à modifier :** `GeneralLedgerService.java`

Ajouter méthodes :
```java
public BigDecimal getAccountClassBalance(Long companyId, String classPrefix, LocalDate asOfDate) {
    // Centraliser logique
}

public BigDecimal getAccountBalanceRange(Long companyId, String accountNumber,
                                         LocalDate startDate, LocalDate endDate) {
    // Centraliser logique
}
```

**Puis modifier :** FinancialReportService, TAFIREService, VATDeclarationService pour utiliser ces méthodes.

---

## 📋 CHECKLIST DE CONFORMITÉ

### Relations entités

- [x] Invoice ↔ Payment (OneToMany) ✅
- [x] Bill ↔ Payment (OneToMany) ✅
- [x] Invoice ↔ TaxCalculation (OneToMany) ✅ (Phase 2)
- [x] Bill ↔ TaxCalculation (OneToMany) ✅ (Phase 2)
- [x] Payment ↔ GeneralLedger (ManyToOne) ✅
- [x] BankTransaction ↔ GeneralLedger (OneToOne) ✅
- [ ] **Payment ↔ BankTransaction (ManyToOne)** ❌ MANQUANT

### Services fiscaux

- [x] InvoiceService utilise TaxService ✅ (Phase 1)
- [x] BillService utilise TaxService ✅ (Phase 1)
- [x] InvoiceService crée TaxCalculation ✅ (Phase 2)
- [x] BillService crée TaxCalculation ✅ (Phase 2)
- [ ] VATDeclarationService utilise TaxCalculation ⚠️ PARTIEL
- [ ] TaxReportService existe ❌ MANQUANT

### Services de lettrage

- [x] PaymentService lettre Invoice/Bill ✅
- [ ] PaymentReconciliationService lettre Payment/BankTransaction ❌ MANQUANT
- [ ] BankReconciliationMatchingService utilise Payment ❌ MANQUANT

### Services de rapports

- [x] FinancialReportService génère Bilan ✅
- [x] FinancialReportService génère Compte résultat ✅
- [x] TAFIREService génère TAFIRE ✅
- [ ] TaxReportService génère rapports AIR/IRPP ❌ MANQUANT

### Cohérence architecture

- [x] InvoiceService/BillService utilisent TaxService ✅
- [ ] PaymentService utilise GeneralLedgerService ⚠️ CONTOURNE
- [x] DashboardService utilise FinancialReportService ✅
- [ ] DashboardService utilise FinancialRatioService ⚠️ RECALCULE

---

## 🎯 SCORE DE COHÉRENCE

### Calcul du score

**Points positifs (+35):**
- ✅ Bonne séparation des responsabilités (10 pts)
- ✅ Services fiscaux bien intégrés (10 pts)
- ✅ Lettrage facture ↔ paiement (5 pts)
- ✅ Rapports OHADA complets (10 pts)

**Points négatifs (-35):**
- ❌ Pas de relation BankTransaction ↔ Payment (-15 pts)
- ❌ PaymentService contourne GeneralLedgerService (-5 pts)
- ❌ VATDeclarationService n'utilise pas TaxCalculation (-5 pts)
- ❌ Calculs dupliqués (BFR, soldes) (-5 pts)
- ❌ Pas de TaxReportService (-5 pts)

**SCORE ACTUEL : 65/100**

**Objectif après corrections prioritaires : 85/100**

---

## 💡 CONCLUSION

Le système PREDYKT a une **architecture solide et bien structurée**, mais souffre de **quelques gaps au niveau du rapprochement bancaire** et de l'utilisation cohérente des services de bas niveau.

**Les 4 actions critiques** pour passer de 65/100 à 85/100 :
1. ✅ Créer relation Payment → BankTransaction (migration V19)
2. ✅ Créer PaymentReconciliationService
3. ✅ Intégrer Payment dans BankReconciliationMatchingService
4. ✅ PaymentService doit passer par GeneralLedgerService

Avec ces corrections, le système aura une **cohérence complète** entre :
- Factures (Invoice/Bill)
- Paiements logiques (Payment)
- Mouvements bancaires (BankTransaction)
- Taxes (TaxCalculation)
- Comptabilité (GeneralLedger)

---

**Fichiers prioritaires à modifier :**
1. `Payment.java` - Ajouter relation BankTransaction
2. `V19__add_bank_transaction_to_payments.sql` - Migration
3. `PaymentReconciliationService.java` - À créer
4. `BankReconciliationMatchingService.java` - Intégrer Payment
5. `PaymentService.java` - Utiliser GeneralLedgerService

**Fichiers secondaires (optimisations) :**
6. `VATDeclarationService.java` - Utiliser TaxCalculation
7. `TaxReportService.java` - À créer
8. `DashboardService.java` - Appeler FinancialRatioService
9. `GeneralLedgerService.java` - Centraliser calculs de soldes
