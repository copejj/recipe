BEGIN;
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'ingredient_classification') THEN
        CREATE TYPE ingredient_classification AS ENUM ('WET', 'DRY', 'SOLID');
    END IF;
END $$;

-- 2. Inject the classification tracking field, defaulting to safe 'SOLID'
ALTER TABLE IF EXISTS ingredients
    ADD COLUMN IF NOT EXISTS classification ingredient_classification NOT NULL DEFAULT 'SOLID';

-- 3. Execute precise category data migrations
-- Map the Liquid/Fluid Volume Track (WET)
UPDATE ingredients
SET classification = 'WET'::ingredient_classification
WHERE grocery_category_id IN (4, 8, 13);

-- Map the Gated Volumetric Dry Track (DRY)
UPDATE ingredients
SET classification = 'DRY'::ingredient_classification
WHERE grocery_category_id IN (6, 7, 9, 11);

-- Map the Discrete Counts & Absolute Mass Weight Track (SOLID)
UPDATE ingredients
SET classification = 'SOLID'::ingredient_classification
WHERE grocery_category_id IN (1, 2, 3, 5, 10, 12, 14, 15, 16);
COMMIT;