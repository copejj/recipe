BEGIN;
-- Step A: Drop the existing composite primary key
ALTER TABLE public.user_roles 
    DROP CONSTRAINT user_roles_pkey;

-- Step B: Add the new auto-incrementing surrogate primary key column
ALTER TABLE public.user_roles 
    ADD COLUMN user_role_id serial NOT NULL;

-- Step C: Set the new column as the official Primary Key
ALTER TABLE public.user_roles 
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (user_role_id);

-- Step D: Enforce a unique constraint so a user cannot have the same role duplicated
ALTER TABLE public.user_roles 
    ADD CONSTRAINT uq_user_roles_user_id_role_id UNIQUE (user_id, role_id);


-- Step A: Drop the existing composite primary key
ALTER TABLE public.recipe_step_ingredient 
    DROP CONSTRAINT recipe_step_ingredient_id;

-- Step B: Add the new auto-incrementing surrogate primary key column
ALTER TABLE public.recipe_step_ingredient 
    ADD COLUMN recipe_step_ingredient_id serial NOT NULL;

-- Step C: Set the new column as the official Primary Key
ALTER TABLE public.recipe_step_ingredient 
    ADD CONSTRAINT recipe_step_ingredient_pkey PRIMARY KEY (recipe_step_ingredient_id);
Commit;