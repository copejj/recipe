BEGIN;

ALTER TABLE blacklisted_domains RENAME TO blacklist;

-- 1. Rename your old domain tracking column to match the new 'pattern' variable
ALTER TABLE blacklist RENAME COLUMN domain_name TO pattern;

-- 2. Add the 'type' column to handle IP, SUBNET, and DOMAIN categorization
ALTER TABLE blacklist ADD COLUMN type VARCHAR(50);

-- 3. Backfill any existing domain records so they don't break NOT NULL constraints
UPDATE blacklist SET type = 'DOMAIN' WHERE type IS NULL;

-- 4. Enforce the NOT NULL constraint on the type column now that it is backfilled
ALTER TABLE blacklist ALTER COLUMN type SET NOT NULL;

-- 5. Add the 'reason' column so you can track notes from your admin dashboard
ALTER TABLE blacklist ADD COLUMN reason VARCHAR(255);

COMMIT;
