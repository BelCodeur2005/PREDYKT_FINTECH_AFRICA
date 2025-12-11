# Guide des Paiements Fractionnés (Option B - Conforme OHADA)

## 📚 Table des matières

1. [Introduction](#introduction)
2. [Principe de fonctionnement](#principe-de-fonctionnement)
3. [Exemples pratiques](#exemples-pratiques)
4. [API Reference](#api-reference)
5. [Écritures comptables générées](#écritures-comptables-générées)
6. [FAQ](#faq)

---

## Introduction

**PREDYKT** implémente la **gestion des paiements fractionnés conforme OHADA** (Option B).

### Qu'est-ce qu'un paiement fractionné ?

Un paiement fractionné est le règlement d'une facture en **plusieurs versements** échelonnés dans le temps, au lieu d'un paiement unique.

**Exemple concret :**
```
Facture FV-2025-0125 : 200 000 XAF (Client ABC SARL)
├─ Paiement 1 : 15/03/2025 → 100 000 XAF (50%)
└─ Paiement 2 : 30/03/2025 → 100 000 XAF (50%)
Total : 200 000 XAF (100%) ✅ PAYÉE
```

---

## Principe de fonctionnement

### Option B : Enregistrement séparé de chaque paiement (RECOMMANDÉ - Conforme OHADA)

**Comment ça marche :**

1. **Création de la facture** (15/02/2025)
   - Facture FV-2025-0125 : 200 000 XAF
   - Statut : `ISSUED` (Émise)
   - Montant payé : 0 XAF
   - Montant dû : 200 000 XAF

2. **Premier paiement** (15/03/2025) - 100 000 XAF
   ```
   POST /api/v1/companies/{companyId}/payments/customer
   {
     "invoiceId": 125,
     "amount": 100000,
     "paymentDate": "2025-03-15",
     "paymentMethod": "BANK_TRANSFER"
   }
   ```
   - ✅ Paiement PAY-2025-0001 créé
   - ✅ Écriture comptable générée automatiquement
   - ✅ Facture mise à jour : `PARTIAL_PAID` (Partiellement payée)
   - Montant payé : 100 000 XAF
   - Montant dû : 100 000 XAF

3. **Deuxième paiement** (30/03/2025) - 100 000 XAF
   ```
   POST /api/v1/companies/{companyId}/payments/customer
   {
     "invoiceId": 125,
     "amount": 100000,
     "paymentDate": "2025-03-30",
     "paymentMethod": "BANK_TRANSFER"
   }
   ```
   - ✅ Paiement PAY-2025-0002 créé
   - ✅ Écriture comptable générée
   - ✅ Facture mise à jour : `PAID` (Totalement payée)
   - ✅ Lettrage automatique effectué
   - Montant payé : 200 000 XAF
   - Montant dû : 0 XAF

---

## Exemples pratiques

### Exemple 1 : Paiement en 2 versements (50% / 50%)

**Contexte :**
- Facture : FV-2025-0125
- Client : ABC SARL
- Montant : 200 000 XAF
- Échéancier : 2 paiements égaux

**Étapes :**

#### 1. Créer la facture
```bash
POST /api/v1/companies/1/invoices
Content-Type: application/json

{
  "customerId": 5,
  "issueDate": "2025-02-15",
  "dueDate": "2025-04-15",
  "description": "Vente de marchandises - Février 2025",
  "lines": [
    {
      "description": "Produit A",
      "quantity": 10,
      "unitPrice": 16778.52,
      "vatRate": 19.25
    }
  ]
}
```

**Réponse :**
```json
{
  "success": true,
  "data": {
    "id": 125,
    "invoiceNumber": "FV-2025-0125",
    "totalTtc": 200000.00,
    "amountPaid": 0.00,
    "amountDue": 200000.00,
    "status": "DRAFT"
  }
}
```

#### 2. Valider la facture (génère l'écriture comptable)
```bash
POST /api/v1/companies/1/invoices/125/validate
```

**Résultat :** Facture passe en statut `ISSUED`, écriture comptable créée :
```
15/02 - Journal VE (Ventes)
DÉBIT  4111001 (Client ABC)       200 000 XAF   ← Créance client
CRÉDIT 701 (Ventes)                              167 785 XAF   ← Chiffre d'affaires HT
CRÉDIT 4431 (TVA collectée)                       32 215 XAF   ← TVA 19.25%
```

#### 3. Enregistrer le premier paiement (15/03 - 50%)
```bash
POST /api/v1/companies/1/payments/customer
Content-Type: application/json

{
  "invoiceId": 125,
  "amount": 100000,
  "paymentDate": "2025-03-15",
  "paymentMethod": "BANK_TRANSFER",
  "transactionReference": "VIR20250315ABC",
  "description": "Acompte 50% - Facture FV-2025-0125"
}
```

**Réponse :**
```json
{
  "success": true,
  "message": "Paiement enregistré (paiement partiel)",
  "data": {
    "id": 201,
    "paymentNumber": "PAY-2025-0001",
    "amount": 100000.00,
    "paymentDate": "2025-03-15",
    "status": "COMPLETED",
    "isReconciled": false
  }
}
```

**Écriture comptable générée automatiquement :**
```
15/03 - Journal BQ (Banque)
DÉBIT  521 (Banque)               100 000 XAF   ← Argent reçu
CRÉDIT 4111001 (Client ABC)                      100 000 XAF   ← Annulation partielle créance
```

**État de la facture après paiement 1 :**
```bash
GET /api/v1/companies/1/invoices/125
```
```json
{
  "invoiceNumber": "FV-2025-0125",
  "totalTtc": 200000.00,
  "amountPaid": 100000.00,
  "amountDue": 100000.00,
  "status": "PARTIAL_PAID",
  "paymentPercentage": 50.00,
  "paymentCount": 1,
  "hasFractionalPayments": false
}
```

#### 4. Enregistrer le deuxième paiement (30/03 - 50%)
```bash
POST /api/v1/companies/1/payments/customer
Content-Type: application/json

{
  "invoiceId": 125,
  "amount": 100000,
  "paymentDate": "2025-03-30",
  "paymentMethod": "BANK_TRANSFER",
  "transactionReference": "VIR20250330ABC",
  "description": "Solde 50% - Facture FV-2025-0125"
}
```

**Réponse :**
```json
{
  "success": true,
  "message": "Paiement enregistré et lettré automatiquement",
  "data": {
    "id": 202,
    "paymentNumber": "PAY-2025-0002",
    "amount": 100000.00,
    "paymentDate": "2025-03-30",
    "status": "COMPLETED",
    "isReconciled": true   ← Lettrage automatique
  }
}
```

**Écriture comptable générée :**
```
30/03 - Journal BQ (Banque)
DÉBIT  521 (Banque)               100 000 XAF   ← Argent reçu
CRÉDIT 4111001 (Client ABC)                      100 000 XAF   ← Annulation créance (solde)
```

**État final de la facture :**
```bash
GET /api/v1/companies/1/invoices/125
```
```json
{
  "invoiceNumber": "FV-2025-0125",
  "totalTtc": 200000.00,
  "amountPaid": 200000.00,
  "amountDue": 0.00,
  "status": "PAID",
  "isReconciled": true,
  "paymentPercentage": 100.00,
  "paymentCount": 2,
  "hasFractionalPayments": true   ← Plus d'un paiement
}
```

#### 5. Consulter l'historique des paiements
```bash
GET /api/v1/companies/1/invoices/125/payments
```

**Réponse complète :**
```json
{
  "success": true,
  "message": "2 paiement(s) enregistré(s) - 100.00% payé (200000 / 200000 XAF)",
  "data": {
    "invoiceId": 125,
    "invoiceNumber": "FV-2025-0125",
    "issueDate": "2025-02-15",
    "dueDate": "2025-04-15",
    "status": "PAID",
    "customerId": 5,
    "customerName": "ABC SARL",
    "totalTtc": 200000.00,
    "amountPaid": 200000.00,
    "amountDue": 0.00,
    "paymentPercentage": 100.00,
    "paymentCount": 2,
    "hasFractionalPayments": true,
    "isFullyPaid": true,
    "isOverdue": false,
    "daysOverdue": 0,
    "payments": [
      {
        "id": 202,
        "paymentNumber": "PAY-2025-0002",
        "paymentDate": "2025-03-30",
        "amount": 100000.00,
        "paymentMethod": "BANK_TRANSFER",
        "status": "COMPLETED",
        "isReconciled": true,
        "description": "Solde 50% - Facture FV-2025-0125"
      },
      {
        "id": 201,
        "paymentNumber": "PAY-2025-0001",
        "paymentDate": "2025-03-15",
        "amount": 100000.00,
        "paymentMethod": "BANK_TRANSFER",
        "status": "COMPLETED",
        "isReconciled": true,
        "description": "Acompte 50% - Facture FV-2025-0125"
      }
    ],
    "paymentHistory": [
      {
        "paymentId": 202,
        "paymentNumber": "PAY-2025-0002",
        "paymentDate": "2025-03-30",
        "amount": 100000.00,
        "paymentMethod": "BANK_TRANSFER",
        "isReconciled": true,
        "description": "Solde 50% - Facture FV-2025-0125"
      },
      {
        "paymentId": 201,
        "paymentNumber": "PAY-2025-0001",
        "paymentDate": "2025-03-15",
        "amount": 100000.00,
        "paymentMethod": "BANK_TRANSFER",
        "isReconciled": true,
        "description": "Acompte 50% - Facture FV-2025-0125"
      }
    ]
  }
}
```

---

### Exemple 2 : Paiement en 3 versements (30% / 40% / 30%)

**Contexte :**
- Facture : FV-2025-0150
- Client : XYZ Enterprises
- Montant : 1 500 000 XAF
- Échéancier : 3 paiements inégaux

**Enregistrement des paiements :**

```bash
# Paiement 1 : 10/03 - 450 000 XAF (30%)
POST /api/v1/companies/1/payments/customer
{
  "invoiceId": 150,
  "amount": 450000,
  "paymentDate": "2025-03-10",
  "paymentMethod": "BANK_TRANSFER",
  "description": "Acompte 30%"
}

# Paiement 2 : 25/03 - 600 000 XAF (40%)
POST /api/v1/companies/1/payments/customer
{
  "invoiceId": 150,
  "amount": 600000,
  "paymentDate": "2025-03-25",
  "paymentMethod": "BANK_TRANSFER",
  "description": "Acompte 40%"
}

# Paiement 3 : 15/04 - 450 000 XAF (30%)
POST /api/v1/companies/1/payments/customer
{
  "invoiceId": 150,
  "amount": 450000,
  "paymentDate": "2025-04-15",
  "paymentMethod": "BANK_TRANSFER",
  "description": "Solde 30%"
}
```

**Résultat :**
- ✅ 3 paiements enregistrés
- ✅ 3 écritures comptables distinctes (chacune à sa date)
- ✅ Facture totalement payée et lettrée automatiquement

---

## API Reference

### 1. Enregistrer un paiement client (encaissement)

**Endpoint :**
```
POST /api/v1/companies/{companyId}/payments/customer
```

**Request Body :**
```json
{
  "invoiceId": 125,                        // ID de la facture à payer
  "amount": 100000,                        // Montant du paiement (peut être partiel)
  "paymentDate": "2025-03-15",             // Date du paiement
  "paymentMethod": "BANK_TRANSFER",        // CASH | BANK_TRANSFER | CHEQUE | MOBILE_MONEY | CARD
  "bankAccountId": 1,                      // ID du compte bancaire (optionnel)
  "transactionReference": "VIR20250315",   // Référence bancaire (optionnel)
  "description": "Acompte 50%",            // Description (optionnel)
  "notes": "Notes internes"                // Notes internes (optionnel)
}
```

**Response :**
```json
{
  "success": true,
  "message": "Paiement enregistré (paiement partiel)",
  "data": {
    "id": 201,
    "paymentNumber": "PAY-2025-0001",
    "amount": 100000.00,
    "paymentDate": "2025-03-15",
    "paymentMethod": "BANK_TRANSFER",
    "status": "COMPLETED",
    "isReconciled": false,
    "invoiceId": 125,
    "invoiceNumber": "FV-2025-0125",
    "customerId": 5,
    "customerName": "ABC SARL",
    "generalLedgerId": 5012   // Écriture comptable générée
  }
}
```

**Validations automatiques :**
- ✅ Montant du paiement ne peut pas dépasser le montant dû
- ✅ Facture doit être en statut `ISSUED` ou `PARTIAL_PAID`
- ✅ Statut de la facture mis à jour automatiquement
- ✅ Lettrage automatique si paiement total

---

### 2. Consulter l'historique des paiements d'une facture

**Endpoint :**
```
GET /api/v1/companies/{companyId}/invoices/{invoiceId}/payments
```

**Response :**
```json
{
  "success": true,
  "message": "2 paiement(s) enregistré(s) - 100.00% payé (200000 / 200000 XAF)",
  "data": {
    "invoiceId": 125,
    "invoiceNumber": "FV-2025-0125",
    "issueDate": "2025-02-15",
    "dueDate": "2025-04-15",
    "status": "PAID",
    "customerId": 5,
    "customerName": "ABC SARL",
    "totalTtc": 200000.00,
    "amountPaid": 200000.00,
    "amountDue": 0.00,
    "paymentPercentage": 100.00,
    "paymentCount": 2,
    "hasFractionalPayments": true,
    "isFullyPaid": true,
    "isOverdue": false,
    "daysOverdue": 0,
    "payments": [...]   // Liste complète des paiements
  }
}
```

---

### 3. Lister toutes les factures avec statistiques de paiements

**Endpoint :**
```
GET /api/v1/companies/{companyId}/invoices
GET /api/v1/companies/{companyId}/invoices?status=PARTIAL_PAID   ← Factures en cours de paiement
```

**Response :**
```json
{
  "success": true,
  "data": [
    {
      "id": 125,
      "invoiceNumber": "FV-2025-0125",
      "totalTtc": 200000.00,
      "amountPaid": 100000.00,
      "amountDue": 100000.00,
      "status": "PARTIAL_PAID",
      "paymentPercentage": 50.00,          // ← Pourcentage payé
      "paymentCount": 1,                   // ← Nombre de paiements
      "hasFractionalPayments": false       // ← Un seul paiement pour l'instant
    }
  ]
}
```

---

### 4. Annuler un paiement (si non lettré)

**Endpoint :**
```
POST /api/v1/companies/{companyId}/payments/{paymentId}/cancel
```

**Conditions :**
- ❌ Impossible si le paiement est déjà lettré (`isReconciled = true`)
- ✅ Remet automatiquement le montant sur la facture
- ✅ Facture repasse en statut `ISSUED` ou `PARTIAL_PAID`

---

## Écritures comptables générées

### Facturation (Validation de la facture)

**Écriture générée lors de la validation :**
```
Date : Date d'émission de la facture
Journal : VE (Ventes)

DÉBIT  4111xxx (Client - Compte auxiliaire)   [Montant TTC]   ← Créance client
CRÉDIT 701 (Ventes de marchandises)           [Montant HT]    ← Chiffre d'affaires
CRÉDIT 4431 (TVA collectée)                   [Montant TVA]   ← TVA à reverser (19.25%)
```

**Exemple :**
```
15/02/2025 - Facture FV-2025-0125 - Client ABC SARL
DÉBIT  4111001   200 000 XAF   ← Créance de 200 000 XAF
CRÉDIT 701                      167 785 XAF   ← Ventes HT
CRÉDIT 4431                      32 215 XAF   ← TVA 19.25%
```

---

### Encaissement (Paiement client)

**Écriture générée pour CHAQUE paiement :**
```
Date : Date du paiement
Journal : BQ (Banque)

DÉBIT  521 (Banque)                           [Montant du paiement]   ← Argent reçu
CRÉDIT 4111xxx (Client - Compte auxiliaire)   [Montant du paiement]   ← Annulation créance
```

**Exemple - Paiement 1 :**
```
15/03/2025 - Paiement PAY-2025-0001 - Acompte 50%
DÉBIT  521         100 000 XAF   ← Banque augmente
CRÉDIT 4111001                    100 000 XAF   ← Créance diminue de 100 000 XAF
```

**Exemple - Paiement 2 :**
```
30/03/2025 - Paiement PAY-2025-0002 - Solde 50%
DÉBIT  521         100 000 XAF   ← Banque augmente
CRÉDIT 4111001                    100 000 XAF   ← Créance soldée (0 XAF restant)
```

---

### Évolution du solde du compte 4111001 (Client ABC)

| Date       | Opération           | Débit      | Crédit     | Solde (Débiteur) |
|------------|---------------------|------------|------------|------------------|
| 15/02/2025 | Facture FV-0125     | 200 000    |            | **200 000 XAF** ← Créance |
| 15/03/2025 | Paiement PAY-0001   |            | 100 000    | **100 000 XAF** ← Reste dû |
| 30/03/2025 | Paiement PAY-0002   |            | 100 000    | **0 XAF** ← Soldé ✅ |

---

## FAQ

### Q1 : Puis-je enregistrer plus de 2 paiements pour une même facture ?

**R :** Oui ! Vous pouvez enregistrer autant de paiements que nécessaire. Chaque paiement :
- Crée une écriture comptable distincte
- Met à jour le solde de la facture
- Est enregistré à sa date effective

**Exemple :** Une facture de 1 000 000 XAF peut être payée en :
- 4 paiements de 250 000 XAF
- 10 paiements de 100 000 XAF
- N'importe quelle combinaison de montants

---

### Q2 : Que se passe-t-il si je me trompe de montant ?

**R :** Vous pouvez **annuler** le paiement si :
- Il n'est **pas encore lettré** (`isReconciled = false`)
- La facture n'est pas complètement payée

**Procédure :**
```bash
POST /api/v1/companies/1/payments/{paymentId}/cancel
```

Le paiement passe en statut `CANCELLED` et le montant est **remis sur la facture**.

⚠️ **Important :** Une fois lettré (`isReconciled = true`), le paiement ne peut plus être annulé. Vous devez créer une **note de crédit** ou contacter votre expert-comptable.

---

### Q3 : Comment savoir combien il reste à payer sur une facture ?

**R :** Consultez la facture via l'API :
```bash
GET /api/v1/companies/1/invoices/125
```

Regardez les champs :
- `amountDue` : Montant restant à payer
- `amountPaid` : Montant déjà payé
- `paymentPercentage` : Pourcentage payé (0-100%)
- `paymentCount` : Nombre de paiements enregistrés

---

### Q4 : Les paiements fractionnés sont-ils conformes OHADA ?

**R :** **OUI, totalement conforme !**

L'Option B (enregistrement séparé de chaque paiement) est la méthode **recommandée** par les normes comptables OHADA car :

✅ **Article 59 SYSCOHADA** : "Toute opération comptable est enregistrée à la date à laquelle elle intervient."
- Chaque encaissement est enregistré à sa date effective

✅ **Principe de séparation des exercices**
- Les paiements de mars 2025 et avril 2025 sont enregistrés dans leurs périodes respectives

✅ **Traçabilité et audit**
- Chaque mouvement de trésorerie a sa propre écriture comptable
- Les auditeurs peuvent reconstituer le solde banque jour par jour

✅ **État de rapprochement bancaire**
- Chaque ligne du relevé bancaire correspond à une écriture comptable

---

### Q5 : Puis-je voir l'historique complet des paiements ?

**R :** Oui ! Utilisez l'endpoint dédié :
```bash
GET /api/v1/companies/1/invoices/125/payments
```

Vous obtiendrez :
- La liste complète de tous les paiements (triés par date décroissante)
- Le montant de chaque paiement
- Le mode de paiement utilisé
- Le statut de lettrage
- Les statistiques globales (% payé, nombre de paiements, etc.)

---

### Q6 : Que signifie "lettrage automatique" ?

**R :** Le **lettrage** (ou **réconciliation**) est le processus de rapprochement entre :
- Une facture (créance client)
- Les paiements reçus

**Dans PREDYKT :**
- ✅ Le lettrage est **automatique** quand le montant payé = montant total de la facture
- ✅ Les champs `isReconciled` passent à `true` sur la facture ET les paiements
- ✅ Le statut de la facture passe à `PAID`
- ✅ La date de lettrage est enregistrée

---

### Q7 : Comment gérer les acomptes avant facture ?

**R :** Pour les acomptes **avant facturation**, vous pouvez :

**Option 1 : Créer une facture d'acompte**
- Créez une facture pour le montant de l'acompte
- Enregistrez le paiement immédiatement
- Créez une facture de solde ultérieurement

**Option 2 : Utiliser un compte de tiers spécial**
- Enregistrez l'acompte sur un compte 4191 "Clients - Avances et acomptes reçus"
- Lors de la facturation finale, imputez l'acompte sur la facture

---

### Q8 : Puis-je avoir un échéancier de paiements prévu ?

**R :** Actuellement, PREDYKT enregistre les paiements **au fur et à mesure** de leur réception.

Pour un échéancier prévisionnel :
- Utilisez le champ `paymentTerms` de la facture pour documenter les conditions
- Exemple : `paymentTerms: "3 paiements égaux : 15/03, 30/03, 15/04"`

**Fonctionnalité future :** Un module d'échéancier automatique est prévu dans une prochaine version.

---

### Q9 : Que faire si un client paie trop (overpayment) ?

**R :** Si un client paie **plus** que le montant dû :
- ❌ L'API refusera le paiement avec une erreur : *"Le montant du paiement dépasse le montant dû"*
- ✅ Vous devez enregistrer uniquement le montant exact restant
- ✅ Le trop-perçu peut être enregistré comme un **avoir** (crédit note) sur une future facture

---

### Q10 : Comment annuler une facture déjà partiellement payée ?

**R :** Une facture avec des paiements **ne peut PAS être annulée directement**.

**Procédure correcte :**
1. Annuler tous les paiements un par un (si non lettrés)
2. Puis annuler la facture via :
   ```bash
   POST /api/v1/companies/1/invoices/125/cancel
   ```

**Alternativ** (recommandé) : Créer une **note de crédit** (avoir) pour compenser la facture.

---

## 🎯 Bonnes pratiques

### ✅ À FAIRE

1. **Enregistrer chaque paiement dès réception**
   - Ne pas attendre d'avoir tous les paiements pour enregistrer

2. **Utiliser des descriptions claires**
   - Exemple : "Acompte 30% - Facture FV-2025-0125"
   - Aide à la traçabilité

3. **Vérifier le relevé bancaire**
   - Rapprocher chaque paiement enregistré avec le relevé bancaire
   - Utiliser le champ `transactionReference` pour la référence bancaire

4. **Consulter l'historique des paiements**
   - Vérifier régulièrement l'état des factures en cours
   - Relancer les clients pour les soldes non payés

5. **Lettrer manuellement si nécessaire**
   - Si le lettrage automatique n'a pas fonctionné
   - Vérifier que tous les paiements sont bien réconciliés

---

### ❌ À ÉVITER

1. ❌ **Ne pas créer une seule écriture globale**
   - Chaque paiement doit avoir sa propre écriture

2. ❌ **Ne pas modifier les montants après validation**
   - Annuler et recréer si erreur

3. ❌ **Ne pas oublier la référence bancaire**
   - Facilite le rapprochement bancaire

4. ❌ **Ne pas enregistrer des paiements futurs**
   - Enregistrer uniquement les paiements effectifs (déjà reçus)

5. ❌ **Ne pas négliger les relances**
   - Suivre régulièrement les factures partiellement payées
   - Utiliser l'endpoint `/invoices?status=PARTIAL_PAID`

---

## Support

Pour toute question ou assistance :
- 📧 Email : support@predykt.com
- 📚 Documentation : https://docs.predykt.com
- 🐛 Issues : https://github.com/predykt/accounting-api/issues

---

**Version :** 1.0.0
**Dernière mise à jour :** 10 Décembre 2025
**Conforme :** OHADA SYSCOHADA Révisé
