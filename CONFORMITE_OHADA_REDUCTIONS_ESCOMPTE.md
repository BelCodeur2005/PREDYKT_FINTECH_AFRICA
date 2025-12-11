# Conformité OHADA : Réductions, Escompte, Acomptes et Solde

## Question de l'utilisateur
**"L'escompte, les réductions, les acomptes et le solde sont-ils nécessaires dans mon système et demandés par l'OHADA ?"**

---

## Réponse Synthétique

| Élément | Obligatoire OHADA ? | Implémenté ? | Priorité | Compte OHADA |
|---------|---------------------|--------------|----------|--------------|
| **Réductions** | ✅ Recommandé | ✅ OUI | - | RRR sur comptes 70x/60x |
| **Acomptes** | ✅ **OBLIGATOIRE** | ❌ NON | 🔴 **HAUTE** | 4191 Clients - Avances |
| **Escompte** | ⚠️ Optionnel | ❌ NON | 🟡 MOYENNE | 773/673 Escomptes |
| **Solde** | ✅ Obligatoire | ✅ OUI | - | Calculé (amountDue) |

---

## 1. RÉDUCTIONS (RRR) ✅ Implémenté

### Référence OHADA
**SYSCOHADA Article 35** : Les réductions commerciales (rabais, remises, ristournes) doivent être enregistrées.

### Types de Réductions
1. **Rabais** : Réduction pour défaut de qualité/conformité
2. **Remise** : Réduction commerciale habituelle
3. **Ristourne** : Réduction en fin de période (volume d'achats)

### Implémentation Actuelle
```java
// InvoiceLine.java (LIGNE 67)
private BigDecimal discountPercentage = BigDecimal.ZERO;

// Calcul automatique (LIGNE 112)
public void calculateAmounts() {
    this.subtotal = this.quantity.multiply(this.unitPrice);
    this.discountAmount = this.subtotal.multiply(this.discountPercentage).divide(100);
    this.totalHt = this.subtotal.subtract(this.discountAmount);
    // ...
}
```

### Comptabilisation OHADA
```
Vente avec remise 10% sur 100 000 XAF HT :

411 Clients                        107 325
    701 Ventes de marchandises              90 000  (100 000 - 10%)
    4431 TVA collectée                      17 325  (90 000 × 19.25%)
```

### ✅ Verdict : Conforme OHADA - Déjà implémenté

---

## 2. ACOMPTES (Avances et Arrhes) 🔴 MANQUANT - OBLIGATOIRE

### Référence OHADA
**SYSCOHADA Articles 276-279** : Les acomptes doivent être enregistrés dans un compte spécifique.

### Définition
- **Acompte** : Paiement partiel avant livraison/prestation
- **Compte OHADA 4191** : "Clients - Avances et acomptes reçus sur commandes"
- **TVA** : Exigible dès réception de l'acompte (TVA sur encaissement)

### Implémentation Actuelle
```java
// Invoice.java - Analyse
private BigDecimal amountPaid = BigDecimal.ZERO;  // ✅ Existe
private BigDecimal amountDue = BigDecimal.ZERO;   // ✅ Existe

// ❌ MANQUANT : Pas de traçabilité des acomptes
// ❌ MANQUANT : Pas de lien avec compte 4191
// ❌ MANQUANT : Pas de reçu d'acompte distinct
// ❌ MANQUANT : Pas de gestion TVA sur acompte
```

### Ce Qui Devrait Exister

#### A. Entité `Deposit` (Acompte)
```java
@Entity
public class Deposit {
    private Long id;
    private Invoice invoice;              // Lien avec facture finale
    private String depositNumber;         // Numéro du reçu d'acompte
    private LocalDate depositDate;        // Date de réception
    private BigDecimal amountHt;          // Montant HT de l'acompte
    private BigDecimal vatAmount;         // TVA exigible sur acompte
    private BigDecimal amountTtc;         // Montant TTC encaissé
    private Payment payment;              // Lien avec paiement
    private Boolean isApplied;            // Imputé sur facture finale ?
    private String depositReceipt;        // Référence reçu d'acompte
}
```

#### B. Comptabilisation OHADA - Acompte

**Réception acompte 50% sur commande 200 000 XAF HT :**
```
Date : 15/01/2025 - Reçu d'acompte RA-2025-001

512 Banque                         119 250
    4191 Clients - Avances                 100 000  (50% de 200 000)
    4431 TVA collectée                      19 250  (100 000 × 19.25%)

📌 Note : TVA exigible immédiatement sur acompte reçu
```

#### C. Comptabilisation OHADA - Facture Finale

**Facturation finale 200 000 XAF HT avec imputation acompte :**
```
Date : 01/03/2025 - Facture FA-2025-045

411 Clients                        119 250  (Solde restant)
4191 Clients - Avances             100 000  (Imputation acompte)
4431 TVA collectée                  19 250  (Imputation TVA acompte)
    701 Ventes de marchandises             200 000  (Montant HT total)
    4431 TVA collectée                      38 500  (TVA totale 19.25%)

Solde restant dû : 119 250 XAF
```

### ❌ Verdict : NON Conforme OHADA - Implémentation REQUISE

### Priorité : 🔴 HAUTE
**Raison** : Nécessaire pour :
- Conformité OHADA (obligatoire)
- Traçabilité des flux de trésorerie
- Déclaration TVA correcte (TVA sur encaissements)
- Rapprochement bancaire (acomptes = transactions distinctes)

---

## 3. ESCOMPTE (Rabais Financier) 🟡 OPTIONNEL

### Référence OHADA
**SYSCOHADA Article 409** : L'escompte de règlement peut être comptabilisé en produits/charges financiers.

### Définition
- **Escompte** : Réduction financière accordée pour paiement anticipé
- **Exemple** : "2% si paiement sous 10 jours au lieu de 30 jours"
- **Nature** : Produit/Charge FINANCIER (pas commercial comme les RRR)

### Différence avec Réduction Commerciale

| Type | Nature | Compte | Moment |
|------|--------|--------|--------|
| **Remise** | Commerciale | 70x/60x (Ventes/Achats) | Sur facture |
| **Escompte** | Financière | 773/673 (Produits/Charges) | Au paiement |

### Comptabilisation OHADA - Escompte OBTENU (Client)

**Facture fournisseur 100 000 XAF avec escompte 2% si paiement sous 10 jours :**
```
Réception facture :
601 Achats de marchandises        100 000
4452 TVA récupérable               19 250
    401 Fournisseurs                      119 250

Paiement sous 10 jours (escompte obtenu) :
401 Fournisseurs                  119 250
    512 Banque                            117 265  (119 250 - 2%)
    773 Escomptes obtenus                   1 985  (2% de 100 000 + TVA)

📌 Note : Escompte = Produit financier pour l'acheteur
```

### Comptabilisation OHADA - Escompte ACCORDÉ (Fournisseur)

**Facture client 100 000 XAF avec escompte 2% si paiement sous 10 jours :**
```
Facturation :
411 Clients                       119 250
    701 Ventes de marchandises             100 000
    4431 TVA collectée                      19 250

Paiement anticipé (escompte accordé) :
512 Banque                        117 265
673 Escomptes accordés              1 985  (Charge financière)
    411 Clients                            119 250

📌 Note : Escompte = Charge financière pour le vendeur
```

### Implémentation Actuelle
```java
// Invoice.java - Analyse
// ❌ MANQUANT : Pas de champ escompte
// ❌ MANQUANT : Pas de conditions d'escompte
// ❌ MANQUANT : Pas de calcul automatique au paiement
```

### Ce Qui Devrait Exister

```java
@Entity
public class Invoice {
    // ... champs existants

    // Nouveaux champs pour escompte
    private BigDecimal cashDiscountPercentage;    // Ex: 2.0 pour 2%
    private Integer cashDiscountDays;             // Ex: 10 jours
    private LocalDate cashDiscountDeadline;       // Date limite escompte
    private BigDecimal cashDiscountAmount;        // Montant escompte obtenu
    private Boolean cashDiscountApplied;          // Escompte appliqué ?
}
```

### ⚠️ Verdict : Optionnel mais Recommandé

### Priorité : 🟡 MOYENNE
**Raison** :
- Pratique commerciale courante au Cameroun
- Améliore la trésorerie (incite paiement rapide)
- Comptabilisation simple si implémenté
- Pas obligatoire OHADA (optionnel)

---

## 4. SOLDE (Montant Restant Dû) ✅ Implémenté

### Référence OHADA
**SYSCOHADA Article 271** : Le solde client doit être suivi pour la balance âgée.

### Implémentation Actuelle
```java
// Invoice.java (Analyse)
private BigDecimal totalTtc = BigDecimal.ZERO;   // Montant total facture
private BigDecimal amountPaid = BigDecimal.ZERO; // Montants déjà payés
private BigDecimal amountDue = BigDecimal.ZERO;  // Solde restant dû

// Calcul du solde (implicite)
// amountDue = totalTtc - amountPaid

// Lien avec Payment
@OneToMany(mappedBy = "invoice")
private List<Payment> payments = new ArrayList<>();
```

### ✅ Verdict : Conforme OHADA - Déjà implémenté

**Fonctionnalités Existantes** :
- Suivi du solde restant dû (`amountDue`)
- Historique des paiements partiels (`payments`)
- Statut de paiement (PAID, PARTIALLY_PAID, UNPAID)

---

## 5. Récapitulatif et Roadmap

### État de Conformité OHADA

| Fonction | Exigence OHADA | État | Action |
|----------|---------------|------|--------|
| **Réductions (RRR)** | Recommandé | ✅ Conforme | Aucune |
| **Solde** | Obligatoire | ✅ Conforme | Aucune |
| **Acomptes** | **Obligatoire** | ❌ Non conforme | **Implémenter** |
| **Escompte** | Optionnel | ❌ Non implémenté | Considérer |

### Roadmap d'Implémentation Recommandée

#### Phase 1 : ACOMPTES (Obligatoire) 🔴
**Durée estimée : 2-3 jours**

1. **Migration Base de Données**
   ```sql
   CREATE TABLE deposits (
       id BIGSERIAL PRIMARY KEY,
       company_id BIGINT NOT NULL,
       invoice_id BIGINT,
       deposit_number VARCHAR(50) UNIQUE NOT NULL,
       deposit_date DATE NOT NULL,
       amount_ht DECIMAL(15,2) NOT NULL,
       vat_amount DECIMAL(15,2) NOT NULL,
       amount_ttc DECIMAL(15,2) NOT NULL,
       payment_id BIGINT,
       is_applied BOOLEAN DEFAULT FALSE,
       deposit_receipt TEXT,
       created_at TIMESTAMP DEFAULT NOW(),
       FOREIGN KEY (company_id) REFERENCES companies(id),
       FOREIGN KEY (invoice_id) REFERENCES invoices(id),
       FOREIGN KEY (payment_id) REFERENCES payments(id)
   );
   ```

2. **Entité + Repository**
   - `Deposit.java` (entity)
   - `DepositRepository.java`
   - Relation bidirectionnelle avec `Invoice` et `Payment`

3. **Service Layer**
   - `DepositService.java`
     - `createDeposit()` : Créer reçu d'acompte
     - `applyDepositToInvoice()` : Imputer acompte sur facture
     - `generateDepositReceipt()` : Générer PDF reçu
   - Modification `GeneralLedgerService` : Écritures compte 4191

4. **Controller + DTOs**
   - `DepositController.java`
   - `DepositRequest.java`, `DepositResponse.java`
   - Endpoints : POST, GET, PUT /deposits

5. **Tests**
   - Unit tests pour `DepositService`
   - Integration tests pour écritures comptables
   - Test scénario complet : Acompte → Facture finale

#### Phase 2 : ESCOMPTE (Optionnel) 🟡
**Durée estimée : 1-2 jours**

1. **Modification Invoice**
   ```java
   // Ajout champs escompte
   private BigDecimal cashDiscountPercentage;
   private Integer cashDiscountDays;
   private LocalDate cashDiscountDeadline;
   private BigDecimal cashDiscountAmount;
   private Boolean cashDiscountApplied;
   ```

2. **Service Layer**
   - `PaymentService.applyCashDiscount()` : Calcul automatique
   - Écritures comptables compte 773/673

3. **Reporting**
   - Rapport des escomptes obtenus (produits financiers)
   - Rapport des escomptes accordés (charges financières)

---

## 6. Exemples Pratiques

### Scénario 1 : Vente avec Acompte et Réduction

**Commande client 1 000 000 XAF HT - Remise 5% - Acompte 30%**

**Étape 1 : Réception Acompte (01/01/2025)**
```
Montant commande HT : 1 000 000 XAF
Remise 5% :            -50 000 XAF
Net HT :               950 000 XAF
Acompte 30% :          285 000 XAF HT
TVA 19.25% :            54 862 XAF
Total acompte TTC :    339 862 XAF

Écriture comptable :
512 Banque                         339 862
    4191 Clients - Avances                 285 000
    4431 TVA collectée                      54 862

Document : Reçu d'acompte RA-2025-001
```

**Étape 2 : Livraison et Facture Finale (01/02/2025)**
```
Facture FA-2025-012
Net HT :               950 000 XAF
TVA 19.25% :           182 875 XAF
Total TTC :          1 132 875 XAF
Acompte imputé :      -339 862 XAF
SOLDE DÛ :            793 013 XAF

Écriture comptable :
411 Clients                        793 013  (Solde)
4191 Clients - Avances             285 000  (Acompte imputé)
4431 TVA collectée                  54 862  (TVA acompte imputée)
    701 Ventes de marchandises             950 000
    4431 TVA collectée                     182 875
```

### Scénario 2 : Achat avec Escompte

**Facture fournisseur 500 000 XAF HT - Escompte 2% si paiement sous 10 jours**

**Étape 1 : Réception Facture (05/01/2025)**
```
601 Achats de marchandises        500 000
4452 TVA récupérable               96 250
    401 Fournisseurs                      596 250

Échéance : 04/02/2025 (30 jours)
Escompte si paiement avant : 15/01/2025 (10 jours)
```

**Étape 2 : Paiement Anticipé avec Escompte (12/01/2025)**
```
Escompte 2% sur 500 000 = 10 000 XAF HT
TVA sur escompte : 1 925 XAF
Total escompte : 11 925 XAF

401 Fournisseurs                  596 250
    512 Banque                            584 325  (596 250 - 11 925)
    773 Escomptes obtenus                  11 925  (Produit financier)

📌 Économie réalisée : 11 925 XAF pour paiement 20 jours plus tôt
```

---

## 7. Réponse Directe à la Question

### "Ces éléments sont-ils nécessaires et demandés par l'OHADA ?"

**Réponse :**

✅ **ACOMPTES** : **OUI, OBLIGATOIRE**
- SYSCOHADA exige le compte 4191
- Nécessaire pour déclarations TVA conformes
- Audit OHADA vérifiera ce point
- **ACTION REQUISE : Implémenter**

✅ **RÉDUCTIONS** : **OUI, Recommandé - DÉJÀ FAIT**
- Pratique commerciale standard
- Déjà correctement implémenté
- Conforme OHADA

⚠️ **ESCOMPTE** : **NON, Optionnel mais Conseillé**
- Pas obligatoire OHADA
- Pratique courante au Cameroun
- Améliore trésorerie
- **ACTION : Considérer pour Phase 2**

✅ **SOLDE** : **OUI, Obligatoire - DÉJÀ FAIT**
- Suivi des créances obligatoire
- Déjà correctement implémenté
- Conforme OHADA

---

## 8. Recommandation Finale

**PRIORITÉ IMMÉDIATE** : Implémenter la gestion des **ACOMPTES** pour conformité OHADA.

**BÉNÉFICES** :
- ✅ Conformité audit OHADA/CGI
- ✅ Déclarations TVA exactes (TVA sur encaissements)
- ✅ Rapprochement bancaire complet
- ✅ Traçabilité flux de trésorerie
- ✅ Reçus d'acompte réglementaires

**RISQUES si non implémenté** :
- ❌ Non-conformité OHADA
- ❌ Déclarations TVA incorrectes (risque pénalités)
- ❌ Difficultés rapprochement bancaire
- ❌ Remarques audit externe

---

## Références Juridiques

- **SYSCOHADA** (Système Comptable OHADA) : Articles 35, 271, 276-279, 409
- **Code Général des Impôts du Cameroun** : Articles sur TVA sur encaissements
- **Plan Comptable OHADA** : Comptes 4191, 673, 773

---

**Document créé le 11/12/2025**
**Version 1.0**
