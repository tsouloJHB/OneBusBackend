-- Database Migration: Add operational_status column to buses table
-- Date: 2025-12-26
-- Purpose: Enable control over which buses are actively tracked even when GPS transmits

-- Step 1: Add operational_status column with default value 'active'
-- This ensures existing buses remain operational
ALTER TABLE buses 
ADD COLUMN IF NOT EXISTS operational_status VARCHAR(20) NOT NULL DEFAULT 'active';

-- Step 2: Add check constraint to ensure only valid status values
ALTER TABLE buses
ADD CONSTRAINT check_operational_status 
CHECK (operational_status IN ('active', 'inactive', 'maintenance', 'retired'));

-- Step 3: Create index for faster status filtering
CREATE INDEX IF NOT EXISTS idx_buses_operational_status 
ON buses(operational_status);

-- Step 4: Create index for tracker IMEI lookups (if not exists)
CREATE INDEX IF NOT EXISTS idx_buses_tracker_imei 
ON buses(tracker_imei);

-- Step 5: Add comment to column
COMMENT ON COLUMN buses.operational_status IS 
'Controls whether bus location data is processed and broadcast. Values: active (track normally), inactive (ignore GPS), maintenance (under repair), retired (decommissioned)';

-- Verification Query
-- Run this to confirm migration success:
/*
SELECT 
    column_name, 
    data_type, 
    column_default, 
    is_nullable
FROM information_schema.columns
WHERE table_name = 'buses' 
  AND column_name = 'operational_status';
*/

-- Test Query
-- Verify all existing buses have been set to 'active':
/*
SELECT operational_status, COUNT(*) as count
FROM buses
GROUP BY operational_status;
*/

-- Expected result: All buses should show 'active'
