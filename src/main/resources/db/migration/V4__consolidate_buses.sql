-- Consolidate RegisteredBus fields into buses table
-- Add missing fields for physical bus registration and maintenance tracking

-- 1) Add columns with safe defaults (allow NULL during creation)
ALTER TABLE buses 
ADD COLUMN IF NOT EXISTS registration_number VARCHAR(50),
ADD COLUMN IF NOT EXISTS model VARCHAR(50),
ADD COLUMN IF NOT EXISTS "year" INTEGER,
ADD COLUMN IF NOT EXISTS capacity INTEGER,
ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE',
ADD COLUMN IF NOT EXISTS route_id BIGINT,
ADD COLUMN IF NOT EXISTS route_name VARCHAR(100),
ADD COLUMN IF NOT EXISTS route_assigned_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS last_inspection DATE,
ADD COLUMN IF NOT EXISTS next_inspection DATE,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 2) Backfill timestamps before enforcing NOT NULL
UPDATE buses SET created_at = NOW() WHERE created_at IS NULL;
UPDATE buses SET updated_at = NOW() WHERE updated_at IS NULL;

-- 3) Enforce NOT NULL on timestamps
ALTER TABLE buses
	ALTER COLUMN created_at SET NOT NULL,
	ALTER COLUMN updated_at SET NOT NULL;

-- Add constraint for status enum values
ALTER TABLE buses 
ADD CONSTRAINT IF NOT EXISTS check_bus_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE', 'RETIRED'));

-- Create index on registration_number for efficient lookups
CREATE INDEX IF NOT EXISTS idx_buses_registration ON buses(registration_number);

-- Create index on company_name for filtering by company
CREATE INDEX IF NOT EXISTS idx_buses_company_name ON buses(bus_company_name);

-- Create index on status for quick filtering
CREATE INDEX IF NOT EXISTS idx_buses_status ON buses(status);
