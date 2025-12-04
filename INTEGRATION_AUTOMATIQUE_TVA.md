# 🔗 INTÉGRATION AUTOMATIQUE DU SYSTÈME DE TVA

## 📋 Table des matières

1. [Vue d'ensemble](#vue-densemble)
2. [Architecture complète](#architecture-complète)
3. [Flux automatique de détection](#flux-automatique-de-détection)
4. [API REST Endpoints](#api-rest-endpoints)
5. [Exemples pratiques](#exemples-pratiques)
6. [Tests et validation](#tests-et-validation)
7. [Dépannage](#dépannage)

---

## 🎯 Vue d'ensemble

Le système de TVA est maintenant **COMPLÈTEMENT INTÉGRÉ** dans l'application. Voici ce qui se passe automatiquement:

### ✅ Détection Automatique

Quand vous **enregistrez une écriture comptable** avec un compte de TVA déductible (4451x):

```
Écriture comptable enregistrée
        ↓
🔍 Détection automatique du compte 4451
        ↓
🤖 Moteur de règles (26 règles)
        ↓
📊 Application du prorata (si activités mixtes)
        ↓
💾 Enregistrement du calcul complet
        ↓
✅ TVA récupérable calculée automatiquement
```

### 📦 Composants Intégrés

| Composant | Rôle | Status |
|-----------|------|--------|
| **GeneralLedgerService** | Détection automatique des écritures TVA | ✅ Intégré |
| **VATRecoverabilityService** | Calcul 2 étapes (Nature + Prorata) | ✅ Intégré |
| **VATProratService** | Gestion du prorata de TVA | ✅ Intégré |
| **VATRecoverabilityRuleEngine** | 26 règles de détection | ✅ Intégré |
| **VATProrataController** | API REST complète | ✅ Disponible |
| **VATProrataRepository** | Accès données prorata | ✅ Disponible |
| **VATRecoveryCalculationRepository** | Accès calculs TVA | ✅ Disponible |

---

## 🏗️ Architecture complète

### 1. Base de données

```sql
-- Tables principales
vat_prorata                        -- Prorata de TVA par année
vat_recovery_calculation           -- Calculs détaillés de TVA
vat_prorata_history                -- Historique des modifications
vat_recoverability_rules           -- 26 règles de détection

-- Triggers automatiques
calculate_prorata_rate()           -- Calcul auto du taux de prorata
track_prorata_history()            -- Traçabilité auto

-- Vues utiles
v_current_prorata                  -- Prorata actifs
v_recovery_summary                 -- Statistiques de récupération
```

### 2. Couches applicatives

```
┌─────────────────────────────────────────────────────────────┐
│                    COUCHE PRÉSENTATION                       │
│  VATProrataController (10 endpoints REST)                   │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    COUCHE SERVICE                            │
│  • VATProratService (gestion prorata)                       │
│  • VATRecoverabilityService (calcul TVA)                    │
│  • GeneralLedgerService (détection auto)                    │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    COUCHE MÉTIER                             │
│  VATRecoverabilityRuleEngine (26 règles)                    │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    COUCHE DONNÉES                            │
│  • VATProrataRepository                                      │
│  • VATRecoveryCalculationRepository                          │
│  • VATRecoverabilityRuleRepository                           │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔄 Flux automatique de détection

### Étape 1: Enregistrement d'une écriture

```java
// POST /api/v1/companies/1/general-ledger/entries
{
  "entryDate": "2025-01-15",
  "reference": "ACH-2025-001",
  "journalCode": "AC",
  "lines": [
    {
      "accountNumber": "605",           // Charge (HT)
      "description": "Achat carburant",
      "debitAmount": 100000,
      "creditAmount": 0
    },
    {
      "accountNumber": "4451",          // 🎯 TVA déductible (détection!)
      "description": "TVA carburant",
      "debitAmount": 19250,
      "creditAmount": 0
    },
    {
      "accountNumber": "401",           // Fournisseur
      "description": "Total TTC",
      "debitAmount": 0,
      "creditAmount": 119250
    }
  ]
}
```

### Étape 2: Détection automatique

Le **GeneralLedgerService** détecte le compte `4451` et déclenche automatiquement:

```java
// Dans GeneralLedgerService.java (lignes 86-91)
if (isVATDeductibleAccount(line.getAccountNumber())) {
    processVATEntry(company, savedEntry, request.getEntryDate());
}

// Vérifie si compte commence par "4451"
private boolean isVATDeductibleAccount(String accountNumber) {
    return accountNumber != null && accountNumber.startsWith("4451");
}
```

### Étape 3: Calcul en 2 étapes

```java
// Appel automatique au service de récupération
VATRecoveryResult result = vatRecoverabilityService.calculateRecoverableVATWithProrata(
    companyId,
    accountNumber,
    description,      // "Achat carburant"
    htAmount,         // 100 000 FCFA
    vatAmount,        // 19 250 FCFA
    vatRate,          // 0.1925
    fiscalYear,       // 2025
    generalLedgerId   // Lien avec l'écriture
);
```

**ÉTAPE 1 - Détection par nature:**
```
Description: "Achat carburant"
     ↓
🤖 Moteur de règles (scan de 26 règles)
     ↓
✅ Règle détectée: "Carburant VU" (règle #7)
     ↓
📊 Catégorie: VU (Véhicules Utilitaires)
     ↓
💰 Récupération: 80% de 19 250 = 15 400 FCFA
```

**ÉTAPE 2 - Application du prorata:**
```
Récupérable par nature: 15 400 FCFA
     ↓
🔍 Recherche prorata actif pour 2025
     ↓
✅ Prorata trouvé: 85% (activités mixtes)
     ↓
💰 Récupérable final: 15 400 × 0.85 = 13 090 FCFA
```

### Étape 4: Enregistrement et traçabilité

Le calcul est enregistré dans `vat_recovery_calculation`:

```sql
INSERT INTO vat_recovery_calculation (
  company_id,
  general_ledger_id,
  account_number,
  description,
  vat_amount,
  recovery_category,              -- VU
  recovery_by_nature_rate,        -- 0.80
  recoverable_by_nature,          -- 15 400
  prorata_id,                     -- ID du prorata
  prorata_rate,                   -- 0.85
  recoverable_with_prorata,       -- 13 090
  recoverable_vat,                -- 13 090 (final)
  non_recoverable_vat,            -- 6 160
  calculation_date,
  created_at,
  created_by
) VALUES (...);
```

### Étape 5: Log automatique

```
✅ TVA détectée et calculée: 19250 FCFA → 13090 FCFA récupérable (après prorata 85%) - Catégorie: VU - Carburant véhicules utilitaires (80%)
```

---

## 🌐 API REST Endpoints

### 1. Gestion du Prorata

#### POST `/api/v1/companies/{id}/vat-prorata`
Créer ou mettre à jour un prorata manuellement

```bash
curl -X POST http://localhost:8080/api/v1/companies/1/vat-prorata \
  -H "Content-Type: application/json" \
  -d '{
    "fiscalYear": 2025,
    "taxableTurnover": 500000000,
    "exemptTurnover": 100000000,
    "prorataType": "DEFINITIVE",
    "notes": "Prorata définitif basé sur CA réel 2025"
  }'
```

**Réponse:**
```json
{
  "success": true,
  "message": "Prorata Définitif créé/mis à jour avec succès: 83.33% récupérable",
  "data": {
    "id": 1,
    "companyId": 1,
    "companyName": "EXEMPLE SARL",
    "fiscalYear": 2025,
    "taxableTurnover": 500000000,
    "exemptTurnover": 100000000,
    "totalTurnover": 600000000,
    "prorataRate": 0.8333,
    "prorataPercentage": 83.33,
    "prorataType": "DEFINITIVE",
    "isActive": true,
    "isLocked": false,
    "infoMessage": "✅ Définitif - 83.33% récupérable"
  }
}
```

#### POST `/api/v1/companies/{id}/vat-prorata/provisional/{year}`
Créer un prorata provisoire automatiquement (basé sur N-1)

```bash
curl -X POST http://localhost:8080/api/v1/companies/1/vat-prorata/provisional/2025
```

**Réponse:**
```json
{
  "success": true,
  "message": "Prorata provisoire créé: 85.00% (basé sur année 2024)",
  "data": {
    "fiscalYear": 2025,
    "prorataType": "PROVISIONAL",
    "prorataPercentage": 85.00,
    "infoMessage": "⏳ Provisoire - 85.00% récupérable"
  }
}
```

#### POST `/api/v1/companies/{id}/vat-prorata/{year}/convert-definitive`
Convertir un prorata provisoire en définitif

```bash
curl -X POST http://localhost:8080/api/v1/companies/1/vat-prorata/2025/convert-definitive \
  -H "Content-Type: application/json" \
  -d '{
    "fiscalYear": 2025,
    "taxableTurnover": 520000000,
    "exemptTurnover": 95000000,
    "prorataType": "DEFINITIVE"
  }'
```

**Si régularisation nécessaire (écart > 10%):**
```json
{
  "success": true,
  "message": "Prorata définitif: 84.55% récupérable ⚠️ RÉGULARISATION EFFECTUÉE (écart > 10%)",
  "data": {
    "prorataType": "DEFINITIVE",
    "prorataPercentage": 84.55
  }
}
```

#### GET `/api/v1/companies/{id}/vat-prorata/{year}`
Récupérer le prorata actif

```bash
curl http://localhost:8080/api/v1/companies/1/vat-prorata/2025
```

#### GET `/api/v1/companies/{id}/vat-prorata`
Lister tous les prorata (historique)

```bash
curl http://localhost:8080/api/v1/companies/1/vat-prorata
```

**Réponse:**
```json
{
  "success": true,
  "message": "3 prorata(s) trouvé(s)",
  "data": [
    {
      "fiscalYear": 2025,
      "prorataPercentage": 84.55,
      "prorataType": "DEFINITIVE",
      "isLocked": false
    },
    {
      "fiscalYear": 2024,
      "prorataPercentage": 85.00,
      "prorataType": "DEFINITIVE",
      "isLocked": true
    },
    {
      "fiscalYear": 2023,
      "prorataPercentage": 82.00,
      "prorataType": "DEFINITIVE",
      "isLocked": true
    }
  ]
}
```

#### GET `/api/v1/companies/{id}/vat-prorata/{year}/apply?vatAmount=100000`
Simuler l'application du prorata

```bash
curl "http://localhost:8080/api/v1/companies/1/vat-prorata/2025/apply?vatAmount=100000"
```

**Réponse:**
```json
{
  "success": true,
  "message": "TVA récupérable: 84550 FCFA sur 100000 FCFA (84.55%)",
  "data": 84550
}
```

#### POST `/api/v1/vat-prorata/{id}/lock`
Verrouiller un prorata (clôture fiscale)

```bash
curl -X POST http://localhost:8080/api/v1/vat-prorata/1/lock
```

#### DELETE `/api/v1/vat-prorata/{id}`
Supprimer un prorata (si non verrouillé)

```bash
curl -X DELETE http://localhost:8080/api/v1/vat-prorata/1
```

#### GET `/api/v1/companies/{id}/vat-prorata/{year}/exists`
Vérifier si un prorata existe

```bash
curl http://localhost:8080/api/v1/companies/1/vat-prorata/2025/exists
```

---

## 💼 Exemples pratiques

### Exemple 1: Entreprise 100% taxable (pas de prorata)

**Contexte:** Entreprise de services sans exports

**Étape 1:** Pas de prorata défini
```bash
curl http://localhost:8080/api/v1/companies/1/vat-prorata/2025
# → Aucun prorata → 100% activités taxables
```

**Étape 2:** Enregistrer un achat
```json
{
  "lines": [
    {"accountNumber": "605", "debitAmount": 100000},
    {"accountNumber": "4451", "debitAmount": 19250}
  ]
}
```

**Résultat automatique:**
```
🔍 Détection: Compte 4451
✅ Catégorie: VU (80%)
✅ Récupérable par nature: 15 400 FCFA
✅ Prorata: Aucun → 100%
✅ RÉCUPÉRABLE FINAL: 15 400 FCFA
```

### Exemple 2: Exportateur (activités mixtes)

**Contexte:** Entreprise avec 70% export (exonéré) + 30% local (taxable)

**Étape 1:** Créer le prorata
```bash
curl -X POST http://localhost:8080/api/v1/companies/2/vat-prorata \
  -H "Content-Type: application/json" \
  -d '{
    "fiscalYear": 2025,
    "taxableTurnover": 300000000,
    "exemptTurnover": 700000000,
    "prorataType": "DEFINITIVE"
  }'

# → Prorata: 30%
```

**Étape 2:** Enregistrer un achat
```json
{
  "lines": [
    {"accountNumber": "605", "description": "Matières premières", "debitAmount": 1000000},
    {"accountNumber": "4451", "debitAmount": 192500}
  ]
}
```

**Résultat automatique:**
```
🔍 Détection: Compte 4451 - Matières premières
✅ ÉTAPE 1 (Nature): VU (100%) → 192 500 FCFA
✅ ÉTAPE 2 (Prorata): 30% × 192 500 = 57 750 FCFA
✅ RÉCUPÉRABLE FINAL: 57 750 FCFA
⚠️ NON RÉCUPÉRABLE: 134 750 FCFA (impact prorata)
```

### Exemple 3: Année N avec prorata provisoire puis définitif

**Janvier 2025:** Créer prorata provisoire
```bash
# Basé sur 2024: 85%
curl -X POST http://localhost:8080/api/v1/companies/3/vat-prorata/provisional/2025

# → Prorata provisoire: 85%
```

**Janvier-Décembre 2025:** Toutes les écritures TVA utilisent 85%
```
Achat #1: 10 000 FCFA TVA → 8 500 FCFA récupérable (85%)
Achat #2: 50 000 FCFA TVA → 42 500 FCFA récupérable (85%)
...
```

**Janvier 2026:** Convertir en définitif avec CA réel 2025
```bash
curl -X POST http://localhost:8080/api/v1/companies/3/vat-prorata/2025/convert-definitive \
  -d '{
    "fiscalYear": 2025,
    "taxableTurnover": 600000000,
    "exemptTurnover": 50000000
  }'

# → Prorata définitif: 92.31%
# ⚠️ Écart: 85% → 92.31% = 7.31% → Régularisation nécessaire!
```

**Régularisation automatique:**
- Prorata provisoire: 85%
- Prorata définitif: 92.31%
- Écart: 7.31% > 10% threshold? NON → Pas de régularisation obligatoire
- Si écart > 10%: Régularisation sur déclaration TVA de mars N+1

---

## 🧪 Tests et validation

### Test 1: Détection automatique

```bash
# 1. Créer une entreprise
curl -X POST http://localhost:8080/api/v1/companies \
  -d '{"name": "TEST SARL", "taxIdentificationNumber": "M123456789"}'

# 2. Enregistrer une écriture avec TVA
curl -X POST http://localhost:8080/api/v1/companies/1/general-ledger/entries \
  -d '{
    "entryDate": "2025-01-15",
    "reference": "TEST-001",
    "journalCode": "AC",
    "lines": [
      {"accountNumber": "605", "debitAmount": 100000, "creditAmount": 0},
      {"accountNumber": "4451", "debitAmount": 19250, "creditAmount": 0},
      {"accountNumber": "401", "debitAmount": 0, "creditAmount": 119250}
    ]
  }'

# 3. Vérifier les logs
# → ✅ TVA détectée et calculée automatiquement
```

### Test 2: Avec prorata

```bash
# 1. Créer un prorata
curl -X POST http://localhost:8080/api/v1/companies/1/vat-prorata \
  -d '{"fiscalYear": 2025, "taxableTurnover": 500000000, "exemptTurnover": 100000000, "prorataType": "DEFINITIVE"}'

# → Prorata: 83.33%

# 2. Enregistrer une écriture TVA
curl -X POST http://localhost:8080/api/v1/companies/1/general-ledger/entries \
  -d '{
    "lines": [
      {"accountNumber": "605", "debitAmount": 100000},
      {"accountNumber": "4451", "debitAmount": 19250}
    ]
  }'

# 3. Vérifier le calcul
# → ✅ TVA calculée avec prorata 83.33%
```

### Test 3: Différentes catégories

```bash
# Carburant VP (0%)
curl -X POST .../entries -d '{"description": "Essence voiture de tourisme", ...}'
# → 0% récupérable

# Carburant VU (80%)
curl -X POST .../entries -d '{"description": "Gasoil camion", ...}'
# → 80% récupérable

# Matériel (100%)
curl -X POST .../entries -d '{"description": "Ordinateur bureau", ...}'
# → 100% récupérable
```

---

## 🔧 Dépannage

### Problème 1: TVA non détectée automatiquement

**Symptôme:** Aucun calcul de TVA n'apparaît dans les logs

**Vérifications:**

1. **Le compte est-il correct?**
   ```bash
   # Doit commencer par "4451"
   grep "4451" general_ledger_entries.json
   ```

2. **Le montant est-il au débit?**
   ```json
   {
     "accountNumber": "4451",
     "debitAmount": 19250,   // ✅ OK
     "creditAmount": 0
   }
   ```

3. **Le service est-il injecté?**
   ```java
   // Dans GeneralLedgerService.java
   private final VATRecoverabilityService vatRecoverabilityService;
   ```

### Problème 2: Prorata non appliqué

**Symptôme:** Le prorata existe mais n'est pas appliqué

**Vérifications:**

1. **Le prorata est-il actif?**
   ```bash
   curl http://localhost:8080/api/v1/companies/1/vat-prorata/2025
   # → Vérifier: isActive = true
   ```

2. **L'année fiscale correspond-elle?**
   ```sql
   SELECT * FROM vat_prorata
   WHERE company_id = 1
     AND fiscal_year = 2025
     AND is_active = true;
   ```

3. **Le type est-il correct?**
   ```
   ✅ PROVISIONAL ou DEFINITIVE
   ❌ Pas d'autres types
   ```

### Problème 3: Règle de détection non trouvée

**Symptôme:** Catégorie "VU" attribuée par défaut

**Vérifications:**

1. **Les règles sont-elles chargées?**
   ```sql
   SELECT COUNT(*) FROM vat_recoverability_rules;
   -- Doit retourner 26
   ```

2. **La description est-elle assez précise?**
   ```
   ❌ "Achat"           → Trop vague
   ✅ "Achat carburant" → Précis
   ```

3. **Les patterns sont-ils corrects?**
   ```sql
   SELECT * FROM vat_recoverability_rules
   WHERE LOWER('achat carburant') LIKE '%' || LOWER(pattern) || '%';
   ```

### Problème 4: Calcul incorrect

**Symptôme:** Montant récupérable ne correspond pas

**Vérifications:**

1. **Vérifier le calcul étape par étape:**
   ```sql
   SELECT
     vat_amount,
     recovery_by_nature_rate,
     recoverable_by_nature,
     prorata_rate,
     recoverable_with_prorata,
     recoverable_vat
   FROM vat_recovery_calculation
   WHERE id = <calculation_id>;
   ```

2. **Formule ÉTAPE 1:**
   ```
   Récupérable = TVA × Taux par nature
   Exemple: 19 250 × 0.80 = 15 400 FCFA
   ```

3. **Formule ÉTAPE 2:**
   ```
   Récupérable final = Récupérable nature × Prorata
   Exemple: 15 400 × 0.85 = 13 090 FCFA
   ```

### Problème 5: Prorata verrouillé

**Symptôme:** Impossible de modifier le prorata

**Solution:**

```bash
# Vérifier le statut
curl http://localhost:8080/api/v1/companies/1/vat-prorata/2024

# Si isLocked = true → Clôture fiscale effectuée
# → Créer un nouveau prorata pour l'année suivante
curl -X POST http://localhost:8080/api/v1/companies/1/vat-prorata/provisional/2025
```

---

## 📊 Statistiques et rapports

### Récupérer les statistiques de TVA

```bash
# Via le service Java
VATRecoveryStatistics stats = vatRecoverabilityService.getRecoveryStatistics(companyId, 2025);
```

**Contenu:**
- Montant total de TVA
- Montant total récupérable
- Montant total non récupérable
- Taux moyen de récupération
- Impact du prorata (en FCFA et %)
- Répartition par catégorie (VU, VP, VER, etc.)

### Vue SQL pour rapports

```sql
-- Vue d'ensemble des calculs
SELECT
  c.name AS company_name,
  vrc.fiscal_year,
  COUNT(*) AS nb_calculations,
  SUM(vrc.vat_amount) AS total_vat,
  SUM(vrc.recoverable_vat) AS total_recoverable,
  SUM(vrc.non_recoverable_vat) AS total_non_recoverable,
  ROUND(AVG(vrc.recoverable_vat / vrc.vat_amount * 100), 2) AS avg_recovery_rate
FROM vat_recovery_calculation vrc
JOIN companies c ON c.id = vrc.company_id
WHERE vrc.fiscal_year = 2025
GROUP BY c.name, vrc.fiscal_year;
```

---

## 🎓 Résumé

### ✅ Ce qui est automatique

1. **Détection des comptes TVA** (4451x)
2. **Application des 26 règles de récupération**
3. **Calcul du prorata** (si défini)
4. **Enregistrement de la traçabilité complète**
5. **Logs détaillés** de chaque calcul

### 🎯 Ce que vous devez faire

1. **Configurer le prorata** (si activités mixtes):
   - Créer un prorata provisoire en début d'année
   - Convertir en définitif en fin d'année

2. **Enregistrer vos écritures normalement**:
   - Le système détecte automatiquement la TVA
   - Aucune action supplémentaire nécessaire

3. **Consulter les calculs** via l'API:
   - Statistiques par période
   - Détail par transaction
   - Impact du prorata

### 📚 Références

- **Migration:** `V12__add_vat_prorata_system.sql`
- **Entités:** `VATProrata.java`, `VATRecoveryCalculation.java`
- **Services:** `VATProratService.java`, `VATRecoverabilityService.java`, `GeneralLedgerService.java`
- **Controller:** `VATProrataController.java`
- **Documentation système:** `SYSTEME_PRORATA_TVA_README.md`
- **Guide des règles:** `RECOVERABILITY_RULE_GUIDE.md`
- **Conformité:** CGI Cameroun Articles 132, 133, 134

---

## ✨ Support

Pour toute question ou problème:

1. Vérifier les logs de l'application
2. Consulter les vues PostgreSQL (`v_current_prorata`, `v_recovery_summary`)
3. Tester avec les endpoints de simulation (`/apply`)
4. Consulter cette documentation

**Le système est maintenant COMPLET et OPÉRATIONNEL!** 🚀
