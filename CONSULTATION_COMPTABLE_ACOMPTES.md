# 📋 Consultation Comptable - Gestion des Acomptes

**Document destiné à** : Expert-comptable / Comptable agréé
**Objet** : Choix de la méthode de gestion des acomptes clients
**Date** : 11 Décembre 2025
**Système** : PREDYKT Accounting API
**Conformité** : OHADA SYSCOHADA & CGI Cameroun

---

## 🎯 Contexte

Nous avons implémenté un système complet de gestion des acomptes (avances clients) dans notre application comptable. Avant de finaliser le déploiement en production, nous souhaitons obtenir votre validation sur l'approche comptable retenue.

**Deux approches sont possibles** :
1. **Reçus d'acompte** (compte 4191) - ✅ Actuellement implémenté
2. **Factures d'acompte** (compte 411) - Alternative possible

Nous avons besoin de votre expertise pour confirmer que l'approche choisie est la plus adaptée à notre contexte d'entreprise et conforme aux normes OHADA et aux pratiques au Cameroun.

---

## ✅ Approche #1 : Reçus d'Acompte (Implémentation Actuelle)

### Principe

Lorsqu'un client verse un acompte **avant facturation**, nous émettons un **reçu d'acompte** (simple document de réception) avec le numéro **RA-YYYY-NNNNNN**.

### Compte OHADA Utilisé

- **4191** - Clients - Avances et acomptes reçus

### Flux Comptable Complet

#### Étape 1 : Réception de l'Acompte (15/01/2025)

**Client** : SARL BELTEC
**Montant** : 100 000 XAF HT
**TVA 19,25%** : 19 250 XAF
**Total TTC** : 119 250 XAF
**Document émis** : Reçu d'acompte RA-2025-000001

**Écriture comptable** :

```
Date : 15/01/2025
Référence : RA-2025-000001
Journal : BQ (Banque)

DÉBIT  512  Banque                        119 250 XAF
    CRÉDIT 4191 Clients - Avances                   100 000 XAF
    CRÉDIT 4431 TVA collectée                        19 250 XAF

Libellé : Réception acompte client SARL BELTEC
```

**À ce stade** :
- ✅ L'argent est en banque (compte 512)
- ✅ La TVA est exigible et doit être déclarée (CGI Art. 128)
- ✅ L'acompte est tracé dans le compte 4191 (OHADA Art. 276-279)
- ⚠️ Aucun chiffre d'affaires n'est reconnu (pas de livraison/prestation)

#### Étape 2 : Livraison et Facturation (01/03/2025)

**Livraison des marchandises** → Émission de la facture de vente

**Facture** : FV-2025-0045
**Montant HT** : 500 000 XAF
**TVA 19,25%** : 96 250 XAF
**Total TTC** : 596 250 XAF

**Écriture comptable (facture classique)** :

```
Date : 01/03/2025
Référence : FV-2025-0045
Journal : VE (Ventes)

DÉBIT  411  Clients                       596 250 XAF
    CRÉDIT 701  Ventes de marchandises             500 000 XAF
    CRÉDIT 4431 TVA collectée                       96 250 XAF

Libellé : Facture de vente SARL BELTEC
```

**À ce stade** :
- ✅ Le chiffre d'affaires est reconnu (500 000 XAF)
- ✅ La TVA supplémentaire (96 250 XAF) est collectée
- ⚠️ Le client nous doit 596 250 XAF (mais a déjà versé 119 250 XAF)

#### Étape 3 : Imputation de l'Acompte sur la Facture (01/03/2025)

**Opération** : Imputation du reçu RA-2025-000001 sur facture FV-2025-0045

**Écriture comptable** :

```
Date : 01/03/2025
Référence : IMP-RA-2025-000001-FV-2025-0045
Journal : OD (Opérations Diverses)

DÉBIT  4191 Clients - Avances             100 000 XAF
DÉBIT  4431 TVA collectée                  19 250 XAF
    CRÉDIT 411  Clients                              119 250 XAF

Libellé : Imputation acompte RA-2025-000001 sur facture FV-2025-0045
```

**Résultat Final** :
- Compte 411 (Clients) : 596 250 - 119 250 = **477 000 XAF** (reste à payer)
- Compte 4191 (Avances) : 100 000 - 100 000 = **0 XAF** (soldé)
- Compte 4431 (TVA) : 19 250 + 96 250 - 19 250 = **96 250 XAF** (TVA nette sur vente)

**Sur la facture client** :
```
Montant TTC facture :     596 250 XAF
Acompte imputé :        - 119 250 XAF
─────────────────────────────────────
NET À PAYER :             477 000 XAF
```

### Avantages de cette Approche

✅ **Conforme OHADA** : Utilise le compte 4191 prévu par SYSCOHADA (Art. 276-279)
✅ **Simplicité** : Seulement 3 écritures (réception + facture + imputation)
✅ **Clarté juridique** : Le reçu n'est pas une facture, pas de confusion
✅ **Reconnaissance du CA au bon moment** : Chiffre d'affaires comptabilisé à la livraison
✅ **Traçabilité** : Séparation claire entre acompte et vente
✅ **Annulation facile** : Pas besoin d'avoir de crédit si commande annulée (remboursement direct)

### Inconvénients

⚠️ **Document supplémentaire** : Nécessite l'émission d'un reçu d'acompte distinct
⚠️ **Imputation manuelle** : Le comptable doit penser à imputer l'acompte lors de la facturation

---

## 🔄 Approche #2 : Factures d'Acompte (Alternative Non Implémentée)

### Principe

Lorsqu'un client verse un acompte, nous émettons une **véritable facture d'acompte** avec le numéro **FA-YYYY-NNNNNN** qui constate immédiatement un chiffre d'affaires partiel.

### Compte OHADA Utilisé

- **411** - Clients (comme une facture normale)

### Flux Comptable Complet

#### Étape 1 : Réception de l'Acompte (15/01/2025)

**Client** : SARL BELTEC
**Montant** : 100 000 XAF HT
**TVA 19,25%** : 19 250 XAF
**Total TTC** : 119 250 XAF
**Document émis** : Facture d'acompte FA-2025-000001

**Écriture comptable** :

```
Date : 15/01/2025
Référence : FA-2025-000001
Journal : VE (Ventes)

DÉBIT  512  Banque                        119 250 XAF
    CRÉDIT 411  Clients                              119 250 XAF

Libellé : Facture d'acompte - Paiement immédiat

DÉBIT  411  Clients                       119 250 XAF
    CRÉDIT 701  Ventes de marchandises             100 000 XAF
    CRÉDIT 4431 TVA collectée                        19 250 XAF

Libellé : Facture d'acompte SARL BELTEC
```

**À ce stade** :
- ✅ L'argent est en banque (compte 512)
- ✅ La TVA est exigible (CGI Art. 128)
- ⚠️ **Le chiffre d'affaires de 100 000 XAF est reconnu IMMÉDIATEMENT** (avant livraison)
- ⚠️ Compte 411 soldé (facture payée immédiatement)

#### Étape 2 : Livraison et Facture Finale (01/03/2025)

**Livraison des marchandises** → Émission de la facture de solde

**Facture finale** : FV-2025-0045
**Montant total HT** : 500 000 XAF
**Acompte facturé** : -100 000 XAF
**Solde HT** : 400 000 XAF
**TVA 19,25% sur solde** : 77 000 XAF
**Solde TTC** : 477 000 XAF

**Écriture comptable** :

```
Date : 01/03/2025
Référence : FV-2025-0045
Journal : VE (Ventes)

DÉBIT  411  Clients                       477 000 XAF
    CRÉDIT 701  Ventes de marchandises             400 000 XAF
    CRÉDIT 4431 TVA collectée                       77 000 XAF

Libellé : Facture de vente SARL BELTEC (solde après acompte FA-2025-000001)
```

#### Étape 3 : Avoir pour Annulation (Si Nécessaire)

⚠️ **Problème** : Si la commande est annulée après l'acompte, il faut émettre un **avoir** (facture de crédit) pour annuler la facture d'acompte FA-2025-000001, ce qui crée une écriture négative au compte de résultat.

### Avantages de cette Approche

✅ **Document unique** : La facture d'acompte est une vraie facture comptable
✅ **Intégration automatique** : L'acompte apparaît directement dans le grand livre des ventes
✅ **Reconnaissance progressive du CA** : Utile pour les contrats long terme

### Inconvénients

⚠️ **Non conforme OHADA préféré** : N'utilise pas le compte 4191 dédié
⚠️ **Reconnaissance anticipée du CA** : Chiffre d'affaires comptabilisé AVANT livraison
⚠️ **Complexité en cas d'annulation** : Nécessite un avoir (document négatif)
⚠️ **Confusion juridique** : Le document s'appelle "facture" mais ne correspond pas à une livraison
⚠️ **Non implémenté** : Nécessiterait 2-3 jours de développement supplémentaire

---

## 📊 Tableau Comparatif

| Critère | Reçus d'Acompte (4191) | Factures d'Acompte (411) |
|---------|------------------------|--------------------------|
| **Compte OHADA** | 4191 (dédié) | 411 (clients) |
| **Type de document** | Reçu (non-facture) | Facture (document commercial) |
| **Reconnaissance CA** | À la livraison ✅ | À la réception acompte ⚠️ |
| **Conformité OHADA** | ✅ Recommandé (Art. 276-279) | ⚠️ Possible mais non préféré |
| **Nombre d'écritures** | 3 écritures | 2-3 écritures |
| **Complexité** | Simple | Moyenne |
| **Annulation** | Remboursement direct | Avoir obligatoire |
| **Traçabilité** | Excellente (compte dédié) | Bonne (journal des ventes) |
| **TVA exigible** | Oui (Art. 128 CGI) | Oui (Art. 128 CGI) |
| **État d'implémentation** | ✅ Complet (600 lignes) | ❌ Non implémenté |
| **Temps développement** | 0 jour (fait) | 2-3 jours |

---

## 💡 Recommandation Technique (Claude Code)

### Mon Avis Professionnel

**Je recommande de conserver l'approche #1 (Reçus d'Acompte avec compte 4191)** pour les raisons suivantes :

#### ✅ Arguments Majeurs

1. **Conformité OHADA stricte**
   - Le SYSCOHADA prévoit explicitement le compte 4191 pour les avances et acomptes (Articles 276-279)
   - C'est l'approche recommandée par les référentiels comptables africains

2. **Principe de prudence comptable**
   - Le chiffre d'affaires n'est reconnu qu'à la livraison effective
   - Pas de risque de surestimation du CA en cas d'annulation

3. **Simplicité opérationnelle**
   - Flux comptable clair et traçable
   - Pas besoin d'avoirs en cas d'annulation de commande
   - Le compte 4191 permet de voir immédiatement le montant des acomptes non imputés

4. **Séparation des préoccupations**
   - Reçu d'acompte = Document de trésorerie (cash reçu)
   - Facture de vente = Document commercial (vente effectuée)
   - Pas de confusion juridique ou fiscale

5. **Déjà implémenté et testé**
   - 600 lignes de code
   - 15 tests unitaires
   - 10 endpoints REST API
   - Migration base de données prête

#### ⚠️ Cas où l'Approche #2 Serait Préférable

La **facture d'acompte** (compte 411) pourrait être envisagée si :
- Contrats de construction long terme (IFRS 15 / IAS 11)
- Obligation contractuelle d'émettre des factures d'acompte
- Marchés publics imposant cette pratique
- Intégration avec un système ERP client qui exige ce format

### Mon Conseil

**Sauf obligation contractuelle ou réglementaire spécifique**, je conseille de :
1. ✅ Conserver l'approche actuelle (Reçus d'Acompte / compte 4191)
2. ✅ Déployer en production tel quel
3. ✅ Former les comptables sur ce flux
4. 🔄 Réévaluer dans 6 mois si besoin métier spécifique émerge

---

## ❓ Questions pour l'Expert-Comptable

Nous avons besoin de votre validation sur les points suivants :

### 1. Conformité Réglementaire
- ❓ L'approche #1 (compte 4191) est-elle conforme aux pratiques comptables au Cameroun ?
- ❓ Existe-t-il des obligations sectorielles spécifiques pour notre activité ?
- ❓ La reconnaissance du CA à la livraison (approche #1) est-elle acceptable pour les contrôles fiscaux ?

### 2. Traitement TVA
- ❓ La TVA exigible sur acompte (19,25%) est-elle correctement traitée dans l'approche #1 ?
- ❓ Doit-on déclarer la TVA sur acompte dans la déclaration du mois de réception ou du mois de facturation finale ?

### 3. Pratiques Professionnelles
- ❓ Quelle est la pratique courante dans les entreprises camerounaises ?
- ❓ Avez-vous déjà rencontré des entreprises utilisant l'approche #1 ou #2 ?
- ❓ Y a-t-il des risques d'audit fiscal avec l'une ou l'autre approche ?

### 4. Cas Particuliers
- ❓ Comment gérer un acompte si la commande est annulée après sa réception ?
- ❓ Peut-on imputer partiellement un acompte (50% sur facture A, 50% sur facture B) ?
- ❓ Faut-il émettre un document fiscal obligatoire lors de la réception d'un acompte ?

### 5. Recommandation Finale
- ❓ Confirmez-vous que l'approche #1 (Reçus d'Acompte / compte 4191) est la meilleure pour notre contexte ?
- ❓ Y a-t-il des ajustements à apporter avant le déploiement en production ?

---

## 📚 Références Réglementaires

### OHADA SYSCOHADA Révisé
- **Articles 276-279** : Comptes de tiers (dont 4191 - Avances et acomptes reçus)
- **Principe de séparation** : Distinction entre avances (avant livraison) et créances (après livraison)

### Code Général des Impôts du Cameroun
- **Article 128** : TVA sur les encaissements (TVA exigible dès réception de l'acompte)
- **Taux standard** : 19,25% (applicable aux acomptes)

### Normes IAS/IFRS (Référence)
- **IFRS 15** : Reconnaissance du revenu (principe : revenu reconnu lors du transfert de contrôle)
- **IAS 18** : Produits des activités ordinaires (ancien standard)

---

## 📞 Contacts & Prochaines Étapes

### Après Votre Validation

Si vous confirmez l'approche #1 :
1. Nous procédons à l'exécution de la migration base de données (V20)
2. Formation des utilisateurs comptables
3. Tests en environnement de production
4. Mise en service progressive

Si vous recommandez l'approche #2 :
1. Nous développons le système de factures d'acompte (2-3 jours)
2. Tests complets
3. Formation spécifique

### Délai de Réponse Souhaité
Nous aimerions déployer ce système d'ici **fin décembre 2025**. Pourriez-vous nous faire un retour dans les **7 jours** suivant la réception de ce document ?

---

## 📎 Annexes Techniques

### Documents Disponibles (Sur Demande)
1. **IMPLEMENTATION_ACOMPTES_RESUME.md** - Résumé technique complet (435 lignes)
2. **CONFORMITE_OHADA_REDUCTIONS_ESCOMPTE.md** - Analyse conformité OHADA
3. **Code source** - DepositService.java (600 lignes) avec commentaires
4. **Tests unitaires** - DepositServiceTest.java (486 lignes, 15 tests)
5. **Documentation API** - Swagger OpenAPI 3 (10 endpoints)

### Architecture Technique
- **Langage** : Java 17 + Spring Boot 3.4.0
- **Base de données** : PostgreSQL 15+ (table `deposits` avec 21 colonnes, 9 index)
- **Migration** : Flyway V20__add_deposits_table.sql (167 lignes)
- **Tests** : JUnit 5 + Mockito + AssertJ (couverture complète)

---

**Version** : 1.0.0
**Date** : 11 Décembre 2025
**Rédacteur** : Claude Code (IA Anthropic) + Équipe Technique PREDYKT
**Destinataire** : Expert-Comptable / Comptable Agréé

---

**Merci de votre expertise professionnelle. Nous attendons avec intérêt votre validation et vos recommandations.**
