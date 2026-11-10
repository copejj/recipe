BEGIN;

-- Table: recipe_saves
CREATE TABLE IF NOT EXISTS recipe_saves
(
    user_id integer NOT NULL,
    recipe_id integer NOT NULL,
    saved_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT recipe_saves_pkey PRIMARY KEY (user_id, recipe_id),
    CONSTRAINT fk_recipe_saves_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE,
    CONSTRAINT fk_recipe_saves_recipe FOREIGN KEY (recipe_id)
        REFERENCES recipes (recipe_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
)
TABLESPACE pg_default;

ALTER TABLE IF EXISTS recipe_saves
    OWNER to recipe_migration_user;

REVOKE ALL ON TABLE recipe_saves FROM recipe_app_user;
GRANT TRUNCATE, INSERT, DELETE, SELECT, UPDATE ON TABLE recipe_saves TO recipe_app_user;
GRANT ALL ON TABLE recipe_saves TO recipe_migration_user;

COMMIT;