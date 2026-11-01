BEGIN;

UPDATE permissions 
SET 
    permission_name = 'can_moderate_recipe_metadata',
    description = 'Allows a user to manage, verify, and clean ingredients, grocery categories, and recipe categories.'
WHERE 
    permission_id = 7;

COMMIT;
