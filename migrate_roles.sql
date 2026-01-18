-- Migrate COMPANY_ADMIN role to FLEET_MANAGER
UPDATE users SET role = 'FLEET_MANAGER' WHERE role = 'COMPANY_ADMIN';

-- Verify the migration
SELECT role, COUNT(*) as count FROM users GROUP BY role;
