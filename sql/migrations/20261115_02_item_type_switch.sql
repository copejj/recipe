BEGIN;

-- 1. Move liquids currently marked DRY to WET
UPDATE ingredients
SET classification = 'WET'
WHERE ingredient_id IN (28, 156);

-- 2. Move solid cheeses and fresh produce currently marked WET to DRY
UPDATE ingredients
SET classification = 'DRY'
WHERE ingredient_id IN (20, 21, 110, 111, 164, 184, 185, 188);

COMMIT;