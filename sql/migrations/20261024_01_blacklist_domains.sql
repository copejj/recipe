BEGIN;
-- 1. Base Table: Storage engine for your blacklisted spam vectors
CREATE TABLE blacklisted_domains (
    id BIGSERIAL PRIMARY KEY,
    domain_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL,
    notes TEXT,
    CONSTRAINT uk_blacklisted_domain_name UNIQUE (domain_name)
);

-- 2. Performance Optimization: Enforces lower-case index normalization 
-- to protect your Spring JPA repository from lookup lag as your list grows.
CREATE INDEX idx_blacklisted_domain_lower 
ON blacklisted_domains (LOWER(domain_name));

-- 3. Standard Entity View: Aligned cleanly with Spring Data JPA mapping conventions
CREATE OR REPLACE VIEW v_active_blacklisted_domains AS
SELECT 
    id,
    LOWER(TRIM(domain_name)) AS domain_name,
    created_at
FROM 
    blacklisted_domains;

-- 4. Analytics & Operational View: Quickly tracks volume trends for your reports
CREATE OR REPLACE VIEW v_blacklist_summary_metrics AS
SELECT 
    COUNT(id) AS total_blocked_domains,
    MAX(created_at) AS last_updated_timestamp,
    CURRENT_DATE - MIN(created_at)::date AS days_active
FROM 
    blacklisted_domains;

-- 5. Seed Core Dataset: Instantly blocks the active threat vectors hitting your domain
INSERT INTO blacklisted_domains (domain_name, notes) VALUES 
('formtests.info', 'Primary bot registry spam vector blocking Spectrum outbound stream'),
('mailinator.com', 'Common anonymous disposable mail destination'),
('trashmail.com', 'Common disposable temporary routing platform'),
('tempmail.com', 'Volatile validation farm domain used for script attacks')
ON CONFLICT (domain_name) DO NOTHING;
COMMIT;
