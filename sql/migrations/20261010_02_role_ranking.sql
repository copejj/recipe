BEGIN;
-- 1. Add the role_rank column with a default value of 0 (baseline user)
ALTER TABLE public.roles 
ADD COLUMN IF NOT EXISTS role_rank integer NOT NULL DEFAULT 0;

-- 2. Seed your existing system roles with their true hierarchical ranks
UPDATE public.roles SET role_rank = 3 WHERE role_name = 'ROLE_SUPER_ADMIN';
UPDATE public.roles SET role_rank = 2 WHERE role_name = 'ROLE_ADMIN';
UPDATE public.roles SET role_rank = 1 WHERE role_name = 'ROLE_MODERATOR';
UPDATE public.roles SET role_rank = 0 WHERE role_name = 'ROLE_USER';
COMMIT;
