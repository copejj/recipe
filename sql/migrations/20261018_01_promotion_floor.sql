BEGIN;

-- ==========================================================================
-- 1. LAYER SCHEMA EXPENSION
-- ==========================================================================
-- Safely add the new floor column only if it hasn't been declared yet
ALTER TABLE measurements ADD COLUMN IF NOT EXISTS min_promotion_floor NUMERIC(10, 6) DEFAULT 0.850000;


-- ==========================================================================
-- 2. RE-ALIGN SPLASH & PINCH INTO VOLUMETRIC FAMILY
-- ==========================================================================
-- Convert Pinch and Splash into the Volumetric track family (base_type_id = 1) 
-- and calibrate their precise base milliliter equivalents.
UPDATE measurements SET base_type_id = 1, base_unit_equivalent = 0.308057, scaling_rank = 1 WHERE measurement_id = 18; -- Pinch
UPDATE measurements SET base_type_id = 1, base_unit_equivalent = 0.616115, scaling_rank = 2 WHERE measurement_id = 19; -- Splash/Dash

-- Re-align and shift Imperial Volumetric scaling ranks so the step-ladder ladder sequence remains unbroken
UPDATE measurements SET scaling_rank = 3 WHERE measurement_id = 2; -- Teaspoon
UPDATE measurements SET scaling_rank = 4 WHERE measurement_id = 3; -- Tablespoon
UPDATE measurements SET scaling_rank = 5 WHERE measurement_id = 4; -- Fluid Ounce
UPDATE measurements SET scaling_rank = 6 WHERE measurement_id = 5; -- Cup
UPDATE measurements SET scaling_rank = 7 WHERE measurement_id = 6; -- Pint
UPDATE measurements SET scaling_rank = 8 WHERE measurement_id = 7; -- Quart
UPDATE measurements SET scaling_rank = 9 WHERE measurement_id = 9; -- Gallon


-- ==========================================================================
-- 3. APPLY MINIMUM COOKBOOK PROMOTION THRESHOLD FLOORS
-- ==========================================================================
UPDATE measurements
SET min_promotion_floor = CASE measurement_id
    -- Volumetric Imperial Clamps
    WHEN 9  THEN 0.250000  -- Gallon (Drops to Quarts below 1/4 Gal)
    WHEN 7  THEN 1.000000  -- Quart (Drops to Pints below 1 Qt)
    WHEN 6  THEN 1.000000  -- Pint (Drops to Cups below 1 Pt)
    WHEN 5  THEN 0.250000  -- Cup (Drops to Tbsp below 1/4 Cup)
    WHEN 3  THEN 1.000000  -- Tablespoon (Drops to Tsp below 1 Tbsp)
    WHEN 2  THEN 0.125000  -- Teaspoon (Drops to Pinch below 1/8 Tsp)

    -- Weight Imperial Clamps
    WHEN 12 THEN 0.250000  -- Pound (Drops to Ounces below 1/4 lb)
    WHEN 11 THEN 0.850000  -- Ounce (Standard proximity check)

    -- Metric System Standard Handoff Boundaries
    WHEN 8  THEN 0.500000  -- Liter (Drops to mL below 500mL)
    WHEN 13 THEN 0.500000  -- Kilogram (Drops to Grams below 500g)
    WHEN 1  THEN 0.850000  -- Milliliter (Baseline foundation)
    WHEN 10 THEN 0.850000  -- Gram (Baseline foundation)

    -- Discrete & Abstract Units (Freeze tracking bounds completely at 0.0)
    WHEN 14 THEN 0.000000  -- Whole
    WHEN 15 THEN 0.000000  -- Slice
    WHEN 16 THEN 0.000000  -- Clove
    WHEN 17 THEN 0.000000  -- Can
    WHEN 18 THEN 0.850000  -- Pinch (Now processes volumetric transitions)
    WHEN 19 THEN 0.850000  -- Splash (Now processes volumetric transitions)
    WHEN 20 THEN 0.000000  -- To Taste
    
    ELSE 0.850000
END
WHERE measurement_id IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20);

COMMIT;
