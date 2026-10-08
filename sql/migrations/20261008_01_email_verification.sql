begin:
ALTER TABLE users_auth
ADD COLUMN is_verified BOOLEAN DEFAULT FALSE,
ADD COLUMN verification_token UUID DEFAULT NULL;

UPDATE users_auth SET is_verified = true WHERE user_id = 1;
commit;