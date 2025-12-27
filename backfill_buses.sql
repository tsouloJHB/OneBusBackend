-- Backfill buses table from registered_buses for existing records
UPDATE buses b
SET 
    registration_number = rb.registration_number,
    bus_company_name = bc.name,
    bus_number = COALESCE(b.bus_number, rb.bus_number),
    model = COALESCE(b.model, rb.model),
    year = COALESCE(b.year, rb.year),
    capacity = COALESCE(b.capacity, rb.capacity),
    driver_id = COALESCE(b.driver_id, rb.driver_id),
    driver_name = COALESCE(b.driver_name, rb.driver_name),
    route_id = COALESCE(b.route_id, rb.route_id),
    route_name = COALESCE(b.route_name, rb.route_name),
    status = COALESCE(b.status, rb.status::varchar),
    operational_status = LOWER(COALESCE(b.status, rb.status::varchar)),
    last_inspection = COALESCE(b.last_inspection, rb.last_inspection),
    next_inspection = COALESCE(b.next_inspection, rb.next_inspection),
    updated_at = NOW()
FROM registered_buses rb
JOIN bus_companies bc ON rb.company_id = bc.id
WHERE b.bus_id = rb.bus_id
  AND (b.registration_number IS NULL OR b.bus_company_name IS NULL);

-- Show results
SELECT 
    bus_id,
    registration_number,
    bus_company_name,
    operational_status
FROM buses
ORDER BY created_at DESC;
