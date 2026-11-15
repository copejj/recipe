BEGIN;
-- 1. Reclassify existing SOLID items to DRY (and fix any obvious liquids if desired)
UPDATE ingredients SET classification = 'DRY' WHERE classification = 'SOLID';

-- 2. Optional: move liquid items currently marked SOLID/DRY to WET
UPDATE ingredients SET classification = 'WET' 
WHERE ingredient_id IN (50, 57, 58, 153, 156, 172, 194);

-- 3. Update the Postgres enum type to remove 'SOLID'
ALTER TABLE ingredients ALTER COLUMN classification DROP DEFAULT;
ALTER TABLE ingredients ALTER COLUMN classification TYPE VARCHAR(20);
DROP TYPE IF EXISTS ingredient_classification; -- replace with your actual Postgres enum type name
CREATE TYPE ingredient_classification AS ENUM ('WET', 'DRY');
ALTER TABLE ingredients ALTER COLUMN classification TYPE ingredient_classification USING classification::ingredient_classification;
ALTER TABLE ingredients ALTER COLUMN classification SET DEFAULT 'DRY';
COMMIT;