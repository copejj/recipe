BEGIN;
update measurements
set is_promotable = true
where unit_name = 'Pint';

-- Step 1: Add the plural_name column to the measurements tracking table
ALTER TABLE measurements ADD COLUMN plural_name VARCHAR(255);
-- Step 2: Batch update all 20 structural units with their correct plural variations
UPDATE measurements
SET plural_name = CASE measurement_id
    WHEN 1  THEN 'Milliliters'
    WHEN 2  THEN 'Teaspoons'
    WHEN 3  THEN 'Tablespoons'
    WHEN 4  THEN 'Fluid Ounces'
    WHEN 5  THEN 'Cups'
    WHEN 6  THEN 'Pints'
    WHEN 7  THEN 'Quarts'
    WHEN 8  THEN 'Liters'
    WHEN 9  THEN 'Gallons'
    WHEN 10 THEN 'Grams'
    WHEN 11 THEN 'Ounces'
    WHEN 12 THEN 'Pounds'
    WHEN 13 THEN 'Kilograms'
    WHEN 14 THEN 'Whole'       -- Kept identical to match singular fallbacks drops
    WHEN 15 THEN 'Slices'
    WHEN 16 THEN 'Cloves'
    WHEN 17 THEN 'Cans'
    WHEN 18 THEN 'Pinches'
    WHEN 19 THEN 'Splashes'
    WHEN 20 THEN 'To Taste'     -- Descriptive metric that ignores standard suffix rules
END
WHERE measurement_id IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20);

COMMIT;