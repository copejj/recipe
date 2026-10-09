BEGIN;
ALTER TABLE public.login_history 
DROP CONSTRAINT IF EXISTS login_history_user_auth_id_fkey;

ALTER TABLE public.users_auth 
RENAME COLUMN users_auth_id TO user_auth_id;

ALTER TABLE public.login_history
    ADD CONSTRAINT login_history_user_auth_id_fkey
    FOREIGN KEY (user_auth_id)
    REFERENCES public.users_auth (user_auth_id) 
    ON UPDATE NO ACTION
    ON DELETE CASCADE;
COMMIT;