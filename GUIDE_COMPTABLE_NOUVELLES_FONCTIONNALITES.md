# Guide Comptable - Nouvelles Fonctionnalités PREDYKT

> **Pour qui ?** Ce guide s'adresse aux comptables et responsables financiers utilisant PREDYKT.
> **Objectif :** Comprendre les nouvelles fonctionnalités de rapprochement et de traçabilité fiscale.

---

## 📋 Table des matières

1. [Vue d'ensemble des améliorations](#vue-densemble-des-améliorations)
2. [Comprendre Payment vs BankTransaction](#comprendre-payment-vs-banktransaction)
3. [Le rapprochement bancaire automatique](#le-rapprochement-bancaire-automatique)
4. [Les nouveaux rapports fiscaux](#les-nouveaux-rapports-fiscaux)
5. [Traçabilité fiscale renforcée](#traçabilité-fiscale-renforcée)
6. [Cas d'usage pratiques](#cas-dusage-pratiques)

---

## Vue d'ensemble des améliorations

Le système PREDYKT a été renforcé pour offrir une **traçabilité complète** conforme aux normes OHADA et aux exigences fiscales camerounaises. Voici les principales améliorations :

### ✅ Ce qui a été ajouté

| Fonctionnalité | Bénéfice pour vous |
|----------------|-------------------|
| **Rapprochement bancaire intelligent** | Réconciliation automatique entre vos paiements enregistrés et vos relevés bancaires |
| **Rapports fiscaux AIR & IRPP** | Génération automatique des déclarations fiscales mensuelles |
| **Alertes fournisseurs sans NIU** | Identification des pénalités fiscales (3,3% de surcoût) |
| **Traçabilité TVA complète** | Historique détaillé de tous les calculs de TVA |
| **Validation automatique** | Vérification de la partie double et des périodes verrouillées |

---

## Comprendre Payment vs BankTransaction

### 🤔 Quelle est la différence ?

En comptabilité, il existe deux moments différents pour un paiement :

#### 1️⃣ **Payment (Paiement logique)**
- **Ce que c'est :** L'enregistrement comptable que VOUS créez quand vous décidez de payer ou encaisser
- **Quand :** Au moment de l'émission du chèque, virement, ou espèces
- **Exemple :** Le 15 janvier, vous enregistrez un paiement de 500 000 XAF au fournisseur Dupont

```
📝 Écriture comptable :
   Débit  : Compte 401 Fournisseurs - Dupont        500 000 XAF
   Crédit : Compte 521 Banque                       500 000 XAF
```

#### 2️⃣ **BankTransaction (Mouvement bancaire réel)**
- **Ce que c'est :** Le mouvement qui apparaît RÉELLEMENT sur votre relevé bancaire
- **Quand :** Quand la banque traite effectivement l'opération
- **Exemple :** Le 17 janvier, vous recevez votre relevé bancaire montrant la sortie de 500 000 XAF

```
🏦 Relevé bancaire :
   17/01/2025 | Virement Dupont | -500 000 XAF
```

### ⚠️ Pourquoi cette distinction est importante ?

#### Problème avant :
- ❌ Vous enregistrez un paiement le 15/01
- ❌ La banque le traite le 17/01
- ❌ **Aucun lien** entre les deux → Risque de double saisie ou d'oubli
- ❌ Impossible de savoir si un paiement enregistré a bien été débité

#### Solution maintenant :
- ✅ Vous enregistrez le Payment le 15/01
- ✅ Vous importez le relevé bancaire (BankTransaction) le 17/01
- ✅ PREDYKT **rapproche automatiquement** les deux
- ✅ Vous voyez instantanément les paiements en attente de traitement bancaire

---

## Le rapprochement bancaire automatique

### 🎯 Objectif

Faire correspondre automatiquement vos **Payments** (enregistrements comptables) avec vos **BankTransactions** (relevés bancaires importés).

### 🤖 Comment ça marche ?

Le système utilise un **algorithme de scoring intelligent** qui compare :

| Critère | Poids | Exemple |
|---------|-------|---------|
| **Montant** | 50% | Payment : 500 000 XAF ≈ BankTransaction : 499 800 XAF (frais bancaires) |
| **Date** | 30% | Payment : 15/01 vs BankTransaction : 17/01 → 2 jours d'écart ✅ |
| **Description** | 20% | Payment : "Facture FA-2025-001" ↔ BankTransaction : "VRT FA-2025-001" |

**Score minimum requis :** 70/100 pour une suggestion automatique

### 📊 Types de rapprochement

#### 1. Rapprochement simple (1 Payment ↔ 1 BankTransaction)
**Cas classique :** Un paiement correspond à un mouvement bancaire

```
Payment #001             BankTransaction #BT-456
Fournisseur Dupont  ←→   Virement Dupont
500 000 XAF              -500 000 XAF
15/01/2025               17/01/2025
```

#### 2. Rapprochement groupé (N Payments ↔ 1 BankTransaction)
**Cas virement groupé :** Plusieurs paiements regroupés en un seul virement bancaire

```
Payment #001: Dupont      100 000 XAF  ┐
Payment #002: Martin      200 000 XAF  ├→  BankTransaction #BT-789
Payment #003: Bernard     150 000 XAF  ┘   Virement groupé
                                           -450 000 XAF
                         TOTAL: 450 000 XAF
```

### 🔍 Tolérance intelligente

Le système accepte de petites différences (normales en pratique) :

- **Montant :** ±1% (frais bancaires, arrondi)
- **Date :** ±5 jours (délais bancaires)
- **Description :** Similarité partielle acceptée

### ✅ Ce que vous pouvez faire

1. **Voir les suggestions automatiques**
   - Le système propose les rapprochements probables
   - Vous validez ou refusez

2. **Rapprocher manuellement**
   - Si l'algorithme ne trouve pas, vous pouvez forcer un rapprochement

3. **Dé-rapprocher si erreur**
   - Annulation possible pour corriger une erreur

4. **Suivre l'état**
   - Paiements rapprochés ✅
   - Paiements en attente ⏳
   - Mouvements bancaires non identifiés ❓

---

## Les nouveaux rapports fiscaux

### 1️⃣ Rapport mensuel AIR (Acompte sur Impôt sur le Revenu)

**Conformité :** Formulaire DGI/D10/A (Direction Générale des Impôts Cameroun)
**Échéance :** 15 du mois suivant

#### Ce que le rapport contient :

```
📊 RAPPORT AIR - Janvier 2025

┌─────────────────────────────────────────────────────────┐
│ Transactions avec NIU (2,2%)                            │
├─────────────────────┬───────────┬───────────┬───────────┤
│ Fournisseur         │ Montant   │ Taux      │ AIR       │
├─────────────────────┼───────────┼───────────┼───────────┤
│ Dupont SARL (NIU)   │ 5 000 000 │ 2,2%      │ 110 000   │
│ Martin SA (NIU)     │ 3 000 000 │ 2,2%      │  66 000   │
├─────────────────────┼───────────┼───────────┼───────────┤
│ TOTAL               │ 8 000 000 │           │ 176 000   │
└─────────────────────┴───────────┴───────────┴───────────┘

┌─────────────────────────────────────────────────────────┐
│ Transactions SANS NIU (5,5%) ⚠️ PÉNALITÉ                │
├─────────────────────┬───────────┬───────────┬───────────┤
│ Fournisseur         │ Montant   │ Taux      │ AIR       │
├─────────────────────┼───────────┼───────────┼───────────┤
│ Bernard (SANS NIU)  │ 2 000 000 │ 5,5%      │ 110 000   │
├─────────────────────┼───────────┼───────────┼───────────┤
│ TOTAL               │ 2 000 000 │           │ 110 000   │
└─────────────────────┴───────────┴───────────┴───────────┘

⚠️ COÛT DE LA PÉNALITÉ: 66 000 XAF
   (3,3% supplémentaire sur 2 000 000 XAF)

💡 Action recommandée: Demander le NIU à Bernard
   Économie potentielle: 66 000 XAF/mois = 792 000 XAF/an
```

### 2️⃣ Rapport IRPP Loyer (Retenue à la source sur loyers)

**Conformité :** CGI Art. 65 (Code Général des Impôts)
**Taux :** 15% retenue à la source
**Échéance :** 15 du mois suivant

#### Ce que le rapport contient :

```
🏠 RAPPORT IRPP LOYER - Janvier 2025

┌──────────────────────────────────────────────────────────────┐
│ Bailleur               │ Loyer brut │ IRPP 15% │ Net versé  │
├────────────────────────┼────────────┼──────────┼────────────┤
│ M. Kamga (Local)       │  500 000   │  75 000  │  425 000   │
│ Mme Ngo (Entrepôt)     │  800 000   │ 120 000  │  680 000   │
├────────────────────────┼────────────┼──────────┼────────────┤
│ TOTAL                  │1 300 000   │ 195 000  │1 105 000   │
└────────────────────────┴────────────┴──────────┴────────────┘

À reverser à la DGI: 195 000 XAF avant le 15/02/2025
```

### 3️⃣ Alerte Fournisseurs sans NIU

**Objectif :** Identifier les fournisseurs vous coûtant une pénalité de 3,3%

```
⚠️ ANALYSE FOURNISSEURS SANS NIU - Période: Janvier-Mars 2025

┌────────────────────────────────────────────────────────────────────┐
│ Fournisseur  │ Transactions │ Total achats │ Pénalité │ % du total │
├──────────────┼──────────────┼──────────────┼──────────┼────────────┤
│ Bernard      │     12       │  6 000 000   │ 198 000  │    45%     │
│ Fotso        │      8       │  3 500 000   │ 115 500  │    26%     │
│ Njoya        │      5       │  2 000 000   │  66 000  │    15%     │
│ Autres       │      9       │  2 200 000   │  72 600  │    14%     │
├──────────────┼──────────────┼──────────────┼──────────┼────────────┤
│ TOTAL        │     34       │ 13 700 000   │ 452 100  │   100%     │
└──────────────┴──────────────┴──────────────┴──────────┴────────────┘

💰 COÛT TOTAL DES PÉNALITÉS: 452 100 XAF (3 mois)
📈 EXTRAPOLATION ANNUELLE: 1 808 400 XAF

🎯 ACTION PRIORITAIRE:
   1. Bernard → Économie potentielle: 792 000 XAF/an
   2. Fotso  → Économie potentielle: 462 000 XAF/an
   3. Njoya  → Économie potentielle: 264 000 XAF/an

📧 Contacts:
   - Bernard: +237 6XX XX XX XX | bernard@email.cm
   - Fotso:   +237 6XX XX XX XX | fotso@email.cm
```

### 4️⃣ Calendrier fiscal

**Objectif :** Ne jamais manquer une échéance fiscale

```
📅 CALENDRIER FISCAL 2025

┌─────────┬────────────┬─────────────┬──────────────────────────────┐
│ Mois    │ Échéance   │ À payer     │ Détail                       │
├─────────┼────────────┼─────────────┼──────────────────────────────┤
│ Janvier │ 15/02/2025 │  1 250 000  │ TVA: 800K, AIR: 300K, IRPP: 150K │
│ Février │ 15/03/2025 │  1 100 000  │ TVA: 700K, AIR: 250K, IRPP: 150K │
│ Mars    │ 15/04/2025 │  1 350 000  │ TVA: 850K, AIR: 320K, IRPP: 180K │
└─────────┴────────────┴─────────────┴──────────────────────────────┘

📌 PROCHAINE ÉCHÉANCE: 15/02/2025 (dans 12 jours)
   Montant à préparer: 1 250 000 XAF
```

---

## Traçabilité fiscale renforcée

### 🔍 Avant vs Après

#### ❌ Avant (calculs manuels)

```
Facture FA-001: 1 000 000 XAF HT
TVA = 1 000 000 × 19.25% = 192 500 XAF ← Calculé manuellement
Total TTC = 1 192 500 XAF

⚠️ Problèmes:
- Impossible de retrouver comment la TVA a été calculée
- Pas d'historique des taux appliqués
- Erreurs possibles de saisie manuelle
- Déclaration TVA = re-calcul manuel
```

#### ✅ Après (traçabilité automatique)

```
Facture FA-001 générée le 15/01/2025

Enregistrement TaxCalculation #TC-001:
┌──────────────────────────────────────────────────┐
│ Type de taxe    : TVA                            │
│ Montant de base : 1 000 000 XAF                  │
│ Taux appliqué   : 19,25%                         │
│ Montant TVA     : 192 500 XAF                    │
│ Compte OHADA    : 4431 (TVA collectée)           │
│ Référence légale: CGI Art. 127                   │
│ Date calcul     : 15/01/2025 10:35               │
│ Statut          : CALCULÉ → POSTÉ → DÉCLARÉ      │
│ Déclaration     : Janvier 2025 (payée le 14/02)  │
└──────────────────────────────────────────────────┘

✅ Avantages:
- Traçabilité complète de chaque calcul
- Déclaration TVA = somme automatique des TaxCalculation
- Audit trail complet pour contrôle fiscal
- Aucun risque d'oubli
```

### 📊 Source unique de vérité

Tous les services utilisent désormais **TaxCalculation** comme source principale :

```
                    ┌─────────────────┐
                    │ TaxCalculation  │ ← SOURCE DE VÉRITÉ
                    │  (historique)   │
                    └────────┬────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
         ▼                   ▼                   ▼
┌────────────────┐  ┌────────────────┐  ┌────────────────┐
│ Déclaration    │  │ Rapports       │  │ Tableaux de    │
│ TVA mensuelle  │  │ AIR & IRPP     │  │ bord           │
└────────────────┘  └────────────────┘  └────────────────┘

Avant: 3 calculs différents = 3 résultats possiblement différents ❌
Après: 1 seul calcul = cohérence garantie ✅
```

---

## Cas d'usage pratiques

### 📝 Cas 1 : Rapprochement d'un paiement fournisseur

**Situation :** Vous avez payé le fournisseur Dupont le 15 janvier

#### Étape 1 : Enregistrement du paiement
```
Date: 15/01/2025
Action: Créer un Payment
  - Fournisseur: Dupont
  - Montant: 500 000 XAF
  - Mode: Virement
  - Facture: FA-2024-158
```

#### Étape 2 : Import du relevé bancaire
```
Date: 20/01/2025
Action: Importer le relevé bancaire (format CSV/Excel)
  → Création automatique de BankTransactions

BankTransaction créé:
  - Date valeur: 17/01/2025
  - Libellé: "VRT DUPONT FA-2024-158"
  - Montant: -499 800 XAF (frais: 200 XAF)
```

#### Étape 3 : Rapprochement automatique
```
🤖 Suggestion automatique:
┌──────────────────────────────────────────────────────┐
│ Payment #P-001 ↔ BankTransaction #BT-456             │
├──────────────────────────────────────────────────────┤
│ Score de correspondance: 95/100                      │
│                                                      │
│ Montant:     500 000 ≈ 499 800 ✅ (99,96%)          │
│ Date:        15/01 vs 17/01    ✅ (2 jours)          │
│ Description: "FA-2024-158" trouvé dans les deux ✅   │
└──────────────────────────────────────────────────────┘

[✅ Valider] [❌ Refuser]
```

#### Résultat final
```
Payment #P-001
  - Statut: RAPPROCHÉ ✅
  - BankTransaction: #BT-456
  - Date rapprochement: 20/01/2025
  - Rapproché par: Marie Comptable
  - Écart: 200 XAF (frais bancaires)
```

### 📝 Cas 2 : Détection d'un fournisseur sans NIU

**Situation :** Nouveau fournisseur sans NIU

#### Scénario
```
Date: 10/02/2025
Facture fournisseur Bernard: 2 000 000 XAF HT

🔍 Système détecte: Bernard n'a pas de NIU

Calcul automatique:
  - AIR avec NIU:    2 000 000 × 2,2%  =  44 000 XAF
  - AIR sans NIU:    2 000 000 × 5,5%  = 110 000 XAF
  - PÉNALITÉ:        2 000 000 × 3,3%  =  66 000 XAF ⚠️

Enregistrement TaxCalculation:
  ┌────────────────────────────────────────┐
  │ Type: AIR_SANS_NIU                     │
  │ Taux: 5,5%                             │
  │ AIR retenu: 110 000 XAF                │
  │ ⚠️ ALERTE ACTIVÉE                      │
  │ Message: "Fournisseur sans NIU -       │
  │          Pénalité de 66 000 XAF"       │
  └────────────────────────────────────────┘
```

#### Alert dans le tableau de bord
```
⚠️ ALERTE FISCALE

Fournisseur Bernard (ID: 158)
  - Transactions ce mois: 3
  - Total achats: 5 500 000 XAF
  - Pénalités payées: 181 500 XAF
  - Économie si NIU obtenu: 181 500 XAF

📧 Contact: bernard@entreprise.cm
📞 Tel: +237 6XX XX XX XX

[📄 Demander NIU] [📊 Voir historique] [✖️ Ignorer]
```

### 📝 Cas 3 : Génération déclaration TVA mensuelle

**Situation :** Fin du mois, préparation de la déclaration TVA

#### Ancien processus (manuel)
```
❌ Étapes longues et risquées:
1. Lister toutes les factures du mois
2. Calculer la TVA collectée manuellement
3. Lister tous les achats du mois
4. Calculer la TVA déductible manuellement
5. Faire la différence
6. Remplir le formulaire DGI
⏱️ Temps: 4-6 heures
⚠️ Risques: Erreurs de calcul, oublis
```

#### Nouveau processus (automatique)
```
✅ Étapes simplifiées:
1. Cliquer sur "Générer déclaration TVA Février 2025"
2. Le système produit instantanément:

┌─────────────────────────────────────────────────────┐
│ DÉCLARATION TVA - Février 2025                      │
├─────────────────────────────────────────────────────┤
│ TVA COLLECTÉE                                       │
│   701 - Ventes marchandises     15 000 000 × 19,25% │
│   → TVA collectée                      2 887 500 XAF │
│                                                     │
│ TVA DÉDUCTIBLE                                      │
│   601 - Achats marchandises      8 000 000 × 19,25% │
│   → TVA déductible (100%)              1 540 000 XAF │
│   605 - Charges externes         2 000 000 × 19,25% │
│   → TVA déductible (60%)                 231 000 XAF │
│   → Total TVA déductible               1 771 000 XAF │
│                                                     │
│ TVA À PAYER                                         │
│   2 887 500 - 1 771 000 =              1 116 500 XAF │
│                                                     │
│ Échéance: 15/03/2025                                │
│ Statut: ⏳ À payer                                   │
└─────────────────────────────────────────────────────┘

[📄 Exporter PDF] [📧 Envoyer DGI] [💾 Marquer comme payé]

⏱️ Temps: 30 secondes
✅ Garantie: Tous les calculs tracés et vérifiables
```

---

## 🎯 Résumé des bénéfices

### Pour le comptable
| Avant | Après |
|-------|-------|
| ❌ Rapprochement bancaire manuel (2-3h/semaine) | ✅ Suggestions automatiques (15 min/semaine) |
| ❌ Déclarations fiscales calculées manuellement | ✅ Rapports générés en 1 clic |
| ❌ Pénalités NIU non détectées | ✅ Alertes proactives + calcul économies |
| ❌ Impossible de tracer les calculs passés | ✅ Historique complet de chaque calcul |
| ❌ Risque d'erreurs de saisie | ✅ Validation automatique partie double |

### Pour la direction
| Indicateur | Avant | Après |
|------------|-------|-------|
| **Temps de clôture mensuelle** | 5-7 jours | 1-2 jours |
| **Erreurs de rapprochement** | ~5-10/mois | ~0-1/mois |
| **Visibilité pénalités NIU** | Aucune | Temps réel |
| **Conformité fiscale** | Manuelle | Automatique |
| **Économies potentielles** | Non mesurées | Chiffrées précisément |

### Pour l'auditeur
- ✅ Traçabilité complète de chaque calcul de taxe
- ✅ Lien direct Payment → BankTransaction → Écriture comptable
- ✅ Conformité OHADA garantie (validation partie double)
- ✅ Historique immuable des opérations
- ✅ Rapports standardisés conformes DGI Cameroun

---

## 📞 Support

**Questions sur ces nouvelles fonctionnalités ?**
- 📧 Email: support@predykt.com
- 📱 Téléphone: +237 6XX XX XX XX
- 📚 Documentation complète: https://docs.predykt.com

**Formation disponible :**
- Formation en ligne: 2h (gratuit)
- Formation sur site: Nous contacter
- Webinaires mensuels: Chaque premier jeudi du mois

---

*Document rédigé le 11/12/2025 - PREDYKT Accounting System v2.0*
*Conforme OHADA & CGI Cameroun*
