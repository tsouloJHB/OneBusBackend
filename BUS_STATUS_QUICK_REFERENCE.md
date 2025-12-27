# Bus Operational Status - Quick Reference Guide

## Status Values

| Status | Meaning | GPS Processing | Appears in Active Buses | Use Case |
|--------|---------|---------------|------------------------|----------|
| **active** | Bus is operational and on route | ✅ Saved & Broadcast | ✅ Yes | Normal service |
| **inactive** | Bus temporarily not in service | ❌ Ignored | ❌ No | End of shift, parked |
| **maintenance** | Bus undergoing repairs | ❌ Ignored | ❌ No | In workshop |
| **retired** | Bus decommissioned | ❌ Ignored | ❌ No | Out of service permanently |

## API Endpoints

### Update Bus Status
```http
PUT /api/buses/{busId}/status
Content-Type: application/json

{
  "operationalStatus": "active"
}
```

**Response (Success):**
```json
{
  "success": true,
  "message": "Bus operational status updated to 'active'",
  "busId": "BUS042",
  "newStatus": "active"
}
```

**Response (Error - Invalid Status):**
```json
{
  "error": "Invalid status. Must be one of: active, inactive, maintenance, retired"
}
```

**Response (Error - Bus Not Found):**
```
HTTP 404 Not Found
```

## Testing Commands

### 1. Check Current Status
```sql
SELECT bus_id, bus_number, tracker_imei, operational_status 
FROM buses 
WHERE bus_id = 'BUS042';
```

### 2. Update Status via API
```bash
curl -X PUT http://localhost:8080/api/buses/BUS042/status \
  -H "Content-Type: application/json" \
  -d '{"operationalStatus": "inactive"}'
```

### 3. Send Test GPS Data
```bash
curl -X POST http://localhost:8080/api/tracker/payload \
  -H "Content-Type: application/json" \
  -d '{
    "trackerImei": "C5NORTH001",
    "busNumber": "C5",
    "lat": -26.2047,
    "lon": 28.0416,
    "timestamp": "2025-12-26T10:00:00",
    "busStopIndex": 1,
    "busCompany": "Rea Vaya",
    "tripDirection": "Northbound"
  }'
```

### 4. Check Backend Logs
```bash
# Look for these messages:
grep "REJECTED\|IGNORED" logs/spring-boot-application.log

# Expected output when GPS is ignored:
# [IGNORED] Bus C5 (IMEI: C5NORTH001) has status 'inactive' - Not saving coordinates
```

### 5. Verify Redis Cache
```bash
# Check if bus location is in Redis
redis-cli GET "bus:location:C5NORTH001"

# If bus is inactive, this should return (nil)
```

## Common Workflows

### Start of Day (Driver Activates Bus)
```
1. Driver logs in → Mobile app authenticated
2. Driver clicks "Start Shift"
3. App calls: PUT /api/buses/{busId}/status {"operationalStatus": "active"}
4. GPS tracker starts transmitting → Backend saves and broadcasts ✅
5. Bus appears on admin dashboard and passenger app
```

### End of Day (Driver Deactivates Bus)
```
1. Driver completes route
2. Driver clicks "End Shift"
3. App calls: PUT /api/buses/{busId}/status {"operationalStatus": "inactive"}
4. GPS tracker still transmits → Backend ignores data ❌
5. Bus disappears from active buses within 10 minutes
```

### Bus Breakdown (Send to Maintenance)
```
1. Admin/dispatcher receives breakdown report
2. Admin opens bus management page
3. Admin changes status to "maintenance"
4. GPS tracker still transmits → Backend ignores data ❌
5. Dispatch assigns replacement bus
```

## Verification Checklist

- [ ] Database column `operational_status` exists on `buses` table
- [ ] Default value is 'active' for new buses
- [ ] BusTrackingService checks status before saving coordinates
- [ ] API endpoint `/api/buses/{busId}/status` works
- [ ] Log messages show "[IGNORED]" for inactive buses
- [ ] Redis cache cleared when bus set to inactive
- [ ] Active buses endpoint excludes inactive buses
- [ ] WebSocket broadcasts stop when bus becomes inactive

## Troubleshooting

### Problem: GPS data still saved despite inactive status
**Check:**
```sql
-- Verify status is actually inactive
SELECT operational_status FROM buses WHERE bus_id = 'BUS042';

-- Check recent location saves
SELECT * FROM bus_locations 
WHERE tracker_imei = 'C5NORTH001' 
ORDER BY last_saved_timestamp DESC 
LIMIT 5;
```

**If data still being saved:**
- Backend may not have been restarted after code changes
- Check logs for "[IGNORED]" messages - if missing, logic not executing
- Verify operational_status column actually exists in database

### Problem: Bus disappears from map unexpectedly
**Check:**
```sql
-- Verify status
SELECT bus_id, operational_status FROM buses WHERE bus_number = 'C5';

-- Check if GPS transmitting
SELECT MAX(last_saved_timestamp) as last_update 
FROM bus_locations 
WHERE bus_number = 'C5';
```

**Common causes:**
- Status accidentally changed to inactive
- GPS tracker powered off/disconnected
- Network connectivity issue (tracker can't reach server)

### Problem: Unregistered IMEI being processed
**Check logs:**
```
grep "REJECTED.*Unregistered trackerImei" logs/spring-boot-application.log
```

**If not seeing rejection:**
- IMEI might actually be registered (check `buses` table)
- Typo in IMEI being searched for vs IMEI in database

## Database Queries for Monitoring

### Active vs Inactive Bus Count
```sql
SELECT operational_status, COUNT(*) as count 
FROM buses 
GROUP BY operational_status;
```

### Buses Transmitting While Inactive (Redis Check)
This requires checking application logs, not a SQL query.
Look for pattern: `[IGNORED] Bus ... has status 'inactive'`

### Recent Location Updates by Bus
```sql
SELECT 
    b.bus_number,
    b.operational_status,
    MAX(bl.last_saved_timestamp) as last_update,
    NOW() - TO_TIMESTAMP(MAX(bl.last_saved_timestamp)/1000) as time_since_update
FROM buses b
LEFT JOIN bus_locations bl ON b.bus_id = bl.bus_id
GROUP BY b.bus_number, b.operational_status
ORDER BY last_update DESC;
```

### Find Stuck Buses (Active Status but No Recent Data)
```sql
SELECT 
    b.bus_id,
    b.bus_number,
    b.operational_status,
    MAX(bl.last_saved_timestamp) as last_update
FROM buses b
LEFT JOIN bus_locations bl ON b.bus_id = bl.bus_id
WHERE b.operational_status = 'active'
GROUP BY b.bus_id, b.bus_number, b.operational_status
HAVING MAX(bl.last_saved_timestamp) < EXTRACT(EPOCH FROM NOW() - INTERVAL '1 hour') * 1000
ORDER BY last_update DESC;
```

## Implementation Checklist

✅ Backend Implementation
- [x] Added `operational_status` field to Bus entity
- [x] Added status validation in processTrackerPayload()
- [x] Created PUT /api/buses/{busId}/status endpoint
- [x] Added updateBusStatus() service method
- [x] Redis cache cleared when bus set to inactive
- [x] Created database migration SQL file
- [x] Added comprehensive logging

⏳ Frontend Implementation (Planned)
- [ ] Add status dropdown to RegisteredBuses edit form
- [ ] Add quick status toggle buttons in bus list
- [ ] Show visual indicator for inactive buses
- [ ] Add status change confirmation dialog

⏳ Mobile Driver App (Future)
- [ ] "Start Shift" button → Set status to active
- [ ] "End Shift" button → Set status to inactive
- [ ] "Report Issue" → Set status to maintenance
- [ ] Authentication/authorization for drivers

## Next Steps

1. **Run Database Migration**
   ```bash
   # Apply migration to add operational_status column
   # The backend will auto-run Flyway migrations on startup
   ```

2. **Restart Backend**
   ```bash
   cd OneBusBackend
   ./mvnw spring-boot:run
   ```

3. **Test with Simulator**
   ```bash
   # Set bus to inactive
   curl -X PUT http://localhost:8080/api/buses/BUS042/status \
     -H "Content-Type: application/json" \
     -d '{"operationalStatus": "inactive"}'
   
   # Run simulator - verify logs show [IGNORED]
   python3 simulate_c5_buses.py
   ```

4. **Verify Behavior**
   - Check logs for "[IGNORED]" messages
   - Verify bus doesn't appear in /api/buses/active
   - Confirm no WebSocket broadcasts for inactive bus
