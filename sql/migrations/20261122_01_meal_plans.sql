BEGIN;

-- 1. Update meal_plans for calendar dates, recurrence, and public share links
ALTER TABLE meal_plans
    ADD COLUMN public_id uuid DEFAULT gen_random_uuid() NOT NULL UNIQUE,
    ADD COLUMN description text,
    ADD COLUMN start_date date,
    ADD COLUMN is_recurring boolean DEFAULT false NOT NULL;

-- 2. Update meal_plan_recipes: nullable day_of_week/meal_type acts as the unscheduled "Recipe Pool"
ALTER TABLE meal_plan_recipes
    ALTER COLUMN day_of_week DROP NOT NULL,
    ALTER COLUMN meal_type DROP NOT NULL,
    ADD COLUMN scheduled_date date,
    ADD COLUMN servings_override integer;

-- 3. Meal Plan Sharing (mirrors recipe_shares)
CREATE TABLE meal_plan_shares (
    meal_plan_share_id serial PRIMARY KEY,
    meal_plan_id integer NOT NULL REFERENCES meal_plans(meal_plan_id) ON DELETE CASCADE,
    shared_user_id integer REFERENCES users(user_id) ON DELETE CASCADE,
    shared_token_id uuid,
    access_level_id integer NOT NULL REFERENCES access_levels(access_level_id),
    shared_at timestamp with time zone DEFAULT now() NOT NULL
);

-- 4. Persistent Shopping Lists
CREATE TABLE shopping_lists (
    shopping_list_id serial PRIMARY KEY,
    meal_plan_id integer REFERENCES meal_plans(meal_plan_id) ON DELETE SET NULL,
    owner_user_id integer NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    list_name text NOT NULL,
    public_id uuid DEFAULT gen_random_uuid() NOT NULL UNIQUE,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

-- 5. Shopping List Items (aggregated recipe ingredients + extra custom store items)
CREATE TABLE shopping_list_items (
    shopping_list_item_id serial PRIMARY KEY,
    shopping_list_id integer NOT NULL REFERENCES shopping_lists(shopping_list_id) ON DELETE CASCADE,
    ingredient_id integer REFERENCES ingredients(ingredient_id) ON DELETE SET NULL,
    custom_item_name text,
    grocery_category_id integer NOT NULL DEFAULT 1 REFERENCES grocery_category(grocery_category_id),
    amount numeric(8,2),
    measurement_id integer REFERENCES measurements(measurement_id),
    note text,
    is_checked boolean DEFAULT false NOT NULL,
    is_manual_addition boolean DEFAULT false NOT NULL,
    added_by_user_id integer REFERENCES users(user_id) ON DELETE SET NULL,
    sort_order integer DEFAULT 1 NOT NULL
);

-- Match your existing role grants
GRANT SELECT, INSERT, UPDATE, DELETE, TRUNCATE ON TABLE
    meal_plan_shares,
    shopping_lists,
    shopping_list_items
TO recipe_app_user;

GRANT SELECT, USAGE ON SEQUENCE
    meal_plan_shares_meal_plan_share_id_seq,
    shopping_lists_shopping_list_id_seq,
    shopping_list_items_shopping_list_item_id_seq
TO recipe_app_user;

COMMIT;
