begin;
UPDATE public.roles
SET role_name = 'ROLE_' || UPPER(role_name)
WHERE role_name NOT LIKE 'ROLE_%';
commit;