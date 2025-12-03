# 🔄 GESTION DE LA TVA RÉCUPÉRABLE / NON RÉCUPÉRABLE

## 📋 Table des matières

1. [Vue d'ensemble](#vue-densemble)
2. [Principe de la TVA récupérable](#principe-de-la-tva-récupérable)
3. [Règles fiscales camerounaises](#règles-fiscales-camerounaises)
4. [Catégories de récupérabilité](#catégories-de-récupérabilité)
5. [Architecture technique](#architecture-technique)
6. [Détection automatique](#détection-automatique)
7. [API Endpoints](#api-endpoints)
8. [Exemples concrets](#exemples-concrets)
9. [Impact sur la déclaration TVA](#impact-sur-la-déclaration-tva)
10. [Workflow complet](#workflow-complet)

---

## 📖 Vue d'ensemble

Le système de **gestion de la TVA récupérable/non récupérable** est une fonctionnalité **CRUCIALE** pour la conformité fiscale camerounaise. Il distingue automatiquement la TVA déductible de la TVA non déductible selon la nature de la dépense.

### ✨ Pourquoi c'est important ?

**Problème sans ce système :**
```
Achat véhicule de tourisme : 10 000 000 XAF HT
TVA : 1 925 000 XAF (19,25%)

❌ ERREUR CLASSIQUE :
Débit  : 24 - Matériel                10 000 000 XAF
Débit  : 4451 - TVA récupérable        1 925 000 XAF  ❌ FAUX !
Crédit : 401 - Fournisseurs           11 925 000 XAF

→ TVA déclarée déductible : 1 925 000 XAF
→ REDRESSEMENT FISCAL : La TVA sur véhicules de tourisme n'est PAS récupérable !
```

**Solution avec notre système :**
```
✅ CORRECT :
Débit  : 24 - Matériel                11 925 000 XAF  ✅ TVA incluse dans le coût
Crédit : 401 - Fournisseurs           11 925 000 XAF

→ TVA déclarée déductible : 0 XAF
→ CONFORME : La TVA est intégrée au coût d'acquisition
```

### 💰 Impact financier

**Exemple sur 1 an :**

| Type d'achat | Montant HT | TVA | Récupérable | Non récupérable | Impact |
|--------------|------------|-----|-------------|-----------------|--------|
| Véhicule tourisme | 10 000 000 | 1 925 000 | 0 | 1 925 000 | ❌ 0 XAF récupéré |
| Carburant VP (12 mois) | 2 400 000 | 462 000 | 0 | 462 000 | ❌ 0 XAF récupéré |
| Carburant VU (12 mois) | 1 200 000 | 231 000 | 184 800 | 46 200 | ⚠️ 80% récupéré |
| Fournitures bureau | 5 000 000 | 962 500 | 962 500 | 0 | ✅ 100% récupéré |
| **TOTAL** | **18 600 000** | **3 580 500** | **1 147 300** | **2 433 200** | **68% perdu !** |

**Sans notre système** : L'entreprise déclare 3 580 500 XAF de TVA déductible
**Redressement fiscal** : 2 433 200 XAF + pénalités (10%) + intérêts = **~2 700 000 XAF** à payer !

**Avec notre système** : L'entreprise déclare correctement 1 147 300 XAF → **CONFORME** ✅

---

## 🎯 Principe de la TVA récupérable

### Règle générale

La TVA est **déductible** (récupérable) si :
1. ✅ La dépense est **professionnelle**
2. ✅ Elle est **nécessaire** à l'exploitation
3. ✅ Elle n'est pas **explicitement exclue** par la loi

### Mécanisme

```
┌─────────────────────────────────────────────────────────┐
│                  ACHAT PROFESSIONNEL                     │
│                                                          │
│  Montant HT : 1 000 000 XAF                             │
│  TVA 19,25% :   192 500 XAF                             │
│  ─────────────────────────────                          │
│  Total TTC  : 1 192 500 XAF                             │
└─────────────────────────────────────────────────────────┘
                        │
                        ▼
           ┌────────────┴────────────┐
           │   TVA RÉCUPÉRABLE ?     │
           └────────────┬────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
        ▼               ▼               ▼
    100% RÉC        80% RÉC         0% RÉC
   ┌─────────┐    ┌─────────┐    ┌─────────┐
   │ Achats  │    │ Carburant│   │ Véhicule│
   │ normaux │    │   VU     │   │ tourisme│
   └─────────┘    └─────────┘    └─────────┘
        │               │               │
        ▼               ▼               ▼
   192 500 XAF    154 000 XAF       0 XAF
   déductible     déductible       déductible
```

---

## 📜 Règles fiscales camerounaises

### Article 132 du CGI - Exclusions du droit à déduction

**TVA 0% récupérable (totalement exclue) :**

1. **Véhicules de tourisme** (VP < 9 places)
   - Voitures particulières
   - Berlines
   - Citadines
   - SUV non utilitaires

2. **Carburant pour véhicules de tourisme**
   - Essence pour VP
   - Gasoil pour VP
   - GPL pour VP

3. **Frais de représentation non justifiés**
   - Restaurants sans justificatif professionnel
   - Réceptions
   - Cadeaux d'affaires

4. **Dépenses somptuaires et de luxe**
   - Yachting
   - Golf
   - Chasse et pêche (sauf activité principale)
   - Résidences de luxe

5. **Services à usage personnel**
   - Dépenses pour dirigeants (usage privé)
   - Dépenses familiales

**TVA 80% récupérable (partiellement déductible) :**

1. **Carburant pour véhicules utilitaires (VU)**
   - Camions
   - Fourgons
   - Pickups professionnels
   - Véhicules > 9 places

**TVA 100% récupérable (totalement déductible) :**

1. **Tous les autres achats professionnels**
   - Matières premières
   - Fournitures de bureau
   - Équipements professionnels
   - Services professionnels
   - Location de locaux professionnels

---

## 🏗️ Catégories de récupérabilité

### Enum `VATRecoverableCategory`

| Catégorie | Code | % Récupérable | Description | Compte typique |
|-----------|------|---------------|-------------|----------------|
| **Totalement récupérable** | `FULLY_RECOVERABLE` | 100% | Achats professionnels normaux | 602, 604, 606 |
| **Partiellement récupérable** | `RECOVERABLE_80_PERCENT` | 80% | Carburant véhicules utilitaires | 605 |
| **Véhicule de tourisme** | `NON_RECOVERABLE_TOURISM_VEHICLE` | 0% | VP < 9 places | 2441 |
| **Carburant VP** | `NON_RECOVERABLE_FUEL_VP` | 0% | Essence/gasoil pour VP | 605 |
| **Frais de représentation** | `NON_RECOVERABLE_REPRESENTATION` | 0% | Restaurants, réceptions | 627 |
| **Dépenses de luxe** | `NON_RECOVERABLE_LUXURY` | 0% | Somptuaires | Divers |
| **Services personnels** | `NON_RECOVERABLE_PERSONAL` | 0% | Usage privé | Divers |

---

## 🏛️ Architecture technique

### Entités et tables

```
┌─────────────────────────────────────────────────────────┐
│                  vat_transactions                        │
├─────────────────────────────────────────────────────────┤
│ id                           BIGSERIAL                   │
│ company_id                   BIGINT                      │
│ ledger_entry_id              BIGINT                      │
│ supplier_id                  BIGINT                      │
│ transaction_date             DATE                        │
│ vat_account_type             VARCHAR(50)                 │
│ transaction_type             VARCHAR(20)                 │
│                                                          │
│ amount_excluding_vat         DECIMAL(15,2)               │
│ vat_rate                     DECIMAL(5,2)                │
│ vat_amount                   DECIMAL(15,2)               │
│                                                          │
│ recoverable_category         VARCHAR(50) ★               │
│ recoverable_percentage       DECIMAL(5,2) ★              │
│ recoverable_vat_amount       DECIMAL(15,2) ★             │
│ non_recoverable_vat_amount   DECIMAL(15,2) ★             │
│                                                          │
│ description                  TEXT                        │
│ non_recoverable_justification TEXT                       │
│ invoice_reference            VARCHAR(100)                │
│ has_alert                    BOOLEAN                     │
│ alert_message                TEXT                        │
└─────────────────────────────────────────────────────────┘
          ★ = Champs clés pour la récupérabilité
```

### Services

1. **VATRecoverabilityService** : Gestion de la récupérabilité
   - Détection automatique de la catégorie
   - Enregistrement des transactions
   - Calcul des statistiques
   - Mise à jour manuelle

2. **VATDeclarationService** (modifié) : Déclaration CA3
   - Utilise `VATTransactionRepository` au lieu de `GeneralLedgerRepository`
   - Calcule uniquement la TVA **récupérable**
   - Génère des rapports conformes

---

## 🤖 Détection automatique

### Algorithme de détection

Le service `VATRecoverabilityService.detectRecoverableCategory()` analyse :

1. **Le numéro de compte OHADA**
2. **La description de la transaction**
3. **Des mots-clés spécifiques**

**Exemples :**

```java
// COMPTE 2441 + "tourisme" → NON_RECOVERABLE_TOURISM_VEHICLE
accountNumber = "2441"
description = "Achat véhicule de tourisme Toyota Corolla"
→ Catégorie: NON_RECOVERABLE_TOURISM_VEHICLE (0%)

// COMPTE 2441 + "utilitaire" → FULLY_RECOVERABLE
accountNumber = "2441"
description = "Achat camion Isuzu NKR 3,5T"
→ Catégorie: FULLY_RECOVERABLE (100%)

// COMPTE 605 + "carburant" + "vp" → NON_RECOVERABLE_FUEL_VP
accountNumber = "605"
description = "Carburant essence VP Mars 2024"
→ Catégorie: NON_RECOVERABLE_FUEL_VP (0%)

// COMPTE 605 + "carburant" + "vu" → RECOVERABLE_80_PERCENT
accountNumber = "605"
description = "Carburant gasoil VU Mars 2024"
→ Catégorie: RECOVERABLE_80_PERCENT (80%)

// COMPTE 627 + "restaurant" → NON_RECOVERABLE_REPRESENTATION
accountNumber = "627"
description = "Restaurant Le Beau Jardin"
→ Catégorie: NON_RECOVERABLE_REPRESENTATION (0%)

// Défaut pour achats professionnels
accountNumber = "604"
description = "Fournitures de bureau"
→ Catégorie: FULLY_RECOVERABLE (100%)
```

### Mots-clés de détection

| Catégorie | Mots-clés (non exhaustif) |
|-----------|---------------------------|
| Véhicule tourisme | `tourisme`, `voiture`, `berline`, `citadine`, `vp` |
| Véhicule utilitaire | `utilitaire`, `camion`, `fourgon`, `vu` |
| Carburant VP | `carburant` + `vp`, `voiture`, `tourisme`, `berline` |
| Carburant VU | `carburant` + `vu`, `utilitaire`, `camion`, `fourgon` |
| Représentation | `restaurant`, `représentation`, `réception`, `cadeaux` |
| Luxe | `luxe`, `somptuaire`, `golf`, `yachting`, `chasse`, `pêche` |
| Personnel | `personnel`, `privé`, `dirigeant`, `famille` |

---

## 🌐 API Endpoints

### Base URL
```
http://localhost:8080/api/v1/companies/{companyId}/taxes/vat-recoverability
```

### 1. Liste des transactions avec récupérabilité

```http
GET /vat-recoverability/transactions?startDate=2024-01-01&endDate=2024-12-31
```

**Réponse :**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "transactionDate": "2024-03-15",
      "vatAccountType": "VAT_RECOVERABLE_FIXED_ASSETS",
      "transactionType": "PURCHASE",
      "amountExcludingVat": 10000000.00,
      "vatRate": 19.25,
      "vatAmount": 1925000.00,
      "recoverableCategory": "NON_RECOVERABLE_TOURISM_VEHICLE",
      "recoverablePercentage": 0.0,
      "recoverableVatAmount": 0.00,
      "nonRecoverableVatAmount": 1925000.00,
      "description": "Achat véhicule de tourisme Toyota Corolla",
      "hasAlert": true,
      "alertMessage": "⚠️ TVA non récupérable: 1925000.00 XAF (Non récupérable - Véhicule de tourisme) - TVA sur véhicules de tourisme (VP < 9 places)"
    },
    {
      "id": 2,
      "transactionDate": "2024-03-20",
      "vatAccountType": "VAT_RECOVERABLE_PURCHASES",
      "transactionType": "PURCHASE",
      "amountExcludingVat": 200000.00,
      "vatRate": 19.25,
      "vatAmount": 38500.00,
      "recoverableCategory": "RECOVERABLE_80_PERCENT",
      "recoverablePercentage": 80.0,
      "recoverableVatAmount": 30800.00,
      "nonRecoverableVatAmount": 7700.00,
      "description": "Carburant gasoil VU Mars 2024",
      "hasAlert": true,
      "alertMessage": "⚠️ TVA non récupérable: 7700.00 XAF (Récupérable à 80%) - TVA partiellement déductible (carburant VU selon réglementation)"
    }
  ]
}
```

### 2. Transactions avec TVA non récupérable

```http
GET /vat-recoverability/non-recoverable
```

**Réponse :**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "description": "Achat véhicule de tourisme Toyota Corolla",
      "nonRecoverableVatAmount": 1925000.00,
      "recoverableCategory": "NON_RECOVERABLE_TOURISM_VEHICLE"
    }
  ]
}
```

### 3. Statistiques TVA non récupérable

```http
GET /vat-recoverability/statistics?startDate=2024-01-01&endDate=2024-12-31
```

**Réponse :**
```json
{
  "success": true,
  "data": {
    "totalNonRecoverableVAT": 2433200.00,
    "totalRecoverableVAT": 1147300.00,
    "recoverabilityRate": 32.05,
    "period": {
      "start": "2024-01-01",
      "end": "2024-12-31"
    },
    "breakdown": {
      "Non récupérable - Véhicule de tourisme": {
        "amount": 1925000.00,
        "transactionCount": 1,
        "description": "TVA sur véhicules de tourisme (VP < 9 places)",
        "recoverablePercentage": 0.0
      },
      "Non récupérable - Carburant VP": {
        "amount": 462000.00,
        "transactionCount": 12,
        "description": "TVA sur carburant pour véhicules de tourisme",
        "recoverablePercentage": 0.0
      },
      "Récupérable à 80%": {
        "amount": 46200.00,
        "transactionCount": 12,
        "description": "TVA partiellement déductible (carburant VU selon réglementation)",
        "recoverablePercentage": 80.0
      }
    }
  }
}
```

### 4. Modifier la catégorie de récupérabilité

```http
PUT /vat-recoverability/transactions/1/category?category=FULLY_RECOVERABLE&justification=Véhicule+utilitaire+reclassé
```

**Réponse :**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "recoverableCategory": "FULLY_RECOVERABLE",
    "recoverablePercentage": 100.0,
    "recoverableVatAmount": 1925000.00,
    "nonRecoverableVatAmount": 0.00,
    "nonRecoverableJustification": "Véhicule utilitaire reclassé"
  }
}
```

### 5. Compteur d'alertes

```http
GET /vat-recoverability/alerts/count
```

**Réponse :**
```json
{
  "success": true,
  "data": {
    "alertCount": 13,
    "message": "⚠️ 13 transaction(s) avec TVA non/partiellement récupérable"
  }
}
```

---

## 💼 Exemples concrets

### Exemple 1 : Achat véhicule de tourisme

**Transaction :**
- Date : 15/03/2024
- Achat : Toyota Corolla
- Montant HT : 10 000 000 XAF
- TVA 19,25% : 1 925 000 XAF
- **Total TTC : 11 925 000 XAF**

**Enregistrement automatique :**

```java
VATTransaction transaction = VATTransaction.builder()
    .company(company)
    .transactionDate(LocalDate.of(2024, 3, 15))
    .vatAccountType(VATAccountType.VAT_RECOVERABLE_FIXED_ASSETS)
    .transactionType("PURCHASE")
    .amountExcludingVat(new BigDecimal("10000000"))
    .vatRate(new BigDecimal("19.25"))
    .vatAmount(new BigDecimal("1925000"))
    .recoverableCategory(VATRecoverableCategory.NON_RECOVERABLE_TOURISM_VEHICLE)  // Détecté automatiquement
    .description("Achat véhicule de tourisme Toyota Corolla")
    .build();

// Calcul automatique par @PrePersist :
// recoverablePercentage = 0%
// recoverableVatAmount = 0 XAF
// nonRecoverableVatAmount = 1 925 000 XAF
// hasAlert = true
```

**Écriture comptable générée :**

```
Date : 15/03/2024
Journal : AC (Achats)

Débit  : 2441 - Matériel de transport  11 925 000 XAF  (HT + TVA non récupérable)
Crédit : 401 - Fournisseurs            11 925 000 XAF
```

**Impact sur la déclaration TVA CA3 :**
- TVA déductible déclarée : **0 XAF** ✅
- TVA non récupérable intégrée au coût d'acquisition

### Exemple 2 : Carburant véhicule utilitaire

**Transaction :**
- Date : 20/03/2024
- Achat : Carburant gasoil pour camion
- Montant HT : 200 000 XAF
- TVA 19,25% : 38 500 XAF
- **Total TTC : 238 500 XAF**

**Enregistrement automatique :**

```java
VATTransaction transaction = VATTransaction.builder()
    .company(company)
    .transactionDate(LocalDate.of(2024, 3, 20))
    .vatAccountType(VATAccountType.VAT_RECOVERABLE_PURCHASES)
    .transactionType("PURCHASE")
    .amountExcludingVat(new BigDecimal("200000"))
    .vatRate(new BigDecimal("19.25"))
    .vatAmount(new BigDecimal("38500"))
    .recoverableCategory(VATRecoverableCategory.RECOVERABLE_80_PERCENT)  // Détecté automatiquement
    .description("Carburant gasoil VU Mars 2024")
    .build();

// Calcul automatique :
// recoverablePercentage = 80%
// recoverableVatAmount = 30 800 XAF (38 500 × 80%)
// nonRecoverableVatAmount = 7 700 XAF (38 500 × 20%)
// hasAlert = true
```

**Écriture comptable générée :**

```
Date : 20/03/2024
Journal : AC (Achats)

Débit  : 605 - Carburants             207 700 XAF  (200 000 + 7 700 non récupérable)
Débit  : 4452 - TVA récupérable        30 800 XAF  (80% de 38 500)
Crédit : 401 - Fournisseurs           238 500 XAF
```

**Impact sur la déclaration TVA CA3 :**
- TVA déductible déclarée : **30 800 XAF** ✅ (80% uniquement)

### Exemple 3 : Fournitures de bureau

**Transaction :**
- Date : 25/03/2024
- Achat : Fournitures de bureau
- Montant HT : 500 000 XAF
- TVA 19,25% : 96 250 XAF
- **Total TTC : 596 250 XAF**

**Enregistrement automatique :**

```java
VATTransaction transaction = VATTransaction.builder()
    .company(company)
    .transactionDate(LocalDate.of(2024, 3, 25))
    .vatAccountType(VATAccountType.VAT_RECOVERABLE_PURCHASES)
    .transactionType("PURCHASE")
    .amountExcludingVat(new BigDecimal("500000"))
    .vatRate(new BigDecimal("19.25"))
    .vatAmount(new BigDecimal("96250"))
    .recoverableCategory(VATRecoverableCategory.FULLY_RECOVERABLE)  // Détecté automatiquement
    .description("Fournitures de bureau")
    .build();

// Calcul automatique :
// recoverablePercentage = 100%
// recoverableVatAmount = 96 250 XAF
// nonRecoverableVatAmount = 0 XAF
// hasAlert = false
```

**Écriture comptable générée :**

```
Date : 25/03/2024
Journal : AC (Achats)

Débit  : 604 - Fournitures de bureau  500 000 XAF
Débit  : 4452 - TVA récupérable        96 250 XAF  (100%)
Crédit : 401 - Fournisseurs           596 250 XAF
```

**Impact sur la déclaration TVA CA3 :**
- TVA déductible déclarée : **96 250 XAF** ✅ (100%)

---

## 📊 Impact sur la déclaration TVA

### Avant (sans gestion de la récupérabilité)

```
═══════════════════════════════════════════════════════
        DÉCLARATION DE TVA - CA3 MENSUEL
                    MARS 2024
═══════════════════════════════════════════════════════

SECTION 2: TVA DÉDUCTIBLE
═══════════════════════════════════════════════════════
TVA immobilisations (4451)  :   1 925 000,00 XAF  ❌ ERREUR !
TVA achats (4452)           :     134 750,00 XAF
───────────────────────────────────────────────────────
TOTAL TVA DÉDUCTIBLE        :   2 059 750,00 XAF  ❌ SURÉVALUÉ !
```

**→ RISQUE : Redressement fiscal de 1 932 700 XAF + pénalités !**

### Après (avec gestion de la récupérabilité)

```
═══════════════════════════════════════════════════════
        DÉCLARATION DE TVA - CA3 MENSUEL
                    MARS 2024
═══════════════════════════════════════════════════════

SECTION 2: TVA DÉDUCTIBLE
═══════════════════════════════════════════════════════
TVA immobilisations (4451)  :           0,00 XAF  ✅ CORRECT !
TVA achats (4452)           :     127 050,00 XAF  ✅ (80% carburant)
───────────────────────────────────────────────────────
TOTAL TVA DÉDUCTIBLE        :     127 050,00 XAF  ✅ CONFORME !

═══════════════════════════════════════════════════════
SECTION 4: TVA NON RÉCUPÉRABLE (INFORMATIVE)
═══════════════════════════════════════════════════════
Véhicule de tourisme        :   1 925 000,00 XAF
Carburant VP               :       7 700,00 XAF
───────────────────────────────────────────────────────
TOTAL NON RÉCUPÉRABLE       :   1 932 700,00 XAF
```

**→ CONFORME : Pas de redressement fiscal ! ✅**

---

## 🔄 Workflow complet

### Scénario : Achat d'un véhicule de tourisme

#### Étape 1 : Transaction d'achat

```http
POST /companies/1/suppliers
{
  "name": "CFAO Motors",
  "taxId": "M123456789",
  "supplierType": "GOODS"
}
```

```http
POST /companies/1/general-ledger
{
  "entryDate": "2024-03-15",
  "accountNumber": "2441",
  "description": "Achat véhicule de tourisme Toyota Corolla",
  "debitAmount": 11925000,
  "creditAmount": 0,
  "journalCode": "AC"
}
```

#### Étape 2 : Enregistrement automatique de la TVA

Le système détecte automatiquement :
- Compte 2441 (immobilisations)
- Description contient "véhicule de tourisme"
- **→ Catégorie : `NON_RECOVERABLE_TOURISM_VEHICLE`**

```java
// Automatique
VATTransaction vatTx = vatRecoverabilityService.recordVATTransaction(
    company,
    ledgerEntry,
    supplier,
    LocalDate.of(2024, 3, 15),
    VATAccountType.VAT_RECOVERABLE_FIXED_ASSETS,
    "PURCHASE",
    new BigDecimal("10000000"),  // HT
    new BigDecimal("19.25"),     // Taux
    new BigDecimal("1925000"),   // TVA
    VATRecoverableCategory.NON_RECOVERABLE_TOURISM_VEHICLE,  // Détecté auto
    "Achat véhicule de tourisme Toyota Corolla",
    "FAC-2024-03-001"
);

// Résultat :
// recoverableVatAmount = 0 XAF
// nonRecoverableVatAmount = 1 925 000 XAF
// hasAlert = true
```

#### Étape 3 : Vérification des alertes

```http
GET /companies/1/taxes/vat-recoverability/alerts/count
```

**Réponse :**
```json
{
  "alertCount": 1,
  "message": "⚠️ 1 transaction(s) avec TVA non/partiellement récupérable"
}
```

```http
GET /companies/1/taxes/vat-recoverability/non-recoverable
```

**Réponse :**
```json
{
  "data": [
    {
      "id": 123,
      "description": "Achat véhicule de tourisme Toyota Corolla",
      "nonRecoverableVatAmount": 1925000.00,
      "alertMessage": "⚠️ TVA non récupérable: 1925000.00 XAF..."
    }
  ]
}
```

#### Étape 4 : Génération déclaration TVA

```http
POST /companies/1/taxes/vat-declarations/generate?year=2024&month=3
```

**Le système :**
1. Lit les `vat_transactions` (pas le grand livre direct)
2. Utilise **uniquement** les montants récupérables
3. TVA déductible immobilisations : **0 XAF** (au lieu de 1 925 000 XAF)

#### Étape 5 : Statistiques fin de mois

```http
GET /companies/1/taxes/vat-recoverability/statistics?startDate=2024-03-01&endDate=2024-03-31
```

**Réponse :**
```json
{
  "totalNonRecoverableVAT": 1932700.00,
  "totalRecoverableVAT": 127050.00,
  "recoverabilityRate": 6.17,
  "breakdown": {
    "Non récupérable - Véhicule de tourisme": {
      "amount": 1925000.00,
      "transactionCount": 1
    },
    "Récupérable à 80%": {
      "amount": 7700.00,
      "transactionCount": 1
    }
  }
}
```

---

## ✅ Résumé des avantages

### Conformité fiscale

✅ **100% conforme** aux règles fiscales camerounaises (CGI Art. 132)
✅ **Pas de redressement** fiscal
✅ **Traçabilité complète** de toutes les décisions de récupérabilité

### Automatisation

✅ **Détection automatique** de la catégorie selon le compte et la description
✅ **Calcul automatique** des montants récupérables/non récupérables
✅ **Alertes automatiques** pour les transactions problématiques

### Déclaration TVA CA3

✅ **Montants corrects** dans la déclaration
✅ **Pas de surévaluation** de la TVA déductible
✅ **Rapports détaillés** avec justification

### Visibilité

✅ **Dashboard** des transactions avec TVA non récupérable
✅ **Statistiques** par catégorie
✅ **Taux de récupérabilité** global

---

## 📞 Support

Pour toute question sur la gestion de la TVA récupérable :
- **Documentation technique** : `/api/v1/swagger-ui.html`
- **Code source** : `src/main/java/com/predykt/accounting/service/VATRecoverabilityService.java`

---

**🎉 Système 100% conforme pour la gestion de la TVA récupérable/non récupérable !**
