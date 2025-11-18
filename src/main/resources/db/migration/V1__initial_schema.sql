-- ============================================
-- V1__initial_schema.sql
-- PREDYKT Core Accounting - Schéma Initial
-- ============================================

-- Extension pour UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- TABLE: companies
-- ============================================
CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    tax_id VARCHAR(50) UNIQUE,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    postal_code VARCHAR(10),
    city VARCHAR(100),
    country VARCHAR(2) NOT NULL DEFAULT 'CM',
    currency VARCHAR(3) NOT NULL DEFAULT 'XAF',
    accounting_standard VARCHAR(20) DEFAULT 'OHADA',
    fiscal_year_start VARCHAR(5) DEFAULT '01-01',
    fiscal_year_end VARCHAR(5) DEFAULT '12-31',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    vat_number VARCHAR(50),
    is_vat_registered BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_companies_active ON companies(is_active);
CREATE INDEX idx_companies_country ON companies(country);

-- ============================================
-- TABLE: chart_of_accounts
-- ============================================
CREATE TABLE chart_of_accounts (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    account_number VARCHAR(20) NOT NULL,
    account_name VARCHAR(200) NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    parent_account_id BIGINT REFERENCES chart_of_accounts(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_reconcilable BOOLEAN DEFAULT FALSE,
    description TEXT,
    opening_balance DECIMAL(15,2) DEFAULT 0.00,
    account_level INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_company_account UNIQUE(company_id, account_number)
);

CREATE INDEX idx_coa_company_number ON chart_of_accounts(company_id, account_number);
CREATE INDEX idx_coa_type ON chart_of_accounts(account_type);
CREATE INDEX idx_coa_active ON chart_of_accounts(is_active);

-- ============================================
-- TABLE: general_ledger
-- ============================================
CREATE TABLE general_ledger (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    account_id BIGINT NOT NULL REFERENCES chart_of_accounts(id),
    entry_date DATE NOT NULL,
    debit_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    credit_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    description TEXT,
    reference VARCHAR(100),
    journal_code VARCHAR(10),
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    period VARCHAR(7),
    fiscal_year VARCHAR(4),
    bank_transaction_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0,
    CONSTRAINT chk_debit_or_credit CHECK (
        (debit_amount > 0 AND credit_amount = 0) OR 
        (credit_amount > 0 AND debit_amount = 0)
    )
);

CREATE INDEX idx_gl_company_date ON general_ledger(company_id, entry_date);
CREATE INDEX idx_gl_account ON general_ledger(account_id);
CREATE INDEX idx_gl_reference ON general_ledger(reference);
CREATE INDEX idx_gl_locked ON general_ledger(is_locked);
CREATE INDEX idx_gl_period ON general_ledger(period);

-- ============================================
-- TABLE: bank_transactions
-- ============================================
CREATE TABLE bank_transactions (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    transaction_date DATE NOT NULL,
    value_date DATE,
    amount DECIMAL(15,2) NOT NULL,
    description TEXT,
    bank_reference VARCHAR(100),
    category VARCHAR(50),
    category_confidence DECIMAL(5,2),
    is_reconciled BOOLEAN NOT NULL DEFAULT FALSE,
    gl_entry_id BIGINT REFERENCES general_ledger(id),
    third_party_name VARCHAR(200),
    imported_at DATE,
    import_source VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_bank_company_date ON bank_transactions(company_id, transaction_date);
CREATE INDEX idx_bank_reconciled ON bank_transactions(is_reconciled);
CREATE INDEX idx_bank_category ON bank_transactions(category);

-- ============================================
-- TABLE: audit_logs
-- ============================================
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    user_id BIGINT,
    username VARCHAR(100),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    changes TEXT
);

CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);

-- ============================================
-- COMMENTAIRES
-- ============================================
COMMENT ON TABLE companies IS 'Entreprises clientes de PREDYKT';
COMMENT ON TABLE chart_of_accounts IS 'Plan comptable OHADA';
COMMENT ON TABLE general_ledger IS 'Grand livre comptable (écritures)';
COMMENT ON TABLE bank_transactions IS 'Transactions bancaires importées';
COMMENT ON TABLE audit_logs IS 'Piste d audit complète (conformité ISO 27001)';