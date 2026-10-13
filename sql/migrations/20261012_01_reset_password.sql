BEGIN;
-- 1. Add the new password reset columns
ALTER TABLE users_auth 
    ADD COLUMN reset_password_token UUID,
    ADD COLUMN reset_password_expires_at TIMESTAMP WITH TIME ZONE;

-- 2. Index the new reset token column
CREATE INDEX idx_users_auth_reset_token ON users_auth (reset_password_token);

-- 3. Index your existing verification token column
CREATE INDEX idx_users_auth_verification_token ON users_auth (verification_token);
COMMIT;