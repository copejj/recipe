BEGIN;
CREATE TABLE IF NOT EXISTS shopping_list_shares (
    shopping_list_share_id SERIAL PRIMARY KEY,
    shopping_list_id INTEGER NOT NULL REFERENCES shopping_lists(shopping_list_id) ON DELETE CASCADE,
    shared_user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    access_level_id INTEGER NOT NULL REFERENCES access_levels(access_level_id),
    shared_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (shopping_list_id, shared_user_id)
);
COMMIT;