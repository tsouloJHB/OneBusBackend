# WebSocket Cleanup Test Guide

## Overview
This guide provides instructions for testing the enhanced WebSocket cleanup functionality that was implemented to fix the issue where WebSocket connections remained active after bus arrival.

## Problem Fixed
- **Issue**: WebSocket connections were not being properly closed when buses arrived at destinations
- **Symptoms**: Active sessions remained in metrics dashboard even after "Bus Has Arrived" modal was dismissed
- **Root Cause**: The `onCancel` callback in Flutter streams was not being triggered reliably when `_stopTrackingOnArrival()` was called

## Solution Implemented
Enhanced the WebSocket cleanup process with multiple layers:

1. **Direct WebSocket Cleanup**: Added `BusCommunicationServices` instance storage and direct cleanup calls
2. **Enhanced `_stopTrackingOnArrival()` Method**: Added direct service cleanup before stream subscription cleanup
3. **Arrival Modal Button Cleanup**: Added direct cleanup to both "Go Home" and "Track Another Bus" buttons
4. **Widget Dispose Cleanup**: Added cleanup in the `dispose()` method as a safety net

## Files Modified

### Flutter App (`OneBus/`)
- `lib/views/maps/track_bus.dart`: Enhanced cleanup in multiple methods
- `lib/services/bus_communication_services.dart`: Added instance-based cleanup methods

### Backend (`OneBusBackend/`)
- `src/main/java/com/backend/onebus/controller/BusStreamingController.java`: Cleanup endpoint
- `src/main/java/com/backend/onebus/service/MetricsService.java`: Session tracking

## Testing Instructions

### Prerequisites
1. Backend server running on `localhost:8080`
2. Bus simulator running (`python3 simulate_c5_buses.py`)
3. Android emulator or device connected
4. Node.js with axios installed for monitoring script

### Step 1: Start Monitoring
```bash
cd OneBusBackend
node test-websocket-cleanup-detailed.js
```

### Step 2: Test WebSocket Cleanup
1. **Initial State Check**: Monitor shows 0 active sessions
2. **Connect Flutter App**:
   - Open Flutter app on emulator
   - Select "Track Bus"
   - Choose "C5" bus
   - Select direction (Southbound recommended - has active buses)
   - Select any bus stop
   - Wait for tracking to start
3. **Verify Connection**: Monitor should show 1 active session
4. **Test Bus Arrival**:
   - Wait for bus to reach destination (distance < 100m)
   - "Bus Has Arrived!" modal should appear
   - Click either "Go Home" or "Track Another Bus"
5. **Verify Cleanup**: Monitor should show 0 active sessions within 2-5 seconds

### Step 3: Alternative Testing Methods

#### Manual Metrics Check
```bash
# Check active sessions
curl -s http://localhost:8080/api/metrics/sessions | jq '.[] | select(.disconnectedAt == null)'

# Check all sessions (should show disconnectedAt for completed sessions)
curl -s http://localhost:8080/api/metrics/sessions | jq '.'
```

#### Admin Dashboard Monitoring
1. Open `http://localhost:3000` (React admin dashboard)
2. Navigate to "Metrics" page
3. Monitor "Active WebSocket Sessions" section in real-time

## Expected Results

### ✅ Success Indicators
- Active sessions count drops to 0 after bus arrival modal is dismissed
- No persistent WebSocket connections in metrics
- Debug logs show successful cleanup messages:
  ```
  DEBUG: Calling direct WebSocket cleanup on bus service instance...
  DEBUG: Direct WebSocket cleanup completed successfully
  ```

### ❌ Failure Indicators
- Active sessions remain after modal dismissal
- Duplicate sessions appear when tracking multiple buses
- Error messages in cleanup process

## Debug Information

### Flutter Debug Logs
Look for these debug messages in Flutter console:
```
DEBUG: ===== STOPPING TRACKING ON ARRIVAL =====
DEBUG: Calling direct WebSocket cleanup on bus service instance...
DEBUG: Direct WebSocket cleanup completed successfully
DEBUG: Closing bus stream subscription...
DEBUG: Stream subscription closed
```

### Backend Debug Logs
Look for these messages in Spring Boot console:
```
Manual cleanup requested for session {sessionId}
```

## Troubleshooting

### Issue: Sessions Still Active After Cleanup
1. Check Flutter debug logs for cleanup errors
2. Verify backend cleanup endpoint is being called
3. Check network connectivity between Flutter and backend

### Issue: Duplicate Sessions
1. Ensure previous session is cleaned up before starting new tracking
2. Check if multiple stream subscriptions are being created

### Issue: App Crashes During Cleanup
1. Check for null pointer exceptions in cleanup code
2. Verify service instance is properly initialized

## Performance Impact
- Minimal performance impact
- Cleanup operations are asynchronous
- Added 200-500ms delay for proper cleanup sequencing

## Monitoring Commands

```bash
# Real-time session monitoring
watch -n 2 'curl -s http://localhost:8080/api/metrics/sessions | jq "length"'

# Check backend logs
tail -f spring-boot.log | grep -i "cleanup\|session"

# Flutter logs (if using VS Code)
# Check Debug Console for cleanup debug messages
```

## Success Criteria
1. ✅ No active WebSocket sessions after bus arrival
2. ✅ Clean session transitions when tracking multiple buses
3. ✅ No memory leaks or resource accumulation
4. ✅ Proper error handling during cleanup failures
5. ✅ Consistent behavior across different user flows (Go Home vs Track Another Bus)