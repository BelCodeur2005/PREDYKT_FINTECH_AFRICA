# Guide d'Importation de Relevés Bancaires

## Vue d'ensemble

Le système PREDYKT supporte maintenant **5 formats standards** de relevés bancaires utilisés par les banques africaines (CEMAC + UEMOA) :

1. **OFX** (Open Financial Exchange) - Format XML international
2. **MT940** (SWIFT) - Format texte SWIFT
3. **CAMT.053** (ISO 20022) - Format XML européen
4. **QIF** (Quicken Interchange Format) - Format texte simple
5. **CSV** - Format tabulaire (générique ou spécifique par banque)

## Formats Supportés par Banque

### CEMAC (Cameroun, Gabon, Congo, RCA, Tchad, Guinée Équatoriale)

| Banque | Formats Supportés | Extensions |
|--------|------------------|------------|
| **Afriland First Bank** | MT940, CSV personnalisé | `.mt940`, `.sta`, `.csv` |
| **BICEC** | MT940, CSV générique | `.mt940`, `.sta`, `.csv` |
| **Ecobank CEMAC** | OFX, CSV personnalisé | `.ofx`, `.qfx`, `.csv` |
| **SCB Cameroun** | CSV générique | `.csv` |
| **SGBC** | CAMT.053, MT940, CSV personnalisé | `.xml`, `.camt`, `.mt940`, `.csv` |
| **UBA Cameroun** | OFX, CSV personnalisé | `.ofx`, `.qfx`, `.csv` |

### UEMOA (Côte d'Ivoire, Sénégal, Bénin, Burkina Faso, Mali, Niger, Togo, Guinée-Bissau)

| Banque | Formats Supportés | Extensions |
|--------|------------------|------------|
| **Bank of Africa (BOA)** | OFX, MT940, CSV générique | `.ofx`, `.mt940`, `.csv` |
| **Coris Bank** | CSV générique | `.csv` |
| **Ecobank UEMOA** | OFX, CSV personnalisé | `.ofx`, `.qfx`, `.csv` |
| **NSIA Banque** | CSV générique | `.csv` |
| **Orabank** | OFX, CSV générique | `.ofx`, `.qfx`, `.csv` |
| **Bridge Bank** | CSV générique | `.csv` |

### Panafricaine

| Banque | Formats Supportés | Extensions |
|--------|------------------|------------|
| **UBA Group** | OFX, CSV personnalisé | `.ofx`, `.qfx`, `.csv` |
| **Standard Bank** | OFX, MT940, CSV générique | `.ofx`, `.mt940`, `.csv` |

## Utilisation de l'API

### 1. Importer des Transactions Bancaires

**Endpoint:** `POST /api/v1/companies/{companyId}/bank-transactions/import`

**Paramètres:**
- `file` (required): Fichier de relevé bancaire (multipart/form-data)
- `bankProvider` (optional): Code de la banque (ex: `ECOBANK_CEMAC`, `SGBC`, `UBA_CAMEROUN`)

**Exemples:**

```bash
# Import automatique (détection du format par extension)
curl -X POST "http://localhost:8080/api/v1/companies/1/bank-transactions/import" \
  -F "file=@releve_ecobank.ofx"

# Import avec spécification de la banque (améliore la précision)
curl -X POST "http://localhost:8080/api/v1/companies/1/bank-transactions/import" \
  -F "file=@releve_sgbc.xml" \
  -F "bankProvider=SGBC"

# Import CSV avec détection spécifique Afriland
curl -X POST "http://localhost:8080/api/v1/companies/1/bank-transactions/import" \
  -F "file=@transactions_afriland.csv" \
  -F "bankProvider=AFRILAND_FIRST_BANK"
```

**Réponse (200 OK):**
```json
{
  "success": true,
  "message": "15 transactions importées avec succès",
  "data": [
    {
      "id": 1,
      "transactionDate": "2024-01-15",
      "valueDate": "2024-01-15",
      "amount": 150000.00,
      "description": "Virement client ABC",
      "bankReference": "REF-2024-001",
      "thirdPartyName": "Entreprise ABC",
      "isReconciled": false,
      "importSource": "OFX (Open Financial Exchange)"
    }
  ]
}
```

### 2. Lister les Formats Supportés

**Endpoint:** `GET /api/v1/companies/{companyId}/bank-transactions/supported-formats`

**Paramètres:**
- `bankProvider` (optional): Code de la banque pour voir ses formats spécifiques

**Exemples:**

```bash
# Liste tous les formats standards
curl "http://localhost:8080/api/v1/companies/1/bank-transactions/supported-formats"

# Formats pour une banque spécifique
curl "http://localhost:8080/api/v1/companies/1/bank-transactions/supported-formats?bankProvider=SGBC"
```

**Réponse (tous les formats):**
```json
{
  "success": true,
  "data": {
    "standardFormats": [
      {
        "name": "OFX",
        "description": "Open Financial Exchange (XML)",
        "extensions": ".ofx, .qfx"
      },
      {
        "name": "MT940",
        "description": "SWIFT MT940 (Texte)",
        "extensions": ".mt940, .sta, .txt"
      },
      {
        "name": "CAMT.053",
        "description": "ISO 20022 (XML)",
        "extensions": ".xml, .camt"
      },
      {
        "name": "QIF",
        "description": "Quicken Interchange Format (Texte)",
        "extensions": ".qif"
      },
      {
        "name": "CSV",
        "description": "CSV générique ou spécifique banque",
        "extensions": ".csv"
      }
    ],
    "banks": {
      "SGBC": ["CAMT.053 (ISO 20022)", "SWIFT MT940", "CSV SGBC"],
      "ECOBANK_CEMAC": ["Open Financial Exchange", "CSV Ecobank"],
      ...
    }
  }
}
```

### 3. Lister les Transactions Importées

**Endpoint:** `GET /api/v1/companies/{companyId}/bank-transactions`

**Paramètres:**
- `startDate`: Date de début (format: yyyy-MM-dd)
- `endDate`: Date de fin (format: yyyy-MM-dd)

**Exemple:**
```bash
curl "http://localhost:8080/api/v1/companies/1/bank-transactions?startDate=2024-01-01&endDate=2024-01-31"
```

### 4. Réconcilier une Transaction

**Endpoint:** `POST /api/v1/companies/{companyId}/bank-transactions/{transactionId}/reconcile`

**Paramètres:**
- `glEntryId`: ID de l'écriture comptable à associer

**Exemple:**
```bash
curl -X POST "http://localhost:8080/api/v1/companies/1/bank-transactions/42/reconcile?glEntryId=123"
```

## Détails des Formats

### 1. OFX (Open Financial Exchange)

**Extensions:** `.ofx`, `.qfx`
**Type:** XML
**Banques:** Ecobank, UBA, BOA, Standard Bank, Orabank

**Structure type:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<OFX>
  <BANKMSGSRSV1>
    <STMTTRNRS>
      <STMTRS>
        <BANKTRANLIST>
          <STMTTRN>
            <TRNTYPE>DEBIT</TRNTYPE>
            <DTPOSTED>20240115</DTPOSTED>
            <TRNAMT>-50000</TRNAMT>
            <FITID>REF123</FITID>
            <NAME>Fournisseur XYZ</NAME>
            <MEMO>Paiement facture</MEMO>
          </STMTTRN>
        </BANKTRANLIST>
      </STMTRS>
    </STMTTRNRS>
  </BANKMSGSRSV1>
</OFX>
```

### 2. MT940 (SWIFT)

**Extensions:** `.mt940`, `.sta`, `.txt`
**Type:** Texte structuré
**Banques:** SGBC, BICEC, Afriland First Bank, BOA, Standard Bank

**Structure type:**
```
:20:STATEMENT123
:25:CM001234567890
:28C:00001/001
:60F:C240101XAF1000000,00
:61:2401150115DR50000,00NSTO//REF123
:86:Paiement fournisseur XYZ
:62F:C240115XAF950000,00
```

**Champs:**
- `:20:` Référence de transaction
- `:25:` Numéro de compte
- `:60F:` Solde d'ouverture
- `:61:` Transaction (date, débit/crédit, montant)
- `:86:` Description
- `:62F:` Solde de clôture

### 3. CAMT.053 (ISO 20022)

**Extensions:** `.xml`, `.camt`
**Type:** XML
**Banques:** SGBC (Société Générale)

**Structure type:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Document xmlns="urn:iso:std:iso:20022:tech:xsd:camt.053.001.02">
  <BkToCstmrStmt>
    <Stmt>
      <Acct>
        <Id><IBAN>CM7612345678901234567890</IBAN></Id>
      </Acct>
      <Ntry>
        <Amt Ccy="XAF">50000.00</Amt>
        <CdtDbtInd>DBIT</CdtDbtInd>
        <BookgDt><Dt>2024-01-15</Dt></BookgDt>
        <NtryDtls>
          <TxDtls>
            <RmtInf><Ustrd>Paiement fournisseur</Ustrd></RmtInf>
          </TxDtls>
        </NtryDtls>
      </Ntry>
    </Stmt>
  </BkToCstmrStmt>
</Document>
```

### 4. QIF (Quicken Interchange Format)

**Extensions:** `.qif`
**Type:** Texte simple
**Utilisation:** Format universel, support générique

**Structure type:**
```
!Type:Bank
D15/01/2024
T-50000.00
PFournisseur XYZ
MPaiement facture
NREF123
^
D16/01/2024
T150000.00
PClient ABC
MVirement reçu
NREF124
^
```

**Codes:**
- `D` = Date
- `T` = Montant (Transaction)
- `P` = Bénéficiaire (Payee)
- `M` = Mémo (description)
- `N` = Numéro/Référence
- `^` = Fin de transaction

### 5. CSV (Comma-Separated Values)

**Extensions:** `.csv`
**Type:** Texte tabulaire
**Formats supportés:**
- CSV générique
- CSV spécifique par banque (Afriland, Ecobank, UBA, SGBC)

**Format générique (simple):**
```csv
Date,Description,Montant,Référence
15/01/2024,Virement client ABC,150000.00,REF-2024-001
16/01/2024,Paiement fournisseur XYZ,-50000.00,REF-2024-002
```

**Format générique (débit/crédit):**
```csv
Date,Débit,Crédit,Description,Référence
15/01/2024,0,150000.00,Virement client ABC,REF-2024-001
16/01/2024,50000.00,0,Paiement fournisseur XYZ,REF-2024-002
```

**Notes CSV:**
- Séparateurs acceptés: `,` (virgule) ou `;` (point-virgule)
- Formats de date: `dd/MM/yyyy`, `yyyy-MM-dd`, `MM/dd/yyyy`
- Encodage: UTF-8

## Détection Automatique du Format

Le système détecte automatiquement le format à partir de :

1. **Extension du fichier** (prioritaire)
   - `.ofx` / `.qfx` → OFX
   - `.mt940` / `.sta` → MT940
   - `.xml` / `.camt` → CAMT.053
   - `.qif` → QIF
   - `.csv` → CSV

2. **Type MIME** (si extension ambiguë)
   - `application/x-ofx` → OFX
   - `application/xml` → CAMT.053
   - `text/csv` → CSV

3. **BankProvider** (pour CSV spécifiques)
   - `AFRILAND_FIRST_BANK` → CSV Afriland
   - `ECOBANK_CEMAC` → CSV Ecobank
   - `SGBC` → CSV SGBC
   - etc.

## Gestion des Doublons

Le système évite automatiquement les doublons en vérifiant la **référence bancaire** (`bankReference`). Si une transaction avec la même référence existe déjà pour cette entreprise, elle est ignorée.

**Compteur de doublons** retourné dans les logs :
```
Import completed: 15 transactions saved, 3 duplicates ignored
```

## Codes des Banques

Pour utiliser le paramètre `bankProvider`, voici les codes disponibles :

### CEMAC
- `AFRILAND_FIRST_BANK`
- `BICEC`
- `ECOBANK_CEMAC`
- `SCB_CAMEROUN`
- `SGBC`
- `UBA_CAMEROUN`

### UEMOA
- `BOA`
- `CORIS_BANK`
- `ECOBANK_UEMOA`
- `NSIA_BANQUE`
- `ORABANK`
- `BRIDGE_BANK`

### Panafricaine
- `UBA_GROUP`
- `STANDARD_BANK`

### Générique
- `GENERIC` (accepte tous les formats)

## Exemples de Fichiers Test

Des fichiers d'exemple sont disponibles dans le répertoire de test :

```
test-data/
  ├── ecobank_sample.ofx          # OFX Ecobank
  ├── sgbc_sample.xml             # CAMT.053 SGBC
  ├── afriland_sample.mt940       # MT940 Afriland
  ├── generic_sample.qif          # QIF générique
  └── csv/
      ├── generic_simple.csv      # CSV format simple
      ├── generic_debit_credit.csv # CSV format débit/crédit
      ├── ecobank.csv             # CSV Ecobank
      └── sgbc.csv                # CSV SGBC
```

## Gestion des Erreurs

### Erreurs Communes

1. **Fichier vide**
   ```json
   {
     "success": false,
     "message": "Le fichier est vide"
   }
   ```

2. **Format non reconnu**
   ```json
   {
     "success": false,
     "message": "Échec de l'import: Format de fichier non supporté"
   }
   ```

3. **Erreur de parsing**
   ```json
   {
     "success": false,
     "message": "Échec de l'import: Invalid XML format at line 15"
   }
   ```

4. **Banque inconnue**
   ```json
   {
     "success": false,
     "message": "Banque inconnue: INVALID_BANK"
   }
   ```

### Résilience

- Les erreurs de parsing d'une ligne n'arrêtent pas l'import complet
- Les lignes invalides sont ignorées et loguées
- Un résumé (succès/erreurs) est retourné à la fin

## Support Technique

Pour toute question ou problème :
1. Consultez les logs de l'application
2. Vérifiez que le format du fichier correspond à la banque
3. Testez avec un fichier d'exemple
4. Contactez l'équipe de support PREDYKT

## Évolutions Futures

- Support de nouveaux formats (BAI2, etc.)
- Parsers CSV personnalisés pour d'autres banques africaines
- Import automatique via API bancaires (PSD2, Open Banking)
- Détection intelligente de devises
- Catégorisation automatique des transactions par IA
