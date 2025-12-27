# Bus Operational Status & Tracking Control System

## Overview
This document describes the bus operational status system that controls which buses are actively tracked, stored in memory, and broadcast via WebSocket. This prevents non-operational buses from appearing in live tracking systems even when their GPS trackers are transmitting data.

## Problem Statement
GPS trackers fitted to buses may transmit location data even when:
- Bus is not on an active route
- Bus is parked/idle
- Bus is being used for non-operational purposes (maintenance, personal use)
- Bus is moving to/from depot without passengers

Without status control, these transmissions would:
- Waste server resources (memory/database storage)
- Show incorrect "active" buses on admin dashboards and passenger apps
- Create confusion about actual service availability

## Solution: Operational Status Control

### Four-Rule System

#### **Rule 1: IMEI Registration Validation**
```
IF tracker IMEI is NOT registered in the buses table
THEN reject the payload (foreign/unauthorized device)
```

**Implementation:**
- Location: `BusTrackingService.processTrackerPayload()` (Line 142)
- Action: Log warning, return without processing
- Log message: `"[REJECTED] Unregistered trackerImei: {imei} - Ignoring foreign device"`

#### **Rule 2: IMEI-Bus Association Verification**
```
IF tracker IMEI exists in buses table
THEN proceed to status check
```

**Implementation:**
- Database lookup: `busRepository.findByTrackerImei()`
- Establishes which bus is associated with this tracker

#### **Rule 3: Operational Status Check**
```
IF bus.operationalStatus != 'active'
THEN ignore coordinates (do NOT save to memory/database/broadcast)
```

**Implementation:**
- Location: `BusTrackingService.processTrackerPayload()` (Lines 151-156)
- Action: Log info message, return without saving
- Log message: `"[IGNORED] Bus {busNumber} (IMEI: {imei}) has status '{status}' - Not saving coordinates"`

**Valid Status Values:**
- `active` - Bus is operational, track and broadcast location
- `inactive` - Bus temporarily not in service (ignore tracking)
- `maintenance` - Bus undergoing repairs (ignore tracking)
- `retired` - Bus decommissioned (ignore tracking)

#### **Rule 4: Memory/Broadcast Coupling**
```
IF coordinates saved to Redis memory
THEN broadcast via WebSocket
ELSE no broadcast occurs
```

**Implementation:**
- Redis save: Line 186 (`redisTemplate.opsForValue().set()`)
- Database save: Line 195 (`busLocationRepository.save()`)
- WebSocket broadcast: Line 199 (`streamingService.broadcastBusUpdate()`)
- **Critical**: If Rules 1-3 reject payload, none of these execute

## Data Flow

```
GPS Tracker Transmits → POST /api/tracker/payload
                              ↓
                        Rule 1: IMEI Registered?
                              ↓ YES
                        Rule 2: Find Associated Bus
                              ↓
                        Rule 3: Status = 'active'?
                              ↓ YES
                        Save to Redis (24hr TTL)
                              ↓
                        Save to Database (permanent)
                              ↓
                        Rule 4: Broadcast WebSocket
                              ↓
                        Admin Map & Flutter App Updated
```

```
GPS Tracker Transmits → POST /api/tracker/payload
                              ↓
                        Rule 1: IMEI Registered?
                              ↓ NO
                        [REJECTED] Log & Exit
```

```
GPS Tracker Transmits → POST /api/tracker/payload
                              ↓
                        Rule 1: IMEI Registered?
                              ↓ YES
                        Rule 2: Find Associated Bus
                              ↓
                        Rule 3: Status = 'active'?
                              ↓ NO (inactive/maintenance/retired)
                        [IGNORED] Log & Exit
```

## Database Schema

### buses Table
```sql
CREATE TABLE buses (
    bus_id VARCHAR(255) PRIMARY KEY,
    tracker_imei VARCHAR(255) UNIQUE NOT NULL,
    bus_number VARCHAR(50),
    route VARCHAR(255),
    bus_company_id BIGINT,
    bus_company_name VARCHAR(255),
    driver_id VARCHAR(255),
    driver_name VARCHAR(255),
    operational_status VARCHAR(20) NOT NULL DEFAULT 'active',
    CONSTRAINT fk_bus_company FOREIGN KEY (bus_company_id) 
        REFERENCES bus_companies(id)
);
```

### bus_locations Table (Historical Record)
```sql
CREATE TABLE bus_locations (
    id BIGSERIAL PRIMARY KEY,
    bus_id VARCHAR(255),
    tracker_imei VARCHAR(255),
    lat DOUBLE PRECISION,
    lon DOUBLE PRECISION,
    timestamp TIMESTAMP,
    bus_stop_index INTEGER,
    bus_company VARCHAR(255),
    trip_direction VARCHAR(50),
    last_saved_timestamp BIGINT,
    bus_number VARCHAR(50),
    bus_driver_id VARCHAR(255),
    bus_driver VARCHAR(255),
    speed_kmh DOUBLE PRECISION
);
```

**Note:** Only buses with `operational_status = 'active'` will have new records inserted into `bus_locations`.

## Redis In-Memory Storage (Cost Optimization)

### Why Redis?
- **Cost Saving**: Reduces database writes for frequently updating GPS data
- **Performance**: Sub-millisecond read/write operations
- **TTL Support**: Automatic cleanup of stale data (24-hour expiration)
- **Geospatial Queries**: Built-in support for finding nearest buses

### Redis Keys
```
bus:location:{trackerImei} → BusLocation object (24hr TTL)
bus:geo → Geospatial index of all active buses
```

### Storage Decision Logic
```java
// Check cache first
BusLocation cached = redisTemplate.opsForValue().get(key);

if (cached == null) {
    // First transmission - fetch bus details from database
    Bus bus = busRepository.findByTrackerImei(imei);
    if (bus != null && "active".equals(bus.getOperationalStatus())) {
        // Save to Redis with 24hr TTL
        redisTemplate.opsForValue().set(key, payload, 24, TimeUnit.HOURS);
        // Also persist to database for historical tracking
        busLocationRepository.save(payload);
    }
} else {
    // Subsequent transmissions - revalidate status
    Bus bus = busRepository.findByTrackerImei(imei);
    if (bus != null && "active".equals(bus.getOperationalStatus())) {
        // Update Redis and database
        redisTemplate.opsForValue().set(key, payload, 24, TimeUnit.HOURS);
        busLocationRepository.save(payload);
    }
}
```

## Admin Dashboard Control

### Updating Bus Operational Status

#### Via Admin UI (Planned Feature)
Future bus drivers will be able to:
1. Log into mobile app
2. Set bus status before/after trips:
   - **Start Shift**: Set status to `active`
   - **End Shift**: Set status to `inactive`
   - **Maintenance Required**: Set status to `maintenance`

#### Via Admin Dashboard (Current)
Administrators can update status in Registered Buses table:
1. Navigate to Company Management → Registered Buses
2. Click Edit on the bus record
3. Change "Operational Status" dropdown
4. Save changes

**Effect**: Immediate - next GPS transmission will be checked against new status.

### API Endpoint (For Driver Apps)
```http
PUT /api/buses/{busId}/status
Content-Type: application/json

{
  "operationalStatus": "active"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Bus operational status updated to 'active'",
  "busId": "BUS001",
  "newStatus": "active"
}
```

## Active Buses Endpoint

### GET /api/buses/active
Returns only buses with:
1. `operational_status = 'active'` in buses table
2. Recent location update (within 10 minutes)

```sql
SELECT DISTINCT ON (bus_number) * 
FROM bus_locations 
WHERE last_saved_timestamp > (CURRENT_TIMESTAMP - INTERVAL '10 minutes')
ORDER BY bus_number, last_saved_timestamp DESC;
```

**Important**: This query only sees buses that passed the operational status check. Inactive buses will never have recent entries in `bus_locations`.

## WebSocket Broadcasting

### Topic Structure
```
/topic/bus/{busNumber}_{direction}
```

### Broadcasting Logic
```java
public void broadcastBusUpdate(BusLocation location) {
    // Only called if operational status was 'active'
    String topic = "/topic/bus/" + location.getBusNumber() + "_" + location.getTripDirection();
    template.convertAndSend(topic, location);
    logger.debug("Broadcasting to {}: {}", topic, location);
}
```

### Subscriber Behavior
- Admin dashboard map subscribes to topics for displayed buses
- Flutter passenger app subscribes to topics for tracked buses
- **Key Point**: If bus status changes to `inactive` mid-trip, broadcasts cease immediately

## Status Change Scenarios

### Scenario 1: Bus Goes Inactive During Trip
```
10:00 - Bus status: active → GPS transmits → Saved & Broadcast ✓
10:30 - Bus status: active → GPS transmits → Saved & Broadcast ✓
11:00 - Admin sets status: inactive
11:30 - GPS still transmits → IGNORED (not saved/broadcast) ✗
12:00 - GPS still transmits → IGNORED ✗
```

**Result**: Bus disappears from active buses list and map within 10 minutes (active timeout).

### Scenario 2: Unregistered Tracker Sends Data
```
GPS with IMEI "UNKNOWN123" transmits
→ Rule 1 fails (not in buses table)
→ Log: "[REJECTED] Unregistered trackerImei: UNKNOWN123"
→ No processing occurs
```

**Result**: Foreign/unauthorized devices are blocked.

### Scenario 3: Bus in Maintenance Transmits
```
Bus registration:
  - bus_id: "BUS042"
  - tracker_imei: "IMEI5678"
  - operational_status: "maintenance"

GPS transmits location
→ Rule 1 passes (IMEI registered)
→ Rule 2 passes (bus found)
→ Rule 3 fails (status != 'active')
→ Log: "[IGNORED] Bus BUS042 (IMEI: IMEI5678) has status 'maintenance'"
→ No save, no broadcast
```

**Result**: Maintenance buses don't pollute active tracking.

## Logging & Monitoring

### Log Levels

**WARN - Security/Registration Issues**
```
[REJECTED] Unregistered trackerImei: {imei} - Ignoring foreign device
```

**INFO - Operational Status Filtering**
```
[IGNORED] Bus {busNumber} (IMEI: {imei}) has status '{status}' - Not saving coordinates
[IGNORED] Cached bus {busNumber} has status '{status}' - Not saving coordinates
```

**DEBUG - Normal Operations**
```
Bus info updated: {payload}
Broadcasting to /topic/bus/{busNumber}_{direction}: {location}
```

### Monitoring Queries

**Count Active vs Inactive Buses**
```sql
SELECT operational_status, COUNT(*) 
FROM buses 
GROUP BY operational_status;
```

**Find Buses Transmitting While Inactive**
```
Check application logs for "[IGNORED]" messages
→ Indicates tracker is on but bus is not operational
→ This is EXPECTED behavior (system working correctly)
```

**Recent Location Updates**
```sql
SELECT bus_number, MAX(last_saved_timestamp) as last_update
FROM bus_locations
GROUP BY bus_number
ORDER BY last_update DESC;
```

## Future Enhancements

### Driver Mobile App Integration
- Driver login authentication
- "Start Trip" button → Sets status to `active`
- "End Trip" button → Sets status to `inactive`
- "Report Issue" → Sets status to `maintenance`

### Automatic Status Management
- Auto-activate based on route schedule
- Auto-deactivate after idle time (e.g., 30 min no movement)
- Integration with dispatch system

### Audit Trail
```sql
CREATE TABLE bus_status_history (
    id BIGSERIAL PRIMARY KEY,
    bus_id VARCHAR(255),
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    changed_by VARCHAR(255),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reason TEXT
);
```

## Testing Checklist

- [ ] Register bus with tracker IMEI
- [ ] Set status to `active`, verify GPS data is saved
- [ ] Set status to `inactive`, verify GPS data is ignored
- [ ] Send GPS data from unregistered IMEI, verify rejection
- [ ] Check Redis for location data (should only exist for active buses)
- [ ] Verify WebSocket broadcasts stop when status changes to inactive
- [ ] Confirm `/api/buses/active` only returns active-status buses

## Summary

This system provides **defense-in-depth** for bus tracking:
1. **Authentication** - Only registered trackers accepted
2. **Authorization** - Only active buses tracked
3. **Resource Optimization** - Redis reduces database load
4. **Real-time Control** - Status changes take immediate effect
5. **Data Integrity** - Historical records only for operational trips

**Key Benefit**: Administrators and drivers can control which buses appear in live tracking systems without physically disconnecting GPS trackers.
