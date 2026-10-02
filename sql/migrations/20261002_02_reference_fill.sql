
BEGIN;

-- 1. Populate Profile Visibilities
INSERT INTO profile_visibility (visibility_name, description) VALUES 
('public', 'Profile is visible to anyone on the web'),
('private', 'Profile is hidden from all public directory searches'),
('friends_only', 'Profile is visible only to accepted connections')
ON CONFLICT (visibility_name) DO NOTHING;

-- 2. Populate Recipe Visibilities
INSERT INTO recipe_visibility (visibility_name, description) VALUES 
('public', 'Anyone can search for and view this recipe'),
('private', 'Only the creator can view this recipe'),
('shared', 'Only the creator and explicitly permitted entities can view this recipe')
ON CONFLICT (visibility_name) DO NOTHING;

-- 3. Populate Master Security Roles
INSERT INTO roles (role_name, description) VALUES 
('super_admin', 'Complete system access and configuration capabilities'),
('admin', 'Full platform monitoring and user management privileges'),
('moderator', 'Can review public recipe flags and manage comments'),
('user', 'Standard registered website visitor and content creator')
ON CONFLICT (role_name) DO NOTHING;

-- 4. Populate Fine-Grained Permissions
INSERT INTO permissions (permission_name, description) VALUES 
('can_hide_any_recipe', 'Allows content moderation of public recipes'),
('can_delete_any_comment', 'Allows cleaning up community discussion sections'),
('can_manage_users', 'Allows admins to disable profiles or update system roles'),
('can_view_audit_logs', 'Allows viewing sensitive system history logs')
ON CONFLICT (permission_name) DO NOTHING;

ALTER TABLE role_permissions 
ADD CONSTRAINT uniq_role_permission UNIQUE (role_id, permission_id);

-- 5. Link Permissions to Roles (Role-Based Access Control Mapping)
-- Note: This subquery fetches the IDs dynamically so it remains safe across environments
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE 
    (r.role_name = 'super_admin') -- Super Admins get everything
    OR (r.role_name = 'admin' AND p.permission_name IN ('can_hide_any_recipe', 'can_delete_any_comment', 'can_manage_users'))
    OR (r.role_name = 'moderator' AND p.permission_name IN ('can_hide_any_recipe', 'can_delete_any_comment'))
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 6. Populate Common Grocery Aisles / Categories
INSERT INTO grocery_category (category_name) VALUES 
('Produce'),
('Meat & Seafood'),
('Dairy & Eggs'),
('Bakery'),
('Pantry & Baking'),
('Canned Goods & Soups'),
('Frozen Foods'),
('Spices & Seasonings'),
('Beverages'),
('Household & Cleaning')
ON CONFLICT (category_name) DO NOTHING;

-- 7. Populate Measurement Base Types (Volume, Weight, Count, etc.)
ALTER TABLE base_type ADD COLUMN IF NOT EXISTS description TEXT;

INSERT INTO base_type (type_name, description) VALUES 
('volume', 'Liquid or volumetric tracking dimensions'),
('weight', 'Mass or heavy tracking dimensions'),
('count', 'Discrete whole items or individual physical pieces'),
('text', 'Non-quantifiable descriptive placeholders')
ON CONFLICT (type_name) DO NOTHING;

COMMIT;
