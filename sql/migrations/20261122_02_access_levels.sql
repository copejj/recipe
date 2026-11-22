BEGIN;

-- 1. Seed access_levels if empty
INSERT INTO access_levels (access_name, description) VALUES
    ('VIEW', 'Can view the meal plan and check off shopping list items'),
    ('EDIT', 'Can add/remove recipes, schedule meals, and manage shopping list items')
ON CONFLICT (access_name) DO NOTHING;

-- 2. Add an "Other / Miscellaneous" grocery aisle at the end of the store
INSERT INTO grocery_category (category_name, display_order) VALUES
    ('Other / Miscellaneous', 999)
ON CONFLICT (category_name) DO NOTHING;

COMMIT;
