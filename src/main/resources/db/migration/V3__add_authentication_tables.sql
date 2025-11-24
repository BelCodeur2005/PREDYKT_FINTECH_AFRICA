-- ============================================
-- V3__add_authentication_tables.sql
-- Tables pour authentification et gestion des utilisateurs
-- ============================================

-- ============================================
-- TABLE: roles
-- ============================================
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Rôles par défaut
INSERT INTO roles (name, description) VALUES
    ('ROLE_ADMIN', 'Administrateur système - Accès complet'),
    ('ROLE_ACCOUNTANT', 'Comptable - Gestion écritures et rapports'),
    ('ROLE_MANAGER', 'Manager - Consultation rapports et analytics'),
    ('ROLE_VIEWER', 'Lecteur - Consultation uniquement');

-- ============================================
-- TABLE: users
-- ============================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_verification_token VARCHAR(255),
    password_reset_token VARCHAR(255),
    password_reset_expires_at TIMESTAMP,
    last_login_at TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_user_email_company UNIQUE(email, company_id)
);

CREATE INDEX idx_users_company ON users(company_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_active ON users(is_active);

-- ============================================
-- TABLE: user_roles
-- ============================================
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    granted_by VARCHAR(100),
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user ON user_roles(user_id);

-- ============================================
-- TABLE: jwt_tokens (Pour révocation)
-- ============================================
CREATE TABLE jwt_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    token_type VARCHAR(20) NOT NULL, -- ACCESS, REFRESH
    expires_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP,
    revoked_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address INET,
    user_agent VARCHAR(255)
);

CREATE INDEX idx_jwt_user ON jwt_tokens(user_id);
CREATE INDEX idx_jwt_token_hash ON jwt_tokens(token_hash);
CREATE INDEX idx_jwt_expires ON jwt_tokens(expires_at);
CREATE INDEX idx_jwt_revoked ON jwt_tokens(is_revoked);

-- Nettoyage automatique des tokens expirés (à exécuter quotidiennement)
-- DELETE FROM jwt_tokens WHERE expires_at < NOW() - INTERVAL '30 days';

-- ============================================
-- TABLE: user_sessions (Pour tracking)
-- ============================================
CREATE TABLE user_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_token VARCHAR(255) NOT NULL UNIQUE,
    ip_address INET,
    user_agent VARCHAR(255),
    device_info VARCHAR(255),
    location VARCHAR(100),
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_sessions_user ON user_sessions(user_id);
CREATE INDEX idx_sessions_active ON user_sessions(is_active);

-- ============================================
-- TABLE: permissions (Pour RBAC fin)
-- ============================================
CREATE TABLE permissions (
    id SERIAL PRIMARY KEY,
    code VARCHAR(100) UNIQUE NOT NULL,
    resource VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Permissions par défaut (exemples)
INSERT INTO permissions (code, resource, action, description) VALUES
    ('COMPANY_READ', 'Company', 'READ', 'Consulter les informations entreprise'),
    ('COMPANY_WRITE', 'Company', 'WRITE', 'Modifier les informations entreprise'),
    ('GL_READ', 'GeneralLedger', 'READ', 'Consulter les écritures comptables'),
    ('GL_WRITE', 'GeneralLedger', 'WRITE', 'Créer/Modifier écritures comptables'),
    ('GL_DELETE', 'GeneralLedger', 'DELETE', 'Supprimer écritures comptables'),
    ('GL_LOCK', 'GeneralLedger', 'LOCK', 'Verrouiller périodes comptables'),
    ('REPORTS_VIEW', 'Reports', 'READ', 'Consulter les rapports financiers'),
    ('USERS_MANAGE', 'Users', 'MANAGE', 'Gérer les utilisateurs'),
    ('IMPORT_DATA', 'Import', 'WRITE', 'Importer des données');

-- ============================================
-- TABLE: role_permissions
-- ============================================
CREATE TABLE role_permissions (
    role_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id INTEGER NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Attribution des permissions par rôle
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ROLE_ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p 
WHERE r.name = 'ROLE_ACCOUNTANT' 
  AND p.code IN ('COMPANY_READ', 'GL_READ', 'GL_WRITE', 'REPORTS_VIEW', 'IMPORT_DATA');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p 
WHERE r.name = 'ROLE_MANAGER' 
  AND p.code IN ('COMPANY_READ', 'GL_READ', 'REPORTS_VIEW');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p 
WHERE r.name = 'ROLE_VIEWER' 
  AND p.code IN ('COMPANY_READ', 'GL_READ', 'REPORTS_VIEW');

-- ============================================
-- VUES UTILITAIRES
-- ============================================

-- Vue: Utilisateurs avec leurs rôles
CREATE OR REPLACE VIEW v_users_with_roles AS
SELECT 
    u.id,
    u.email,
    u.first_name,
    u.last_name,
    u.company_id,
    c.name AS company_name,
    u.is_active,
    u.last_login_at,
    STRING_AGG(r.name, ', ') AS roles
FROM users u
JOIN companies c ON u.company_id = c.id
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
GROUP BY u.id, u.email, u.first_name, u.last_name, u.company_id, c.name, u.is_active, u.last_login_at;

-- Vue: Sessions actives
CREATE OR REPLACE VIEW v_active_sessions AS
SELECT 
    s.id,
    u.email,
    u.first_name,
    u.last_name,
    c.name AS company_name,
    s.ip_address,
    s.started_at,
    s.last_activity_at,
    EXTRACT(EPOCH FROM (NOW() - s.last_activity_at))/60 AS idle_minutes
FROM user_sessions s
JOIN users u ON s.user_id = u.id
JOIN companies c ON u.company_id = c.id
WHERE s.is_active = TRUE
ORDER BY s.last_activity_at DESC;

-- ============================================
-- TRIGGERS
-- ============================================

-- Trigger: Verrouiller compte après 5 tentatives échouées
CREATE OR REPLACE FUNCTION lock_user_after_failed_attempts()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.failed_login_attempts >= 5 THEN
        NEW.locked_until := NOW() + INTERVAL '30 minutes';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_lock_user
BEFORE UPDATE ON users
FOR EACH ROW
WHEN (NEW.failed_login_attempts >= 5 AND OLD.failed_login_attempts < 5)
EXECUTE FUNCTION lock_user_after_failed_attempts();

-- Trigger: Déverrouiller automatiquement si délai expiré
CREATE OR REPLACE FUNCTION auto_unlock_user()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.locked_until IS NOT NULL AND NEW.locked_until < NOW() THEN
        NEW.failed_login_attempts := 0;
        NEW.locked_until := NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_auto_unlock
BEFORE UPDATE ON users
FOR EACH ROW
WHEN (NEW.locked_until IS NOT NULL)
EXECUTE FUNCTION auto_unlock_user();

-- ============================================
-- COMMENTAIRES
-- ============================================
COMMENT ON TABLE users IS 'Utilisateurs du système (multi-tenant)';
COMMENT ON TABLE roles IS 'Rôles de sécurité (RBAC)';
COMMENT ON TABLE permissions IS 'Permissions granulaires par ressource';
COMMENT ON TABLE jwt_tokens IS 'Tokens JWT pour révocation (logout, sécurité)';
COMMENT ON TABLE user_sessions IS 'Sessions utilisateurs actives (tracking)';
COMMENT ON VIEW v_users_with_roles IS 'Vue consolidée utilisateurs + rôles';
COMMENT ON VIEW v_active_sessions IS 'Sessions actives avec durée inactivité';