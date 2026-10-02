BEGIN;

-- =========================================================================
-- 1. ADD PUBLIC ID TO USERS TABLE
-- =========================================================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS public_id UUID DEFAULT gen_random_uuid() NOT NULL;

-- Enforce uniqueness explicitly
ALTER TABLE users ADD CONSTRAINT uq_users_public_id UNIQUE (public_id);


-- =========================================================================
-- 2. ADD PUBLIC ID TO RECIPES TABLE
-- =========================================================================
ALTER TABLE recipes ADD COLUMN IF NOT EXISTS public_id UUID DEFAULT gen_random_uuid() NOT NULL;

-- Enforce uniqueness explicitly
ALTER TABLE recipes ADD CONSTRAINT uq_recipes_public_id UNIQUE (public_id);

COMMIT;
