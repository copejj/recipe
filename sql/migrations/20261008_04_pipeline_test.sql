BEGIN;

-- 1. Test table creation with hardcoded public. prefix
CREATE TABLE IF NOT EXISTS public.pipeline_test_table (
    id serial NOT NULL,
    test_message text NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    PRIMARY KEY (id)
);

-- 2. Test inserting data into the hardcoded public path
INSERT INTO public.pipeline_test_table (test_message) 
VALUES ('Pipeline migration routing test successful!');

COMMIT;
