BEGIN;
CREATE TABLE IF NOT EXISTS public.ip_overrides (
    ip_override_id serial PRIMARY KEY,
    ip_address text NOT NULL UNIQUE,
    override_until timestamp with time zone NOT NULL
);

-- Grant standard permissions to your application user
GRANT INSERT, SELECT, UPDATE, DELETE ON TABLE public.ip_overrides TO recipe_app_user;
COMMIT;

