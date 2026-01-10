-- V5: Add cumulative_distances_json column to full_routes table
-- This column stores pre-calculated cumulative distances along the route polyline
-- Format: JSON array of doubles [0, 50.2, 125.8, 200.3, ...]
-- Each value represents the distance in meters from the start of the route to that coordinate

ALTER TABLE full_routes 
ADD COLUMN IF NOT EXISTS cumulative_distances_json TEXT;

-- Add comment to explain the column purpose
COMMENT ON COLUMN full_routes.cumulative_distances_json IS 'Pre-calculated cumulative distances in meters from route start for linear referencing';
