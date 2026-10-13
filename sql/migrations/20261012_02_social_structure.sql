BEGIN;
-- 1. Establish Recipe Lineage (Tracking copies/forks of original recipes)
ALTER TABLE public.recipes 
ADD COLUMN parent_recipe_id INTEGER NULL,
ADD CONSTRAINT fk_recipes_parent 
    FOREIGN KEY (parent_recipe_id) 
    REFERENCES public.recipes (recipe_id) 
    ON DELETE SET NULL;

-- 2. Build the Meal Planning Domain Tables
CREATE TABLE public.meal_plans (
    meal_plan_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    plan_name TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_meal_plans_user FOREIGN KEY (user_id) REFERENCES public.users (user_id) ON DELETE CASCADE
);

CREATE TABLE public.meal_plan_recipes (
    meal_plan_recipe_id SERIAL PRIMARY KEY,
    meal_plan_id INTEGER NOT NULL,
    recipe_id INTEGER NOT NULL,
    day_of_week SMALLINT NOT NULL, -- 1 = Monday, 7 = Sunday etc.
    meal_type TEXT NOT NULL,       -- 'Breakfast', 'Lunch', 'Dinner', 'Snack'
    sort_order SMALLINT NOT NULL DEFAULT 1,
    CONSTRAINT fk_mpr_plan FOREIGN KEY (meal_plan_id) REFERENCES public.meal_plans (meal_plan_id) ON DELETE CASCADE,
    CONSTRAINT fk_mpr_recipe FOREIGN KEY (recipe_id) REFERENCES public.recipes (recipe_id) ON DELETE CASCADE
);

-- 3. Build Social Blocking Architecture
CREATE TABLE public.user_blocks (
    block_id SERIAL PRIMARY KEY,
    blocking_user_id INTEGER NOT NULL,
    blocked_user_id INTEGER NOT NULL,
    blocked_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_block_initiator FOREIGN KEY (blocking_user_id) REFERENCES public.users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_block_target FOREIGN KEY (blocked_user_id) REFERENCES public.users (user_id) ON DELETE CASCADE,
    CONSTRAINT unique_block_pair UNIQUE (blocking_user_id, blocked_user_id)
);
COMMIT;