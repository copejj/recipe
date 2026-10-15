
BEGIN;

-- 1. Insert master dataset with safe subquery foreign key lookups
-- Sets is_verified = TRUE so they populate your main creation pools instantly
INSERT INTO ingredients (ingredient_name, grocery_category_id, is_verified) VALUES
  ('Roma Tomatoes', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Produce & Fresh Vegetables'), true),
  ('Yellow Onions', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Produce & Fresh Vegetables'), true),
  ('Fresh Garlic', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Produce & Fresh Vegetables'), true),
  ('Baby Spinach', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Produce & Fresh Vegetables'), true),
  ('Carrots', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Produce & Fresh Vegetables'), true),
  ('Bell Peppers', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Produce & Fresh Vegetables'), true),
  ('Russet Potatoes', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Produce & Fresh Vegetables'), true),
  
  ('Granny Smith Apples', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Fruits & Berries'), true),
  ('Fresh Lemons', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Fruits & Berries'), true),
  ('Bananas', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Fruits & Berries'), true),
  ('Strawberries', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Fruits & Berries'), true),

  ('Boneless Chicken Breast', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Meat, Poultry & Seafood'), true),
  ('Lean Ground Beef', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Meat, Poultry & Seafood'), true),
  ('Thick-Cut Bacon', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Meat, Poultry & Seafood'), true),
  ('Atlantic Salmon Fillets', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Meat, Poultry & Seafood'), true),

  ('Whole Milk', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Dairy, Eggs & Plant-Based Milk'), true),
  ('Large Eggs', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Dairy, Eggs & Plant-Based Milk'), true),
  ('Unsalted Butter', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Dairy, Eggs & Plant-Based Milk'), true),
  ('Heavy Cream', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Dairy, Eggs & Plant-Based Milk'), true),
  ('Shredded Mozzarella', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Dairy, Eggs & Plant-Based Milk'), true),
  ('Grated Parmesan Cheese', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Dairy, Eggs & Plant-Based Milk'), true),

  ('All-Purpose Flour', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Baking Supplies, Flour & Sugars'), true),
  ('Granulated Sugar', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Baking Supplies, Flour & Sugars'), true),
  ('Light Brown Sugar', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Baking Supplies, Flour & Sugars'), true),
  ('Baking Powder', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Baking Supplies, Flour & Sugars'), true),
  ('Baking Soda', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Baking Supplies, Flour & Sugars'), true),
  ('Active Dry Yeast', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Baking Supplies, Flour & Sugars'), true),
  ('Pure Vanilla Extract', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Baking Supplies, Flour & Sugars'), true),
  ('Semisweet Chocolate Chips', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Baking Supplies, Flour & Sugars'), true),

  ('Kosher Salt', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Spices, Seasonings & Dried Herbs'), true),
  ('Black Peppercorns', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Spices, Seasonings & Dried Herbs'), true),
  ('Ground Cinnamon', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Spices, Seasonings & Dried Herbs'), true),
  ('Dried Oregano', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Spices, Seasonings & Dried Herbs'), true),
  ('Smoked Paprika', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Spices, Seasonings & Dried Herbs'), true),
  ('Garlic Powder', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Spices, Seasonings & Dried Herbs'), true),
  ('Onion Powder', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Spices, Seasonings & Dried Herbs'), true),
  ('Cayenne Pepper', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Spices, Seasonings & Dried Herbs'), true),

  ('Extra Virgin Olive Oil', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Oils, Vinegars & Condiments'), true),
  ('Canola Oil', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Oils, Vinegars & Condiments'), true),
  ('Apple Cider Vinegar', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Oils, Vinegars & Condiments'), true),
  ('Balsamic Vinegar', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Oils, Vinegars & Condiments'), true),
  ('Dijon Mustard', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Oils, Vinegars & Condiments'), true),
  ('Mayonnaise', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Oils, Vinegars & Condiments'), true),

  ('Long-Grain White Rice', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Grains, Pasta, Rice & Beans'), true),
  ('Jasmine Rice', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Grains, Pasta, Rice & Beans'), true),
  ('Spaghetti', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Grains, Pasta, Rice & Beans'), true),
  ('Penne Pasta', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Grains, Pasta, Rice & Beans'), true),
  ('Dried Black Beans', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Grains, Pasta, Rice & Beans'), true),

  ('Chicken Broth', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Canned Goods & Prepared Soups'), true),
  ('Tomato Paste', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Canned Goods & Prepared Soups'), true),
  ('Diced Tomatoes', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Canned Goods & Prepared Soups'), true),

  ('Raw Almonds', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Nuts, Seeds & Dried Fruits'), true),
  ('Walnuts', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Nuts, Seeds & Dried Fruits'), true),
  ('Chia Seeds', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'Nuts, Seeds & Dried Fruits'), true),

  ('Honey', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'International Foods & Specialty Ingredients'), true),
  ('Soy Sauce', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'International Foods & Specialty Ingredients'), true),
  ('Sriracha Hot Sauce', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'International Foods & Specialty Ingredients'), true),
  ('Coconut Milk', (SELECT grocery_category_id FROM grocery_category WHERE category_name = 'International Foods & Specialty Ingredients'), true)
ON CONFLICT DO NOTHING;

COMMIT;
