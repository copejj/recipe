BEGIN;

-- Rename the table from plural to singular
ALTER TABLE profile_visiblities RENAME TO profile_visibility;

-- Rename the primary key column (fixes your 'profile_visibilitiy_id' typo to match the singular table)
ALTER TABLE profile_visibility RENAME COLUMN profile_visibilitiy_id TO profile_visibility_id;

-- changes column name to make more sense in the table
ALTER TABLE profile_visibility RENAME COLUMN profile_name TO visibility_name;

-- Rename the table from plural to singular
ALTER TABLE recipe_visibilities RENAME TO recipe_visibility;

COMMIT;