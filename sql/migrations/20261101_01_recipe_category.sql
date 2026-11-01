BEGIN;

-- 1. Create the recipe_categories table
CREATE TABLE recipe_categories (
    recipe_category_id SERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

-- 2. Populate initial taxonomy lookup data
INSERT INTO recipe_categories (name) VALUES 
('Breakfast & Brunch'), 
('Lunch'), 
('Dinner'), 
('Appetizers & Starters'), 
('Side Dishes'), 
('Soups & Stews'), 
('Salads'), 
('Desserts'), 
('Snacks'), 
('Drinks & Beverages'), 
('Sauces, Dips & Dressings'),
('Vegetarian'), 
('Vegan'), 
('Gluten-Free'), 
('Keto & Low-Carb'), 
('Dairy-Free'), 
('Paleo'), 
('Halal'), 
('Kosher');

-- 3. Modify the recipes table to add the matching singular foreign key column
ALTER TABLE recipes 
ADD COLUMN recipe_category_id INT;

-- 4. Add the Foreign Key constraint pointing to the correct plural table
ALTER TABLE recipes 
ADD CONSTRAINT fk_recipes_recipe_categories 
FOREIGN KEY (recipe_category_id) REFERENCES recipe_categories(recipe_category_id);

COMMIT;
