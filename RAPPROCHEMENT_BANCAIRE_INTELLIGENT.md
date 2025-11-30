# Guide du Rapprochement Bancaire Intelligent
## Pour les comptables et responsables financiers

---

## Table des matières
1. [Introduction](#introduction)
2. [Comment ça fonctionne](#comment-ça-fonctionne)
3. [Types de correspondances détectées](#types-de-correspondances-détectées)
4. [Niveaux de confiance](#niveaux-de-confiance)
5. [Workflow d'utilisation](#workflow-dutilisation)
6. [Cas d'usage pratiques](#cas-dusage-pratiques)
7. [Limitations et quand intervenir manuellement](#limitations-et-quand-intervenir-manuellement)
8. [FAQ](#faq)

---

## Introduction

Le système de **rapprochement bancaire intelligent** est un assistant automatique qui analyse vos transactions bancaires et vos écritures comptables pour identifier automatiquement les correspondances et vous suggérer des rapprochements.

### Pourquoi c'est utile ?

Au lieu de chercher manuellement chaque transaction bancaire dans votre grand livre, le système :
- Analyse automatiquement des centaines de transactions en quelques secondes
- Identifie les correspondances exactes et probables
- Détecte les chèques non encaissés, virements en transit, frais bancaires non comptabilisés
- Vous propose des suggestions avec un niveau de confiance
- Vous fait gagner des heures de travail manuel

### Ce que le système NE fait PAS

Le système est un **assistant intelligent**, pas un remplacement du comptable :
- Il **suggère** des correspondances, mais c'est **vous qui décidez** d'accepter ou rejeter
- Il ne crée pas automatiquement d'écritures comptables sans votre validation
- Il ne modifie pas vos données existantes sans autorisation

---

## Comment ça fonctionne

### Vue d'ensemble

Le système utilise un **algorithme de scoring multicritères** pour analyser chaque paire de transactions (bancaire vs comptable) et calculer un score de correspondance de 0 à 100.

### Les critères analysés

#### 1. Comparaison des montants (50 points maximum)
- **Montant exactement identique** : +50 points
- **Montant proche (différence ≤ 5%)** : +30 points
- **Montants trop différents** : 0 point (pas de correspondance possible)

**Pourquoi 5% de tolérance ?**
Pour gérer les cas de :
- Arrondis (ex: 1000.45 vs 1000.50)
- Frais de virement non comptabilisés (ex: virement de 1000 mais 995 reçu après frais)

#### 2. Comparaison des dates (50 points maximum)
- **Date identique** : +50 points
- **Écart de 1 à 3 jours** : +40 points
- **Écart de 4 à 7 jours** : +25 points
- **Écart de 8 à 15 jours** : +10 points
- **Écart > 15 jours** : 0 point

**Pourquoi accepter des écarts de dates ?**
- Les virements prennent 1-3 jours ouvrés
- Les chèques peuvent mettre plusieurs jours à être encaissés
- Vous avez pu enregistrer une opération avant qu'elle n'apparaisse sur le relevé

#### 3. Comparaison des références (10 points bonus)
Si le numéro de référence bancaire correspond exactement à la référence comptable : +10 points

#### 4. Comparaison des descriptions (5 points bonus)
Si les descriptions sont similaires (analyse de mots-clés) : +5 points

### Les phases d'analyse

Le système travaille en **4 phases séquentielles** :

#### Phase 1 : Correspondances EXACTES (Score = 100)
Recherche les paires avec montant identique ET date identique.
- **Confiance** : 100% - Auto-approuvable
- **Action** : Vous pouvez appliquer sans vérification

#### Phase 2 : Correspondances PROBABLES (Score ≥ 90)
Recherche les paires avec montant exact mais date proche (1-3 jours d'écart).
- **Confiance** : 90-99%
- **Action** : Vérification rapide recommandée

#### Phase 2.5 : Correspondances MULTIPLES
Recherche les cas spéciaux :
- **N-à-1** : Plusieurs transactions bancaires = 1 écriture comptable
  - Ex: 3 virements de 500€, 300€, 200€ = 1 facture de 1000€
- **1-à-N** : 1 transaction bancaire = plusieurs écritures comptables
  - Ex: 1 virement de 1500€ = paiement de 2 factures (1000€ + 500€)

**Confiance** : 75% (révision manuelle obligatoire)

#### Phase 3 : Transactions bancaires sans correspondance
Le système analyse les transactions bancaires qui n'ont pas de correspondance en comptabilité et **devine** leur nature :

| Mots-clés détectés | Type suggéré | Confiance |
|-------------------|--------------|-----------|
| "virement", "vir" | Virement reçu non comptabilisé | 85% |
| "intérêt", "interet" | Intérêts bancaires non enregistrés | 90% |
| "frais", "commission" | Frais bancaires non enregistrés | 90% |
| "agios", "interet debiteur" | Agios non enregistrés | 90% |
| "prelevement", "prel" | Prélèvement automatique non comptabilisé | 85% |
| Autres crédits | Crédit bancaire non identifié | 70% |
| Autres débits | Débit bancaire non identifié | 70% |

#### Phase 4 : Écritures comptables sans correspondance
Le système analyse les écritures comptables sans contrepartie bancaire :

| Indices détectés | Type suggéré | Confiance |
|-----------------|--------------|-----------|
| Référence contient "CHQ", "CHEQUE" | Chèque émis non encaissé | 90% |
| Description contient "virement" | Virement en transit | 80% |
| Crédit sans référence | Paiement en cours | 65% |
| Débit sans référence | Encaissement en cours | 70% |

---

## Types de correspondances détectées

Le système identifie 10 types d'opérations en suspens conformes OHADA :

### Opérations affectant le solde BANQUE

| Type | Description | Exemple | Impact |
|------|-------------|---------|--------|
| **Chèques émis non encaissés** | Vous avez émis un chèque mais le bénéficiaire ne l'a pas encore encaissé | Chèque n°123 de 5000 F CFA au fournisseur (en comptabilité) mais pas encore débité en banque | À ajouter au solde bancaire |
| **Dépôts/virements en cours** | Vous avez enregistré un virement mais il n'apparaît pas encore sur le relevé | Virement envoyé le 28/11 (en comptabilité) mais traité par la banque le 02/12 | À soustraire du solde bancaire |
| **Erreur bancaire** | La banque a fait une erreur (débit en double, mauvais montant) | Débit de 10000 F au lieu de 1000 F | À corriger avec la banque |

### Opérations affectant le solde COMPTABLE

| Type | Description | Exemple | Impact |
|------|-------------|---------|--------|
| **Virements reçus non comptabilisés** | Argent reçu en banque mais pas encore enregistré en comptabilité | Virement client de 50000 F visible sur le relevé mais pas dans le journal | À enregistrer en comptabilité |
| **Prélèvements non comptabilisés** | Prélèvement bancaire pas encore enregistré | Loyer prélevé automatiquement (30000 F) non enregistré | À enregistrer en comptabilité |
| **Frais bancaires non enregistrés** | Frais de tenue de compte, commissions | Commission de 2500 F sur le relevé | À enregistrer en comptabilité |
| **Intérêts non enregistrés** | Intérêts créditeurs sur le compte | Intérêts de 1200 F crédités par la banque | À enregistrer en comptabilité |
| **Agios non enregistrés** | Intérêts débiteurs (découvert) | Agios de 8000 F sur découvert | À enregistrer en comptabilité |
| **Prélèvements automatiques non comptabilisés** | Prélèvement mensuel récurrent | Abonnement téléphone (15000 F) prélevé | À enregistrer en comptabilité |

---

## Niveaux de confiance

Le système attribue un **score de confiance** à chaque suggestion :

### Excellent (95-100%)
- **Signification** : Correspondance quasi-certaine
- **Caractéristiques** : Montant exact + date identique (ou écart de 1 jour max) + référence identique
- **Action recommandée** : Appliquer directement
- **Exemple** : Virement de 125000 F le 15/11 (banque) = Virement de 125000 F le 15/11 (compta)

### Bon (80-94%)
- **Signification** : Correspondance très probable
- **Caractéristiques** : Montant exact + date proche (2-3 jours)
- **Action recommandée** : Vérifier rapidement puis appliquer
- **Exemple** : Chèque de 45000 F le 20/11 (banque) = Chèque de 45000 F le 18/11 (compta)

### Acceptable (60-79%)
- **Signification** : Correspondance possible
- **Caractéristiques** : Montant proche (±5%) ou date éloignée (4-7 jours)
- **Action recommandée** : Vérifier attentivement avant application
- **Exemple** : Virement de 99500 F le 10/11 (banque) = Facture de 100000 F le 05/11 (compta)

### Faible (< 60%)
- **Signification** : Correspondance incertaine
- **Caractéristiques** : Basé sur heuristiques (mots-clés, types détectés)
- **Action recommandée** : Analyser manuellement
- **Exemple** : Débit de 5000 F avec description "FRAIS" détecté comme frais bancaires

---

## Workflow d'utilisation

### Étape 1 : Créer le rapprochement bancaire

```
1. Allez dans "Rapprochements bancaires"
2. Créez un nouveau rapprochement
3. Sélectionnez :
   - Le compte bancaire (ex: 521 - Banque locale)
   - La période (ex: Novembre 2024)
   - Le solde du relevé bancaire au 30/11
```

### Étape 2 : Lancer l'analyse automatique

```
4. Cliquez sur "Lancer le matching automatique"
5. Le système analyse en quelques secondes
6. Vous recevez un rapport avec :
   - X correspondances exactes (100%)
   - Y correspondances probables (90%+)
   - Z suggestions basées sur heuristiques
   - Liste des transactions non réconciliées
```

### Étape 3 : Examiner les suggestions

Le système vous présente chaque suggestion avec :

```
┌─────────────────────────────────────────────────────────────┐
│ SUGGESTION #1                            Confiance: 100%    │
├─────────────────────────────────────────────────────────────┤
│ Type: Correspondance exacte 1-à-1                           │
│                                                              │
│ TRANSACTION BANCAIRE:                                       │
│   Date: 15/11/2024                                          │
│   Montant: 125 000 F CFA                                    │
│   Description: VIR CLIENT ABC SARL                          │
│   Référence: VIR20241115001                                 │
│                                                              │
│ ÉCRITURE COMPTABLE:                                         │
│   Date: 15/11/2024                                          │
│   Montant: 125 000 F CFA                                    │
│   Compte: 521 - Banque locale                               │
│   Description: Règlement facture FAC-2024-0158              │
│   Référence: VIR20241115001                                 │
│                                                              │
│ RAISON DU MATCHING:                                         │
│   ✓ Montant exact: 125000                                   │
│   ✓ Date identique                                          │
│   ✓ Référence identique                                     │
│                                                              │
│ [✓ APPLIQUER]  [✗ REJETER]                                 │
└─────────────────────────────────────────────────────────────┘
```

### Étape 4 : Prendre vos décisions

Pour chaque suggestion, 3 choix :

#### A. APPLIQUER la suggestion
- Le système crée automatiquement une "opération en suspens" dans le rapprochement
- La transaction bancaire est marquée "réconciliée"
- L'écriture comptable est liée au rapprochement
- Le solde du rapprochement est recalculé

#### B. REJETER la suggestion
- La suggestion est marquée "rejetée" (avec votre raison optionnelle)
- Aucune modification n'est faite
- Vous devrez traiter manuellement cette transaction

#### C. IGNORER (ne rien faire)
- La suggestion reste "en attente"
- Vous pourrez y revenir plus tard

### Étape 5 : Traiter les transactions non réconciliées

Le système liste les transactions pour lesquelles **aucune correspondance** n'a été trouvée :

```
TRANSACTIONS BANCAIRES NON RÉCONCILIÉES:
- 28/11/2024 | -2 500 F | FRAIS TENUE COMPTE
  → Raison: Aucune écriture comptable correspondante
  → Action suggérée: Enregistrer les frais bancaires

- 30/11/2024 | +1 200 F | INTERETS CREDITEURS
  → Raison: Aucune écriture comptable correspondante
  → Action suggérée: Enregistrer les intérêts

ÉCRITURES COMPTABLES NON RÉCONCILIÉES:
- 25/11/2024 | Chèque n°456 | -15 000 F | Fournisseur XYZ
  → Raison: Pas encore débité en banque
  → Action suggérée: Chèque émis non encaissé (opération en suspens)
```

Vous devez :
1. Vérifier chaque transaction non réconciliée
2. Soit créer manuellement une opération en suspens
3. Soit enregistrer l'écriture manquante en comptabilité

### Étape 6 : Finaliser le rapprochement

```
7. Vérifiez que le solde réconcilié = solde du relevé
8. Si équilibré : "Soumettre pour révision"
9. Le responsable approuve ou rejette
10. Une fois approuvé : le rapprochement est figé
```

---

## Cas d'usage pratiques

### Cas 1 : Virement client avec 2 jours de décalage

**Situation :**
- Vous avez enregistré le virement client le 15/11 (date de facturation)
- La banque l'a crédité le 17/11 (date de valeur)

**Ce que fait le système :**
```
✓ Correspondance PROBABLE détectée (Score: 90%)
  - Montant exact: 250 000 F CFA
  - Écart de dates: 2 jours
  - Type: Correspondance 1-à-1

  Transaction bancaire: 17/11/2024 | +250 000 F | VIR CLIENT DEF
  Écriture comptable:   15/11/2024 | Débit 521 - 250 000 F

  RECOMMANDATION: Vérifier puis appliquer
```

**Votre action :**
1. Vérifier que c'est bien le même client
2. Appliquer la suggestion
3. Le rapprochement est fait automatiquement

---

### Cas 2 : Paiement groupé (3 factures en 1 virement)

**Situation :**
- Vous avez enregistré 3 factures fournisseur : 100 000 F + 75 000 F + 50 000 F
- Le fournisseur reçoit 1 seul virement de 225 000 F

**Ce que fait le système :**
```
✓ Correspondance MULTIPLE détectée (Score: 75%)
  Type: Matching 1-à-N

  1 transaction bancaire:
    - 20/11/2024 | -225 000 F | VIR FOURNISSEUR ABC

  Correspond à 3 écritures comptables:
    - 18/11/2024 | Crédit 401 - 100 000 F | Facture FAC-001
    - 19/11/2024 | Crédit 401 - 75 000 F  | Facture FAC-002
    - 20/11/2024 | Crédit 401 - 50 000 F  | Facture FAC-003

  Total GL: 225 000 F = Total BT: 225 000 F

  RECOMMANDATION: Vérifier les 3 factures puis appliquer
```

**Votre action :**
1. Vérifier que les 3 factures concernent bien ce fournisseur
2. Appliquer la suggestion
3. Le système lie automatiquement le virement aux 3 écritures

---

### Cas 3 : Frais bancaires non comptabilisés

**Situation :**
- La banque a prélevé 3 500 F de frais de tenue de compte
- Vous ne l'avez pas encore enregistré en comptabilité

**Ce que fait le système :**
```
✓ Suggestion HEURISTIQUE (Score: 90%)
  Type: Frais bancaires non enregistrés

  Transaction bancaire:
    - 30/11/2024 | -3 500 F | FRAIS TENUE COMPTE NOVEMBRE

  Aucune écriture comptable correspondante trouvée

  Raison de détection: Mots-clés "FRAIS" + "COMPTE" détectés

  RECOMMANDATION: Enregistrer en comptabilité puis rapprocher
```

**Votre action :**
Option 1 - Appliquer la suggestion :
- Le système crée une opération en suspens "Frais bancaires non enregistrés"
- Vous enregistrerez l'écriture comptable ultérieurement

Option 2 - Enregistrer maintenant :
- Passer l'écriture : Débit 627 (Services bancaires) 3 500 F / Crédit 521 (Banque) 3 500 F
- Relancer le matching automatique
- Le système trouvera la correspondance exacte

---

### Cas 4 : Chèque émis non encaissé

**Situation :**
- Vous avez émis le chèque n°789 de 80 000 F au fournisseur le 10/11
- Au 30/11, le chèque n'est toujours pas encaissé

**Ce que fait le système :**
```
✓ Suggestion HEURISTIQUE (Score: 90%)
  Type: Chèque émis non encaissé

  Écriture comptable:
    - 10/11/2024 | Crédit 521 - 80 000 F | CHQ789 Fournisseur GHI

  Aucune transaction bancaire correspondante trouvée

  Raison de détection: Référence contient "CHQ" + pas de débit bancaire

  RECOMMANDATION: Ajouter en opération en suspens (ajuste solde banque)
```

**Votre action :**
1. Vérifier que le chèque n'a effectivement pas été encaissé (sur le relevé)
2. Appliquer la suggestion
3. Le système ajoute +80 000 F au solde bancaire (car en banque on a encore l'argent)
4. Le rapprochement s'équilibre

---

### Cas 5 : Rapprochement complexe avec 50 transactions

**Situation :**
- Novembre 2024 : 50 transactions bancaires, 48 écritures comptables
- Mix de virements, chèques, prélèvements, frais

**Ce que fait le système :**
```
RAPPORT D'ANALYSE AUTOMATIQUE
═══════════════════════════════════════════════════════

TRANSACTIONS ANALYSÉES:
  • 50 transactions bancaires
  • 48 écritures comptables

RÉSULTATS:
  ✓ 35 correspondances EXACTES (100% confiance)
  ~ 8 correspondances PROBABLES (90%+ confiance)
  ? 3 suggestions MULTIPLES (75% confiance)
  ✗ 4 transactions bancaires sans correspondance
  ✗ 2 écritures comptables sans correspondance

STATISTIQUES:
  • Confiance moyenne: 94.2%
  • 35 suggestions auto-approuvables
  • 11 suggestions nécessitent révision manuelle
  • Temps d'analyse: 2.3 secondes

ACTIONS RECOMMANDÉES:
  1. Appliquer les 35 correspondances exactes (gain: 35 rapprochements)
  2. Vérifier les 8 correspondances probables (5 min)
  3. Analyser les 3 matchings multiples (10 min)
  4. Traiter manuellement les 6 transactions non réconciliées (15 min)

  TEMPS ESTIMÉ TOTAL: 30 minutes
  (vs 3-4 heures sans assistant automatique)
```

**Votre action :**
1. Appliquer en masse les 35 suggestions à 100% (1 clic)
2. Examiner une par une les 11 autres suggestions
3. Traiter manuellement les 6 exceptions
4. Finaliser le rapprochement

**Gain de temps : 70-80%**

---

## Limitations et quand intervenir manuellement

### Limitations du système

#### 1. Performance sur gros volumes
**Limitation :** L'algorithme de matching multiple génère toutes les combinaisons possibles.
- Si 100 transactions non matchées : peut générer des millions de combinaisons
- Le calcul peut prendre plusieurs minutes (voire timeout)

**Solution actuelle :** Limité à 5 transactions maximum par matching multiple

**Quand intervenir :**
- Si l'analyse prend plus de 30 secondes
- Si vous avez >200 transactions dans la période
- **Action** : Diviser en plusieurs rapprochements (ex: par semaine)

#### 2. Similarité textuelle basique
**Limitation :** Compare seulement les mots entiers identiques (algorithme Jaccard).
- Ne détecte pas les fautes de frappe (ex: "FOURNISSEUR" vs "FOURNISEUR")
- Ne comprend pas les abréviations (ex: "VIR" vs "VIREMENT")

**Quand intervenir :**
- Descriptions très différentes entre banque et compta
- **Action** : Vérifier manuellement les montants identiques avec dates proches

#### 3. Tolérance fixe de 5%
**Limitation :** Ne s'adapte pas au contexte.
- 5% de 1 000 000 F = 50 000 F d'écart accepté (trop généreux)
- 5% de 5 000 F = 250 F d'écart (peut être trop strict)

**Quand intervenir :**
- Gros montants avec petits écarts (ex: 1 000 000 F vs 1 001 000 F)
- **Action** : Rejeter la suggestion si l'écart ne correspond pas à des frais connus

#### 4. Pas d'apprentissage automatique
**Limitation :** L'algorithme ne s'améliore pas avec vos validations.
- Si vous rejetez toujours les suggestions à 75%, le système ne le sait pas
- Les seuils sont fixes (codés en dur ou configurés manuellement)

**Quand intervenir :**
- Si vous remarquez que le système fait souvent la même erreur
- **Action** : Contacter l'administrateur pour ajuster la configuration

#### 5. Pas de vérification de cohérence comptable
**Limitation :** Compare seulement les montants absolus.
- Ne vérifie pas le sens débit/crédit
- Ne vérifie pas les comptes de contrepartie
- Ne valide pas les écritures équilibrées

**Quand intervenir :**
- Avant d'appliquer toute suggestion
- **Action** : Vérifier que le sens de l'opération est logique

### Cas nécessitant intervention manuelle obligatoire

#### Cas 1 : Transactions en devises étrangères
Le système ne gère pas les conversions de devises.

**Exemple :**
- Transaction bancaire : 1000 EUR = 655 957 F CFA (taux du jour)
- Écriture comptable : 1000 EUR = 650 000 F CFA (taux comptable)
- Écart de 5 957 F dû à la différence de taux

**Action manuelle :**
1. Identifier les 2 écritures (banque et compta)
2. Passer l'écriture de gain/perte de change
3. Rapprocher manuellement

#### Cas 2 : Erreurs de saisie comptable
Le système ne corrige pas les erreurs.

**Exemple :**
- Transaction bancaire : 125 000 F
- Écriture comptable : 152 000 F (inversion de chiffres)
- Le système ne trouvera AUCUNE correspondance

**Action manuelle :**
1. Identifier l'erreur
2. Corriger l'écriture comptable (avec justificatif)
3. Relancer le matching

#### Cas 3 : Opérations complexes multi-comptes
Le système analyse uniquement le compte 521.

**Exemple :**
- Virement interne : 521 Banque A → 522 Banque B (100 000 F)
- En banque A : -100 000 F
- En banque B : +100 000 F
- En compta : 2 écritures (débit 522, crédit 521)

**Action manuelle :**
- Faire 2 rapprochements distincts (un par compte bancaire)
- Ou créer manuellement l'opération en suspens "Virement interne"

#### Cas 4 : Régularisations de fin d'exercice
Le système ne comprend pas les écritures de régularisation.

**Exemple :**
- OD de régularisation de caisse (écart de 500 F)
- Aucune transaction bancaire correspondante (normal)

**Action manuelle :**
- Ne pas chercher de correspondance
- Exclure du rapprochement ou documenter

---

## FAQ

### Questions générales

#### Q: Le système peut-il faire des erreurs ?
**R:** Oui, c'est pour cela qu'il propose des **suggestions** et non des rapprochements automatiques. Vous restez maître de la validation. Les suggestions à faible confiance (<80%) nécessitent toujours une vérification attentive.

#### Q: Que se passe-t-il si j'applique une mauvaise suggestion ?
**R:** Tant que le rapprochement n'est pas approuvé, vous pouvez supprimer l'opération en suspens créée. Une fois le rapprochement approuvé, il faut le rejeter et le recommencer (ou faire une note de correction).

#### Q: Puis-je modifier une suggestion avant de l'appliquer ?
**R:** Non directement. Vous devez soit :
- Appliquer la suggestion, puis modifier l'opération en suspens créée
- Ou rejeter la suggestion et créer manuellement l'opération avec les bons paramètres

#### Q: Le système crée-t-il des écritures comptables automatiquement ?
**R:** Non. Il crée uniquement des **opérations en suspens** dans le rapprochement. C'est à vous de passer les écritures comptables manquantes (ex: frais bancaires) après avoir identifié ce qui manque.

### Questions sur les performances

#### Q: Combien de temps prend l'analyse automatique ?
**R:**
- Moins de 50 transactions : **< 5 secondes**
- 50-200 transactions : **5-30 secondes**
- Plus de 200 transactions : **30 secondes à plusieurs minutes**

#### Q: Que faire si l'analyse est trop lente ?
**R:** Diviser votre rapprochement en périodes plus courtes :
- Au lieu de "Novembre 2024" (30 jours)
- Faire "Novembre semaine 1", "Novembre semaine 2", etc.

### Questions sur la confiance

#### Q: Pourquoi certaines suggestions ont une confiance de seulement 70% ?
**R:** Ce sont des suggestions basées sur des **heuristiques** (règles de devinettes) :
- Détection de mots-clés dans les descriptions
- Analyse de patterns (chèques, virements, etc.)
- **Important** : Ces suggestions nécessitent toujours votre validation

#### Q: Puis-je faire confiance aux suggestions à 100% ?
**R:** Les suggestions à 100% de confiance sont des correspondances **exactes** (montant + date identiques). Elles sont très fiables, mais il est toujours prudent de vérifier rapidement, surtout pour les gros montants.

### Questions sur les cas spéciaux

#### Q: Comment gérer les virements fractionnés ?
**R:** Le système détecte automatiquement les matchings multiples (1-à-N ou N-à-1) avec une confiance de 75%. Vérifiez toujours que la somme est correcte et que les transactions concernent bien le même tiers.

#### Q: Le système gère-t-il les chèques de banque ?
**R:** Oui, comme des chèques normaux. Si la référence contient "CHQ" ou "CHEQUE", il détectera automatiquement le type.

#### Q: Et les prélèvements SEPA récurrents ?
**R:** Le système les détecte si la description contient "PRELEVEMENT" ou "PREL". Pour améliorer la détection, utilisez toujours la même description en comptabilité pour les prélèvements récurrents.

### Questions sur la configuration

#### Q: Puis-je ajuster les seuils de l'algorithme ?
**R:** Oui, si vous êtes administrateur système. Les seuils sont configurables dans le fichier `application.yaml` :
- Tolérance de montant (défaut: 5%)
- Seuils de dates (défaut: 0, 3, 7, 15 jours)
- Scores de confiance
- Activation/désactivation du matching multiple

#### Q: Puis-je désactiver le matching multiple ?
**R:** Oui, via la configuration `predykt.reconciliation.matching.multipleMatching.enabled=false`. Utile si vous préférez gérer manuellement les cas groupés.

---

## Conseils et bonnes pratiques

### Pour maximiser l'efficacité du système

#### 1. Harmonisez vos descriptions comptables
**Mauvais :**
- Écriture 1 : "Virement client ABC"
- Écriture 2 : "VIR client ABC SARL"
- Écriture 3 : "Règlt client ABC"

**Bon :**
- Toutes les écritures : "VIR CLIENT ABC SARL"

→ Le système détectera mieux les correspondances

#### 2. Utilisez toujours les références
- Numéro de chèque dans la référence : "CHQ789"
- Numéro de facture : "FAC-2024-0123"
- Numéro de virement : "VIR20241115001"

→ Le système gagne 10 points de confiance si les références correspondent

#### 3. Enregistrez les opérations rapidement
Plus vous attendez, plus l'écart de dates sera grand :
- Enregistrement le jour même : **100% de confiance** possible
- Enregistrement 1 semaine après : **90% de confiance** maximum

#### 4. Faites des rapprochements réguliers
- **Hebdomadaire** : 20-30 transactions → analyse en 5 secondes
- **Mensuel** : 100-200 transactions → analyse en 30 secondes
- **Trimestriel** : 300-600 transactions → analyse lente + risque d'erreurs

#### 5. Appliquez en masse les suggestions à 100%
Gagnez du temps :
1. Triez par confiance décroissante
2. Appliquez toutes les suggestions ≥ 95% d'un coup (si disponible)
3. Concentrez votre temps sur les cas douteux (<80%)

---

## Support et amélioration continue

### Traçabilité des décisions

Le système enregistre :
- Toutes les suggestions générées (même rejetées)
- Vos décisions (appliquée, rejetée, raison du rejet)
- Les scores de confiance

**Utilité :**
- Audit trail complet
- Amélioration future de l'algorithme
- Formation de nouveaux comptables

### Feedback pour améliorer le système

Si vous constatez que :
- Le système fait souvent la même erreur
- Un type de transaction est mal détecté
- Les seuils ne sont pas adaptés à votre activité

**Action :** Contactez l'administrateur avec :
- Des exemples concrets
- Vos suggestions d'amélioration
- Les statistiques de taux de rejet par type

---

## Annexes

### Annexe A : Glossaire

| Terme | Définition |
|-------|------------|
| **Matching** | Processus d'appariement automatique entre transactions bancaires et écritures comptables |
| **Score de confiance** | Pourcentage de 0 à 100 indiquant la certitude de la correspondance |
| **Suggestion** | Proposition de rapprochement générée par l'algorithme |
| **Opération en suspens** | Élément du rapprochement bancaire expliquant un écart temporaire |
| **Heuristique** | Règle de "devinette" basée sur des mots-clés ou patterns |
| **Matching multiple** | Correspondance entre plusieurs transactions (N-à-1 ou 1-à-N) |
| **Auto-approuvable** | Suggestion avec confiance ≥ 95% qu'on peut valider sans vérification poussée |

### Annexe B : Formule de calcul du score

```
Score total = Score montant + Score date + Bonus référence + Bonus description

Score montant (50 points max):
  - Si montant1 == montant2 : +50
  - Sinon si |montant1 - montant2| ≤ 5% de montant1 : +30
  - Sinon : 0 (pas de correspondance)

Score date (50 points max):
  - Si date1 == date2 : +50
  - Sinon si écart ≤ 3 jours : +40
  - Sinon si écart ≤ 7 jours : +25
  - Sinon si écart ≤ 15 jours : +10
  - Sinon : 0

Bonus référence (10 points max):
  - Si référence1 == référence2 (ignorer casse) : +10
  - Sinon : 0

Bonus description (5 points max):
  - Si similarité_jaccard(desc1, desc2) > 70% : +5
  - Sinon : 0

Score maximum théorique : 115 points
Score maximum affiché : 100 points (plafonné)
```

### Annexe C : Codes de types d'opérations

| Code | Type | Côté | Impact |
|------|------|------|--------|
| `CHEQUE_ISSUED_NOT_CASHED` | Chèque émis non encaissé | BANK | +Montant (solde banque) |
| `DEPOSIT_IN_TRANSIT` | Dépôt en cours | BANK | -Montant (solde banque) |
| `BANK_ERROR` | Erreur bancaire | BANK | ±Montant (selon nature) |
| `CREDIT_NOT_RECORDED` | Virement reçu non comptabilisé | BOOK | +Montant (solde livre) |
| `DEBIT_NOT_RECORDED` | Prélèvement non comptabilisé | BOOK | -Montant (solde livre) |
| `BANK_FEES_NOT_RECORDED` | Frais bancaires non enregistrés | BOOK | -Montant (solde livre) |
| `INTEREST_NOT_RECORDED` | Intérêts non enregistrés | BOOK | +Montant (solde livre) |
| `DIRECT_DEBIT_NOT_RECORDED` | Prélèvement auto non comptabilisé | BOOK | -Montant (solde livre) |
| `BANK_CHARGES_NOT_RECORDED` | Agios non enregistrés | BOOK | -Montant (solde livre) |
| `UNCATEGORIZED` | Non catégorisé | OTHER | À analyser |

---

**Version du document :** 1.0
**Date de dernière mise à jour :** 30 Novembre 2024
**Système :** PREDYKT Core Accounting API - Rapprochement Bancaire Intelligent
**Conforme :** Normes OHADA

---

Pour toute question ou assistance, contactez votre administrateur système ou consultez la documentation technique dans `CLAUDE.md`.
