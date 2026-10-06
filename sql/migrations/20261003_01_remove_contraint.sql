BEGIN;
ALTER TABLE public.recipes 
DROP CONSTRAINT recipes_user_id_key;
COMMIT;
