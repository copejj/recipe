BEGIN;
INSERT INTO public.permissions (permission_name, description)
VALUES ('can_manage_roles', 'Allows provisioning system roles, adjusting baseline configurations, and mapping authorities.')
ON CONFLICT (permission_name) 
DO UPDATE SET description = EXCLUDED.description;

-- 2. Dynamically stitch the new permission exclusively to the SUPER_ADMIN role
INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id 
FROM public.roles r, public.permissions p
WHERE r.role_name = 'ROLE_SUPER_ADMIN' 
  AND p.permission_name = 'can_manage_roles'
ON CONFLICT DO NOTHING;
COMMIT;