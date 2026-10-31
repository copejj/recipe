BEGIN;

-- Create table structure under system migration authority
CREATE TABLE IF NOT EXISTS persistent_logins (
    username varchar(64) NOT NULL,
    series varchar(64) NOT NULL,
    token varchar(64) NOT NULL,
    last_used timestamp with time zone NOT NULL,
    CONSTRAINT persistent_logins_pkey PRIMARY KEY (series)
);

-- Align table ownership
ALTER TABLE IF EXISTS persistent_logins
    OWNER to recipe_migration_user;

-- Isolate application connectivity privileges
REVOKE ALL ON TABLE persistent_logins FROM recipe_app_user;

GRANT INSERT, DELETE, SELECT, UPDATE ON TABLE persistent_logins TO recipe_app_user;
GRANT ALL ON TABLE persistent_logins TO recipe_migration_user;

COMMIT;
