# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PREDYKT Core Accounting API is a professional REST API for African OHADA accounting and financial management. It's designed for the Pan-African financial analysis and prediction platform, natively built for the African business context.

**Current Phase: MVP (Backend Java Only)**
- The MVP focuses exclusively on the Java backend for accounting operations
- Financial predictions and ML-based forecasting are planned for future phases
- For now, all features are implemented in Java without external Python/ML services

**Technology Stack:**
- Java 17 with Spring Boot 3.4.0
- PostgreSQL 15+ (with Flyway migrations)
- Redis 7+ (caching)
- MapStruct + Lombok (mapping & boilerplate)
- OpenCSV (CSV imports)
- SpringDoc OpenAPI 3 (Swagger)

## Build & Run Commands

### Development
```bash
# Start infrastructure (PostgreSQL, Redis, PgAdmin)
docker-compose up -d

# Build the project
./mvnw clean package -DskipTests

# Run the application
./mvnw spring-boot:run

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=shared
./mvnw spring-boot:run -Dspring-boot.run.profiles=dedicated -DPREDYKT_TENANT_ID=tenant123
./mvnw spring-boot:run -Dspring-boot.run.profiles=cabinet -DPREDYKT_TENANT_CABINET_ID=cabinet456
```

### Testing
```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=FinancialRatioServiceTest

# Run tests with pattern
./mvnw test -Dtest=*ServiceTest

# Integration tests
./mvnw verify

# Generate code coverage report (outputs to target/site/jacoco/index.html)
./mvnw jacoco:report
```

### Database Migrations
```bash
# Run Flyway migrations manually (normally auto-executed on startup)
./mvnw flyway:migrate

# Validate migrations
./mvnw flyway:validate

# Show migration info
./mvnw flyway:info
```

### Production Build
```bash
# Build production JAR
./mvnw clean package -DskipTests

# Build Docker image
docker build -t predykt/accounting-api:1.0.0 .
```

## Multi-Tenant Architecture

**Critical Design Pattern**: This application implements a sophisticated multi-tenant architecture with THREE deployment modes:

### 1. SHARED Mode (PME - Default)
- **Use Case**: Multiple small businesses sharing the same database
- **Tenant Isolation**: Row-level security via `company_id` column
- **Tenant Detection**: Extracted from JWT token (`companyId` claim)
- **Database**: Single shared PostgreSQL database `predykt_db`
- **Activation**: Set `SPRING_PROFILE=shared` or `PREDYKT_TENANT_MODE=SHARED`

### 2. DEDICATED Mode (ETI)
- **Use Case**: Large enterprise with isolated database
- **Tenant Isolation**: Database-level (separate database per tenant)
- **Tenant Detection**: From environment variable `PREDYKT_TENANT_ID`
- **Database**: Dedicated database `predykt_tenant_{TENANT_ID}`
- **Activation**: Set `SPRING_PROFILE=dedicated` AND `PREDYKT_TENANT_ID=xxx`
- **IMPORTANT**: `PREDYKT_TENANT_ID` is REQUIRED for this mode

### 3. CABINET Mode (Accounting Firms)
- **Use Case**: Accounting firm managing multiple client companies
- **Tenant Isolation**: Database per accounting firm, row-level per client
- **Tenant Detection**: Cabinet from ENV, company from JWT
- **Database**: `predykt_cabinet_{CABINET_ID}`
- **Activation**: Set `SPRING_PROFILE=cabinet` AND `PREDYKT_TENANT_CABINET_ID=xxx`

### Tenant Context Management
The `TenantContextHolder` (ThreadLocal-based) stores the current tenant context:
- **TenantInterceptor** (src/main/java/com/predykt/accounting/config/TenantInterceptor.java:24) intercepts all requests to resolve tenant
- Extracts JWT from `Authorization: Bearer {token}` header
- Sets `TenantContext` with mode, tenantId, companyId, cabinetId
- **CRITICAL**: Always cleared in `afterCompletion()` to prevent context leaks

When working with tenant-related code:
- ALL repository queries MUST filter by `company_id` in SHARED mode
- Service layer retrieves `companyId` from `TenantContextHolder.getContext()`
- NEVER hardcode tenant/company IDs
- Test with different tenant modes to ensure isolation

## OHADA Accounting System

**Core Business Logic**: The application implements the OHADA (Organization for the Harmonization of Business Law in Africa) accounting standard.

### Chart of Accounts (Plan Comptable)
- **Pre-configured**: OHADA chart loaded from `src/main/resources/ohada/chart-of-accounts-ohada.json`
- **Auto-initialization**: Created when a new Company is registered (see ChartOfAccountsService.java:34)
- **Account Number Structure**:
  - Class 1-5: Balance Sheet (Assets, Liabilities, Equity)
  - Class 6: Expenses
  - Class 7: Revenue
  - Class 8: Special accounts
- **Account Type Detection**: Automatic via `AccountType.fromAccountNumber()` based on first digit

### Double-Entry Bookkeeping
- **Golden Rule**: Every journal entry MUST be balanced (total debits = total credits)
- **Validation**: Enforced in `GeneralLedgerService` (src/main/java/com/predykt/accounting/service/GeneralLedgerService.java)
- **Journal Codes**: VE (ventes), AC (achats), BQ (banque), OD (opérations diverses)
- **Period Locking**: Prevents modifications to closed fiscal periods

### CSV Import Intelligence
The `CsvImportService` (src/main/java/com/predykt/accounting/service/CsvImportService.java:36) provides automatic mapping of business activities to OHADA accounts:

**Expected CSV Format (activités.csv):**
```csv
date de saisie;Activitées;description;Montant Brut;Type;Années
14/04/2021;Wholesale Sales;Vente - Wholesale Sales - client 9850;1 606 982;Revenu;2021
26/09/2021;Maintenance;Charge - Maintenance - fournisseur 428;257 025;Dépenses;2021
25/01/2021;Capex - Machinery/Equipment;Achat d'équipement de production;-16 796 504;Capex;2021
```

**Additional CSV Files Available:**
- `cashflow.csv`: Historical cash flow data (2021-2025) with operating, investing, and financing flows
- `compte-de-resultat.csv`: Income statement data including revenue, expenses, margins, and net income
- `structure du bilan.csv`: Balance sheet structure with assets, liabilities, and equity breakdowns

These CSV files contain real sample data that can be used for testing imports and financial analysis features.

**Automatic Account Mapping Examples (from actual data):**
- Wholesale Sales / Export Sales / Retail Sales → 701 (Ventes de marchandises)
- Administrative Salaries / Direct Labor → 661 (Rémunérations)
- Rent → 622 (Loyers)
- Maintenance → 625 (Entretien et réparations)
- Marketing & Sales → 627 (Publicité et relations publiques)
- Professional fees → 632 (Honoraires)
- Factory Overheads (Utilities) → 605 (Autres charges)
- Packaging Purchases → 602 (Achats d'emballages)
- Depreciation → 681 (Dotations aux amortissements)
- Capex - Machinery/Equipment → 24 (Matériel et outillage)
- Other Operating Income → 758 (Produits divers)

The service handles:
- Multiple date formats (DD/MM/YYYY, YYYY-MM-DD)
- Multiple separators (`;` or `,`)
- Amount cleaning (spaces, commas)
- Balanced entry creation (automatic debit/credit allocation)

## Financial Analysis Features

### Financial Ratios (20+ KPIs)
The `FinancialRatioService` (src/main/java/com/predykt/accounting/service/FinancialRatioService.java:28) calculates:

**Profitability Ratios:**
- ROA (Return on Assets) = Net Income / Total Assets
- ROE (Return on Equity) = Net Income / Equity
- Gross Margin % = Gross Profit / Revenue
- Net Margin % = Net Income / Revenue

**Liquidity Ratios:**
- Current Ratio = Current Assets / Current Liabilities
- Quick Ratio = (Current Assets - Inventory) / Current Liabilities

**Activity Ratios:**
- DSO (Days Sales Outstanding) = Receivables turnover
- DIO (Days Inventory Outstanding)
- DPO (Days Payables Outstanding)

**Solvency Ratios:**
- Debt-to-Equity = Total Debt / Total Equity
- Debt-to-Assets = Total Debt / Total Assets

### Financial Reports
1. **Balance Sheet** (Bilan): Assets vs Liabilities + Equity at a point in time
2. **Income Statement** (Compte de Résultat): Revenue - Expenses for a period
3. **Trial Balance** (Balance de vérification): All account balances
4. **General Ledger**: Detailed transaction history per account

## Security & Authentication

### JWT-Based Authentication
- **Token Provider**: JwtTokenProvider (src/main/java/com/predykt/accounting/security/JwtTokenProvider.java)
- **Claims Structure**: Contains `userId`, `companyId`, `roles`, `email`
- **Token Expiration**: Configured via `spring.security.jwt.expiration` (default 24h)
- **Secret Key**: Set via `JWT_SECRET` environment variable

### Current Security State
- **MVP Phase**: Security is DISABLED (see SecurityConfig.java:44 - `.anyRequest().permitAll()`)
- **Production TODO**: Enable authentication for all endpoints except health/swagger
- **Password Encoding**: BCrypt with strength 12

### User Roles & Permissions
Users can have roles: ADMIN, ACCOUNTANT, VIEWER (stored in `users` and `roles` tables)
- Future implementation will use `@PreAuthorize` annotations on controllers
- Role-based access control (RBAC) infrastructure is in place but not enforced yet

## Database Schema

### Key Tables (Flyway Migrations)
- **V1__initial_schema.sql**: Companies, ChartOfAccounts, GeneralLedger, BankTransactions, AuditLog
- **V2__add_ratios_table.sql**: FinancialRatio, CashFlowProjection, Budget
- **V3__add_authentication_tables.sql**: Users, Roles, PasswordResetTokens
- **V4__multi_tenant_support_tables.sql**: Tenant management tables, isolation setup

### Important Relationships
- `Company` (1) → (N) `ChartOfAccounts`: Each company has its own chart of accounts
- `ChartOfAccounts` (1) → (N) `GeneralLedger`: Account → Transactions
- `Company` (1) → (N) `FinancialRatio`: One ratio per fiscal period
- `User` (N) ↔ (M) `Company`: Users can access multiple companies (tenant-dependent)

### Indexes & Performance
- Indexes on `company_id` for all tenant-isolated tables
- Composite index on `(company_id, account_number)` for account lookups
- Indexes on `entry_date` for date-range queries
- All queries MUST use prepared statements (JPA handles this)

## API Structure

**Base URL**: `http://localhost:8080/api/v1`
**Swagger UI**: `http://localhost:8080/api/v1/swagger-ui.html`

### Controller Naming Pattern
- `{Entity}Controller`: REST endpoints (e.g., CompanyController, GeneralLedgerController)
- All use `@RestController` + `@RequestMapping("/path")`
- Responses wrapped in `ApiResponse<T>` with success/error structure

### Request/Response DTOs
- **Requests**: `dto/request/{Entity}CreateRequest`, `{Entity}UpdateRequest`
- **Responses**: `dto/response/{Entity}Response`, `ApiResponse<T>`
- **Mapping**: MapStruct interfaces in `mapper/` package

### Error Handling
Global exception handler (GlobalExceptionHandler.java:8) catches:
- `ResourceNotFoundException` → 404
- `ValidationException` → 400
- `UnbalancedEntryException` → 400 (accounting-specific)
- `ImportException` → 400 (CSV import errors)
- `AccountingException` → 500 (general business logic errors)

## Common Development Patterns

### Service Layer Pattern
All business logic goes in `service/` classes:
1. Inject repositories via constructor (`@RequiredArgsConstructor`)
2. Use `@Transactional` for write operations
3. Throw `ResourceNotFoundException` for missing entities
4. Return DTOs, not entities
5. Log important operations with SLF4J `@Slf4j`

### Repository Queries
- Use Spring Data JPA method naming: `findByCompanyAndAccountNumber`
- Complex queries use `@Query` with JPQL
- ALWAYS filter by company in SHARED mode
- Use `Optional<T>` for single results

### Caching Strategy
Redis caching enabled for:
- Chart of Accounts lookups (`@Cacheable("chartOfAccounts")`)
- Company data
- Cache invalidation on updates using `@CacheEvict`

### Audit Trail
The `AuditEntityListener` (src/main/java/com/predykt/accounting/domain/listener/AuditEntityListener.java) automatically tracks:
- `createdAt`, `updatedAt` timestamps on all entities extending `BaseEntity`
- User actions logged to `AuditLog` table via `AuditService`

## Testing Guidelines

### Test Structure
- **Unit Tests**: `src/test/java/.../service/*ServiceTest.java` - Mock dependencies
- **Integration Tests**: `src/test/java/.../integration/*IntegrationTest.java` - Use H2 in-memory DB
- **Controller Tests**: `src/test/java/.../controller/*ControllerTest.java` - MockMvc

### Test Configuration
- Use `@SpringBootTest` for integration tests
- Use `@WebMvcTest` for controller tests
- Use `@Transactional` in tests for automatic rollback
- Test data available in root CSV files (activités.csv, cashflow.csv, etc.)

## Environment Variables

### Required
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`: PostgreSQL connection
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`: Redis connection
- `JWT_SECRET`: JWT signing key (min 256 bits)

### Tenant Configuration
- `PREDYKT_TENANT_MODE`: SHARED | DEDICATED | CABINET
- `PREDYKT_TENANT_ID`: Required for DEDICATED mode
- `PREDYKT_TENANT_CABINET_ID`: Required for CABINET mode

### Optional
- `SERVER_PORT`: API port (default 8080)
- `SPRING_PROFILE`: dev | shared | dedicated | cabinet

## Sample Data & Test Files

The repository includes real sample data for testing and development:

### CSV Test Files
Located in the project root:

1. **activités.csv** (1500+ transactions)
   - Real business activities from 2021-2025
   - Multiple activity types: Sales (Wholesale, Export, Retail), Operating expenses, Capex, Financing
   - Used to test the CSV import service and automatic OHADA mapping
   - Format: `date de saisie;Activitées;description;Montant Brut;Type;Années`

2. **cashflow.csv** (5 years)
   - Cash flow statements from 2021-2025
   - Operating, investing, and financing cash flows
   - Includes depreciation, working capital changes, capex
   - Format: Years, net income, depreciation, receivables/inventory changes, cash flows

3. **compte-de-resultat.csv** (5 years)
   - Income statements from 2021-2025
   - Revenue, variable costs, gross margin, fixed costs, EBIT, taxes, net income
   - Includes average debt and interest calculations
   - Tax rate: 19.25%, Interest rate: 5%

4. **structure du bilan.csv** (5 years)
   - Balance sheets from 2021-2025
   - Assets: Cash, receivables, inventory, fixed assets
   - Liabilities: Long-term debt, short-term debt, equity
   - Complete with average debt calculations

### Using Sample Data
When testing or demonstrating features:
```bash
# Import activities for a company
curl -X POST http://localhost:8080/api/v1/companies/1/import/activities-csv \
  -F "file=@activités.csv"

# Calculate ratios for imported data
curl -X POST "http://localhost:8080/api/v1/companies/1/ratios/calculate?startDate=2021-01-01&endDate=2021-12-31"

# Generate financial reports
curl "http://localhost:8080/api/v1/companies/1/reports/balance-sheet?asOfDate=2021-12-31"
```

## Important Notes

1. **OHADA Compliance**: When adding accounting features, consult OHADA standards. Account numbers and classifications must follow OHADA nomenclature.

2. **Tenant Isolation**: ALWAYS verify tenant isolation when modifying repository queries. In SHARED mode, forgetting to filter by `company_id` is a critical security vulnerability.

3. **Double-Entry Validation**: ANY code that creates journal entries must ensure debits = credits. The `GeneralLedgerService.validateBalancedEntry()` method must be called.

4. **CSV Import Resilience**: The import service is designed to be fault-tolerant. Errors on individual rows should NOT fail the entire import. Return `ImportResultResponse` with success/error counts.

5. **Migration Versioning**: Flyway migrations are IMMUTABLE. Never modify existing migrations. Create new `V{N+1}__description.sql` files.

6. **API Versioning**: The API is versioned in the context path (`/api/v1`). Breaking changes require a new version (`/api/v2`).

7. **Lombok + MapStruct Order**: In `pom.xml`, the annotation processor order matters. Lombok must process BEFORE MapStruct. This is correctly configured.

8. **Windows Development**: Use `./mvnw` (not `mvn`) to ensure correct Maven wrapper execution on Windows.

9. **Logging Format**: Console logs include tenant mode indicator: `[🟢SHARED]`, `[🔵DEDICATED-{id}]`, `[🟡CABINET-{id}]` for easy debugging.

10. **Health Check**: Always verify the application is running via `curl http://localhost:8080/api/v1/health` before running tests or imports.

11. **MVP Scope**: The current MVP is backend-only (Java). Prediction/forecasting features exist in the schema (CashFlowProjection, prediction_details) but are NOT implemented yet. Focus on core accounting operations, imports, ratios calculation, and financial reports.

12. **JWT Key Security**: The current JWT secret key is too short (320 bits). For production, generate a secure 512-bit key using `io.jsonwebtoken.security.Keys.secretKeyFor(SignatureAlgorithm.HS512)` or ensure the `JWT_SECRET` environment variable contains a 64+ character string.

13. **CSV Encoding**: CSV files use UTF-8 with BOM and may contain special French characters (é, è, à). The CsvImportService handles this with `StandardCharsets.UTF_8` and robust parsing.
