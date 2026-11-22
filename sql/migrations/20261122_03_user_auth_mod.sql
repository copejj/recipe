BEGIN;
ALTER TABLE IF EXISTS users_auth
    ADD COLUMN IF NOT EXISTS verification_sent_at timestamp with time zone;
COMMIT;