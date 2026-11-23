BEGIN;

-- 1. Temporarily drop the FK so we can truncate & reset sequence to 1 safely
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_profile_visibility_id_fkey;

TRUNCATE TABLE profile_visibility RESTART IDENTITY;

INSERT INTO profile_visibility (profile_visibility_id, visibility_name, description) VALUES
    (1, 'PUBLIC', 'Profile is visible to anyone on the web'),
    (2, 'PRIVATE', 'Profile is hidden from all directory searches'),
    (3, 'ONSITE', 'Profile is visible only to registered users');

-- Advance sequence past 3
SELECT setval('profile_visiblities_profile_visibilitiy_id_seq', 3, true);

-- 2. Set default on users table to 3 (ONSITE) and normalize any invalid IDs
ALTER TABLE users
    ALTER COLUMN profile_visibility_id SET DEFAULT 3;

UPDATE users
SET profile_visibility_id = 3
WHERE profile_visibility_id NOT IN (1, 2, 3);

-- 3. Restore the foreign key constraint
ALTER TABLE users
    ADD CONSTRAINT users_profile_visibility_id_fkey
    FOREIGN KEY (profile_visibility_id) REFERENCES profile_visibility(profile_visibility_id);

COMMIT;