BEGIN;
-- Step 1: Drop the old, overly strict global unique constraint
ALTER TABLE ingredients 
DROP CONSTRAINT uniq_ingredient_name;

-- Step 2: Create a Regular-Expression based Partial Unique Index
-- 'g' stands for 'global' (replace all occurrences, not just the first one)
-- '\s' matches any whitespace character (space, tab, newline, carriage return)
CREATE UNIQUE INDEX uniq_verified_ingredient_name_canonical 
ON ingredients (lower(regexp_replace(ingredient_name, '\s+', '', 'g'))) 
WHERE (is_verified = TRUE);

COMMIT;