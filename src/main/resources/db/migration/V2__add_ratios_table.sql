-- ============================================
-- V2__add_ratios_and_projections.sql
-- Tables pour ratios financiers et prévisions
-- ============================================

-- ============================================
-- TABLE: financial_ratios
-- ============================================
CREATE TABLE financial_ratios (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    fiscal_year VARCHAR(4) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    
    -- Rentabilité
    gross_margin_pct DECIMAL(5,2),          -- Marge brute %
    net_margin_pct DECIMAL(5,2),            -- Marge nette %
    roa_pct DECIMAL(5,2),                   -- Return on Assets %
    roe_pct DECIMAL(5,2),                   -- Return on Equity %
    
    -- Liquidité
    current_ratio DECIMAL(5,2),             -- Ratio de liquidité générale
    quick_ratio DECIMAL(5,2),               -- Ratio de liquidité réduite
    cash_ratio DECIMAL(5,2),                -- Ratio de liquidité immédiate
    
    -- Solvabilité
    debt_ratio_pct DECIMAL(5,2),            -- Taux d'endettement %
    debt_to_equity DECIMAL(5,2),            -- Dette/Capitaux propres
    interest_coverage DECIMAL(5,2),         -- Couverture des intérêts
    
    -- Activité
    asset_turnover DECIMAL(5,2),            -- Rotation des actifs
    inventory_turnover DECIMAL(5,2),        -- Rotation des stocks
    receivables_turnover DECIMAL(5,2),      -- Rotation des créances
    
    -- Délais moyens (en jours)
    dso_days INTEGER,                       -- Délai moyen de recouvrement
    dio_days INTEGER,                       -- Délai moyen de stockage
    dpo_days INTEGER,                       -- Délai moyen de paiement
    cash_conversion_cycle INTEGER,          -- Cycle de conversion de trésorerie
    
    -- Données brutes pour calculs
    total_revenue DECIMAL(15,2),
    total_expenses DECIMAL(15,2),
    net_income DECIMAL(15,2),
    total_assets DECIMAL(15,2),
    total_equity DECIMAL(15,2),
    total_debt DECIMAL(15,2),
    working_capital DECIMAL(15,2),          -- BFR (Besoin en Fonds de Roulement)
    
    calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0,
    
    CONSTRAINT uk_company_fiscal_year UNIQUE(company_id, fiscal_year, period_end)
);

CREATE INDEX idx_ratios_company_year ON financial_ratios(company_id, fiscal_year);
CREATE INDEX idx_ratios_period ON financial_ratios(period_start, period_end);

-- ============================================
-- TABLE: cash_flow_projections
-- ============================================
CREATE TABLE cash_flow_projections (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    projection_date DATE NOT NULL,
    projection_horizon INTEGER NOT NULL,    -- J+30, J+60, J+90
    
    -- Soldes
    opening_balance DECIMAL(15,2) NOT NULL,
    projected_balance DECIMAL(15,2) NOT NULL,
    
    -- Flux entrants prévus
    projected_inflows DECIMAL(15,2) DEFAULT 0.00,
    receivables_collection DECIMAL(15,2) DEFAULT 0.00,
    other_income DECIMAL(15,2) DEFAULT 0.00,
    
    -- Flux sortants prévus
    projected_outflows DECIMAL(15,2) DEFAULT 0.00,
    payables_payment DECIMAL(15,2) DEFAULT 0.00,
    payroll_payment DECIMAL(15,2) DEFAULT 0.00,
    tax_payment DECIMAL(15,2) DEFAULT 0.00,
    other_expenses DECIMAL(15,2) DEFAULT 0.00,
    
    -- Métadonnées ML
    model_used VARCHAR(50),                 -- ARIMA, Prophet, etc.
    confidence_score DECIMAL(5,2),          -- Score de confiance 0-100
    prediction_details JSONB,               -- Détails du modèle ML (format JSON)
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_projection_company_date ON cash_flow_projections(company_id, projection_date);
CREATE INDEX idx_projection_horizon ON cash_flow_projections(projection_horizon);

-- ============================================
-- TABLE: budgets (pour Phase II)
-- ============================================
CREATE TABLE budgets (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    fiscal_year VARCHAR(4) NOT NULL,
    account_id BIGINT REFERENCES chart_of_accounts(id),
    
    budget_type VARCHAR(20) NOT NULL,       -- ANNUAL, QUARTERLY, MONTHLY
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    
    budgeted_amount DECIMAL(15,2) NOT NULL,
    actual_amount DECIMAL(15,2) DEFAULT 0.00,
    variance DECIMAL(15,2) DEFAULT 0.00,
    variance_pct DECIMAL(5,2) DEFAULT 0.00,
    
    is_locked BOOLEAN DEFAULT FALSE,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0,
    
    CONSTRAINT uk_budget_period UNIQUE(company_id, account_id, period_start, period_end)
);

CREATE INDEX idx_budget_company_year ON budgets(company_id, fiscal_year);
CREATE INDEX idx_budget_account ON budgets(account_id);

-- ============================================
-- TABLE: imported_activities (pour CSV)
-- ============================================
CREATE TABLE imported_activities (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    import_batch_id UUID NOT NULL,          -- Identifiant unique du lot d'import
    
    entry_date DATE NOT NULL,
    activity_type VARCHAR(100),
    description TEXT,
    amount DECIMAL(15,2) NOT NULL,
    transaction_type VARCHAR(20),           -- Revenu, Dépenses, Capex, Financing
    fiscal_year VARCHAR(4),
    
    -- Mapping vers le GL
    is_processed BOOLEAN DEFAULT FALSE,
    gl_entry_id BIGINT REFERENCES general_ledger(id),
    account_number VARCHAR(20),
    
    -- Métadonnées import
    import_source VARCHAR(50),
    imported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    imported_by VARCHAR(100),
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_activities_company_batch ON imported_activities(company_id, import_batch_id);
CREATE INDEX idx_activities_date ON imported_activities(entry_date);
CREATE INDEX idx_activities_processed ON imported_activities(is_processed);

-- ============================================
-- VUES UTILITAIRES
-- ============================================

-- Vue: Ratios historiques par année
CREATE OR REPLACE VIEW v_ratios_history AS
SELECT 
    c.id AS company_id,
    c.name AS company_name,
    fr.fiscal_year,
    fr.gross_margin_pct,
    fr.net_margin_pct,
    fr.roa_pct,
    fr.roe_pct,
    fr.current_ratio,
    fr.debt_ratio_pct,
    fr.dso_days,
    fr.total_revenue,
    fr.net_income,
    fr.calculated_at
FROM financial_ratios fr
JOIN companies c ON fr.company_id = c.id
ORDER BY c.id, fr.fiscal_year DESC;

-- Vue: Projection de trésorerie la plus récente
CREATE OR REPLACE VIEW v_latest_cash_projection AS
SELECT DISTINCT ON (company_id, projection_horizon)
    company_id,
    projection_date,
    projection_horizon,
    opening_balance,
    projected_balance,
    projected_inflows,
    projected_outflows,
    confidence_score,
    created_at
FROM cash_flow_projections
ORDER BY company_id, projection_horizon, projection_date DESC, created_at DESC;

-- ============================================
-- COMMENTAIRES
-- ============================================
COMMENT ON TABLE financial_ratios IS 'Ratios financiers calculés (ROA, ROE, Liquidité, etc.)';
COMMENT ON TABLE cash_flow_projections IS 'Prévisions de trésorerie (J+30/60/90) générées par IA';
COMMENT ON TABLE budgets IS 'Budgets prévisionnels et suivi des écarts';
COMMENT ON TABLE imported_activities IS 'Données brutes importées depuis CSV avant mapping';
COMMENT ON VIEW v_ratios_history IS 'Historique des ratios financiers par entreprise';
COMMENT ON VIEW v_latest_cash_projection IS 'Dernières projections de trésorerie';