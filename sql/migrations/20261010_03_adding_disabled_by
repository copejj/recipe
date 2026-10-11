-- 1. Add the disabled_by_user_id column pointing back to the users table primary key
ALTER TABLE public.users 
ADD COLUMN IF NOT EXISTS disabled_by_user_id integer,
ADD CONSTRAINT fk_users_disabled_by FOREIGN KEY (disabled_by_user_id) REFERENCES public.users(user_id);

-- 2. Update existing role/permission description mappings if necessary to use the word "Inactive"
UPDATE public.permissions 
SET description = 'Grants comprehensive access to user registries, account access management, and security matrices.'
WHERE permission_name = 'can_manage_users';
