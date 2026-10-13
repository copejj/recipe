BEGIN;
-- Track whether the ingredient is global/verified, and who introduced it
ALTER TABLE public.ingredients
ADD COLUMN is_verified BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN created_by_user_id INTEGER NULL,
ADD CONSTRAINT fk_ingredients_creator 
    FOREIGN KEY (created_by_user_id) 
    REFERENCES public.users (user_id) ON DELETE SET NULL;

-- 1. Insert the permission target into the permissions catalog if it doesn't exist
INSERT INTO public.permissions (permission_name, description)
VALUES ('can_moderate_ingredients', 'Allows a user to verify, merge, and clean the public ingredient pool.')
ON CONFLICT DO NOTHING;

-- 2. Dynamically bind the permission to ROLE_MODERATOR by its string name
INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM public.roles r, public.permissions p
WHERE r.role_name = 'ROLE_MODERATOR' 
  AND p.permission_name = 'can_moderate_ingredients'
ON CONFLICT DO NOTHING;

-- 3. Dynamically bind the permission to ROLE_ADMIN by its string name
INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM public.roles r, public.permissions p
WHERE r.role_name = 'ROLE_ADMIN' 
  AND p.permission_name = 'can_moderate_ingredients'
ON CONFLICT DO NOTHING;

-- 4. Dynamically bind the permission to ROLE_SUPER_ADMIN by its string name
INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM public.roles r, public.permissions p
WHERE r.role_name = 'ROLE_SUPER_ADMIN' 
  AND p.permission_name = 'can_moderate_ingredients'
ON CONFLICT DO NOTHING;

COMMIT;