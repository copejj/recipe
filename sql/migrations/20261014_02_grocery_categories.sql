BEGIN;
INSERT INTO grocery_category (grocery_category_id, category_name) VALUES
  (1, 'Produce & Fresh Vegetables'),
  (2, 'Fruits & Berries'),
  (3, 'Meat, Poultry & Seafood'),
  (4, 'Dairy, Eggs & Plant-Based Milk'),
  (5, 'Bakery & Fresh Bread'),
  (6, 'Baking Supplies, Flour & Sugars'),
  (7, 'Spices, Seasonings & Dried Herbs'),
  (8, 'Oils, Vinegars & Condiments'),
  (9, 'Grains, Pasta, Rice & Beans'),
  (10, 'Canned Goods & Prepared Soups'),
  (11, 'Nuts, Seeds & Dried Fruits'),
  (12, 'Snacks, Crackers & Confectionery'),
  (13, 'Beverages, Coffee & Teas'),
  (14, 'Frozen Foods'),
  (15, 'International Foods & Specialty Ingredients')
ON CONFLICT (grocery_category_id) 
DO UPDATE SET category_name = EXCLUDED.category_name;

-- Reset the underlying serial sequence context generator so future auto-generated IDs do not collide
SELECT setval(pg_get_serial_sequence('grocery_category', 'grocery_category_id'), COALESCE(MAX(grocery_category_id), 1)) FROM grocery_category;

COMMIT;