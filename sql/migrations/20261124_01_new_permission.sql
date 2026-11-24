BEGIN;
INSERT INTO permissions (permission_id, permission_name, description)
VALUES (
    10,
    'can_manage_messages',
    'Allows creating, editing, expiring, and deleting global broadcast banners and targeted user dashboard messages.'
)
ON CONFLICT (permission_name) DO NOTHING;

-- Advance the sequence so future inserts don't collide with ID 10
SELECT setval(pg_get_serial_sequence('permissions', 'permission_id'), COALESCE((SELECT MAX(permission_id) FROM permissions), 1));INSERT INTO permissions (permission_id, permission_name, description)
VALUES (
    10,
    'can_manage_messages',
    'Allows creating, editing, expiring, and deleting global broadcast banners and targeted user dashboard messages.'
)
ON CONFLICT (permission_name) DO NOTHING;

-- Advance the sequence so future inserts don't collide with ID 10
SELECT setval(pg_get_serial_sequence('permissions', 'permission_id'), COALESCE((SELECT MAX(permission_id) FROM permissions), 1));
COMMIT;