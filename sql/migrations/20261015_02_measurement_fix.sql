BEGIN;
-- 1. Properly mark Metric-system volume dimensions
UPDATE measurements 
SET is_metric = true, is_promotable = true 
WHERE measurement_id IN (1, 8); -- Milliliter, Liter

-- 2. Properly mark Metric-system weight dimensions
UPDATE measurements 
SET is_metric = true, is_promotable = true 
WHERE measurement_id IN (10, 13); -- Gram, Kilogram

-- 3. Ensure your continuous base scaling units cannot be skipped by promotion loops
UPDATE measurements 
SET is_promotable = true 
WHERE measurement_id IN (2, 11); -- Teaspoon, Ounce (Rank 1 Fallbacks)
COMMIT;