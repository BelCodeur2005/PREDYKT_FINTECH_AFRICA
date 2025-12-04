# 📘 GUIDE COMPLET : RecoverabilityRule - Qu'est-ce que c'est et Comment ça Fonctionne

## 🎯 Table des Matières

1. [Qu'est-ce qu'une RecoverabilityRule ?](#quest-ce-quune-recoverabilityrule)
2. [Explication Simple avec Exemples Concrets](#explication-simple-avec-exemples-concrets)
3. [Anatomie d'une Règle](#anatomie-dune-règle)
4. [Comment le Système Utilise les Règles](#comment-le-système-utilise-les-règles)
5. [Exemples Pratiques d'Utilisation](#exemples-pratiques-dutilisation)
6. [Créer Vos Propres Règles](#créer-vos-propres-règles)
7. [Cas d'Usage Réels](#cas-dusage-réels)

---

## 🤔 Qu'est-ce qu'une RecoverabilityRule ?

### Définition Simple

Une **RecoverabilityRule** (Règle de Récupérabilité) est comme un **détective automatique** qui analyse vos factures et vous dit si la TVA est récupérable ou non.

Imaginez que vous avez un assistant expert-comptable qui connaît par cœur toutes les règles fiscales. Il lit chaque facture et vous dit instantanément :
- ✅ "Cette TVA est récupérable !"
- ❌ "Cette TVA n'est PAS récupérable !"
- ⚠️ "Cette TVA est partiellement récupérable (80%)"

**C'est exactement ça une RecoverabilityRule !**

### Pourquoi en Base de Données ?

Au lieu de programmer les règles dans le code (compliqué à modifier), on les stocke dans une **table PostgreSQL**. Comme ça :

✅ **Facile à modifier** : Pas besoin de recompiler l'application
✅ **Facile à ajouter** : Créer une nouvelle règle = 1 simple INSERT SQL
✅ **Facile à désactiver** : Désactiver une règle = UPDATE is_active = false
✅ **Historique complet** : On garde toutes les stats (combien de fois utilisée, taux de précision, etc.)

---

## 📖 Explication Simple avec Exemples Concrets

### Exemple 1 : La Règle "Véhicule de Tourisme"

Imaginons que vous achetez une voiture. Comment le système sait-il que c'est un véhicule de tourisme ?

#### 🔍 Votre Facture
```
Date : 15/01/2024
Fournisseur : Renault Douala
Compte : 2441 (Matériel de transport)
Description : "Achat Renault Clio berline 5 portes"
Montant HT : 10 000 000 FCFA
TVA 19.25% : 1 925 000 FCFA
```

#### 🤖 Comment le Système Analyse

Le système va chercher dans sa table `recoverability_rules` et trouve cette règle :

```sql
-- Règle n°1 : Véhicules de tourisme
{
  id: 1,
  name: "VP - Termes généraux (FR+EN)",
  priority: 10,
  account_pattern: "^2441",          ← Commence par 2441 ?
  description_pattern: "(?i)\b(tourisme|voiture|berline|citadine|vp)\b",
  excluded_keywords: "utilitaire,camion,vu,fourgon",
  category: "NON_RECOVERABLE_TOURISM_VEHICLE",
  reason: "Véhicule de tourisme - TVA non récupérable"
}
```

#### ✅ Le Système Vérifie (étape par étape)

**Étape 1** : Le compte commence par "2441" ?
```
Compte dans facture : "2441"
Pattern de la règle : "^2441"
→ ✅ OUI, ça matche !
```

**Étape 2** : La description contient "berline" ou "voiture" ?
```
Description : "Achat Renault Clio berline 5 portes"
Pattern : "(tourisme|voiture|berline|citadine|vp)"
→ ✅ OUI, contient "berline" !
```

**Étape 3** : La description contient des mots exclus ?
```
Mots exclus : "utilitaire, camion, vu, fourgon"
Description : "Achat Renault Clio berline 5 portes"
→ ✅ NON, aucun mot exclu présent
```

#### 🎯 Résultat Final

```
✅ Règle appliquée : "VP - Termes généraux (FR+EN)"
Catégorie : NON_RECOVERABLE_TOURISM_VEHICLE
TVA récupérable : 0 FCFA
TVA non récupérable : 1 925 000 FCFA
Raison : "Véhicule de tourisme - TVA non récupérable selon CGI Art. 132"
```

### Exemple 2 : La Règle "Véhicule Utilitaire"

#### 🔍 Votre Facture
```
Date : 16/01/2024
Fournisseur : Renault Douala
Compte : 2441
Description : "Achat Renault Master fourgon L3H2 pour livraisons"
Montant HT : 12 000 000 FCFA
TVA 19.25% : 2 310 000 FCFA
```

#### 🤖 Le Système Analyse

```sql
-- Règle n°7 : Véhicules utilitaires
{
  id: 7,
  name: "VU - Véhicules lourds/utilitaires (FR+EN)",
  priority: 21,
  account_pattern: "^2441",
  description_pattern: "(?i)\b(camion|fourgon|fourgonnette|pick-up)\b",
  excluded_keywords: "tourisme,berline,particulier",
  category: "FULLY_RECOVERABLE",
  reason: "Véhicule utilitaire/poids lourd - TVA 100% récupérable"
}
```

#### ✅ Vérifications

**Étape 1** : Compte = "2441" → ✅ Matche
**Étape 2** : Description contient "fourgon" → ✅ Matche
**Étape 3** : Pas de mots exclus → ✅ OK

#### 🎯 Résultat

```
✅ Règle appliquée : "VU - Véhicules lourds/utilitaires (FR+EN)"
Catégorie : FULLY_RECOVERABLE
TVA récupérable : 2 310 000 FCFA
TVA non récupérable : 0 FCFA
Raison : "Véhicule utilitaire/poids lourd - TVA 100% récupérable"
```

---

## 🔬 Anatomie d'une Règle

Chaque **RecoverabilityRule** dans la base de données a ces champs :

### 1️⃣ **Identification** (Qui est cette règle ?)

```java
id: 1
name: "VP - Termes généraux (FR+EN)"
description: "Détecte les véhicules de tourisme via termes généraux"
rule_type: "VEHICLE"  // VEHICLE, FUEL, REPRESENTATION, LUXURY, PERSONAL
```

**Explication** : Le nom et la description permettent de comprendre rapidement ce que fait la règle.

### 2️⃣ **Priorité et Scoring** (Quelle est son importance ?)

```java
priority: 10           // 1 = plus haute priorité, 100 = plus basse
confidence_score: 95   // 0-100% - À quel point on est sûr de cette règle
```

**Explication** :
- Si 2 règles matchent, celle avec la **priorité la plus petite** gagne
- Le `confidence_score` ajuste le score final (95% = très fiable)

### 3️⃣ **Patterns de Détection** (Comment reconnaître ?)

```java
account_pattern: "^2441"  // Regex pour le compte OHADA
description_pattern: "(?i)\b(tourisme|voiture|berline)\b"  // Regex pour description
```

**Explication des Regex** :
- `^2441` = Commence par "2441"
- `(?i)` = Insensible à la casse (VOITURE = voiture)
- `\b` = Limite de mot (trouve "voiture" mais pas "demi-voiture")
- `(tourisme|voiture|berline)` = OU logique

### 4️⃣ **Mots-clés** (Affiner la détection)

```java
required_keywords: "carburant,utilitaire"  // TOUS doivent être présents
excluded_keywords: "tourisme,berline,vp"   // Si présents, règle NE s'applique PAS
```

**Exemple** :
```
Description : "Carburant diesel pour fourgon utilitaire"
Required : "carburant,utilitaire"  → ✅ Les deux présents
Excluded : "tourisme,berline,vp"   → ✅ Aucun présent
→ Règle applicable !
```

### 5️⃣ **Résultat** (Que doit-on faire ?)

```java
category: "NON_RECOVERABLE_TOURISM_VEHICLE"  // Catégorie de récupérabilité
reason: "Véhicule de tourisme - TVA non récupérable selon CGI Art. 132"
legal_reference: "CGI Art. 132 - Exclusion véhicules de tourisme"
```

**Les 7 catégories possibles** :
1. `FULLY_RECOVERABLE` → 100% récupérable
2. `RECOVERABLE_80_PERCENT` → 80% récupérable (carburant VU)
3. `NON_RECOVERABLE_TOURISM_VEHICLE` → 0% (VP)
4. `NON_RECOVERABLE_FUEL_VP` → 0% (carburant VP)
5. `NON_RECOVERABLE_REPRESENTATION` → 0% (restaurants, cadeaux)
6. `NON_RECOVERABLE_LUXURY` → 0% (golf, yacht, spa)
7. `NON_RECOVERABLE_PERSONAL` → 0% (usage privé, famille)

### 6️⃣ **Machine Learning** (Comment la règle s'améliore ?)

```java
match_count: 1523          // Nombre de fois que la règle a matché
correction_count: 18       // Nombre de corrections manuelles
accuracy_rate: 98.82       // Taux de précision auto-calculé
last_used_at: 2024-01-15   // Dernière utilisation
```

**Calcul automatique** :
```
accuracy_rate = ((match_count - correction_count) / match_count) × 100
                = ((1523 - 18) / 1523) × 100
                = 98.82%
```

### 7️⃣ **État** (Active ou pas ?)

```java
is_active: true  // true = règle active, false = désactivée
```

---

## 🎬 Comment le Système Utilise les Règles

### Flux Complet (du début à la fin)

```
┌─────────────────────────────────────────────────────────┐
│  1. VOUS ENREGISTREZ UNE TRANSACTION                    │
│                                                          │
│  POST /companies/1/general-ledger                       │
│  {                                                       │
│    "accountNumber": "2441",                             │
│    "description": "Achat Renault Clio berline",         │
│    "amount": 11925000  (10M HT + 1.925M TVA)            │
│  }                                                       │
└────────────────────────┬────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│  2. LE SYSTÈME APPELLE LE DÉTECTEUR                     │
│                                                          │
│  VATRecoverabilityService.detectRecoverableCategory()   │
│    → accountNumber = "2441"                             │
│    → description = "Achat Renault Clio berline"         │
└────────────────────────┬────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│  3. LE MOTEUR CHARGE LES 26 RÈGLES DE LA DB            │
│                                                          │
│  SELECT * FROM recoverability_rules                     │
│  WHERE is_active = true                                 │
│  ORDER BY priority ASC                                  │
│                                                          │
│  → 26 règles chargées (avec cache 5 min)               │
└────────────────────────┬────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│  4. NORMALISATION DU TEXTE                              │
│                                                          │
│  TextNormalizer.normalize()                             │
│    Input  : "Achat Renault Clio berline"               │
│    Output : "achat renault clio berline"               │
│                                                          │
│  TextNormalizer.normalizeWithSynonyms()                 │
│    Output : "achat renault clio berline voiture auto"  │
│             (ajout des synonymes)                       │
└────────────────────────┬────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│  5. ÉVALUATION DE CHAQUE RÈGLE (1 par 1)               │
│                                                          │
│  Pour chaque règle (priorité 10, 11, 12, ...) :        │
│                                                          │
│  Règle #1 : VP - Termes généraux                       │
│    ✅ Compte "2441" matche "^2441"         → +20 pts   │
│    ✅ Description contient "berline"       → +30 pts   │
│    ✅ Pas de mots exclus                   → +10 pts   │
│    ✅ Confidence 95%                       → ×0.95     │
│    ✅ Accuracy 98%                         → ×0.98     │
│    ✅ Bonus priorité (100-10)              → +90 pts   │
│                                                          │
│    SCORE TOTAL = 147 points                            │
│                                                          │
│  Règle #2 : VP - Types carrosserie                     │
│    ✅ Compte matche                        → +20 pts   │
│    ✅ Description contient "berline"       → +30 pts   │
│    ...                                                   │
│    SCORE TOTAL = 142 points                            │
│                                                          │
│  Règle #7 : VU - Véhicules utilitaires                │
│    ✅ Compte matche                        → +20 pts   │
│    ❌ Description ne contient pas "fourgon"            │
│    → Règle NON APPLICABLE                              │
└────────────────────────┬────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│  6. TRI PAR SCORE DÉCROISSANT                           │
│                                                          │
│  1. Règle #1 (VP - Termes généraux)    → 147 points   │
│  2. Règle #2 (VP - Carrosserie)        → 142 points   │
│  3. Règle #4 (VP - Modèles)            → 128 points   │
└────────────────────────┬────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│  7. SÉLECTION DU MEILLEUR MATCH + ALTERNATIVES         │
│                                                          │
│  Meilleur : Règle #1 (147 pts)                         │
│  Alternative 1 : Règle #2 (142 pts) - 96% du meilleur │
│  Alternative 2 : Règle #4 (128 pts) - 87% du meilleur │
└────────────────────────┬────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│  8. MISE À JOUR DES STATS DE LA RÈGLE                  │
│                                                          │
│  UPDATE recoverability_rules                            │
│  SET match_count = match_count + 1,                    │
│      last_used_at = NOW()                               │
│  WHERE id = 1                                           │
└────────────────────────┬────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│  9. RÉSULTAT RETOURNÉ                                   │
│                                                          │
│  {                                                       │
│    "category": "NON_RECOVERABLE_TOURISM_VEHICLE",      │
│    "confidence": 95,                                    │
│    "appliedRule": {                                     │
│      "id": 1,                                           │
│      "name": "VP - Termes généraux (FR+EN)",           │
│      "reason": "Véhicule de tourisme - TVA non..."     │
│    },                                                    │
│    "alternatives": [                                    │
│      {                                                   │
│        "category": "NON_RECOVERABLE_TOURISM_VEHICLE",  │
│        "confidence": 92,                                │
│        "reason": "VP par type carrosserie"             │
│      }                                                   │
│    ],                                                    │
│    "executionTimeMicros": 87.5                         │
│  }                                                       │
└────────────────────────┬────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│  10. CRÉATION DE LA VAT_TRANSACTION                     │
│                                                          │
│  INSERT INTO vat_transactions (                         │
│    company_id,                                          │
│    transaction_date,                                    │
│    vat_amount,                                          │
│    recoverable_category,                                │
│    recoverable_percentage,                              │
│    recoverable_vat_amount,                              │
│    non_recoverable_vat_amount                           │
│  ) VALUES (                                             │
│    1,                                                    │
│    '2024-01-15',                                        │
│    1925000,                                             │
│    'NON_RECOVERABLE_TOURISM_VEHICLE',                  │
│    0,                          ← 0% récupérable        │
│    0,                          ← 0 FCFA récupérable    │
│    1925000                     ← 1.925M non récupérable│
│  )                                                       │
└─────────────────────────────────────────────────────────┘
```

---

## 💡 Exemples Pratiques d'Utilisation

### Utilisation Automatique (Recommandé)

Quand vous créez une transaction, le système détecte **automatiquement** :

```java
// Dans votre code Java
VATTransaction transaction = vatRecoverabilityService.recordVATTransaction(
    company,
    ledgerEntry,
    supplier,
    LocalDate.of(2024, 1, 15),
    VATAccountType.VAT_DEDUCTIBLE_IMMOBILIZATIONS,
    "PURCHASE",
    new BigDecimal("10000000"),  // Montant HT
    new BigDecimal("19.25"),      // Taux TVA
    new BigDecimal("1925000"),    // Montant TVA
    null,                         // ← Catégorie = null → Détection AUTO !
    "Achat Renault Clio berline", // Description
    "FACT-2024-001"               // Référence facture
);

// Le système détecte automatiquement :
// → category = NON_RECOVERABLE_TOURISM_VEHICLE
// → recoverablePercentage = 0
// → recoverableVatAmount = 0 FCFA
// → nonRecoverableVatAmount = 1 925 000 FCFA
```

### Utilisation Manuelle (Test/Debug)

Pour tester une règle sans créer de transaction :

```bash
# Via API REST
curl -X POST "http://localhost:8080/api/v1/companies/1/taxes/vat-recoverability/detect" \
  -d "accountNumber=2441" \
  -d "description=Achat Renault Clio berline"

# Résultat JSON
{
  "success": true,
  "data": {
    "category": "NON_RECOVERABLE_TOURISM_VEHICLE",
    "confidence": 95,
    "appliedRule": {
      "id": 1,
      "name": "VP - Termes généraux (FR+EN)",
      "priority": 10,
      "reason": "Véhicule de tourisme - TVA non récupérable selon CGI Art. 132"
    },
    "alternatives": [],
    "executionTimeMicros": 87.5
  }
}
```

---

## 🛠️ Créer Vos Propres Règles

### Cas d'Usage : Détecter les Véhicules Électriques

**Contexte** : Dans votre pays, les véhicules électriques ont une TVA 100% récupérable même s'ils sont des VP.

#### Étape 1 : Définir la Règle

```json
{
  "name": "Véhicules électriques - Incitation fiscale",
  "description": "Les véhicules électriques ont une TVA récupérable même si VP",
  "priority": 9,  // Plus haute que la règle VP (priorité 10)
  "confidenceScore": 90,
  "accountPattern": "^2441",
  "descriptionPattern": "(?i)\\b(electrique|electric|ev|hybride|hybrid|tesla|leaf|zoe|e-tron)\\b",
  "requiredKeywords": null,
  "excludedKeywords": "thermique,essence,diesel",
  "category": "FULLY_RECOVERABLE",
  "reason": "Véhicule électrique - TVA 100% récupérable (incitation fiscale)",
  "legalReference": "Loi de finances 2024 - Art. XX",
  "ruleType": "VEHICLE",
  "isActive": true
}
```

#### Étape 2 : Créer la Règle via API

```bash
curl -X POST "http://localhost:8080/api/v1/companies/1/taxes/vat-recoverability/rules" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Véhicules électriques - Incitation fiscale",
    "description": "Les véhicules électriques ont une TVA récupérable même si VP",
    "priority": 9,
    "confidenceScore": 90,
    "accountPattern": "^2441",
    "descriptionPattern": "(?i)\\\\b(electrique|electric|ev|hybride|hybrid|tesla|leaf|zoe|e-tron)\\\\b",
    "requiredKeywords": null,
    "excludedKeywords": "thermique,essence,diesel",
    "category": "FULLY_RECOVERABLE",
    "reason": "Véhicule électrique - TVA 100% récupérable (incitation fiscale)",
    "legalReference": "Loi de finances 2024 - Art. XX",
    "ruleType": "VEHICLE",
    "isActive": true
}'
```

#### Étape 3 : Tester la Règle

```bash
# Test 1 : Voiture électrique
curl -X POST "http://localhost:8080/api/v1/companies/1/taxes/vat-recoverability/detect" \
  -d "accountNumber=2441" \
  -d "description=Achat Tesla Model 3 voiture électrique"

# Résultat attendu :
# → category = FULLY_RECOVERABLE (grâce à la nouvelle règle !)
# → appliedRule = "Véhicules électriques - Incitation fiscale"

# Test 2 : Voiture thermique (pour vérifier que l'ancienne règle marche encore)
curl -X POST "http://localhost:8080/api/v1/companies/1/taxes/vat-recoverability/detect" \
  -d "accountNumber=2441" \
  -d "description=Achat Renault Clio essence"

# Résultat attendu :
# → category = NON_RECOVERABLE_TOURISM_VEHICLE
# → appliedRule = "VP - Termes généraux"
```

#### Comment ça Fonctionne ?

```
Facture : "Achat Tesla Model 3 voiture électrique"

1. Le système évalue TOUTES les règles par ordre de priorité :

   Règle #NEW (priorité 9) : Véhicules électriques
   ✅ Compte "2441" matche "^2441"
   ✅ Description contient "electrique"
   ❌ PAS de mots exclus ("thermique","essence","diesel")
   → Score = 145 points

   Règle #1 (priorité 10) : VP - Termes généraux
   ✅ Compte "2441" matche
   ✅ Description contient "voiture"
   → Score = 140 points

2. La règle #NEW gagne (score + priorité plus élevés)

3. Résultat : FULLY_RECOVERABLE (100% récupérable) ✅
```

---

## 🎯 Cas d'Usage Réels

### Cas 1 : Entreprise de Transport

**Besoin** : Détecter les péages d'autoroute (TVA 100% récupérable)

```json
{
  "name": "Péages autoroute - Professionnel",
  "priority": 35,
  "accountPattern": "^625",  // Compte Frais de transport
  "descriptionPattern": "(?i)\\b(peage|toll|autoroute|highway|vinci|sanef)\\b",
  "category": "FULLY_RECOVERABLE",
  "reason": "Péage autoroute usage professionnel - TVA récupérable"
}
```

### Cas 2 : Société de Construction

**Besoin** : Détecter les engins de chantier spécifiques

```json
{
  "name": "Engins BTP - Spécifiques",
  "priority": 22,
  "accountPattern": "^2441",
  "descriptionPattern": "(?i)\\b(dumper|compacteur|finisseur|centrale a beton|malaxeur)\\b",
  "category": "FULLY_RECOVERABLE",
  "reason": "Engin de chantier BTP - TVA 100% récupérable"
}
```

### Cas 3 : Cabinet Comptable

**Besoin** : Détecter les abonnements logiciels professionnels

```json
{
  "name": "Logiciels professionnels",
  "priority": 45,
  "accountPattern": "^6183",  // Compte Logiciels
  "descriptionPattern": "(?i)\\b(sage|ebp|ciel|quadratus|office 365|adobe|saas|cloud)\\b",
  "category": "FULLY_RECOVERABLE",
  "reason": "Logiciel professionnel - TVA récupérable"
}
```

### Cas 4 : Entreprise avec Flotte Mixte

**Besoin** : Identifier précisément les VP vs VU par immatriculation

```json
{
  "name": "Identification par immatriculation",
  "priority": 8,  // Très haute priorité
  "accountPattern": "^605|^622",  // Carburant ou location
  "descriptionPattern": "(?i)immat[. ](VP|VT|LT)[-]?[0-9]",  // VP-123, VT-456
  "excludedKeywords": "vu,utilitaire",
  "category": "NON_RECOVERABLE_FUEL_VP",
  "reason": "Immatriculation VP identifiée"
}
```

---

## 📊 Monitoring et Amélioration

### Voir les Statistiques de Vos Règles

```bash
# Statistiques globales
curl http://localhost:8080/api/v1/companies/1/taxes/vat-recoverability/rules/statistics

# Résultat :
{
  "totalRules": 27,           # 26 de base + 1 nouvelle
  "activeRules": 27,
  "totalMatches": 15420,
  "totalCorrections": 78,
  "avgAccuracy": 98.75,       # Excellente précision !
  "rulesNeedingReview": 0
}
```

### Identifier les Règles Problématiques

```bash
# Règles avec accuracy < 70%
curl http://localhost:8080/api/v1/companies/1/taxes/vat-recoverability/rules/needing-review

# Si une règle pose problème :
[
  {
    "id": 13,
    "name": "Carburant générique",
    "accuracyRate": 65.5,
    "matchCount": 450,
    "correctionCount": 155,
    "reason": "⚠️ Trop de corrections - À réviser"
  }
]
```

### Corriger une Règle

```bash
# Désactiver temporairement
curl -X PUT "http://localhost:8080/api/v1/companies/1/taxes/vat-recoverability/rules/13/toggle?active=false"

# Modifier la règle
curl -X PUT "http://localhost:8080/api/v1/companies/1/taxes/vat-recoverability/rules/13" \
  -H "Content-Type: application/json" \
  -d '{
    "priority": 45,
    "descriptionPattern": "(?i)\\b(carburant|fuel)\\b.*(station|pompe|total|shell)\\b"
  }'

# Réactiver
curl -X PUT "http://localhost:8080/api/v1/companies/1/taxes/vat-recoverability/rules/13/toggle?active=true"
```

---

## 🎓 Résumé Final

### Ce qu'il Faut Retenir

1. **RecoverabilityRule = Détective Automatique**
   - Lit vos factures
   - Applique les règles fiscales
   - Vous dit si TVA récupérable ou non

2. **Stockée en Base de Données**
   - 26 règles pré-configurées
   - Facile à modifier sans recompiler
   - Historique et stats automatiques

3. **Système Intelligent**
   - Scoring multi-critères
   - Gestion des priorités
   - Suggestions d'alternatives
   - Machine learning simple (apprend des corrections)

4. **Utilisation Simple**
   - Automatique : Le système détecte seul
   - Manuelle : API REST pour tester
   - Extensible : Créer vos propres règles

5. **Performance**
   - 50-100 microsecondes par détection
   - Cache intelligent
   - Supporte des milliers de transactions/jour

---

## 📞 Questions Fréquentes

**Q: Combien de règles puis-je créer ?**
R: Illimité ! Le système supporte des centaines de règles sans impact performance.

**Q: Que se passe-t-il si 2 règles matchent ?**
R: Le système choisit celle avec le score le plus élevé (priorité + critères matchés).

**Q: Puis-je avoir des règles par entreprise ?**
R: Actuellement, les règles sont globales. Pour du multi-tenant, ajouter `company_id` à la table.

**Q: Comment tester sans affecter la production ?**
R: Utiliser l'endpoint `/detect` qui ne crée pas de transaction.

**Q: Les règles supportent-elles plusieurs langues ?**
R: Oui, FR+EN par défaut. Extensible à d'autres langues via les patterns regex.

---

**Version** : 1.0.0
**Auteur** : PREDYKT Accounting System
**Contact** : support@predykt.com
