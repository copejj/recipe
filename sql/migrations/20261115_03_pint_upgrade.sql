BEGIN;

UPDATE measurements
SET min_promotion_floor = 1.500000
WHERE measurement_id = 6; 

COMMIT;