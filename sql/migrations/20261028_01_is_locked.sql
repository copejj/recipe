BEGIN;
ALTER TABLE ingredients 
ADD COLUMN is_locked BOOLEAN NOT NULL DEFAULT FALSE;

-- Optional: Index this column since your queue query will filter by it
CREATE INDEX idx_ingredients_is_locked ON ingredients(is_locked);
COMMIT;