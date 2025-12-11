# Guide Comptable - Réductions et Acomptes

> **Conformité OHADA & Cameroun**
> Ce guide explique comment gérer les réductions commerciales et les acomptes dans PREDYKT.

---

## 📋 Table des matières

1. [Réductions commerciales](#réductions-commerciales)
2. [Acomptes (Avances clients)](#acomptes-avances-clients)
3. [Exemples de factures](#exemples-de-factures)
4. [Écritures comptables OHADA](#écritures-comptables-ohada)

---

## Réductions commerciales

### ✅ Fonctionnalité disponible

Les réductions sont **déjà supportées** dans PREDYKT au niveau de chaque ligne de facture.

### 🎯 Types de réductions

#### 1. Remise commerciale (par ligne)
**Quand l'utiliser :** Remise négociée avec le client sur un article spécifique

**Exemple :**
```
Article: Ordinateur portable
Prix catalogue:     1 000 000 XAF
Quantité:           5
Remise négociée:    10%

Calcul automatique:
  Sous-total:       5 000 000 XAF  (5 × 1 000 000)
  Remise 10%:        -500 000 XAF
  ─────────────────────────────────
  Total HT:         4 500 000 XAF
  TVA 19,25%:         866 250 XAF
  ─────────────────────────────────
  Total TTC:        5 366 250 XAF
```

#### 2. Remise sur quantité
**Quand l'utiliser :** Réduction automatique selon barème de quantité

**Exemple barème :**
```
1-10 unités    → 0% de remise
11-50 unités   → 5% de remise
51-100 unités  → 10% de remise
100+ unités    → 15% de remise
```

#### 3. Remise fin de série / Promotion
**Exemple :**
```
Article: Stock ancien modèle
Prix normal:        500 000 XAF
Remise promo:       25%
Prix soldé:         375 000 XAF
```

### 📝 Comment créer une facture avec réductions

#### Méthode API (JSON)
```json
{
  "invoiceNumber": "FA-2025-001",
  "customer": { "id": 123 },
  "issueDate": "2025-01-15",
  "dueDate": "2025-02-15",
  "lines": [
    {
      "lineNumber": 1,
      "description": "Ordinateur portable Dell",
      "productCode": "PC-DELL-001",
      "quantity": 5,
      "unit": "Unité",
      "unitPrice": 1000000,
      "discountPercentage": 10.00,  ← REMISE ICI
      "vatRate": 19.25
    },
    {
      "lineNumber": 2,
      "description": "Souris sans fil",
      "productCode": "ACC-MOUSE-001",
      "quantity": 5,
      "unit": "Unité",
      "unitPrice": 15000,
      "discountPercentage": 5.00,  ← REMISE ICI
      "vatRate": 19.25
    }
  ]
}
```

#### Résultat facture
```
┌────────────────────────────────────────────────────────────────────┐
│                    FACTURE FA-2025-001                             │
│                                                                    │
│ Client: ABC Entreprise                Date: 15/01/2025            │
│ NIU: CM-M-2024-XXXX                    Échéance: 15/02/2025       │
└────────────────────────────────────────────────────────────────────┘

┌──────┬─────────────────┬─────┬────────┬────────┬────────┬──────────┐
│ Nº   │ Description     │ Qté │ P.U.   │ Remise │ HT     │ Total HT │
├──────┼─────────────────┼─────┼────────┼────────┼────────┼──────────┤
│  1   │ Ordinateur      │  5  │1 000 K │  10%   │ 900 K  │4 500 000 │
│      │ portable Dell   │     │        │-500 K  │        │          │
├──────┼─────────────────┼─────┼────────┼────────┼────────┼──────────┤
│  2   │ Souris sans fil │  5  │  15 K  │   5%   │14 250  │   71 250 │
│      │                 │     │        │ -3 750 │        │          │
└──────┴─────────────────┴─────┴────────┴────────┴────────┴──────────┘

                                        Sous-total HT:  4 571 250 XAF
                                        TVA 19,25%:       879 966 XAF
                                        ────────────────────────────────
                                        TOTAL TTC:      5 451 216 XAF

                        NET À PAYER:    5 451 216 XAF
```

### 💡 Bonnes pratiques

#### ✅ À FAIRE
- **Toujours documenter** la raison de la remise dans la description
- **Respecter les barèmes** de remise de votre entreprise
- **Obtenir validation** pour remises > 15%
- **Mentionner la remise** sur la facture pour transparence client

#### ❌ À ÉVITER
- Ne pas cumuler remise ligne + remise globale (choisir l'un ou l'autre)
- Ne pas dépasser 30% de remise sans accord direction
- Ne pas modifier le prix unitaire au lieu d'utiliser la remise (traçabilité)

---

## Acomptes (Avances clients)

### ✅ Fonctionnalité IMPLÉMENTÉE (Phase 3 - Décembre 2025)

Les acomptes sont **désormais disponibles** dans PREDYKT avec implémentation complète conforme OHADA.

### 🎯 Qu'est-ce qu'un acompte ?

Un **acompte** (ou avance) est un paiement partiel versé par le client **AVANT** la livraison ou la facturation finale.

**Différence avec un paiement :**
```
┌────────────────────────────────────────────────────────────────┐
│ ACOMPTE                       │ PAIEMENT                       │
├───────────────────────────────┼────────────────────────────────┤
│ AVANT la facturation finale   │ APRÈS la facturation finale    │
│ Compte 4191 "Avances clients" │ Compte 411 "Clients"           │
│ Apparaît sur facture finale   │ Solde la facture               │
└───────────────────────────────┴────────────────────────────────┘
```

### 📊 Cycle de vie d'un acompte

#### Étape 1 : Réception de l'acompte (AVANT facturation)
```
Date: 10/01/2025
Client ABC commande pour 10 000 000 XAF TTC
Acompte demandé: 30% = 3 000 000 XAF
```

**Écriture comptable OHADA :**
```
Débit  : 521 Banque                           3 000 000 XAF
Crédit : 4191 Clients - Avances et acomptes   3 000 000 XAF
```

**Reçu d'acompte émis :**
```
┌────────────────────────────────────────────────────────────┐
│              REÇU D'ACOMPTE N° AV-2025-001                 │
│                                                            │
│ Client: ABC Entreprise                                     │
│ Date: 10/01/2025                                          │
│                                                            │
│ Montant reçu: 3 000 000 XAF                               │
│ Mode paiement: Virement bancaire                          │
│                                                            │
│ Commande: Fourniture équipements informatiques            │
│ Montant total commande: 10 000 000 XAF TTC               │
│                                                            │
│ Reste à facturer: 7 000 000 XAF                          │
└────────────────────────────────────────────────────────────┘
```

#### Étape 2 : Livraison et facturation finale (AVEC acompte)
```
Date: 25/01/2025
Livraison effectuée
Émission facture FA-2025-010
```

**Facture avec déduction acompte :**
```
┌────────────────────────────────────────────────────────────────────┐
│                    FACTURE FA-2025-010                             │
│                                                                    │
│ Client: ABC Entreprise                Date: 25/01/2025            │
│ NIU: CM-M-2024-XXXX                    Échéance: 25/02/2025       │
└────────────────────────────────────────────────────────────────────┘

┌──────┬─────────────────────────────┬─────┬────────────┬──────────┐
│ Nº   │ Description                 │ Qté │ Prix unit. │ Total HT │
├──────┼─────────────────────────────┼─────┼────────────┼──────────┤
│  1   │ PC Dell XPS 15              │ 10  │  800 000   │8 000 000 │
│  2   │ Écran Dell 27"              │ 10  │   80 000   │  800 000 │
└──────┴─────────────────────────────┴─────┴────────────┴──────────┘

                                        Total HT:       8 800 000 XAF
                                        TVA 19,25%:     1 694 000 XAF
                                        ────────────────────────────────
                                        TOTAL TTC:     10 494 000 XAF

                            Acompte versé 10/01/2025: -3 000 000 XAF
                                        ────────────────────────────────
                                NET À PAYER:            7 494 000 XAF
```

**Écriture comptable OHADA :**
```
Créance client (TTC):
  Débit  : 411 Clients                      10 494 000 XAF
  Crédit : 701 Ventes de marchandises        8 800 000 XAF
  Crédit : 4431 TVA collectée                1 694 000 XAF

Imputation acompte:
  Débit  : 4191 Clients - Avances            3 000 000 XAF
  Crédit : 411 Clients                       3 000 000 XAF
```

**Résultat :**
```
Compte 411 "Clients - ABC":
  Débit:  10 494 000 (facture)
  Crédit:  3 000 000 (acompte imputé)
  ────────────────────────────────
  SOLDE:   7 494 000 XAF ← Ce que le client doit encore
```

### 📋 Gestion TVA sur acomptes

⚠️ **IMPORTANT - Règle TVA Cameroun :**

La TVA est **exigible dès l'encaissement** (CGI Art. 129).

#### Cas 1 : Acompte TTC
```
Acompte reçu: 3 000 000 XAF TTC

Décomposition:
  HT:  3 000 000 / 1,1925 = 2 515 723 XAF
  TVA: 3 000 000 - 2 515 723 = 484 277 XAF

Écriture:
  Débit  : 521 Banque                     3 000 000
  Crédit : 4191 Avances clients HT        2 515 723
  Crédit : 4431 TVA collectée               484 277

⚠️ TVA à déclarer dès réception acompte !
```

#### Cas 2 : Acompte HT
```
Acompte reçu HT: 2 500 000 XAF
TVA 19,25%:        481 250 XAF
Total TTC:       2 981 250 XAF

Écriture:
  Débit  : 521 Banque                     2 981 250
  Crédit : 4191 Avances clients HT        2 500 000
  Crédit : 4431 TVA collectée               481 250
```

### ✅ Fonctionnalités DISPONIBLES dans PREDYKT

#### 1. Gestion complète des acomptes
```
Module: Deposits (Acomptes clients)

✅ Reçu d'acompte automatique: RA-2025-000001
✅ Calcul automatique TVA 19.25% (CGI Cameroun)
✅ Statut: disponible / imputé
✅ Relations: client, facture, paiement
✅ Multi-tenant (company_id)

Endpoints REST:
- POST   /api/v1/companies/{id}/deposits           # Créer
- GET    /api/v1/companies/{id}/deposits/{id}      # Consulter
- POST   /api/v1/companies/{id}/deposits/{id}/apply  # Imputer
- GET    /api/v1/companies/{id}/deposits/customer/{id}/available  # Dispos
```

#### 2. Imputation automatique acompte → facture
```
✅ Validation automatique:
   - Client identique
   - Montant acompte ≤ Montant facture
   - Acompte non déjà imputé

✅ Mise à jour automatique facture:
   - amountPaid += deposit.amountTtc
   - amountDue recalculé
   - status mis à jour (PARTIAL_PAID / PAID)
```

#### 3. Écritures comptables automatiques OHADA
```
✅ À la réception acompte:
  DÉBIT  512 Banque
  CRÉDIT 4191 Clients - Avances (HT)
  CRÉDIT 4431 TVA collectée

✅ À l'imputation sur facture:
  DÉBIT  4191 Clients - Avances (HT)
  DÉBIT  4431 TVA collectée
  CRÉDIT 411 Clients (TTC)

Générées automatiquement par GeneralLedgerService
```

#### 4. Documentation complète
Consultez les guides détaillés:
- **IMPLEMENTATION_ACOMPTES_RESUME.md** : Résumé technique
- **CONFORMITE_OHADA_REDUCTIONS_ESCOMPTE.md** : Conformité réglementaire

---

## Exemples de factures

### Exemple 1 : Facture avec remises (sans acompte)

```
┌────────────────────────────────────────────────────────────────────┐
│                    FACTURE FA-2025-025                             │
│                                                                    │
│ PREDYKT Services SARL               Client: Hôtel Paradis         │
│ NIU: CM-M-2024-001234                NIU: CM-M-2024-005678        │
│ Douala, Cameroun                     Date: 20/01/2025             │
│                                      Échéance: 19/02/2025 (30j)   │
└────────────────────────────────────────────────────────────────────┘

┌────┬────────────────────┬────┬──────────┬──────┬──────────┬─────────┐
│ Nº │ Description        │Qté │  P.U.    │Remise│   HT     │Total HT │
├────┼────────────────────┼────┼──────────┼──────┼──────────┼─────────┤
│ 1  │ Logiciel gestion   │ 5  │ 500 000  │ 15%  │ 425 000  │2 125 000│
│    │ hôtelière          │lic.│          │      │  /lic.   │         │
│    │ (Licence annuelle) │    │          │-75 K │          │         │
├────┼────────────────────┼────┼──────────┼──────┼──────────┼─────────┤
│ 2  │ Formation staff    │ 3  │ 200 000  │  0%  │ 200 000  │ 600 000 │
│    │ (2 jours)          │pers│          │      │  /pers   │         │
├────┼────────────────────┼────┼──────────┼──────┼──────────┼─────────┤
│ 3  │ Maintenance        │ 12 │  50 000  │ 10%  │  45 000  │ 540 000 │
│    │ mensuelle          │mois│          │      │  /mois   │         │
│    │                    │    │          │-5 K  │          │         │
└────┴────────────────────┴────┴──────────┴──────┴──────────┴─────────┘

Détail réductions:
  - Ligne 1: Remise volume 5 licences                    -  375 000 XAF
  - Ligne 3: Remise engagement annuel                    -   60 000 XAF
                                                          ────────────
  TOTAL RÉDUCTIONS ACCORDÉES:                            -  435 000 XAF

                                        Total HT:        3 265 000 XAF
                                        TVA 19,25%:        628 512 XAF
                                        ────────────────────────────────
                                        TOTAL TTC:       3 893 512 XAF

                              NET À PAYER:               3 893 512 XAF

Conditions de paiement: 30 jours net
Mode de paiement: Virement bancaire
IBAN: CM21 1000 2000 3000 4000 5000 67

                                         Merci de votre confiance !
```

### Exemple 2 : Facture avec acompte (À IMPLÉMENTER)

```
┌────────────────────────────────────────────────────────────────────┐
│                    FACTURE FA-2025-042                             │
│                                                                    │
│ PREDYKT Services SARL               Client: SuperMarché Plus      │
│ NIU: CM-M-2024-001234                NIU: CM-M-2024-009876        │
│ Douala, Cameroun                     Date: 28/01/2025             │
│                                      Échéance: 27/02/2025 (30j)   │
└────────────────────────────────────────────────────────────────────┘

COMMANDE Nº CMD-2025-015 du 05/01/2025

┌────┬────────────────────┬────┬──────────┬──────┬──────────┬─────────┐
│ Nº │ Description        │Qté │  P.U.    │Remise│   HT     │Total HT │
├────┼────────────────────┼────┼──────────┼──────┼──────────┼─────────┤
│ 1  │ Serveur Dell R750  │ 2  │8 000 000 │  5%  │7 600 000 │15200000 │
│    │                    │    │          │-400K │  /unité  │         │
├────┼────────────────────┼────┼──────────┼──────┼──────────┼─────────┤
│ 2  │ Installation +     │ 1  │1 500 000 │  0%  │1 500 000 │1 500 000│
│    │ configuration      │forf│          │      │          │         │
└────┴────────────────────┴────┴──────────┴──────┴──────────┴─────────┘

                                        Total HT:       16 700 000 XAF
                                        TVA 19,25%:      3 214 750 XAF
                                        ────────────────────────────────
                                        TOTAL TTC:      19 914 750 XAF

Acomptes versés:
  ⤷ Reçu AV-2025-003 du 10/01/2025                      -5 000 000 XAF
  ⤷ Reçu AV-2025-008 du 20/01/2025                      -3 000 000 XAF
                                                        ────────────────
  Total acomptes                                        -8 000 000 XAF
                                                        ════════════════
                              NET À PAYER:              11 914 750 XAF
                                                        ════════════════

Historique paiements:
┌────────────┬─────────────────┬─────────────┬──────────────────┐
│ Date       │ Type            │ Montant     │ Référence        │
├────────────┼─────────────────┼─────────────┼──────────────────┤
│ 10/01/2025 │ Acompte 25%     │ 5 000 000   │ VRT20250110-001  │
│ 20/01/2025 │ Acompte 15%     │ 3 000 000   │ VRT20250120-042  │
│            │ À la livraison  │11 914 750   │ (Cette facture)  │
└────────────┴─────────────────┴─────────────┴──────────────────┘

Conditions: Solde à 30 jours après livraison
Mode de paiement: Virement bancaire
IBAN: CM21 1000 2000 3000 4000 5000 67
```

### Exemple 3 : Facture avec remises multiples + acompte

```
┌────────────────────────────────────────────────────────────────────┐
│                    FACTURE FA-2025-055                             │
│                                                                    │
│ PREDYKT Mobilier SARL               Client: Ministère Éducation   │
│ NIU: CM-M-2024-001234                NIU: ADMIN-GOUV-001          │
│ Yaoundé, Cameroun                    Date: 15/02/2025             │
│                                      Échéance: 17/03/2025 (30j)   │
└────────────────────────────────────────────────────────────────────┘

MARCHÉ PUBLIC Nº MP-2024-EDU-158

┌────┬────────────────────┬────┬──────────┬──────┬──────────┬─────────┐
│ Nº │ Description        │Qté │  P.U.    │Remise│   HT     │Total HT │
├────┼────────────────────┼────┼──────────┼──────┼──────────┼─────────┤
│ 1  │ Bureau prof        │ 500│   85 000 │  8%  │  78 200  │39100000 │
│    │ standard           │    │          │-6.8K │  /unité  │         │
├────┼────────────────────┼────┼──────────┼──────┼──────────┼─────────┤
│ 2  │ Chaise ergonomique │ 500│   45 000 │  8%  │  41 400  │20700000 │
│    │                    │    │          │-3.6K │  /unité  │         │
├────┼────────────────────┼────┼──────────┼──────┼──────────┼─────────┤
│ 3  │ Armoire métallique │ 200│  120 000 │  8%  │ 110 400  │22080000 │
│    │ 2 portes           │    │          │-9.6K │  /unité  │         │
├────┼────────────────────┼────┼──────────┼──────┼──────────┼─────────┤
│ 4  │ Livraison + montage│  1 │2 500 000 │  5%  │2 375 000 │2 375 000│
│    │                    │forf│          │      │          │         │
└────┴────────────────────┴────┴──────────┴──────┴──────────┴─────────┘

Détail réductions:
  - Lignes 1-3: Remise marché public 8%              -  6 760 000 XAF
  - Ligne 4: Remise livraison groupée 5%             -    125 000 XAF
                                                     ────────────────
  TOTAL RÉDUCTIONS ACCORDÉES:                       -  6 885 000 XAF
  (Économie réalisée vs. tarif public)

                                        Total HT:       84 255 000 XAF
                                        TVA 19,25%:     16 219 088 XAF
                                        ────────────────────────────────
                                        TOTAL TTC:     100 474 088 XAF

Acomptes versés (marché public):
  ⤷ Avance démarrage 20% (AV-2025-012)               -20 000 000 XAF
  ⤷ Acompte mi-parcours 15% (AV-2025-021)            -15 000 000 XAF
                                                    ────────────────
  Total acomptes versés                             -35 000 000 XAF
                                                    ════════════════
                    SOLDE NET À PAYER:               65 474 088 XAF
                                                    ════════════════

Conditions marché public:
  - Solde payable sous 30 jours après livraison conforme
  - Retenue de garantie 10%: 10 047 409 XAF (restituable après 6 mois)
  - NET VERSÉ À CE JOUR: 55 426 679 XAF

Mode de paiement: Mandat administratif
Compte bancaire: CM21 1000 2000 3000 4000 5000 67
```

---

## Écritures comptables OHADA

### Cas 1 : Facture avec remise (sans acompte)

```
Facture FA-2025-025: 3 893 512 XAF TTC
HT: 3 265 000 XAF | TVA: 628 512 XAF

┌────────────────────────────────────────────────────────────┐
│ ÉCRITURE: Vente de marchandises/services                  │
├────────────────────────────────────────────────────────────┤
│ Débit  411 Clients - Hôtel Paradis     3 893 512 XAF      │
│ Crédit 701 Ventes de marchandises      3 265 000 XAF      │
│ Crédit 4431 TVA collectée                 628 512 XAF      │
└────────────────────────────────────────────────────────────┘

Note: La remise ne génère PAS d'écriture séparée.
Elle diminue simplement le montant comptabilisé en 701.
```

### Cas 2 : Réception acompte (AVANT facturation)

```
Date: 10/01/2025
Acompte reçu: 5 000 000 XAF TTC
Décomposition: HT 4 193 548 | TVA 806 452

┌────────────────────────────────────────────────────────────┐
│ ÉCRITURE: Réception acompte client                        │
├────────────────────────────────────────────────────────────┤
│ Débit  521 Banque                       5 000 000 XAF      │
│ Crédit 4191 Clients - Avances reçues    4 193 548 XAF      │
│ Crédit 4431 TVA collectée                  806 452 XAF      │
│                                                            │
│ Libellé: Acompte 25% commande CMD-2025-015                │
│ Réf: Reçu AV-2025-003                                      │
└────────────────────────────────────────────────────────────┘

⚠️ TVA EXIGIBLE dès encaissement → À déclarer ce mois-ci
```

### Cas 3 : Facturation finale avec imputation acompte

```
Date: 28/01/2025
Facture: 19 914 750 XAF TTC
HT: 16 700 000 | TVA: 3 214 750
Acomptes: 8 000 000 XAF déjà versés

┌────────────────────────────────────────────────────────────┐
│ ÉCRITURE 1: Constatation vente                            │
├────────────────────────────────────────────────────────────┤
│ Débit  411 Clients - SuperMarché Plus  19 914 750 XAF      │
│ Crédit 701 Ventes de marchandises      16 700 000 XAF      │
│ Crédit 4431 TVA collectée                3 214 750 XAF      │
│                                                            │
│ Libellé: Facture FA-2025-042 - Serveurs Dell              │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│ ÉCRITURE 2: Imputation acomptes                           │
├────────────────────────────────────────────────────────────┤
│ Débit  4191 Clients - Avances reçues    6 708 459 XAF      │
│ Débit  4431 TVA collectée (à annuler)   1 291 541 XAF      │
│ Crédit 411 Clients - SuperMarché Plus   8 000 000 XAF      │
│                                                            │
│ Libellé: Imputation acomptes AV-003 + AV-008              │
└────────────────────────────────────────────────────────────┘

RÉSULTAT COMPTE 411:
  Débit:  19 914 750  (facture)
  Crédit:  8 000 000  (acomptes)
  ────────────────────
  SOLDE:  11 914 750 ← À payer par le client
```

### Cas 4 : Paiement final

```
Date: 15/02/2025
Client paie le solde: 11 914 750 XAF

┌────────────────────────────────────────────────────────────┐
│ ÉCRITURE: Encaissement solde facture                      │
├────────────────────────────────────────────────────────────┤
│ Débit  521 Banque                      11 914 750 XAF      │
│ Crédit 411 Clients - SuperMarché Plus  11 914 750 XAF      │
│                                                            │
│ Libellé: Règlement FA-2025-042                            │
│ Réf: Virement VRT20250215-089                             │
└────────────────────────────────────────────────────────────┘

COMPTE 411 - Soldé:
  Débit:  19 914 750  (facture)
  Crédit:   8 000 000  (acomptes)
  Crédit:  11 914 750  (paiement)
  ────────────────────
  SOLDE:           0 ✅
```

---

## 📊 Résumé - Ce qui existe vs. ce qui manque

| Fonctionnalité | Statut | Action |
|----------------|--------|--------|
| **Réductions par ligne** | ✅ Disponible | Utiliser `discountPercentage` |
| **Calcul auto remise** | ✅ Disponible | Automatique via `calculateAmounts()` |
| **Affichage remise facture** | ✅ Disponible | Dans subtotal et totalHt |
| **Réception acomptes** | ✅ **IMPLÉMENTÉ** | POST /deposits |
| **Reçus d'acompte** | ✅ **IMPLÉMENTÉ** | Numéro RA-YYYY-NNNNNN |
| **Imputation acompte/facture** | ✅ **IMPLÉMENTÉ** | POST /deposits/{id}/apply |
| **Compte 4191** | ✅ **IMPLÉMENTÉ** | Écritures auto générées |
| **TVA sur acomptes** | ✅ **IMPLÉMENTÉ** | Calcul auto 19.25% |
| **Escompte (cash discount)** | ⚠️ Optionnel | Pas encore implémenté |

---

## 🎯 Recommandations

### ✅ Utiliser les réductions (Disponible maintenant)

1. **Créer vos factures** via API avec `discountPercentage` sur chaque ligne
2. **Documenter les remises** dans les notes de facture
3. **Respecter vos barèmes** commerciaux

### ✅ Utiliser les acomptes (Implémenté - Phase 3)

**Fonctionnalités disponibles :**
- ✅ Module complet gestion acomptes clients
- ✅ Génération automatique reçus (RA-YYYY-NNNNNN)
- ✅ Imputation automatique sur factures
- ✅ Écritures comptables OHADA conformes
- ✅ Gestion TVA sur encaissements (19.25%)

**Comment utiliser :**
```bash
# 1. Créer un acompte
POST /api/v1/companies/{companyId}/deposits
{
  "depositDate": "2025-01-15",
  "amountHt": 100000,
  "customerId": 42
}

# 2. Imputer sur facture
POST /api/v1/companies/{companyId}/deposits/{depositId}/apply
{
  "invoiceId": 123
}
```

**Documentation complète :** Voir `IMPLEMENTATION_ACOMPTES_RESUME.md`

### ⚠️ Escomptes (Pas nécessaire - Optionnel)

**Question :** Les escomptes (cash discount) sont-ils nécessaires ?

**Réponse :** **NON, pas obligatoire OHADA**

Selon le document `CONFORMITE_OHADA_REDUCTIONS_ESCOMPTE.md` :

| Critère | Évaluation |
|---------|------------|
| **Obligatoire OHADA** | ❌ NON (Optionnel) |
| **Pratique commerciale** | ✅ Courante au Cameroun |
| **Complexité** | 🟡 Moyenne |
| **Priorité** | 🟡 Phase 2 (si besoin métier) |

**Si implémenté plus tard, voici comment ça fonctionnerait :**

#### Définition
**Escompte** = Réduction financière pour paiement anticipé
- Exemple : "2% si paiement sous 10 jours au lieu de 30 jours"
- Nature : **Produit/Charge FINANCIER** (pas commercial)
- Comptes OHADA : 773 (escomptes obtenus) / 673 (escomptes accordés)

#### Différence avec Réduction
```
┌────────────────────────────────────────────────────────────┐
│ RÉDUCTION COMMERCIALE  │  ESCOMPTE (CASH DISCOUNT)        │
├────────────────────────┼──────────────────────────────────┤
│ Sur la facture         │  Au moment du paiement           │
│ Compte 70x/60x         │  Compte 773/673                  │
│ Réduction de prix      │  Incitation financière           │
│ Déjà implémenté ✅     │  Pas encore implémenté ❌        │
└────────────────────────┴──────────────────────────────────┘
```

#### Exemple d'utilisation future
```
Facture : 100 000 XAF TTC (échéance 30 jours)
Conditions : Escompte 2% si paiement sous 10 jours

Si paiement rapide (jour 8) :
  Facture TTC :           100 000 XAF
  Escompte 2% :            -2 000 XAF (produit financier vendeur)
  ────────────────────────────────
  Net payé par client :    98 000 XAF

Écriture comptable (FOURNISSEUR) :
  512 Banque                        98 000
  673 Escomptes accordés             2 000 (charge financière)
      411 Clients                           100 000
```

**Conclusion escomptes :**
- ⚠️ **Pas prioritaire** : Utilisez d'abord les réductions et acomptes
- 💡 **Si besoin métier** : Peut être ajouté en Phase 2
- 🎯 **Workaround actuel** : Utilisez les réductions commerciales (`discountPercentage`) pour l'instant

---

*Document créé : Session précédente*
*Mis à jour : 11/12/2025 - Phase 3 (Acomptes implémentés)*
*Conforme OHADA & CGI Cameroun (TVA exigible sur encaissement)*
