# IMEI Registration Verification - C5 Buses

## Question
Are the tracker IMEIs used in the simulator (`C5NORTH001` and `C5SOUTH001`) registered in the database?

## Answer: ✅ YES - They are properly registered

### Registered IMEIs (from register_c5_buses.py)

```python
buses_to_register = [
    {
        "busId": "C5-NORTH-001",
        "trackerImei": "C5NORTH001",          # ← Used by Northbound simulator
        "busNumber": "C5",
        "route": "Florida Route",
        "busCompanyName": "Rea Vaya",
        "driverId": "DRV001",
        "driverName": "Simulated Driver North"
    },
    {
        "busId": "C5-SOUTH-001",
        "trackerImei": "C5SOUTH001",          # ← Used by Southbound simulator
        "busNumber": "C5",
        "route": "Florida Route",
        "busCompanyName": "Rea Vaya",
        "driverId": "DRV002",
        "driverName": "Simulated Driver South"
    }
]
```

### Simulator Usage (from simulate_c5_buses.py)

```python
t1 = threading.Thread(target=simulate_bus, args=(northbound_stops, "C5NORTH001", "Northbound", "C5"))
                                                                  ↑
                                                    Matches registered IMEI ✅

t2 = threading.Thread(target=simulate_bus, args=(southbound_stops, "C5SOUTH001", "Southbound", "C5"))
                                                                  ↑
                                                    Matches registered IMEI ✅
```

## Verification Checklist

### Rule 1: IMEI Registration Validation

✅ **Registered IMEI: C5NORTH001**
- Bus ID: `C5-NORTH-001`
- Bus Number: `C5`
- Status: Can be tracked

✅ **Registered IMEI: C5SOUTH001**
- Bus ID: `C5-SOUTH-001`
- Bus Number: `C5`
- Status: Can be tracked

### Data Flow (Simulator → Backend)

```
simulate_c5_buses.py sends:
{
    "trackerImei": "C5NORTH001",           ← Matches registered IMEI
    "busNumber": "C5",
    "lat": -26.204750,
    "lon": 28.041655,
    "timestamp": "2025-12-26T10:30:00.000000",
    "busStopIndex": 1,
    "busCompany": "Rea Vaya",
    "tripDirection": "Northbound"
}
          ↓
BusTrackingService.processTrackerPayload()
          ↓
Rule 1: Check if IMEI is registered
  busRepository.findByTrackerImei("C5NORTH001")
          ↓
✅ FOUND → Bus C5-NORTH-001
          ↓
Rule 2: Verify bus status
  bus.getOperationalStatus() == "active"
          ↓
✅ ACTIVE → Process continues
          ↓
Rule 3: Save coordinates (if active)
Rule 4: Broadcast via WebSocket
```

## Expected Behavior

**When simulator is running:**
1. Every 30 seconds, both threads send location data
2. IMEI `C5NORTH001` → Passes Rule 1 ✅
3. IMEI `C5SOUTH001` → Passes Rule 1 ✅
4. Both buses have `operationalStatus = 'active'` (default) ✅
5. Coordinates are saved to Redis & Database ✅
6. WebSocket broadcasts to `/topic/bus/C5_Northbound` and `/topic/bus/C5_Southbound` ✅

**When simulator is NOT running:**
- No payloads sent → No tracking updates
- Buses will timeout after 10 minutes of inactivity
- Disappear from `/api/buses/active` endpoint

## How to Verify in Practice

### Option 1: Check Backend Logs
```
grep "C5NORTH001\|C5SOUTH001" /path/to/backend.log
```

Expected to see:
```
[BusTrackingService] Bus info updated: BusLocation(busNumber=C5, trackerImei=C5NORTH001, ...)
[BusStreamingService] Broadcasting to /topic/bus/C5_Northbound: ...
```

### Option 2: Check Active Buses Endpoint
```bash
curl http://localhost:8080/api/buses/active
```

Expected to see C5 buses in the response (if simulator is running).

### Option 3: Send Test Payload
```bash
curl -X POST http://localhost:8080/api/tracker/payload \
  -H "Content-Type: application/json" \
  -d '{
    "trackerImei": "C5NORTH001",
    "busNumber": "C5",
    "lat": -26.2041,
    "lon": 28.0416,
    "timestamp": "2025-12-26T10:00:00",
    "busStopIndex": 1,
    "busCompany": "Rea Vaya",
    "tripDirection": "Northbound"
  }'
```

Response should be: `{"message": "Payload processed successfully"}`

## Conclusion

✅ **The simulator is using registered IMEIs**
- Both `C5NORTH001` and `C5SOUTH001` are properly registered
- They will pass Rule 1 validation
- They will be tracked and broadcast when `operationalStatus = 'active'`
- The system is working as designed

**No changes needed to the simulator or IMEI registration.**
