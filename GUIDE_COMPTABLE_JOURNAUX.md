# 📚 GUIDE COMPTABLE - SYSTÈME DE JOURNAUX

## 👥 À qui s'adresse ce guide?

Ce guide est destiné aux **comptables**, **experts-comptables** et **gestionnaires** qui utiliseront le système PREDYKT pour la tenue de leur comptabilité.

**Vous n'avez pas besoin de connaissances techniques!** Ce guide explique le système en termes comptables que vous connaissez déjà.

---

## 📋 Table des matières

1. [Vue d'ensemble du système](#vue-densemble-du-système)
2. [Les 5 journaux comptables](#les-5-journaux-comptables)
3. [Enregistrement d'une écriture](#enregistrement-dune-écriture)
4. [Plan comptable OHADA intégré](#plan-comptable-ohada-intégré)
5. [TVA automatique](#tva-automatique)
6. [Consultations et rapports](#consultations-et-rapports)
7. [Sécurité et traçabilité](#sécurité-et-traçabilité)
8. [Cas pratiques](#cas-pratiques)
9. [Questions fréquentes](#questions-fréquentes)

---

## 🎯 Vue d'ensemble du système

### Qu'est-ce que PREDYKT?

PREDYKT est un **système comptable complet** conforme aux normes **OHADA** et à la **législation camerounaise**. Il permet de:

✅ Tenir vos journaux comptables (AC, VE, BQ, CA, OD)
✅ Enregistrer vos écritures en partie double
✅ Calculer automatiquement la TVA récupérable
✅ Gérer le prorata de TVA (activités mixtes)
✅ Générer tous vos états financiers
✅ Respecter les obligations fiscales et légales

### Principes comptables respectés

| Principe | Explication | Dans PREDYKT |
|----------|-------------|--------------|
| **Partie double** | Débit = Crédit | ✅ Vérification automatique |
| **Traçabilité** | Audit trail complet | ✅ Historique de toutes les modifications |
| **Numérotation continue** | Pas de trous dans la numérotation | ✅ Séquences automatiques par journal |
| **Immutabilité** | Écritures définitives | ✅ Verrouillage après validation |
| **Plan comptable OHADA** | Comptes normalisés | ✅ Plan pré-chargé et personnalisable |

---

## 📖 Les 5 journaux comptables

Le système PREDYKT gère les **5 journaux obligatoires** selon les normes OHADA:

### 1. Journal des Achats (AC)

**Code:** `AC`

**Utilisation:** Enregistrer toutes vos factures fournisseurs

**Comptes typiques:**
- Débit: 60x (Achats), 61x-64x (Charges), 4451 (TVA récupérable)
- Crédit: 401 (Fournisseurs), 521 (Banque)

**Exemple d'écriture:**
```
Référence: FACH-2025-001
Date: 15/01/2025
Journal: AC

Débit  605  Achats de marchandises       100 000 FCFA
Débit  4451 TVA récupérable               19 250 FCFA
Crédit 401  Fournisseur ABC                        119 250 FCFA

Libellé: Achat matières premières - Facture F-2025-042
```

**Avantages:**
- 📊 Livre des achats automatique
- 💰 TVA récupérable calculée automatiquement
- 📈 Suivi des dettes fournisseurs
- 📑 Justification fiscale des charges

---

### 2. Journal des Ventes (VE)

**Code:** `VE`

**Utilisation:** Enregistrer toutes vos factures clients

**Comptes typiques:**
- Débit: 411 (Clients), 521 (Banque si vente comptant)
- Crédit: 70x (Ventes), 4431 (TVA collectée)

**Exemple d'écriture:**
```
Référence: FVEN-2025-042
Date: 16/01/2025
Journal: VE

Débit  411  Client XYZ                   238 500 FCFA
Crédit 701  Ventes de marchandises                 200 000 FCFA
Crédit 4431 TVA collectée                            38 500 FCFA

Libellé: Vente produits - Facture V-2025-042
```

**Avantages:**
- 📊 Livre des ventes automatique
- 💰 TVA collectée calculée automatiquement
- 📈 Suivi des créances clients
- 📑 Justification du chiffre d'affaires

---

### 3. Journal de Banque (BQ)

**Code:** `BQ`

**Utilisation:** Enregistrer tous les mouvements de vos comptes bancaires

**Comptes typiques:**
- Débit/Crédit: 521 (Banque)
- Contrepartie: 401 (Fournisseurs), 411 (Clients), etc.

**Exemple d'écriture - Paiement fournisseur:**
```
Référence: VIR-2025-015
Date: 17/01/2025
Journal: BQ

Débit  401  Fournisseur ABC              119 250 FCFA
Crédit 521  Banque BCA                            119 250 FCFA

Libellé: Paiement facture F-2025-042 par virement
```

**Exemple d'écriture - Encaissement client:**
```
Référence: ENC-2025-028
Date: 18/01/2025
Journal: BQ

Débit  521  Banque BCA                   238 500 FCFA
Crédit 411  Client XYZ                             238 500 FCFA

Libellé: Encaissement facture V-2025-042 par chèque
```

**Avantages:**
- 💳 Rapprochement bancaire facilité
- 📊 Livre de banque automatique
- 💰 Trésorerie en temps réel
- 🔍 Traçabilité des paiements

---

### 4. Journal de Caisse (CA)

**Code:** `CA`

**Utilisation:** Enregistrer tous les mouvements d'espèces

**Comptes typiques:**
- Débit/Crédit: 57 (Caisse)
- Contrepartie: Tous comptes

**Exemple d'écriture - Dépense:**
```
Référence: CA-2025-008
Date: 18/01/2025
Journal: CA

Débit  605  Achats fournitures             5 000 FCFA
Crédit 571  Caisse                                  5 000 FCFA

Libellé: Achat fournitures de bureau en espèces
```

**Exemple d'écriture - Recette:**
```
Référence: CA-2025-009
Date: 19/01/2025
Journal: CA

Débit  571  Caisse                        50 000 FCFA
Crédit 707  Ventes de prestations                  50 000 FCFA

Libellé: Prestation service client - Paiement comptant
```

**Avantages:**
- 💵 Livre de caisse automatique
- 📊 Solde de caisse en temps réel
- 🔒 Contrôle du cash
- 📑 Justification des espèces

---

### 5. Journal des Opérations Diverses (OD)

**Code:** `OD`

**Utilisation:** Enregistrer toutes les autres opérations

**Exemples d'utilisation:**
- Salaires et charges sociales
- Amortissements
- Provisions
- Régularisations
- Corrections d'erreurs
- Écritures de clôture

**Exemple d'écriture - Salaires:**
```
Référence: SAL-2025-01
Date: 31/01/2025
Journal: OD

Débit  661  Salaires bruts               500 000 FCFA
Débit  664  Charges sociales             100 000 FCFA
Crédit 421  Personnel - Rémunérations              400 000 FCFA
Crédit 431  Organismes sociaux                     200 000 FCFA

Libellé: Salaires janvier 2025
```

**Exemple d'écriture - Amortissement:**
```
Référence: AMORT-2025-01
Date: 31/01/2025
Journal: OD

Débit  681  Dotations aux amortissements  83 333 FCFA
Crédit 284  Amortissements matériel                83 333 FCFA

Libellé: Amortissement mensuel véhicule (1/12 de 1 000 000)
```

**Avantages:**
- 📊 Flexibilité pour toutes les opérations
- 💼 Gestion des salaires
- 📉 Calcul automatique des amortissements
- 🔧 Corrections et régularisations

---

## ✍️ Enregistrement d'une écriture

### Méthode 1: Via l'interface web (recommandé)

**Étape 1:** Sélectionner le journal approprié
```
[ Achats (AC) ] [ Ventes (VE) ] [ Banque (BQ) ] [ Caisse (CA) ] [ Divers (OD) ]
```

**Étape 2:** Saisir les informations générales
```
Date:           [ 15/01/2025 ]
Référence:      [ FACH-2025-001 ] (générée automatiquement)
Pièce justif.:  [ F-2025-042 ]
```

**Étape 3:** Saisir les lignes d'écriture
```
Compte     | Libellé                         | Débit      | Crédit
-----------|----------------------------------|------------|------------
605        | Achats marchandises              | 100 000    |
4451       | TVA récupérable                  |  19 250    |
401        | Fournisseur ABC                  |            | 119 250
-----------|----------------------------------|------------|------------
TOTAL      |                                  | 119 250    | 119 250  ✅
```

**Étape 4:** Valider
```
[ Enregistrer en brouillon ] [ Valider définitivement ]
```

**Résultat:**
- ✅ Écriture enregistrée dans le journal AC
- ✅ TVA récupérable calculée automatiquement
- ✅ Balance mise à jour en temps réel
- ✅ Traçabilité complète (qui, quand, quoi)

---

### Méthode 2: Via l'API (pour intégrations)

**Pour les cabinets d'expertise comptable** qui souhaitent importer des écritures depuis un autre logiciel:

```bash
POST /api/v1/companies/1/general-ledger/entries

{
  "entryDate": "2025-01-15",
  "reference": "FACH-2025-001",
  "journalCode": "AC",
  "lines": [
    {
      "accountNumber": "605",
      "description": "Achats marchandises",
      "debitAmount": 100000,
      "creditAmount": 0
    },
    {
      "accountNumber": "4451",
      "description": "TVA récupérable",
      "debitAmount": 19250,
      "creditAmount": 0
    },
    {
      "accountNumber": "401",
      "description": "Fournisseur ABC",
      "debitAmount": 0,
      "creditAmount": 119250
    }
  ]
}
```

**Avantages:**
- 🔄 Import automatique depuis Excel/CSV
- 🚀 Gain de temps pour saisies répétitives
- 🔗 Intégration avec logiciels tiers (facturation, paie, etc.)

---

## 📊 Plan comptable OHADA intégré

### Le système est pré-configuré avec le plan OHADA

Le plan comptable **OHADA complet** est déjà chargé dans le système:

#### Classe 1: Comptes de ressources durables
- 10 - Capital
- 12 - Résultats reportés
- 13 - Subventions d'investissement
- 16 - Emprunts et dettes assimilées

#### Classe 2: Comptes d'actif immobilisé
- 21 - Immobilisations incorporelles
- 22 - Terrains
- 23 - Bâtiments
- 24 - Matériel et outillage
- 25 - Mobilier et matériel de bureau
- 28 - Amortissements

#### Classe 3: Comptes de stocks
- 31 - Marchandises
- 32 - Matières premières
- 33 - Autres approvisionnements
- 35 - Produits finis
- 37 - Stocks de marchandises

#### Classe 4: Comptes de tiers
- 40 - Fournisseurs et comptes rattachés
- 41 - Clients et comptes rattachés
- 42 - Personnel
- 43 - Organismes sociaux
- 44 - État et collectivités publiques (TVA!)
- 46 - Débiteurs et créditeurs divers
- 47 - Comptes transitoires

#### Classe 5: Comptes de trésorerie
- 52 - Banques
- 53 - Établissements financiers
- 57 - Caisse

#### Classe 6: Comptes de charges
- 60 - Achats
- 61 - Transports
- 62 - Services extérieurs
- 63 - Autres services extérieurs
- 64 - Impôts et taxes
- 65 - Autres charges
- 66 - Charges de personnel
- 67 - Frais financiers
- 68 - Dotations aux amortissements

#### Classe 7: Comptes de produits
- 70 - Ventes
- 71 - Subventions d'exploitation
- 72 - Production immobilisée
- 73 - Variations de stocks
- 75 - Autres produits
- 77 - Produits financiers
- 78 - Reprises d'amortissements

#### Classe 8: Comptes spéciaux
- 80 - Comptes de liaison
- 89 - Bilan

### Personnalisation du plan comptable

Vous pouvez **ajouter vos propres sous-comptes**:

```
Exemple: Vous avez plusieurs banques

521   Banques (compte général OHADA)
5211  Banque BCA (votre sous-compte)
5212  Banque SGBC (votre sous-compte)
5213  Banque UBA (votre sous-compte)
```

**Comment ajouter un compte?**
```
Menu: Paramètres > Plan comptable > Nouveau compte

Numéro:      5211
Libellé:     Banque BCA - Compte courant
Type:        Actif
Nature:      Débit
Compte père: 521
```

---

## 💰 TVA automatique

### Le système calcule la TVA récupérable automatiquement!

**C'est LA grande force du système PREDYKT.**

#### Comment ça fonctionne?

**Étape 1:** Vous enregistrez une écriture normale
```
Journal: AC
Débit  605  Achats carburant              100 000 FCFA
Débit  4451 TVA récupérable                19 250 FCFA
Crédit 401  Fournisseur Total                      119 250 FCFA
```

**Étape 2:** Le système détecte automatiquement le compte 4451
```
🔍 Détection: Compte TVA récupérable (4451)
```

**Étape 3:** Le moteur de règles analyse la description
```
🤖 Analyse: "Achats carburant"
✅ Règle trouvée: Carburant véhicule utilitaire
📊 Catégorie: VU (Véhicules Utilitaires)
💰 Taux de récupération par nature: 80%
```

**Étape 4:** Application du prorata (si activités mixtes)
```
💵 TVA facturée: 19 250 FCFA
📉 Récupérable par nature (80%): 15 400 FCFA
📊 Prorata de l'entreprise: 85%
✅ TVA RÉCUPÉRABLE FINALE: 13 090 FCFA
⚠️ TVA NON RÉCUPÉRABLE: 6 160 FCFA
```

**Étape 5:** Enregistrement automatique
```
✅ Calcul enregistré dans la base
✅ Traçabilité complète
✅ Prêt pour déclaration TVA
```

---

### Les 26 règles de récupération (CGI Cameroun)

Le système intègre **26 règles automatiques** conformes au Code Général des Impôts du Cameroun:

#### Catégorie VP - Véhicules de Tourisme (0%)

**TVA NON RÉCUPÉRABLE**

| Règle | Détection | Taux |
|-------|-----------|------|
| Voiture particulière | "voiture", "berline", "sedan" | 0% |
| Carburant VP | "essence voiture", "carburant VP" | 0% |
| Assurance VP | "assurance véhicule tourisme" | 0% |
| Entretien VP | "réparation voiture particulière" | 0% |

#### Catégorie VU - Véhicules Utilitaires (80%)

**TVA PARTIELLEMENT RÉCUPÉRABLE**

| Règle | Détection | Taux |
|-------|-----------|------|
| Camion | "camion", "poids lourd" | 80% |
| Camionnette | "camionnette", "fourgon" | 80% |
| Carburant VU | "gasoil camion", "carburant utilitaire" | 80% |

#### Catégorie VER - 100% Récupérable

**TVA ENTIÈREMENT RÉCUPÉRABLE**

| Règle | Détection | Taux |
|-------|-----------|------|
| Matières premières | "matières premières", "matériaux" | 100% |
| Fournitures | "fournitures", "consommables" | 100% |
| Services | "prestations", "honoraires" | 100% |
| Matériel | "ordinateur", "machine", "équipement" | 100% |

#### Catégorie VNRE - Non Récupérable (0%)

**TVA NON RÉCUPÉRABLE (Art. 132 CGI)**

| Règle | Détection | Taux |
|-------|-----------|------|
| Cadeaux | "cadeau", "don" | 0% |
| Hôtels/restaurants | "hôtel", "restaurant" | 0% |
| Logement | "immobilier habitation" | 0% |

### Gérer le prorata de TVA

**Qu'est-ce que le prorata?**

Le **prorata de TVA** s'applique aux entreprises ayant des **activités mixtes**:
- Activités taxables (ventes locales) → TVA récupérable
- Activités exonérées (exports) → TVA NON récupérable

**Formule:**
```
Prorata = (CA taxable ÷ CA total) × 100
```

**Exemple:**

Entreprise avec:
- CA taxable (local): 500 000 000 FCFA
- CA exonéré (export): 100 000 000 FCFA
- **CA total: 600 000 000 FCFA**

```
Prorata = (500 000 000 ÷ 600 000 000) × 100
Prorata = 83,33%
```

**Conséquence:**

Sur un achat de 100 000 FCFA HT (19 250 FCFA TVA):
```
ÉTAPE 1 (Nature): Matière première → 100% = 19 250 FCFA
ÉTAPE 2 (Prorata): 19 250 × 83,33% = 16 041 FCFA

✅ TVA récupérable: 16 041 FCFA
⚠️ TVA non récupérable: 3 209 FCFA
```

**Comment configurer le prorata dans PREDYKT?**

```
Menu: TVA > Prorata > Nouveau prorata

Année fiscale:        2025
CA taxable:           500 000 000 FCFA
CA exonéré:           100 000 000 FCFA
Type:                 [ Définitif ]

→ Prorata calculé automatiquement: 83,33%
```

**Types de prorata:**

1. **Prorata provisoire** (début d'année)
   - Basé sur l'année N-1
   - Utilisé toute l'année N
   - Régularisé en fin d'année

2. **Prorata définitif** (fin d'année)
   - Basé sur le CA réel de l'année N
   - Régularisation si écart > 10%
   - Déclaration mars N+1

---

## 📑 Consultations et rapports

### Rapports disponibles dans PREDYKT

#### 1. Grand livre

**Qu'est-ce que c'est?**
Le détail de tous les mouvements d'un compte sur une période.

**Comment l'obtenir?**
```
Menu: Rapports > Grand livre

Compte:        605 (Achats de marchandises)
Du:            01/01/2025
Au:            31/01/2025
```

**Résultat:**
```
GRAND LIVRE - Compte 605 - Achats de marchandises
Période: 01/01/2025 - 31/01/2025

Date       | Journal | Référence    | Libellé              | Débit     | Crédit    | Solde
-----------|---------|--------------|----------------------|-----------|-----------|----------
15/01/2025 | AC      | FACH-2025-001| Achat matières       | 100 000   |           | 100 000
20/01/2025 | AC      | FACH-2025-002| Achat fournitures    |  50 000   |           | 150 000
25/01/2025 | AC      | FACH-2025-003| Achat emballages     |  30 000   |           | 180 000
-----------|---------|--------------|----------------------|-----------|-----------|----------
TOTAUX                                                      | 180 000   |     0     | 180 000
```

---

#### 2. Balance de vérification

**Qu'est-ce que c'est?**
Le résumé de tous les comptes avec leurs soldes.

**Comment l'obtenir?**
```
Menu: Rapports > Balance

Du:            01/01/2025
Au:            31/01/2025
```

**Résultat:**
```
BALANCE DE VÉRIFICATION
Période: 01/01/2025 - 31/01/2025

Compte | Libellé                    | Débit     | Crédit    | Solde débiteur | Solde créditeur
-------|----------------------------|-----------|-----------|----------------|----------------
521    | Banque                     | 500 000   | 300 000   | 200 000        |
605    | Achats marchandises        | 180 000   |           | 180 000        |
661    | Salaires                   | 500 000   |           | 500 000        |
401    | Fournisseurs               |           | 119 250   |                | 119 250
411    | Clients                    | 238 500   |           | 238 500        |
701    | Ventes marchandises        |           | 1 000 000 |                | 1 000 000
-------|----------------------------|-----------|-----------|----------------|----------------
TOTAUX                              | 1 418 500 | 1 419 250 | 1 118 500      | 1 119 250

✅ Balance équilibrée (écart: 750 FCFA à régulariser)
```

---

#### 3. Journal (livre-journal)

**Qu'est-ce que c'est?**
Le détail de toutes les écritures d'un journal sur une période.

**Comment l'obtenir?**
```
Menu: Rapports > Journal

Journal:       AC (Achats)
Du:            01/01/2025
Au:            31/01/2025
```

**Résultat:**
```
LIVRE-JOURNAL DES ACHATS (AC)
Période: 01/01/2025 - 31/01/2025

Date       | Référence    | Compte | Libellé                      | Débit     | Crédit
-----------|--------------|--------|------------------------------|-----------|----------
15/01/2025 | FACH-2025-001| 605    | Achats marchandises          | 100 000   |
           |              | 4451   | TVA récupérable              |  19 250   |
           |              | 401    | Fournisseur ABC              |           | 119 250
-----------|--------------|--------|------------------------------|-----------|----------
20/01/2025 | FACH-2025-002| 605    | Achats fournitures           |  50 000   |
           |              | 4451   | TVA récupérable              |   9 625   |
           |              | 401    | Fournisseur XYZ              |           |  59 625
-----------|--------------|--------|------------------------------|-----------|----------
TOTAUX JOURNAL AC                                               | 178 875   | 178 875

✅ Journal équilibré
```

---

#### 4. Bilan comptable

**Qu'est-ce que c'est?**
La situation patrimoniale de l'entreprise à une date donnée.

**Comment l'obtenir?**
```
Menu: Rapports > Bilan

Au:            31/12/2025
```

**Résultat:**
```
BILAN COMPTABLE au 31/12/2025

ACTIF                                      |  PASSIF
-------------------------------------------|-------------------------------------------
ACTIF IMMOBILISÉ                           |  CAPITAUX PROPRES
  Immobilisations corporelles   5 000 000  |    Capital                    10 000 000
  Amortissements               -1 000 000  |    Résultat de l'exercice      2 000 000
                                           |
ACTIF CIRCULANT                            |  DETTES
  Stocks                        2 000 000  |    Emprunts                    3 000 000
  Clients                       1 500 000  |    Fournisseurs                1 200 000
  Banque                          500 000  |    TVA à payer                   300 000
  Caisse                          100 000  |
-------------------------------------------|-------------------------------------------
TOTAL ACTIF                    13 100 000  |  TOTAL PASSIF                 13 100 000

✅ Bilan équilibré
```

---

#### 5. Compte de résultat

**Qu'est-ce que c'est?**
Le résumé des produits et charges sur une période.

**Comment l'obtenir?**
```
Menu: Rapports > Compte de résultat

Du:            01/01/2025
Au:            31/12/2025
```

**Résultat:**
```
COMPTE DE RÉSULTAT
Exercice: 2025

CHARGES                                    |  PRODUITS
-------------------------------------------|-------------------------------------------
CHARGES D'EXPLOITATION                     |  PRODUITS D'EXPLOITATION
  Achats marchandises          10 000 000  |    Ventes marchandises        20 000 000
  Services extérieurs           2 000 000  |    Autres produits               500 000
  Charges de personnel          5 000 000  |
  Dotations aux amort.          1 000 000  |
                                           |
CHARGES FINANCIÈRES                        |  PRODUITS FINANCIERS
  Intérêts emprunts               300 000  |    Produits financiers            50 000
-------------------------------------------|-------------------------------------------
TOTAL CHARGES                  18 300 000  |  TOTAL PRODUITS               20 550 000

RÉSULTAT BÉNÉFICIAIRE           2 250 000
-------------------------------------------|-------------------------------------------
TOTAL GÉNÉRAL                  20 550 000  |  TOTAL GÉNÉRAL                20 550 000

✅ Bénéfice: 2 250 000 FCFA
```

---

#### 6. État de la TVA

**Qu'est-ce que c'est?**
Le calcul de la TVA à payer ou crédit de TVA.

**Comment l'obtenir?**
```
Menu: Rapports > TVA

Période:       Janvier 2025
```

**Résultat:**
```
DÉCLARATION TVA - Janvier 2025

A. TVA COLLECTÉE
   Ventes taxables (compte 70x)           10 000 000 FCFA
   TVA collectée au taux 19,25%            1 925 000 FCFA

B. TVA RÉCUPÉRABLE
   TVA sur immobilisations (4451)             50 000 FCFA
   TVA sur achats (4451)                     300 000 FCFA
   TVA sur services (4451)                   100 000 FCFA
   ------------------------------------------------
   Total TVA déductible                      450 000 FCFA

   Impact prorata (83,33%)                  -75 000 FCFA
   ------------------------------------------------
   TVA récupérable finale                    375 000 FCFA

C. TVA À PAYER
   TVA collectée                           1 925 000 FCFA
   TVA récupérable                          -375 000 FCFA
   ------------------------------------------------
   TVA DUE                                 1 550 000 FCFA

Date limite de paiement: 15/02/2025
```

---

#### 7. Statistiques TVA (nouveau!)

**Qu'est-ce que c'est?**
L'analyse détaillée de votre TVA par catégorie.

**Comment l'obtenir?**
```
Menu: TVA > Statistiques

Année:         2025
```

**Résultat:**
```
STATISTIQUES TVA - Année 2025

1. RÉCAPITULATIF GÉNÉRAL
   Nombre de calculs:                     245
   TVA totale facturée:              4 500 000 FCFA
   TVA récupérable (après prorata):  3 200 000 FCFA
   TVA non récupérable:              1 300 000 FCFA
   Taux moyen de récupération:           71,11%

2. RÉPARTITION PAR CATÉGORIE
   ┌────────────┬───────┬──────────────┬──────────────┬──────┐
   │ Catégorie  │ Nb    │ TVA facturée │ TVA récup.   │ Taux │
   ├────────────┼───────┼──────────────┼──────────────┼──────┤
   │ VER (100%) │  180  │  3 500 000   │  2 800 000   │ 80%  │
   │ VU (80%)   │   50  │    800 000   │    533 000   │ 67%  │
   │ VP (0%)    │   15  │    200 000   │          0   │  0%  │
   └────────────┴───────┴──────────────┴──────────────┴──────┘

3. IMPACT DU PRORATA
   Récupérable AVANT prorata:        3 840 000 FCFA
   Prorata appliqué:                     83,33%
   Récupérable APRÈS prorata:        3 200 000 FCFA
   ------------------------------------------------
   Impact prorata (perte):             640 000 FCFA (-16,67%)

4. ALERTES
   ⚠️  15 transactions en catégorie VP (TVA non récupérable)
   ℹ️  Prorata actif: 83,33% (Définitif)
```

---

## 🔒 Sécurité et traçabilité

### Le système garantit la sécurité de vos données

#### 1. Traçabilité complète

**Chaque opération est tracée:**
- ✅ Qui a fait l'opération? (utilisateur)
- ✅ Quand? (date et heure exacte)
- ✅ Quoi? (type d'opération)
- ✅ Sur quoi? (compte, montant, etc.)

**Exemple de journal d'audit:**
```
[15/01/2025 09:30:42] Utilisateur: marie.dupont@cabinet.com
  Action: CRÉATION ÉCRITURE
  Journal: AC
  Référence: FACH-2025-001
  Montant: 119 250 FCFA

[15/01/2025 09:31:05] Utilisateur: marie.dupont@cabinet.com
  Action: VALIDATION ÉCRITURE
  Référence: FACH-2025-001

[20/01/2025 14:15:30] Utilisateur: jean.martin@cabinet.com
  Action: CONSULTATION GRAND LIVRE
  Compte: 605
  Période: 01/01/2025 - 31/01/2025
```

---

#### 2. Verrouillage des périodes

**Principe:** Une fois une période clôturée, aucune modification n'est possible.

**Comment clôturer une période?**
```
Menu: Paramètres > Clôture de période

Période:       Du 01/01/2025 au 31/01/2025
Motif:         Clôture mensuelle janvier 2025

[Clôturer définitivement]

⚠️ ATTENTION: Cette action est IRRÉVERSIBLE!
   Aucune écriture ne pourra être modifiée ou supprimée.
```

**Après clôture:**
```
❌ Impossible de modifier une écriture de janvier
❌ Impossible de supprimer une écriture de janvier
✅ Possible de consulter les écritures
✅ Possible de créer de nouvelles écritures en février
```

---

#### 3. Numérotation séquentielle

**Le système génère automatiquement les numéros d'écriture:**

```
Journal AC (Achats):
  FACH-2025-001
  FACH-2025-002
  FACH-2025-003
  ... (pas de trous possibles)

Journal VE (Ventes):
  FVEN-2025-001
  FVEN-2025-002
  FVEN-2025-003
  ... (numérotation continue garantie)
```

**Avantage:** Impossible de tricher (pas de numéros manquants).

---

#### 4. Multi-tenant (isolation des données)

**Votre cabinet gère plusieurs clients?**

Le système **isole complètement** les données de chaque entreprise:

```
Cabinet EXPERTISE COMPTA
├── Client A (SARL ABC)
│   ├── Plan comptable propre
│   ├── Écritures propres
│   ├── Rapports propres
│   └── Utilisateurs propres
│
├── Client B (SA XYZ)
│   ├── Plan comptable propre
│   ├── Écritures propres
│   ├── Rapports propres
│   └── Utilisateurs propres
│
└── Client C (ETS 123)
    ├── Plan comptable propre
    ├── Écritures propres
    ├── Rapports propres
    └── Utilisateurs propres
```

**Garanties:**
- ❌ Client A ne peut JAMAIS voir les données de Client B
- ❌ Client B ne peut JAMAIS modifier les données de Client C
- ✅ Chaque client est dans une "bulle" hermétique
- ✅ Le cabinet voit tous ses clients (vue d'ensemble)

---

#### 5. Sauvegardes automatiques

**Le système sauvegarde automatiquement:**
- Toutes les 6 heures
- Avant chaque clôture de période
- Sur demande manuelle

**Conservation:**
- Sauvegarde quotidienne: 30 jours
- Sauvegarde mensuelle: 12 mois
- Sauvegarde annuelle: 10 ans

---

## 💼 Cas pratiques

### Cas 1: Achat fournisseur avec paiement différé

**Situation:**
Vous achetez des marchandises pour 100 000 FCFA HT (TVA 19,25%). Le fournisseur vous accorde un crédit de 30 jours.

**Écriture 1 - Réception facture (15/01/2025):**
```
Journal: AC
Référence: FACH-2025-001

Débit  605  Achats marchandises       100 000 FCFA
Débit  4451 TVA récupérable            19 250 FCFA
Crédit 401  Fournisseur ABC                    119 250 FCFA

Libellé: Achat marchandises - Facture F-042 - Paiement à 30 jours
```

**Écriture 2 - Paiement (15/02/2025):**
```
Journal: BQ
Référence: VIR-2025-015

Débit  401  Fournisseur ABC           119 250 FCFA
Crédit 521  Banque BCA                         119 250 FCFA

Libellé: Paiement facture F-042 par virement
```

**TVA:**
```
✅ TVA récupérable: 19 250 FCFA (calculée automatiquement)
📅 Récupération: Déclaration février 2025 (date de facture)
```

---

### Cas 2: Vente client avec encaissement comptant

**Situation:**
Vous vendez des produits pour 200 000 FCFA HT (TVA 19,25%). Le client paie comptant par chèque.

**Écriture unique - Vente et encaissement (16/01/2025):**
```
Journal: VE
Référence: FVEN-2025-042

Débit  521  Banque BCA                238 500 FCFA
Crédit 701  Ventes marchandises               200 000 FCFA
Crédit 4431 TVA collectée                      38 500 FCFA

Libellé: Vente produits client XYZ - Facture V-042 - Chèque n°123456
```

**TVA:**
```
✅ TVA collectée: 38 500 FCFA
📅 À déclarer: Février 2025
```

---

### Cas 3: Achat carburant (camion de livraison)

**Situation:**
Vous achetez du gasoil pour votre camion de livraison: 100 000 FCFA HT.

**Écriture (17/01/2025):**
```
Journal: AC
Référence: FACH-2025-002

Débit  605  Achats carburant camion   100 000 FCFA
Débit  4451 TVA récupérable            19 250 FCFA
Crédit 521  Banque BCA                         119 250 FCFA

Libellé: Carburant gasoil camion de livraison - Station Total
```

**Calcul TVA automatique:**
```
🔍 Détection: "carburant camion"
🤖 Règle: Carburant véhicule utilitaire
📊 Catégorie: VU (80%)
💰 TVA facturée: 19 250 FCFA
✅ Récupérable par nature: 15 400 FCFA (80%)
📊 Prorata (si existe): 85%
✅ RÉCUPÉRABLE FINAL: 13 090 FCFA
⚠️ Non récupérable: 6 160 FCFA
```

---

### Cas 4: Achat carburant (voiture de direction)

**Situation:**
Vous achetez de l'essence pour la voiture du directeur: 50 000 FCFA HT.

**Écriture (18/01/2025):**
```
Journal: AC
Référence: FACH-2025-003

Débit  605  Achats carburant VP        50 000 FCFA
Débit  4451 TVA NON récupérable         9 625 FCFA
Crédit 521  Banque BCA                          59 625 FCFA

Libellé: Carburant essence voiture direction - Station Shell
```

**Calcul TVA automatique:**
```
🔍 Détection: "carburant essence voiture"
🤖 Règle: Carburant véhicule de tourisme
📊 Catégorie: VP (0%)
💰 TVA facturée: 9 625 FCFA
❌ RÉCUPÉRABLE: 0 FCFA (VP = 0%)
⚠️ Non récupérable: 9 625 FCFA (CGI Art. 132)
```

---

### Cas 5: Salaires du mois

**Situation:**
Vous payez les salaires de janvier: 500 000 FCFA brut, charges sociales 100 000 FCFA.

**Écriture 1 - Constatation des salaires (31/01/2025):**
```
Journal: OD
Référence: SAL-2025-01

Débit  661  Salaires bruts            500 000 FCFA
Débit  664  Charges sociales          100 000 FCFA
Crédit 421  Personnel - Salaires nets          400 000 FCFA
Crédit 431  Organismes sociaux                 200 000 FCFA

Libellé: Salaires et charges janvier 2025
```

**Écriture 2 - Paiement des salaires (31/01/2025):**
```
Journal: BQ
Référence: VIR-2025-020

Débit  421  Personnel - Salaires nets 400 000 FCFA
Crédit 521  Banque BCA                         400 000 FCFA

Libellé: Virement salaires janvier 2025
```

**Écriture 3 - Paiement charges sociales (15/02/2025):**
```
Journal: BQ
Référence: VIR-2025-025

Débit  431  Organismes sociaux        200 000 FCFA
Crédit 521  Banque BCA                         200 000 FCFA

Libellé: Paiement CNPS janvier 2025
```

---

### Cas 6: Amortissement mensuel

**Situation:**
Vous avez acheté un véhicule à 10 000 000 FCFA en janvier 2024. Amortissement sur 5 ans (linéaire).

**Calcul:**
```
Valeur: 10 000 000 FCFA
Durée: 5 ans = 60 mois
Amortissement mensuel: 10 000 000 ÷ 60 = 166 667 FCFA/mois
```

**Écriture mensuelle (31/01/2025):**
```
Journal: OD
Référence: AMORT-2025-01

Débit  681  Dotations amortissements  166 667 FCFA
Crédit 284  Amortissement matériel             166 667 FCFA

Libellé: Amortissement mensuel véhicule 1/60
```

---

## ❓ Questions fréquentes

### Q1: Puis-je modifier une écriture déjà enregistrée?

**R:** Oui, TANT QUE la période n'est pas clôturée.

**Méthode:**
```
1. Rechercher l'écriture (Menu: Écritures > Recherche)
2. Cliquer sur "Modifier"
3. Effectuer les modifications
4. Cliquer sur "Enregistrer"

⚠️ L'historique des modifications est conservé (traçabilité).
```

**Après clôture:** ❌ Modification impossible. Faire une écriture de contre-passation.

---

### Q2: Comment corriger une erreur après clôture?

**R:** Écriture de contre-passation + écriture correcte.

**Exemple:**

**Écriture erronée (15/01/2025) - Déjà clôturée:**
```
Débit  605  Achats               100 000 FCFA
Crédit 521  Banque                        100 000 FCFA
(Erreur: oublié la TVA!)
```

**Correction (05/02/2025):**

**Étape 1 - Contre-passation:**
```
Journal: OD
Référence: CORREC-2025-001

Débit  521  Banque               100 000 FCFA
Crédit 605  Achats                        100 000 FCFA

Libellé: Contre-passation FACH-2025-001 (erreur TVA oubliée)
```

**Étape 2 - Écriture correcte:**
```
Journal: OD
Référence: CORREC-2025-002

Débit  605  Achats               100 000 FCFA
Débit  4451 TVA récupérable       19 250 FCFA
Crédit 521  Banque                        119 250 FCFA

Libellé: Correction FACH-2025-001 (avec TVA)
```

---

### Q3: La TVA est-elle toujours calculée automatiquement?

**R:** Oui, dès que vous utilisez un compte 4451 (TVA récupérable).

**Conditions:**
- ✅ Compte 4451x utilisé dans l'écriture
- ✅ Description renseignée (pour détecter la catégorie)
- ✅ Montant au débit (pas au crédit)

**Si la description est vague:**
```
Description: "Achat"
→ Catégorie par défaut: VU (80%)
⚠️ Recommandation: Préciser "Achat carburant" ou "Achat matériel"
```

---

### Q4: Comment gérer plusieurs entreprises?

**R:** Le système supporte le multi-tenant.

**Mode 1: Cabinet d'expertise comptable**
```
Cabinet EXPERTISE COMPTA
├── Client A
├── Client B
├── Client C
└── Client D

→ Une seule connexion
→ Bascule facile entre clients
→ Vue d'ensemble du portefeuille
```

**Mode 2: Entreprises indépendantes**
```
Entreprise A → Base de données dédiée
Entreprise B → Base de données dédiée
Entreprise C → Base de données dédiée

→ Isolation totale
→ Sécurité maximale
```

---

### Q5: Puis-je importer mes écritures depuis Excel?

**R:** Oui, via l'API ou l'import CSV.

**Format CSV attendu:**
```csv
date;journal;reference;compte;libelle;debit;credit
15/01/2025;AC;FACH-2025-001;605;Achats marchandises;100000;0
15/01/2025;AC;FACH-2025-001;4451;TVA récupérable;19250;0
15/01/2025;AC;FACH-2025-001;401;Fournisseur ABC;0;119250
```

**Procédure:**
```
1. Menu: Import > Écritures comptables
2. Sélectionner le fichier CSV
3. Mapper les colonnes (si nécessaire)
4. Valider l'import
5. Vérifier les écritures importées
```

---

### Q6: Le système gère-t-il plusieurs exercices comptables?

**R:** Oui, sans limite.

**Exemple:**
```
Exercice 2023 → Clôturé et verrouillé
Exercice 2024 → Clôturé et verrouillé
Exercice 2025 → En cours
Exercice 2026 → Non démarré
```

**Navigation:**
```
Menu: Paramètres > Exercice comptable

[ 2023 ] [ 2024 ] [→ 2025 ←] [ 2026 ]

→ Bascule facile entre exercices
→ Rapports comparatifs possibles
```

---

### Q7: Peut-on avoir plusieurs utilisateurs?

**R:** Oui, avec gestion des droits.

**Rôles disponibles:**

| Rôle | Droits |
|------|--------|
| **Administrateur** | Tous les droits (création, modification, suppression, clôture) |
| **Comptable** | Création et modification écritures, consultation rapports |
| **Saisisseur** | Création écritures uniquement (pas de modification) |
| **Consultant** | Consultation uniquement (lecture seule) |

**Exemple:**
```
Cabinet EXPERTISE COMPTA
├── Marie Dupont (Administrateur)
│   → Peut tout faire
│
├── Jean Martin (Comptable)
│   → Peut saisir et modifier
│
└── Pierre Durand (Saisisseur)
    → Peut saisir uniquement
```

---

### Q8: Comment faire un rapprochement bancaire?

**R:** Module dédié dans le système.

**Procédure:**
```
1. Menu: Trésorerie > Rapprochement bancaire

2. Importer le relevé bancaire (PDF ou CSV)

3. Le système compare:
   - Écritures comptables (journal BQ)
   - Lignes du relevé bancaire

4. Rapprocher les lignes:
   [✅] 15/01 - Virement 119 250 FCFA → FACH-2025-001
   [✅] 16/01 - Chèque 238 500 FCFA → FVEN-2025-042
   [❓] 20/01 - Prélèvement 5 000 FCFA → Non comptabilisé!

5. Créer les écritures manquantes

6. Valider le rapprochement
```

---

## 🎓 Conclusion

### Le système PREDYKT est-il bien adapté aux journaux?

**✅ OUI, complètement!**

**Résumé des forces:**

1. ✅ **5 journaux OHADA** (AC, VE, BQ, CA, OD)
2. ✅ **Numérotation séquentielle** automatique
3. ✅ **Plan comptable OHADA** pré-chargé
4. ✅ **Partie double** vérifiée automatiquement
5. ✅ **TVA automatique** (26 règles CGI Cameroun)
6. ✅ **Prorata de TVA** géré automatiquement
7. ✅ **Traçabilité totale** (audit trail)
8. ✅ **Rapports complets** (Grand livre, Balance, Bilan, etc.)
9. ✅ **Sécurité maximale** (verrouillage, multi-tenant)
10. ✅ **Conforme OHADA** et législation camerounaise

### Pour qui?

- ✅ Entreprises (PME, ETI, Grandes entreprises)
- ✅ Cabinets d'expertise comptable
- ✅ Comptables indépendants
- ✅ Associations et ONG

### Prochaines étapes

**Pour démarrer avec PREDYKT:**

1. **Formation initiale** (1/2 journée)
   - Présentation du système
   - Configuration du plan comptable
   - Saisie des premières écritures

2. **Paramétrage** (1 jour)
   - Import du plan comptable personnalisé
   - Configuration du prorata (si nécessaire)
   - Création des utilisateurs

3. **Migration** (optionnel)
   - Import des soldes d'ouverture
   - Import de l'historique
   - Vérification de la balance

4. **Production** (dès le lendemain!)
   - Saisie quotidienne
   - Rapports en temps réel
   - TVA automatique

---

## 📞 Support

**Besoin d'aide?**

- 📧 Email: support@predykt.com
- 📞 Téléphone: +237 xxx xxx xxx
- 💬 Chat en ligne: predykt.com/chat
- 📚 Documentation complète: predykt.com/docs

**Formations disponibles:**
- Formation comptable (½ journée)
- Formation administrateur (1 journée)
- Formation expert-comptable (2 jours)

---

**Ce guide a été rédigé pour vous, comptables!**

Si vous avez des questions ou suggestions pour améliorer ce guide, n'hésitez pas à nous contacter.

**Bonne comptabilité avec PREDYKT!** 📚✨

---

*Version: 1.0 | Date: Janvier 2025 | PREDYKT Accounting System*
