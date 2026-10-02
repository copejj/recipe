BEGIN;

-- Clean out previous test entries to prevent constraint conflicts
TRUNCATE TABLE measurements CASCADE;

-- Populate your system with your explicit types
INSERT INTO measurements (unit_name, abbreviation, base_type_id, base_unit_equivalent, scaling_rank) VALUES 
-- VOLUME UNITS (Base Metric Unit: Milliliter)
('Milliliter',      'ml',      (SELECT base_type_id FROM base_type WHERE type_name = 'volume'), 1.000000,   1),
('Teaspoon',        'tsp',     (SELECT base_type_id FROM base_type WHERE type_name = 'volume'), 4.928922,   2),
('Tablespoon',      'tbsp',    (SELECT base_type_id FROM base_type WHERE type_name = 'volume'), 14.786765,  3),
('Fluid Ounce',     'fl oz',   (SELECT base_type_id FROM base_type WHERE type_name = 'volume'), 29.573530,  4),
('Cup',             'c',       (SELECT base_type_id FROM base_type WHERE type_name = 'volume'), 236.588237, 5),
('Pint',            'pt',      (SELECT base_type_id FROM base_type WHERE type_name = 'volume'), 473.176473, 6),
('Quart',           'qt',      (SELECT base_type_id FROM base_type WHERE type_name = 'volume'), 946.352946, 7),
('Liter',           'l',       (SELECT base_type_id FROM base_type WHERE type_name = 'volume'), 1000.000000,8),
('Gallon',          'gal',     (SELECT base_type_id FROM base_type WHERE type_name = 'volume'), 3785.411784,9),

-- WEIGHT UNITS (Base Metric Unit: Gram)
('Gram',            'g',       (SELECT base_type_id FROM base_type WHERE type_name = 'weight'), 1.000000,   1),
('Ounce',           'oz',      (SELECT base_type_id FROM base_type WHERE type_name = 'weight'), 28.349523,  2),
('Pound',           'lb',      (SELECT base_type_id FROM base_type WHERE type_name = 'weight'), 453.592370, 3),
('Kilogram',        'kg',      (SELECT base_type_id FROM base_type WHERE type_name = 'weight'), 1000.000000,4),

-- DISCRETE / COUNT UNITS (No baseline crossover dimensions)
('Whole',           'pc',      (SELECT base_type_id FROM base_type WHERE type_name = 'count'),  1.000000,   1),
('Slice',           'sl',      (SELECT base_type_id FROM base_type WHERE type_name = 'count'),  1.000000,   2),
('Clove',           'clove',   (SELECT base_type_id FROM base_type WHERE type_name = 'count'),  1.000000,   3),
('Can',             'can',     (SELECT base_type_id FROM base_type WHERE type_name = 'count'),  1.000000,   4),

-- TEXT PLACEHOLDERS
('Pinch',           'pinch',   (SELECT base_type_id FROM base_type WHERE type_name = 'text'),   1.000000,   1),
('Splash',          'splash',  (SELECT base_type_id FROM base_type WHERE type_name = 'text'),   1.000000,   2),
('To Taste',        'to taste',(SELECT base_type_id FROM base_type WHERE type_name = 'text'),   1.000000,   3);

COMMIT;
