# 🔍 Audit TVA & Conformité Fiscale - PREDYKT

**Date de l'audit :** 10 Décembre 2025
**Version du système :** 1.0.0-SNAPSHOT
**Normes de référence :** OHADA SYSCOHADA / Code Général des Impôts (CGI) Cameroun

---

## 📋 Résumé Exécutif

### ✅ Points forts identifiés

1. **Services spécialisés sophistiqués** existants :
   - `VATRecoverabilityService` avec moteur de règles et prorata CGI Art. 133
   - `TaxService` centralisant toutes les taxes camerounaises
   - `VATService` pour calculs de base

2. **Conformité technique** :
   - Taux TVA correct : **19.25%** (Cameroun)
   - Comptes OHADA corrects : **4431** (collectée), **4451** (déductible)
   - Gestion TVA non récupérable (VP, représentation, etc.)

3. **Fonctionnalités avancées** :
   - Prorata de TVA (activités mixtes)
   - Moteur de règles avec apprentissage automatique
   - Calcul AIR, IRPP Loyer, IS, CNPS

### ❌ Problèmes critiques identifiés

1. **Duplication et incohérence** :
   - 3 services différents (`VATService`, `VATRecoverabilityService`, `TaxService`)
   - Services non utilisés par les autres modules

2. **Calculs manuels dans les services métier** :
   - `InvoiceService` : Calcule TVA manuellement (ligne 453)
   - `BillService` : Calcule AIR et IRPP manuellement (lignes 56-58, 301-312)

3. **Taux hardcodés** :
   - Taux de 19.25% en dur dans le code
   - Pas d'utilisation des configurations centralisées

4. **Absence d'intégration** :
   - `TaxService` n'est utilisé que par `CompanyService`
   - `VATRecoverabilityService` n'est utilisé que par `GeneralLedgerService`
   - `VATService` n'est utilisé par PERSONNE

### ⚠️ Risques

- **Maintenance difficile** : Changement de taux nécessite modification de plusieurs fichiers
- **Incohérence de calculs** : Risque de calculs différents selon le service
- **Non-conformité future** : Si loi change, difficile de tout mettre à jour

---

## 📊 Analyse Détaillée

### 1. Services de TVA existants

#### 1.1 VATService.java

**Emplacement :** `src/main/java/com/predykt/accounting/service/VATService.java`

**Fonctionnalités :**
- Calcul TVA collectée/déductible
- Calcul HT ↔ TTC
- Résumé TVA par période
- Vérification assujettissement TVA

**Conformité OHADA :**
```java
✅ Comptes corrects :
   - COMPTE_TVA_COLLECTEE = "4431"  // Créditeur (ventes)
   - COMPTE_TVA_DEDUCTIBLE = "4451" // Débiteur (achats)
   - COMPTE_TVA_A_PAYER = "4441"    // TVA à payer

✅ Taux correct :
   - TAUX_TVA_NORMAL = 19.25%  // Cameroun
   - TAUX_TVA_REDUIT = 0%      // Exonéré

✅ Calculs corrects :
   - calculateVATAmount(HT, taux) = HT × (taux / 100)
   - calculateAmountExcludingVAT(TTC, taux) = TTC / (1 + taux/100)
```

**⚠️ Problème :** Service non utilisé par les autres modules !

```java
// Recherche dans le projet : AUCUNE injection de VATService
// grep "private final VATService" → 0 résultat
```

---

#### 1.2 VATRecoverabilityService.java

**Emplacement :** `src/main/java/com/predykt/accounting/service/VATRecoverabilityService.java`

**Fonctionnalités :**
- ✅ Gestion TVA non récupérable (CGI Cameroun)
- ✅ Prorata de TVA (Art. 133 CGI) - Activités mixtes
- ✅ Moteur de règles avec ML
- ✅ Catégorisation automatique des dépenses :
  - `FULLY_RECOVERABLE` (100%)
  - `RECOVERABLE_80_PERCENT` (80%)
  - `NON_RECOVERABLE_TOURISM_VEHICLE` (0%)
  - `NON_RECOVERABLE_FUEL_VP` (0%)
  - `NON_RECOVERABLE_REPRESENTATION` (0%)
  - `NON_RECOVERABLE_LUXURY` (0%)
  - `NON_RECOVERABLE_PERSONAL` (0%)

**Conformité fiscale Cameroun :**

```java
✅ Respect CGI Art. 127-133 :
   - TVA non déductible sur VP (véhicules de tourisme)
   - TVA non déductible sur carburant VP
   - TVA non déductible sur frais de représentation
   - TVA non déductible sur biens de luxe
   - Prorata calculé selon formule légale

✅ Calcul en 2 étapes (conforme CGI) :
   ÉTAPE 1 : Récupérabilité PAR NATURE
   ÉTAPE 2 : Application du PRORATA

Exemple :
   TVA 100 000 XAF sur carburant VP
   → ÉTAPE 1 : 0% récupérable (carburant VP)
   → ÉTAPE 2 : Prorata N/A (déjà 0%)
   → RÉSULTAT : 0 XAF récupérable, 100 000 XAF non récupérable
```

**✅ Utilisation :** Utilisé par `GeneralLedgerService` (ligne 32, 90)

```java
// GeneralLedgerService.java:32
private final VATRecoverabilityService vatRecoverabilityService;

// GeneralLedgerService.java:86-91
if (isVATDeductibleAccount(line.getAccountNumber())) {
    processVATEntry(company, savedEntry, request.getEntryDate());
}
```

---

#### 1.3 TaxService.java

**Emplacement :** `src/main/java/com/predykt/accounting/service/TaxService.java`

**Fonctionnalités :**
- ✅ Service CENTRAL pour TOUTES les taxes camerounaises :
  - TVA 19.25%
  - Acompte IS (IMF) 2.2%
  - AIR 2.2% (avec NIU) / 5.5% (sans NIU)
  - IRPP Loyer 15%
  - CNPS ~20%

**Conformité fiscale Cameroun :**

```java
✅ Toutes les taxes camerounaises :
   - TVA : 19.25% (taux normal)
   - IS Advance (IMF) : 2.2% sur CA (Acompte Mensuel)
   - AIR (Précompte IR) : 2.2% avec NIU, 5.5% sans NIU
   - IRPP Loyer : 15% (bailleur reçoit 85%)
   - CNPS : ~20% (estimation pour provision)

✅ Alertes automatiques :
   - Alerte si fournisseur sans NIU (pénalité +3.3%)
   - Calcul automatique du surcoût

✅ Configurations par entreprise :
   - TaxConfiguration pour chaque taxe
   - Activation/désactivation dynamique
   - Modification des taux
```

**⚠️ Problème :** Service utilisé UNIQUEMENT par `CompanyService` (initialisation) !

```java
// CompanyService.java:22
private final TaxService taxService;

// Utilisé UNIQUEMENT pour initialiser les configs
taxService.initializeDefaultTaxConfigurations(company);

// ❌ PAS utilisé par InvoiceService
// ❌ PAS utilisé par BillService
// ❌ PAS utilisé par PaymentService
```

---

### 2. Utilisation actuelle dans les services métier

#### 2.1 InvoiceService.java (Factures clients)

**Comment la TVA est calculée :**

```java
// InvoiceService.java:453 - TAUX HARDCODÉ
.vatRate(request.getVatRate() != null ? request.getVatRate() : new BigDecimal("19.25"))

// InvoiceService.java:425-428 - Écriture TVA collectée
GeneralLedger vatEntry = GeneralLedger.builder()
    .accountNumber(VAT_COLLECTED_ACCOUNT)  // 4431
    .description("TVA 19.25% sur facture " + invoice.getInvoiceNumber())
    .creditAmount(invoice.getVatAmount())
    .build();
```

**❌ Problèmes :**
1. Taux hardcodé (19.25%) au lieu d'utiliser `TaxService` ou `VATService`
2. Pas de gestion des exonérations via configuration
3. Pas de traçabilité fiscale via `TaxCalculation`

---

#### 2.2 BillService.java (Factures fournisseurs)

**Comment AIR et IRPP sont calculés :**

```java
// BillService.java:56-58 - TAUX HARDCODÉS
private static final BigDecimal AIR_RATE_WITH_NIU = new BigDecimal("2.2");
private static final BigDecimal AIR_RATE_WITHOUT_NIU = new BigDecimal("5.5");
private static final BigDecimal IRPP_RENT_RATE = new BigDecimal("15.0");

// BillService.java:301-312 - Calcul AIR manuel
BigDecimal airRate = bill.getSupplierHasNiu() ? AIR_RATE_WITH_NIU : AIR_RATE_WITHOUT_NIU;
BigDecimal airAmount = totalHt.multiply(airRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
bill.setAirAmount(airAmount);

// Calcul IRPP Loyer manuel
if (bill.getSupplier().getSupplierType() == SupplierType.RENT) {
    irppRentAmount = totalHt.multiply(IRPP_RENT_RATE).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    bill.setIrppRentAmount(irppRentAmount);
}
```

**❌ Problèmes :**
1. Taux hardcodés au lieu d'utiliser `TaxService`
2. Pas de traçabilité via `TaxCalculation`
3. Duplication de la logique de calcul AIR (existe déjà dans `TaxService`)
4. Pas d'alertes automatiques pour fournisseurs sans NIU (existe dans `TaxService`)

---

#### 2.3 GeneralLedgerService.java

**✅ BONNE PRATIQUE :** Utilise `VATRecoverabilityService`

```java
// GeneralLedgerService.java:32
private final VATRecoverabilityService vatRecoverabilityService;

// GeneralLedgerService.java:86-91 - Détection automatique TVA
if (isVATDeductibleAccount(line.getAccountNumber())) {
    processVATEntry(company, savedEntry, request.getEntryDate());
}
```

**✅ Points positifs :**
- Utilisation correcte du service spécialisé
- Détection automatique des comptes TVA (445x)
- Calcul automatique de la récupérabilité

---

### 3. Tableau comparatif des services

| Service | Fonctionnalité | Conformité | Utilisation | Recommandation |
|---------|----------------|------------|-------------|----------------|
| **VATService** | Calculs TVA de base | ✅ Conforme OHADA | ❌ Non utilisé | ⚠️ À supprimer ou fusionner |
| **VATRecoverabilityService** | TVA non récupérable + Prorata | ✅ Conforme CGI Art. 133 | ✅ Par GeneralLedgerService | ✅ À utiliser partout |
| **TaxService** | TOUTES les taxes Cameroun | ✅ Conforme CGI | ⚠️ Uniquement initialisation | ✅ À utiliser dans Invoice/Bill |
| **InvoiceService** (calcul manuel) | TVA collectée | ⚠️ Taux hardcodé | ✅ Actuellement utilisé | ❌ À remplacer par TaxService |
| **BillService** (calcul manuel) | AIR + IRPP | ⚠️ Taux hardcodés | ✅ Actuellement utilisé | ❌ À remplacer par TaxService |

---

## 🎯 Recommandations

### Priorité 1 - CRITIQUE

#### 1. Centraliser les calculs fiscaux

**Objectif :** Tous les calculs de taxes doivent passer par `TaxService`

**Actions :**

```java
// ❌ AVANT (InvoiceService.java:453)
.vatRate(request.getVatRate() != null ? request.getVatRate() : new BigDecimal("19.25"))

// ✅ APRÈS
private final TaxService taxService;

BigDecimal vatRate = taxService.getTaxConfigurations(companyId).stream()
    .filter(config -> config.getTaxType() == TaxType.VAT)
    .findFirst()
    .map(TaxConfiguration::getTaxRate)
    .orElse(new BigDecimal("19.25"));
```

**Ou mieux, utiliser la méthode complète :**

```java
// InvoiceService - Lors de la validation de la facture
List<TaxCalculation> taxes = taxService.calculateAllTaxesForTransaction(
    company,
    invoice.getTotalHt(),
    "SALE",
    null,  // Pas de fournisseur pour une vente
    "701", // Compte de vente
    invoice.getIssueDate()
);

// Récupérer la TVA calculée
TaxCalculation vatCalculation = taxes.stream()
    .filter(t -> t.getTaxType() == TaxType.VAT)
    .findFirst()
    .orElseThrow();

invoice.setVatAmount(vatCalculation.getTaxAmount());
```

---

#### 2. Utiliser TaxService dans BillService

**Actions :**

```java
// ❌ AVANT (BillService.java:301-312) - Calculs manuels
BigDecimal airRate = bill.getSupplierHasNiu() ? AIR_RATE_WITH_NIU : AIR_RATE_WITHOUT_NIU;
BigDecimal airAmount = totalHt.multiply(airRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

// ✅ APRÈS
private final TaxService taxService;

List<TaxCalculation> taxes = taxService.calculateAllTaxesForTransaction(
    company,
    bill.getTotalHt(),
    "PURCHASE",
    bill.getSupplier(),
    bill.getLines().get(0).getAccountNumber(),
    bill.getIssueDate()
);

// Récupérer AIR
TaxCalculation airCalculation = taxes.stream()
    .filter(t -> t.getTaxType().name().startsWith("AIR"))
    .findFirst()
    .orElse(null);

if (airCalculation != null) {
    bill.setAirAmount(airCalculation.getTaxAmount());

    // ✅ BONUS: Alertes automatiques si fournisseur sans NIU
    if (airCalculation.hasAlerts()) {
        log.warn("⚠️ {}", airCalculation.getAlerts());
    }
}

// Récupérer IRPP Loyer
TaxCalculation irppCalculation = taxes.stream()
    .filter(t -> t.getTaxType() == TaxType.IRPP_RENT)
    .findFirst()
    .orElse(null);

if (irppCalculation != null) {
    bill.setIrppRentAmount(irppCalculation.getTaxAmount());
}
```

**✅ Avantages :**
- ✅ Taux configurables dynamiquement
- ✅ Alertes automatiques pour fournisseurs sans NIU
- ✅ Traçabilité complète via `TaxCalculation`
- ✅ Calcul du surcoût automatique
- ✅ Un seul endroit à maintenir

---

#### 3. Supprimer les taux hardcodés

**Fichiers à modifier :**

```java
// ❌ À SUPPRIMER de InvoiceService.java:58
private static final String VAT_COLLECTED_ACCOUNT = "4431";

// ❌ À SUPPRIMER de BillService.java:53-58
private static final String AIR_ACCOUNT = "4421";
private static final String IRPP_RENT_ACCOUNT = "4422";
private static final BigDecimal AIR_RATE_WITH_NIU = new BigDecimal("2.2");
private static final BigDecimal AIR_RATE_WITHOUT_NIU = new BigDecimal("5.5");
private static final BigDecimal IRPP_RENT_RATE = new BigDecimal("15.0");
```

**✅ Remplacer par :**

```java
// Récupérer depuis TaxConfiguration
TaxConfiguration config = taxConfigRepository.findByCompanyAndTaxType(company, TaxType.VAT)
    .orElseThrow(() -> new ResourceNotFoundException("Configuration TVA non trouvée"));

String vatAccount = config.getAccountNumber();  // 4431
BigDecimal vatRate = config.getTaxRate();       // 19.25
```

---

### Priorité 2 - IMPORTANT

#### 4. Fusionner ou supprimer VATService

**Constat :** `VATService` fait doublon avec `TaxService`

**Options :**

**Option A : Supprimer VATService** (RECOMMANDÉ)
- Toutes ses fonctionnalités existent déjà dans `TaxService`
- Aucun service ne l'utilise actuellement
- Évite la duplication

**Option B : Fusionner dans TaxService**
- Migrer les méthodes utiles (`calculateAmountExcludingVAT`, etc.) vers `TaxService`
- Supprimer `VATService`

---

#### 5. Améliorer la traçabilité fiscale

**Objectif :** Toutes les taxes calculées doivent être enregistrées dans `TaxCalculation`

**Actions :**

```java
// InvoiceService - Lors de la validation
List<TaxCalculation> taxes = taxService.calculateAllTaxesForTransaction(...);

// Associer les calculs à la facture
for (TaxCalculation tax : taxes) {
    tax.setInvoiceId(invoice.getId());  // Ajouter cette relation si manquante
    taxCalculationRepository.save(tax);
}
```

**✅ Avantages :**
- ✅ Audit trail complet
- ✅ Déclarations fiscales simplifiées (tout est tracé)
- ✅ Statistiques par type de taxe
- ✅ Détection d'anomalies

---

#### 6. Ajouter des tests unitaires pour TaxService

**Constat :** Service critique mais probablement pas assez testé

**Tests à ajouter :**

```java
// TaxServiceTest.java
@Test
void calculateVAT_shouldApply19_25Percent() {
    BigDecimal ht = new BigDecimal("100000");
    List<TaxCalculation> taxes = taxService.calculateAllTaxesForTransaction(
        company, ht, "SALE", null, "701", LocalDate.now()
    );

    TaxCalculation vat = taxes.stream()
        .filter(t -> t.getTaxType() == TaxType.VAT)
        .findFirst().orElseThrow();

    assertEquals(new BigDecimal("19250.00"), vat.getTaxAmount());
}

@Test
void calculateAIR_withNIU_shouldApply2_2Percent() {
    Supplier supplier = createSupplierWithNIU();
    BigDecimal ht = new BigDecimal("100000");

    List<TaxCalculation> taxes = taxService.calculateAllTaxesForTransaction(
        company, ht, "PURCHASE", supplier, "601", LocalDate.now()
    );

    TaxCalculation air = taxes.stream()
        .filter(t -> t.getTaxType().name().startsWith("AIR"))
        .findFirst().orElseThrow();

    assertEquals(new BigDecimal("2200.00"), air.getTaxAmount());
    assertFalse(air.hasAlerts());
}

@Test
void calculateAIR_withoutNIU_shouldApply5_5Percent_andGenerateAlert() {
    Supplier supplier = createSupplierWithoutNIU();
    BigDecimal ht = new BigDecimal("100000");

    List<TaxCalculation> taxes = taxService.calculateAllTaxesForTransaction(
        company, ht, "PURCHASE", supplier, "601", LocalDate.now()
    );

    TaxCalculation air = taxes.stream()
        .filter(t -> t.getTaxType().name().startsWith("AIR"))
        .findFirst().orElseThrow();

    assertEquals(new BigDecimal("5500.00"), air.getTaxAmount());
    assertTrue(air.hasAlerts());
    assertTrue(air.getAlerts().contains("sans NIU"));

    // Vérifier surcoût calculé
    BigDecimal expectedPenalty = new BigDecimal("3300.00");  // (5.5% - 2.2%) * 100000
    assertEquals(expectedPenalty, air.calculatePenaltyCost());
}
```

---

### Priorité 3 - AMÉLIORATION

#### 7. Documenter l'architecture fiscale

**Créer un document :** `ARCHITECTURE_FISCALE.md`

```markdown
# Architecture Fiscale - PREDYKT

## Services

### TaxService (Service Central)
- **Rôle :** Calcul de TOUTES les taxes camerounaises
- **Utilisé par :** InvoiceService, BillService, PaymentService
- **Taxes gérées :** TVA, AIR, IRPP, IS, CNPS

### VATRecoverabilityService
- **Rôle :** Gestion de la TVA non récupérable + Prorata
- **Utilisé par :** GeneralLedgerService
- **Conformité :** CGI Art. 127-133

## Flux de calcul

### Facture Client (Invoice)
1. InvoiceService appelle TaxService.calculateAllTaxesForTransaction()
2. TaxService calcule TVA (19.25%)
3. Écriture comptable générée avec TVA collectée (4431)
4. TaxCalculation enregistré pour traçabilité

### Facture Fournisseur (Bill)
1. BillService appelle TaxService.calculateAllTaxesForTransaction()
2. TaxService calcule TVA déductible + AIR + IRPP (si applicable)
3. Alertes générées si fournisseur sans NIU
4. Écritures comptables générées
5. TaxCalculation enregistré

### Écriture Manuelle (GL)
1. GeneralLedgerService détecte compte TVA (445x)
2. Appelle VATRecoverabilityService pour calcul récupérabilité
3. Applique prorata si entreprise à activités mixtes
4. Enregistre VATRecoveryCalculation
```

---

## 📈 Plan d'action

### Phase 1 - Refactoring critique (Sprint 1)

| Tâche | Priorité | Effort | Impact |
|-------|----------|--------|--------|
| 1. Injecter TaxService dans InvoiceService | P0 | 2h | HAUT |
| 2. Injecter TaxService dans BillService | P0 | 2h | HAUT |
| 3. Remplacer calculs manuels par TaxService | P0 | 4h | HAUT |
| 4. Supprimer taux hardcodés | P0 | 1h | MOYEN |
| 5. Tests de régression | P0 | 4h | CRITIQUE |

**Total Phase 1 :** ~13h (2 jours)

---

### Phase 2 - Amélioration traçabilité (Sprint 2)

| Tâche | Priorité | Effort | Impact |
|-------|----------|--------|--------|
| 6. Associer TaxCalculation aux factures | P1 | 3h | MOYEN |
| 7. Ajouter tests unitaires TaxService | P1 | 6h | HAUT |
| 8. Supprimer VATService (doublon) | P2 | 2h | FAIBLE |
| 9. Créer documentation architecture | P2 | 3h | MOYEN |

**Total Phase 2 :** ~14h (2 jours)

---

### Phase 3 - Optimisations (Sprint 3)

| Tâche | Priorité | Effort | Impact |
|-------|----------|--------|--------|
| 10. Dashboard fiscal (taxes par période) | P3 | 8h | MOYEN |
| 11. Export déclarations fiscales | P3 | 6h | MOYEN |
| 12. Alertes proactives (échéances) | P3 | 4h | FAIBLE |

**Total Phase 3 :** ~18h (2-3 jours)

---

## ✅ Checklist de conformité

### Conformité OHADA

- [x] Comptes TVA corrects (4431, 4451)
- [x] Double-entry bookkeeping respecté
- [x] Nomenclature OHADA
- [ ] Utilisation systématique des services centralisés
- [ ] Documentation complète

### Conformité CGI Cameroun

- [x] TVA 19.25%
- [x] AIR 2.2% / 5.5%
- [x] IRPP Loyer 15%
- [x] TVA non récupérable (VP, représentation, etc.)
- [x] Prorata de TVA (Art. 133)
- [ ] Alertes automatiques fournisseurs sans NIU dans les factures
- [ ] Traçabilité complète via TaxCalculation

### Meilleure pratique logicielle

- [ ] Centralisation des calculs fiscaux
- [ ] Absence de duplication de code
- [ ] Taux configurables (pas hardcodés)
- [ ] Tests unitaires complets
- [ ] Documentation architecture

---

## 📝 Conclusion

### État actuel : ⚠️ PARTIELLEMENT CONFORME

**Points positifs :**
- ✅ Services spécialisés sophistiqués existent
- ✅ Conformité technique OHADA/CGI
- ✅ Gestion avancée (prorata, TVA non récupérable)

**Points à améliorer :**
- ❌ Services non utilisés par les modules métier
- ❌ Calculs manuels dupliqués
- ❌ Taux hardcodés

### Recommandation finale

**REFACTORING URGENT RECOMMANDÉ** pour :
1. Centraliser tous les calculs via `TaxService`
2. Supprimer les calculs manuels
3. Améliorer la traçabilité
4. Éviter la dette technique

**Effort estimé :** 5-7 jours de développement + tests

**Bénéfices :**
- ✅ Code maintenable
- ✅ Conformité garantie
- ✅ Évolutivité (nouveaux taux, nouvelles taxes)
- ✅ Traçabilité complète
- ✅ Alertes automatiques

---

**Auditeur :** Claude Sonnet 4.5 (AI Assistant)
**Validé par :** [À compléter]
