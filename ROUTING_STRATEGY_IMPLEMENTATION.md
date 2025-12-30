# Routing Strategy Implementation - Integration Complete

## Summary
Successfully implemented a **company-specific routing strategy pattern** in the OneBus backend to intelligently infer bus direction when trackers only provide latitude/longitude without trip direction or bus stop index.

## Architecture Overview

### Design Pattern: Strategy Pattern
- **Base Class**: `BusCompanyRoutingStrategy` (abstract)
- **Concrete Strategies**: 
  - `ReaVayaRoutingStrategy` - Rea Vaya specific logic
  - `MetroBusRoutingStrategy` - Metro Bus specific logic (uses Rea Vaya fallback)
  - `DefaultRoutingStrategy` - Unknown companies (uses Rea Vaya fallback)
- **Factory**: `BusCompanyStrategyFactory` - Selects appropriate strategy

### Benefits
1. **Extensible**: New bus companies can be added without modifying existing code
2. **Encapsulated**: Company-specific logic isolated in dedicated classes
3. **Testable**: Each strategy can be tested independently
4. **Maintainable**: Clear separation of concerns
5. **Safe**: Try-catch fallback ensures existing logic continues if strategy fails

---

## Global Rules (Apply to ALL Companies)

### Rule 1: First GPS Point Detection
**When**: Bus location received with NO direction and `busStopIndex` is 0 or null  
**Action**: Set direction to "Southbound"  
**Reason**: First observation of any bus should default to a consistent direction

### Rule 2: End-of-Route Direction Switching
**When**: Bus reaches the end of its route (`busStopIndex == total stops in direction`)  
**Action**: Switch to opposite direction (Northbound ↔ Southbound)  
**Reason**: Ensure buses complete full route loops and don't get stuck at endpoints

---

## Implementation Files Created

### 1. BusCompanyRoutingStrategy.java (Base Class)
**Location**: `src/main/java/com/backend/onebus/service/routing/`

**Key Methods**:
- `applyGlobalRules()` - Implements global rules for all companies
- `inferDirection()` - Abstract method for company-specific direction inference
- `handleEndOfRoute()` - Abstract method for end-of-route handling
- `getTotalStopsInDirection()` - Get stop count for a direction
- `isNearStop()` - Check proximity to a stop
- `distanceMeters()` - Haversine distance calculation
- `switchDirection()` - Toggle between Northbound/Southbound

**Global Utilities**:
- `getFirstStop()`, `getLastStop()` - Terminal point detection
- `isAtStartStop()`, `isAtEndStop()` - Positional checks
- `STOP_PROXIMITY_METERS = 30.0` - Distance threshold for nearby stops

---

### 2. ReaVayaRoutingStrategy.java
**Location**: `src/main/java/com/backend/onebus/service/routing/`

**Logic**:
1. Detects when bus is at **start stop** → Confirms current direction
2. Detects when bus is at **end stop** → Switches direction
3. During intermediate travel → Uses cached direction

**Code Example**:
```java
if (isAtEndStop(current, route, currentDirection)) {
    String newDirection = switchDirection(currentDirection);
    return newDirection; // Switch to opposite direction
}
```

---

### 3. MetroBusRoutingStrategy.java
**Location**: `src/main/java/com/backend/onebus/service/routing/`

**Current Behavior**: 
- Uses `ReaVayaRoutingStrategy` as fallback
- Can be extended with Metro-specific rules in future

**Future Extension Point**:
```java
// Metro-specific rules can be added here before Rea Vaya fallback:
if (isMetroSpecificCondition()) {
    return handleMetroSpecificLogic();
}
// Fall back to Rea Vaya if no Metro rule applies
return reaVayaFallback.inferDirection(...);
```

---

### 4. DefaultRoutingStrategy.java
**Location**: `src/main/java/com/backend/onebus/service/routing/`

**Purpose**: 
- Handles unknown/new bus companies
- Uses Rea Vaya as default fallback
- Ensures system gracefully handles companies not yet explicitly configured

---

### 5. BusCompanyStrategyFactory.java
**Location**: `src/main/java/com/backend/onebus/service/routing/`

**Responsibility**:
- Selects correct strategy based on company name
- Injects `RouteStopRepository` into strategies
- Logs strategy selection for debugging

**Example**:
```java
BusCompanyRoutingStrategy strategy = factory.getStrategy("Rea Vaya");
// Returns: ReaVayaRoutingStrategy instance
```

---

## Integration into BusTrackingService

### Modified: `BusTrackingService.java`

**Changes**:
1. **Added Dependency Injection**:
   ```java
   @Autowired
   private BusCompanyStrategyFactory strategyFactory;
   ```

2. **Updated `processTrackerPayload()` method** with 3-step strategy application:

   **Step 1**: Apply global rules (first GPS, end-of-route)
   ```java
   String globalDirection = strategy.applyGlobalRules(payload, previousLocation, route);
   if (globalDirection != null) {
       payload.setTripDirection(globalDirection);
   }
   ```

   **Step 2**: Apply company-specific inference
   ```java
   String inferredDirection = strategy.inferDirection(payload, previousLocation, route);
   if (inferredDirection != null) {
       payload.setTripDirection(inferredDirection);
   }
   ```

   **Step 3**: Update busStopIndex based on proximity (existing logic)
   ```java
   BusStop nearbyStop = findNearbyStop(company, busNumber, ...);
   if (nearbyStop != null) {
       payload.setBusStopIndex(nearbyStop.getBusStopIndex());
   }
   ```

3. **Error Handling**: Try-catch wraps strategy execution
   - If strategy fails, falls back to existing proximity logic
   - Logs error with full stack trace for debugging
   - Ensures backward compatibility

---

## Data Flow in processTrackerPayload()

```
Tracker Payload Received
    ↓
[Validate IMEI] → Reject if unregistered
    ↓
[Validate Status] → Ignore if not "active"
    ↓
[Set Bus Metadata] → busId, busNumber, company
    ↓
[Get Strategy] → BusCompanyStrategyFactory.getStrategy(company)
    ↓
[Get Route] → routeRepository.findByCompanyAndBusNumber()
    ↓
[Get Previous Location] → From Redis cache
    ↓
[Step 1: Global Rules] → applyGlobalRules(payload, previous, route)
    ↓ (if direction changed)
[Step 2: Company Inference] → inferDirection(payload, previous, route)
    ↓ (if direction changed)
[Step 3: Proximity Check] → findNearbyStop() and update busStopIndex
    ↓
[Cache in Redis] → 24-hour TTL
    ↓
[Persist to DB] → BusLocationRepository.save()
    ↓
[Broadcast Update] → WebSocket to clients
```

---

## Testing & Verification

### Manual Testing Checklist
- [ ] Start backend with new strategy code
- [ ] Send tracker payload with company="Rea Vaya" (no direction)
- [ ] Verify global rule 1 applies: "Southbound" assigned
- [ ] Move bus to end of route, send payload
- [ ] Verify global rule 2 applies: direction switches
- [ ] Send tracker payload with company="Metro Bus"
- [ ] Verify Metro uses Rea Vaya fallback correctly
- [ ] Test unknown company
- [ ] Verify default strategy uses Rea Vaya fallback
- [ ] Check Redis contains updated direction
- [ ] Verify BusLocation records in database

### Debugging: Check Logs for Strategy Messages
```
[Global Rule 1] Bus 101 first GPS point with no direction and index 0/null - assigning Southbound
[Global Rule 2] Bus 101 reached end of route (stop 42/42), switching Southbound → Northbound
[Strategy] Global rule applied: Bus 101 direction set to Southbound
[Strategy] Company inference applied: Bus 101 direction set to Northbound
[Rea Vaya] Bus 101 at end of Southbound route, switching to Northbound
[Metro Bus] Using Rea Vaya fallback for direction inference
[Default] Unknown company, using Rea Vaya fallback for bus 101
```

---

## Backward Compatibility

✅ **Fully backward compatible**:
- Old tracker payloads with `tripDirection` included continue to work
- Proximity-based detection (within 30m of stop) still functions
- Existing BusTrackingService logic preserved as fallback
- No database schema changes required
- Redis caching unchanged

---

## Future Enhancements

### Adding a New Bus Company Strategy

1. **Create new class** extending `BusCompanyRoutingStrategy`:
   ```java
   public class MyBusCompanyStrategy extends BusCompanyRoutingStrategy {
       @Override
       public String getCompanyName() {
           return "My Bus Company";
       }
       
       @Override
       public String inferDirection(BusLocation current, BusLocation previous, Route route) {
           // Custom logic here
       }
       
       @Override
       public String handleEndOfRoute(BusLocation current, Route route) {
           // Custom logic here
       }
   }
   ```

2. **Register in factory**:
   ```java
   public BusCompanyRoutingStrategy getStrategy(String companyName) {
       if (companyName.equalsIgnoreCase("My Bus Company")) {
           strategy = new MyBusCompanyStrategy();
       }
       // ... rest of logic
   }
   ```

3. **No other changes needed** - polymorphism handles the rest!

---

## Performance Impact

- **Minimal**: Strategy selection is O(1) string comparison
- **Database Queries**: Only fetches route once per payload
- **Distance Calculations**: Only for nearby stops (max ~30 per payload)
- **Logging**: Info level only in production
- **Fallback**: Existing logic maintains same performance if strategy fails

---

## File Locations Summary

| File | Location | Purpose |
|------|----------|---------|
| BusCompanyRoutingStrategy.java | `service/routing/` | Base class with global rules |
| ReaVayaRoutingStrategy.java | `service/routing/` | Rea Vaya-specific logic |
| MetroBusRoutingStrategy.java | `service/routing/` | Metro Bus with Rea Vaya fallback |
| DefaultRoutingStrategy.java | `service/routing/` | Unknown companies with fallback |
| BusCompanyStrategyFactory.java | `service/routing/` | Strategy factory for selection |
| BusTrackingService.java | `service/` | Updated with strategy integration |

---

## Compilation Status

✅ **All files compile successfully**  
✅ **No breaking changes**  
✅ **Ready for testing**  

**Next Steps**:
1. Start backend with new code
2. Monitor logs for strategy application
3. Test with live tracker payloads
4. Verify direction inference accuracy
5. Extend strategies with company-specific rules as needed

---

## Questions & Support

If issues arise:
1. Check `[Strategy]` and `[Global Rule]` log messages
2. Verify route data in database (RouteStop records)
3. Test with known bus number and company
4. Review stack trace in error logs
5. Strategy fallback preserves existing functionality

Implementation follows SOLID principles:
- **S**ingle Responsibility: Each strategy handles one company
- **O**pen/Closed: Open for extension (new companies), closed for modification
- **L**iskov Substitution: All strategies interchangeable via base class
- **I**nterface Segregation: Strategies only implement required methods
- **D**ependency Inversion: Depends on abstractions, not concrete classes
