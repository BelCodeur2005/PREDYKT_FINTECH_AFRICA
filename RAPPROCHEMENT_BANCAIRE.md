# 🏦 GUIDE COMPLET DU RAPPROCHEMENT BANCAIRE OHADA

## 📚 Table des matières

1. [Qu'est-ce qu'un rapprochement bancaire ?](#quest-ce-quun-rapprochement-bancaire)
2. [Pourquoi est-ce obligatoire selon OHADA ?](#pourquoi-est-ce-obligatoire-selon-ohada)
3. [Le problème : Pourquoi les soldes diffèrent](#le-problème--pourquoi-les-soldes-diffèrent)
4. [La solution : L'état de rapprochement](#la-solution--létat-de-rapprochement)
5. [Exemple pratique étape par étape](#exemple-pratique-étape-par-étape)
6. [Utilisation de l'API](#utilisation-de-lapi)
7. [Workflow de validation](#workflow-de-validation)
8. [Types d'opérations en suspens](#types-dopérations-en-suspens)
9. [Cas d'usage réels](#cas-dusage-réels)

---

## Qu'est-ce qu'un rapprochement bancaire ?

Le **rapprochement bancaire** est une procédure comptable qui consiste à **vérifier que le solde de votre compte bancaire selon la banque correspond au solde de votre comptabilité** (compte 52X dans le plan comptable OHADA).

### 🎯 Objectif simple

Répondre à cette question : **"Pourquoi le solde que je vois sur mon relevé bancaire est différent du solde dans ma comptabilité ?"**

---

## Pourquoi est-ce obligatoire selon OHADA ?

Selon le **Système Comptable OHADA (SYSCOHADA)** :

1. ✅ **Obligation mensuelle** : Chaque entreprise DOIT faire un rapprochement bancaire **au moins une fois par mois**
2. ✅ **Contrôle interne** : C'est un contrôle comptable obligatoire pour détecter les erreurs et fraudes
3. ✅ **Audit** : Les auditeurs/commissaires aux comptes vérifient TOUJOURS les rapprochements bancaires
4. ✅ **Liasse fiscale** : Nécessaire pour justifier les écritures de régularisation

---

## Le problème : Pourquoi les soldes diffèrent

### 📊 Situation typique

```
📅 Au 31 décembre 2024

🏦 Relevé bancaire dit :        2 500 000 FCFA
📖 Ma comptabilité dit :         2 100 000 FCFA

❓ ÉCART = 400 000 FCFA - POURQUOI ???
```

### 🔍 Raisons courantes de l'écart

#### A) Opérations enregistrées en comptabilité mais PAS ENCORE sur le relevé

1. **Chèques émis non encaissés**
   - Vous avez écrit un chèque de 150 000 FCFA à un fournisseur le 28/12
   - Vous l'avez enregistré dans votre comptabilité le 28/12
   - MAIS le fournisseur ne l'a déposé que le 5/01
   - ➡️ La banque ne l'a pas encore débité au 31/12

2. **Virements en attente**
   - Vous avez fait un virement de 50 000 FCFA le 31/12 à 16h
   - Enregistré en compta le 31/12
   - MAIS traité par la banque le 2/01
   - ➡️ N'apparaît pas sur le relevé de décembre

#### B) Opérations sur le relevé bancaire mais PAS ENCORE en comptabilité

1. **Frais bancaires prélevés**
   - La banque a prélevé 25 000 FCFA de frais de tenue de compte
   - Vous ne le savez que quand vous recevez le relevé
   - ➡️ Pas encore enregistré dans votre comptabilité

2. **Virements reçus non comptabilisés**
   - Un client a viré 300 000 FCFA directement
   - Apparaît sur le relevé
   - MAIS vous ne l'avez pas encore saisi en comptabilité

3. **Prélèvements automatiques**
   - Assurance, électricité, téléphone prélevés automatiquement
   - Sur le relevé mais pas encore enregistrés

---

## La solution : L'état de rapprochement

### 📝 Structure de l'état OHADA

L'état de rapprochement est un document en **3 sections** qui explique l'écart :

```
┌─────────────────────────────────────────────────────────────┐
│        ÉTAT DE RAPPROCHEMENT BANCAIRE                       │
│        Entreprise ABC - Compte 521001                       │
│        Au 31 décembre 2024                                  │
└─────────────────────────────────────────────────────────────┘

╔═════════════════════════════════════════════════════════════╗
║ A) SOLDE SELON RELEVÉ BANCAIRE                              ║
╚═════════════════════════════════════════════════════════════╝

Solde selon relevé bancaire                    2 500 000 FCFA
(+) Chèques émis non encaissés                   150 000 FCFA
     • Chèque n°12345 - Fournisseur XYZ          150 000
(-) Virements en attente                          50 000 FCFA
     • Virement du 31/12 vers fournisseur Y       50 000
(+/-) Erreurs bancaires                                0 FCFA
                                              ─────────────────
= SOLDE BANCAIRE RECTIFIÉ (A)                  2 600 000 FCFA


╔═════════════════════════════════════════════════════════════╗
║ B) SOLDE SELON LIVRE COMPTABLE                              ║
╚═════════════════════════════════════════════════════════════╝

Solde selon livre (compte 521)                 2 100 000 FCFA
(+) Virements reçus non comptabilisés            300 000 FCFA
     • Client Z - Virement du 30/12              300 000
(-) Frais bancaires non enregistrés               25 000 FCFA
     • Frais tenue de compte décembre             25 000
(-) Prélèvements non comptabilisés               225 000 FCFA
     • Assurance                                   75 000
     • Électricité ENEO                           100 000
     • Téléphone Orange                            50 000
(+/-) Erreurs comptables                                0 FCFA
                                              ─────────────────
= SOLDE LIVRE RECTIFIÉ (B)                     2 600 000 FCFA


╔═════════════════════════════════════════════════════════════╗
║ C) ÉCART                                                     ║
╚═════════════════════════════════════════════════════════════╝

ÉCART = (A) - (B)                                       0 FCFA

✅ RAPPROCHEMENT ÉQUILIBRÉ

─────────────────────────────────────────────────────────────
Préparé par: Marie KOUASSI      Date: 05/01/2025
Vérifié par: Jean DIALLO        Date: 06/01/2025
Approuvé par: Fatou MBAYE       Date: 07/01/2025
─────────────────────────────────────────────────────────────
```

### ✅ Principe fondamental

**L'écart DOIT TOUJOURS être = 0 à la fin !**

Si écart ≠ 0 :
- 🔴 Il y a une erreur quelque part
- 🔴 Il faut chercher jusqu'à trouver
- 🔴 Le rapprochement ne peut pas être approuvé

---

## Exemple pratique étape par étape

### 📅 Contexte

Vous êtes comptable de l'entreprise **"SAVANA SARL"**.
Vous devez faire le rapprochement bancaire du **compte 521001** au **31/12/2024**.

### Étape 1️⃣ : Récupérer les informations

```
🏦 RELEVÉ BANCAIRE (reçu de la banque)
   Solde au 31/12/2024 : 5 000 000 FCFA

📖 COMPTABILITÉ (Grand Livre compte 521)
   Solde au 31/12/2024 : 4 200 000 FCFA

❓ ÉCART = 800 000 FCFA
```

### Étape 2️⃣ : Analyser les écarts

Vous comparez ligne par ligne le relevé bancaire et votre journal de banque.

**🔍 Ce que vous trouvez :**

| Opération | Comptabilité | Relevé banque | Explication |
|-----------|-------------|---------------|-------------|
| Chèque n°001 du 28/12 - 300 000 | ✅ Enregistré | ❌ Absent | Fournisseur pas encore encaissé |
| Chèque n°002 du 30/12 - 150 000 | ✅ Enregistré | ❌ Absent | Pas encore présenté |
| Virement reçu du 29/12 - 500 000 | ❌ Absent | ✅ Présent | Client a viré, vous ne saviez pas |
| Frais bancaires - 50 000 | ❌ Absent | ✅ Présent | Prélevé par la banque |

### Étape 3️⃣ : Remplir l'état de rapprochement

#### Section A : Ajuster le solde bancaire

```
Solde relevé                          5 000 000
(+) Chèques non encaissés               450 000  (300 000 + 150 000)
(-) Virements en attente                      0
= Solde bancaire rectifié             5 450 000
```

#### Section B : Ajuster le solde comptable

```
Solde comptable                       4 200 000
(+) Virements reçus non enregistrés     500 000
(-) Frais bancaires non enregistrés      50 000
(-) Autres prélèvements                  700 000  (on a trouvé d'autres prélèvements)
= Solde comptable rectifié            5 450 000
```

#### Section C : Vérifier

```
ÉCART = 5 450 000 - 5 450 000 = 0 ✅
```

### Étape 4️⃣ : Passer les écritures de régularisation

Maintenant que vous avez identifié les écarts, vous devez **enregistrer en comptabilité** les opérations qui étaient sur le relevé mais pas dans vos livres :

**Écriture 1 : Virement reçu**
```
521 Banque                     500 000
    411 Client                         500 000
Virement reçu non comptabilisé
```

**Écriture 2 : Frais bancaires**
```
627 Frais bancaires             50 000
    521 Banque                          50 000
Frais de tenue de compte
```

**Écriture 3 : Prélèvements automatiques**
```
605 Autres charges             700 000
    521 Banque                         700 000
Prélèvements électricité, assurance
```

### Étape 5️⃣ : Vérification finale

Après avoir passé ces écritures, votre **nouveau solde comptable = 5 450 000 FCFA**.

✅ Ce solde explique parfaitement le solde bancaire en tenant compte des chèques en circulation.

---

## Utilisation de l'API

### 1️⃣ Créer un nouveau rapprochement

```bash
POST /api/v1/companies/1/bank-reconciliations
Content-Type: application/json

{
  "company": {
    "id": 1
  },
  "reconciliationDate": "2024-12-31",
  "periodStart": "2024-12-01",
  "periodEnd": "2024-12-31",
  "bankAccountNumber": "521001",
  "bankName": "BICEC",
  "bankStatementBalance": 5000000,
  "bookBalance": 4200000,
  "glAccountNumber": "521",
  "notes": "Rapprochement mensuel décembre 2024"
}
```

**Réponse :**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "reconciliationDate": "2024-12-31",
    "bankAccountNumber": "521001",
    "bankStatementBalance": 5000000.00,
    "bookBalance": 4200000.00,
    "adjustedBankBalance": 5000000.00,
    "adjustedBookBalance": 4200000.00,
    "difference": 800000.00,
    "isBalanced": false,
    "status": "DRAFT"
  }
}
```

### 2️⃣ Ajouter des opérations en suspens

#### Ajouter un chèque non encaissé

```bash
POST /api/v1/companies/1/bank-reconciliations/1/items
Content-Type: application/json

{
  "itemType": "CHEQUE_ISSUED_NOT_CASHED",
  "transactionDate": "2024-12-28",
  "amount": 300000,
  "description": "Chèque n°001 - Fournisseur SOTRAFER",
  "reference": "CHQ-001",
  "thirdParty": "SOTRAFER"
}
```

#### Ajouter des frais bancaires non enregistrés

```bash
POST /api/v1/companies/1/bank-reconciliations/1/items
Content-Type: application/json

{
  "itemType": "BANK_FEES_NOT_RECORDED",
  "transactionDate": "2024-12-31",
  "amount": 50000,
  "description": "Frais de tenue de compte - Décembre",
  "reference": "FRAIS-12-2024"
}
```

#### Ajouter un virement reçu non comptabilisé

```bash
POST /api/v1/companies/1/bank-reconciliations/1/items
Content-Type: application/json

{
  "itemType": "CREDIT_NOT_RECORDED",
  "transactionDate": "2024-12-29",
  "amount": 500000,
  "description": "Virement client GOLDEN TRUST",
  "reference": "VIR-2024-12-29-001",
  "thirdParty": "GOLDEN TRUST"
}
```

**⚡ Calcul automatique :**
À chaque ajout d'opération, l'API recalcule automatiquement :
- Les soldes rectifiés
- L'écart
- Le statut d'équilibre

### 3️⃣ Vérifier l'état du rapprochement

```bash
GET /api/v1/companies/1/bank-reconciliations/1
```

**Réponse :**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "reconciliationDate": "2024-12-31",
    "bankStatementBalance": 5000000.00,
    "chequesIssuedNotCashed": 450000.00,
    "depositsInTransit": 0.00,
    "adjustedBankBalance": 5450000.00,
    "bookBalance": 4200000.00,
    "creditsNotRecorded": 500000.00,
    "bankFeesNotRecorded": 50000.00,
    "debitsNotRecorded": 700000.00,
    "adjustedBookBalance": 5450000.00,
    "difference": 0.00,
    "isBalanced": true,
    "status": "DRAFT",
    "pendingItems": [
      {
        "itemType": "CHEQUE_ISSUED_NOT_CASHED",
        "amount": 300000,
        "description": "Chèque n°001 - Fournisseur SOTRAFER"
      },
      {
        "itemType": "CHEQUE_ISSUED_NOT_CASHED",
        "amount": 150000,
        "description": "Chèque n°002 - Fournisseur ABC"
      },
      {
        "itemType": "CREDIT_NOT_RECORDED",
        "amount": 500000,
        "description": "Virement client GOLDEN TRUST"
      },
      {
        "itemType": "BANK_FEES_NOT_RECORDED",
        "amount": 50000,
        "description": "Frais de tenue de compte"
      }
    ]
  }
}
```

### 4️⃣ Soumettre pour validation

Une fois que `isBalanced = true` (écart = 0), vous pouvez soumettre :

```bash
POST /api/v1/companies/1/bank-reconciliations/1/submit?preparedBy=Marie+KOUASSI
```

**Réponse :**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "status": "PENDING_REVIEW",
    "preparedBy": "Marie KOUASSI",
    "preparedAt": "2025-01-05T10:30:00"
  },
  "message": "Rapprochement soumis pour révision"
}
```

### 5️⃣ Approuver le rapprochement

Le responsable approuve :

```bash
POST /api/v1/companies/1/bank-reconciliations/1/approve?approvedBy=Fatou+MBAYE
```

### 6️⃣ Exporter en PDF

```bash
GET /api/v1/companies/1/exports/bank-reconciliation/1/pdf
```

Télécharge un PDF professionnel avec l'état de rapprochement complet.

### 7️⃣ Exporter en Excel

```bash
GET /api/v1/companies/1/exports/bank-reconciliation/1/excel
```

---

## Workflow de validation

### 🔄 Circuit d'approbation

```
┌─────────────┐
│   DRAFT     │  ← Comptable prépare le rapprochement
│ (Brouillon) │    - Saisit le solde banque et livre
└──────┬──────┘    - Ajoute les opérations en suspens
       │           - Équilibre doit être = 0
       │
       │ submit()
       ↓
┌──────────────────┐
│ PENDING_REVIEW   │  ← En attente de révision
│ (En attente)     │    - Rapprochement équilibré
└──────┬───────────┘    - Attend validation comptable
       │
       │ approve() ou reject()
       ↓
┌──────────────┐         ┌──────────────┐
│   REVIEWED   │    OU   │   REJECTED   │
│  (Révisé)    │         │  (Rejeté)    │
└──────┬───────┘         └──────┬───────┘
       │                        │
       │ approve()              │ → Retour en DRAFT
       ↓                        │    pour correction
┌──────────────┐                │
│   APPROVED   │ ←──────────────┘
│  (Approuvé)  │
└──────┬───────┘
       │
       │ (optionnel)
       ↓
┌──────────────┐
│   ARCHIVED   │  ← Archivé pour l'historique
│  (Archivé)   │
└──────────────┘
```

### ⚠️ Règles importantes

1. **Seul un rapprochement ÉQUILIBRÉ peut être soumis**
   - Si `isBalanced = false`, le bouton "Soumettre" est désactivé

2. **Modification uniquement en DRAFT ou REJECTED**
   - Une fois approuvé, on ne peut plus modifier

3. **Traçabilité complète**
   - Qui a préparé + quand
   - Qui a approuvé + quand
   - Raison du rejet si rejeté

---

## Types d'opérations en suspens

### 📋 Tableau récapitulatif

| Type | Code | Affecte | Ajouter/Soustraire | Exemple |
|------|------|---------|-------------------|---------|
| **Chèques émis non encaissés** | `CHEQUE_ISSUED_NOT_CASHED` | Solde banque | ➕ Ajouter | Chèque écrit mais pas encore présenté |
| **Dépôts en transit** | `DEPOSIT_IN_TRANSIT` | Solde banque | ➖ Soustraire | Virement fait tard, traité le lendemain |
| **Erreur bancaire** | `BANK_ERROR` | Solde banque | ➕/➖ | Banque a débité 2 fois par erreur |
| **Virements reçus non enregistrés** | `CREDIT_NOT_RECORDED` | Solde livre | ➕ Ajouter | Client a viré, vous ne le saviez pas |
| **Prélèvements non enregistrés** | `DEBIT_NOT_RECORDED` | Solde livre | ➖ Soustraire | Prélèvement automatique inconnu |
| **Frais bancaires non enregistrés** | `BANK_FEES_NOT_RECORDED` | Solde livre | ➖ Soustraire | Frais de tenue de compte |
| **Intérêts non enregistrés** | `INTEREST_NOT_RECORDED` | Solde livre | ➕ Ajouter | Intérêts créditeurs |
| **Prélèvement auto non enregistré** | `DIRECT_DEBIT_NOT_RECORDED` | Solde livre | ➖ Soustraire | Assurance, électricité |
| **Agios non enregistrés** | `BANK_CHARGES_NOT_RECORDED` | Solde livre | ➖ Soustraire | Agios sur découvert |

---

## Cas d'usage réels

### 🏢 Cas 1 : PME avec beaucoup de chèques

**Situation :**
- Société de BTP qui paie ses fournisseurs par chèque
- En fin de mois, 15 chèques émis mais seulement 8 encaissés

**Solution :**
- Créer le rapprochement
- Ajouter 7 items de type `CHEQUE_ISSUED_NOT_CASHED`
- Le solde bancaire sera ajusté automatiquement

### 🏭 Cas 2 : Grande entreprise avec virements automatiques

**Situation :**
- Industrie avec prélèvements automatiques (eau, électricité, salaires)
- Comptable découvre les montants sur le relevé

**Solution :**
- Ajouter des items `DIRECT_DEBIT_NOT_RECORDED`
- Passer les écritures de régularisation
- Ajuster le solde comptable

### 🏪 Cas 3 : Commerce avec paiements mobiles

**Situation :**
- Boutique qui reçoit des paiements Orange Money / MTN Mobile
- Les virements arrivent avec 1-2 jours de délai

**Solution :**
- Identifier les virements sur le relevé
- Ajouter items `CREDIT_NOT_RECORDED`
- Enregistrer les ventes correspondantes

### 🏦 Cas 4 : Multi-comptes bancaires

**Situation :**
- Entreprise avec 3 comptes : BICEC, SGBC, Afriland
- Doit faire 3 rapprochements séparés

**Solution :**
- Créer 3 rapprochements avec `bankAccountNumber` différents :
  - "521001" pour BICEC
  - "521002" pour SGBC
  - "521003" pour Afriland

---

## 📌 Bonnes pratiques

### ✅ À FAIRE

1. **Faire le rapprochement CHAQUE MOIS** (obligation OHADA)
2. **Le faire dès réception du relevé** (max 5 jours)
3. **Archiver les états approuvés** (garder 10 ans minimum)
4. **Former 2 personnes** (préparateur + validateur différents)
5. **Documenter les écarts importants** (notes détaillées)

### ❌ À ÉVITER

1. ❌ Attendre plusieurs mois avant de faire le rapprochement
2. ❌ La même personne prépare ET approuve
3. ❌ Ignorer les petits écarts ("c'est juste 500 FCFA")
4. ❌ Ne pas passer les écritures de régularisation
5. ❌ Forcer l'équilibre avec une écriture de régularisation incorrecte

---

## 🆘 Questions fréquentes

### Q1 : Que faire si je ne trouve pas l'écart ?

**Réponse :** Techniques de recherche :
1. Diviser l'écart par 2 → chercher un montant qui aurait été mis du mauvais côté
2. Diviser par 9 → chercher une erreur de saisie (inversion de chiffres)
3. Vérifier les reports de solde (solde initial correct ?)
4. Pointer ligne par ligne relevé vs comptabilité
5. Demander à un collègue de vérifier (regard frais)

### Q2 : Combien de temps ça prend ?

**Réponse :**
- Petit compte (< 50 opérations/mois) : 30 minutes à 1 heure
- Compte moyen (50-200 opérations) : 1 à 2 heures
- Gros compte (> 200 opérations) : 2 à 4 heures

Avec l'API PREDYKT, diviser ce temps par 2-3 grâce aux calculs automatiques.

### Q3 : Puis-je avoir plusieurs rapprochements pour le même mois ?

**Réponse :** Non, il ne peut y avoir qu'**un seul rapprochement par compte et par date**. Si vous devez corriger, utilisez la fonction "Rejeter" puis "Modifier".

### Q4 : Que faire des chèques très anciens jamais encaissés ?

**Réponse :** Après le délai de prescription (généralement 3 ans) :
1. Contacter le bénéficiaire
2. Si pas de réponse, faire opposition sur le chèque
3. Passer une écriture de reprise :
   ```
   521 Banque                     XXX
       758 Produits divers              XXX
   Reprise chèque n°xxx prescrit
   ```

### Q5 : L'API calcule-t-elle le solde comptable automatiquement ?

**Réponse :** Oui ! Si vous ne fournissez pas `bookBalance` lors de la création, l'API :
1. Va chercher le compte 52X dans votre plan comptable
2. Calcule le solde cumulé depuis le début
3. Remplit automatiquement le champ

---

## 📞 Support

Pour toute question sur le rapprochement bancaire :
- 📧 Email : support@predykt.com
- 📱 Téléphone : +237 XXX XX XX XX
- 📘 Documentation API : https://api.predykt.com/docs

---

**🎯 Objectif final : Avoir un rapprochement bancaire équilibré (écart = 0) approuvé chaque mois pour TOUS vos comptes bancaires.**

✅ Conformité OHADA garantie
✅ Contrôle interne renforcé
✅ Audit facilité
✅ Tranquillité d'esprit assurée
