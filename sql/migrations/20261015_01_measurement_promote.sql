BEGIN;
-- 1. Safely add the lane sorting and filtering flag constraints
ALTER TABLE measurements ADD COLUMN is_promotable BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE measurements ADD COLUMN is_metric BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX idx_measurements_flags ON measurements (base_type_id, is_metric, is_promotable);

UPDATE measurements 
SET is_promotable = false 
WHERE measurement_id IN (1, 6, 8);

UPDATE measurements 
SET is_promotable = false 
WHERE base_type_id = 3;

UPDATE measurements 
SET is_promotable = false 
WHERE base_type_id = 4;
COMMIT;