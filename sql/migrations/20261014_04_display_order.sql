BEGIN;
-- 1. Add the column with a default value of 10
ALTER TABLE grocery_category 
ADD COLUMN display_order integer NOT NULL DEFAULT 10;

-- 2. Populate all existing rows to be their primary key multiplied by 10
UPDATE grocery_category 
SET display_order = grocery_category_id * 10;
COMMIT;