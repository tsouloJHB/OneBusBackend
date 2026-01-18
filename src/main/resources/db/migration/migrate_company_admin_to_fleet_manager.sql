-- Migration script to update existing COMPANY_ADMIN roles to FLEET_MANAGER
-- Run this script after deploying the new code

-- Update all existing COMPANY_ADMIN users to FLEET_MANAGER
UPDATE users 
SET role = 'FLEET_MANAGER' 
WHERE role = 'COMPANY_ADMIN';

-- Verify the update
SELECT 
    id,
    email,
    full_name,
    role,
    company_id,
    created_at
FROM users 
WHERE role = 'FLEET_MANAGER'
ORDER BY created_at DESC;

-- Count users by role to verify migration
SELECT 
    role,
    COUNT(*) as user_count
FROM users 
GROUP BY role
ORDER BY role;
