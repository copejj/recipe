BEGIN;
-- Make the foreign key column optional so we can track untrusted inputs
ALTER TABLE login_history ALTER COLUMN user_auth_id DROP NOT NULL;
ALTER TABLE login_history ADD COLUMN attempted_email TEXT DEFAULT NULL;
COMMIT;
