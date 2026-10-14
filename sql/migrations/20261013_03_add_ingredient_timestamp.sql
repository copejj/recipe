BEGIN;

-- Add the missing timestamp column with a safe default value matching today's runtime date
ALTER TABLE ingredients ADD COLUMN created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();

COMMIT;
