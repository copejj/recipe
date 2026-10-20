BEGIN;
INSERT INTO permissions (permission_name, description) 
VALUES ('can_view_errors', 'Grants permission to error information to appropriate users');
COMMIT;