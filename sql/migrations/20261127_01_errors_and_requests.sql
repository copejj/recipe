BEGIN;

-- 1. Automatic Error Telemetry Table
CREATE TABLE IF NOT EXISTS error_logs (
    error_log_id      SERIAL PRIMARY KEY,
    reference_code    VARCHAR(16) NOT NULL UNIQUE,
    status_code       INTEGER NOT NULL,
    request_uri       TEXT,
    http_method       VARCHAR(16),
    exception_class   TEXT,
    exception_message TEXT,
    stack_trace       TEXT,
    user_id           INTEGER REFERENCES users(user_id) ON DELETE SET NULL,
    user_agent        TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_error_logs_created_at ON error_logs (created_at DESC);

-- 2. User Feedback, Suggestions & Error Reports Table
CREATE TABLE IF NOT EXISTS user_feedback (
    feedback_id       SERIAL PRIMARY KEY,
    feedback_type     VARCHAR(24) NOT NULL, -- 'BUG', 'SUGGESTION', 'ERROR_REPORT'
    status            VARCHAR(24) NOT NULL DEFAULT 'OPEN', -- 'OPEN', 'IN_PROGRESS', 'RESOLVED', 'DISMISSED'
    subject           VARCHAR(200),
    description       TEXT NOT NULL,
    page_url          TEXT,
    user_agent        TEXT,
    error_log_id      INTEGER REFERENCES error_logs(error_log_id) ON DELETE SET NULL,
    submitted_by_id   INTEGER REFERENCES users(user_id) ON DELETE SET NULL,
    admin_notes       TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_feedback_status_created ON user_feedback (status, created_at DESC);

-- 3. "What's New" Changelog Entries Table
CREATE TABLE IF NOT EXISTS changelog_entries (
    changelog_id      SERIAL PRIMARY KEY,
    title             VARCHAR(200) NOT NULL,
    summary           TEXT NOT NULL,
    category          VARCHAR(24) NOT NULL DEFAULT 'NEW', -- 'NEW', 'IMPROVED', 'FIXED'
    version_tag       VARCHAR(40),
    published_date    DATE NOT NULL DEFAULT CURRENT_DATE,
    is_published      BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_id     INTEGER REFERENCES users(user_id) ON DELETE SET NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_changelog_published_date ON changelog_entries (is_published, published_date DESC, changelog_id DESC);

COMMIT;
