BEGIN;

-- Test dropping the table using the hardcoded public. prefix
DROP TABLE IF EXISTS public.pipeline_test_table CASCADE;

COMMIT;