BEGIN;
INSERT INTO permissions (permission_name, description) 
VALUES ('can_delete_users', 'Grants permission to delete users form the database along with their content');
COMMIT;