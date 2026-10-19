BEGIN;
-- 1. Remove the strict blocker constraint
ALTER TABLE recipe_step_ingredient 
DROP CONSTRAINT IF EXISTS recipe_step_ingredient_recipe_id_fkey;

-- 2. Add it back with instructions to clear out smoothly
ALTER TABLE recipe_step_ingredient 
ADD CONSTRAINT recipe_step_ingredient_recipe_id_fkey 
FOREIGN KEY (recipe_id) REFERENCES recipes (recipe_id) 
ON DELETE CASCADE;
COMMIT;