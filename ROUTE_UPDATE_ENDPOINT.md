# Enhanced Route Update Endpoint with Smart Index Management

## Overview
This document describes the enhanced PUT endpoint for updating existing routes with intelligent bus stop index management in the OneBus backend API.

## 🚌 Bus Stop Index Management System

### **The Problem**
When adding a new bus stop with `"busStopIndex": 2`, traditional systems create index conflicts:

```
BEFORE: Stop A(1) → Stop B(2) → Stop C(3)
ADD: New Stop with index 2
RESULT: Stop A(1) → Stop B(2) → New Stop(2) → Stop C(3)  ❌ CONFLICT!
```

### **The Solution: Smart Index Management**
Our enhanced system automatically resolves conflicts by shifting existing stops:

```
BEFORE: Stop A(1) → Stop B(2) → Stop C(3)
ADD: New Stop with index 2
RESULT: Stop A(1) → New Stop(2) → Stop B(3) → Stop C(4)  ✅ RESOLVED!
```

## Endpoint Details

### PUT /api/routes/{routeId}
Updates an existing route with comprehensive stop management.

**URL:** `PUT /api/routes/{routeId}`

**Parameters:**
- `routeId` (path parameter): The ID of the route to update (required)

**Request Body:** `RouteUpdateDTO`
```json
{
  "company": "string (optional, max 100 chars)",
  "busNumber": "string (optional, max 20 chars)", 
  "routeName": "string (optional, max 200 chars)",
  "description": "string (optional, max 500 chars)",
  "active": "boolean (optional)",
  "stops": [
    {
      "id": "number (optional - for updating existing stops)",
      "latitude": "number (required for new stops, -90 to 90)",
      "longitude": "number (required for new stops, -180 to 180)",
      "address": "string (optional, max 255 chars)",
      "busStopIndex": "number (optional - auto-managed)",
      "direction": "string (optional, max 50 chars)",
      "type": "string (optional, max 50 chars)",
      "northboundIndex": "number (optional)",
      "southboundIndex": "number (optional)"
    }
  ]
}
```

**Response Codes:**
- `200 OK`: Route updated successfully with complete stop information
- `400 Bad Request`: Invalid input data (validation errors)
- `404 Not Found`: Route with specified ID does not exist
- `500 Internal Server Error`: Server error occurred

## 🔧 Index Management Features

### 1. **Automatic Conflict Resolution**
When you add a stop at an existing index, conflicting stops are automatically shifted:

**Request:**
```json
{
  "stops": [
    {
      "latitude": -26.20282,
      "longitude": 28.04011,
      "address": "Library Gardens Bus Station",
      "busStopIndex": 2,
      "direction": "Southbound",
      "type": "Bus station"
    }
  ]
}
```

**What Happens:**
1. System finds existing stops at index ≥ 2
2. Automatically shifts them: 2→3, 3→4, 4→5, etc.
3. Inserts new stop at index 2
4. Logs all changes for transparency

### 2. **Direction-Aware Management**
Index conflicts are resolved per direction to avoid unnecessary shifts:

```json
{
  "stops": [
    {
      "busStopIndex": 2,
      "direction": "Northbound"  // Only affects Northbound stops
    }
  ]
}
```

### 3. **Update vs Insert Logic**
- **With `id`**: Updates existing stop, preserves index if not specified
- **Without `id`**: Creates new stop, manages index conflicts

### 4. **Bidirectional Stop Support**
Supports complex routing with direction-specific indices:

```json
{
  "direction": "bidirectional",
  "northboundIndex": 5,
  "southboundIndex": 12
}
```

## 📝 Usage Examples

### Example 1: Add New Stop (Auto Index Management)
**Request:**
```bash
curl -X PUT "http://localhost:8080/api/routes/1" \
  -H "Content-Type: application/json" \
  -d '{
    "stops": [
      {
        "latitude": -26.20282,
        "longitude": 28.04011,
        "address": "Library Gardens Bus Station",
        "busStopIndex": 2,
        "direction": "Southbound",
        "type": "Bus station"
      }
    ]
  }'
```

**System Actions:**
1. ✅ Validates coordinates (-90≤lat≤90, -180≤lon≤180)
2. ✅ Finds existing stops at index ≥2 in Southbound direction
3. ✅ Shifts conflicting stops: index+1
4. ✅ Inserts new stop at index 2
5. ✅ Returns complete updated route with all stops

### Example 2: Update Existing Stop
**Request:**
```bash
curl -X PUT "http://localhost:8080/api/routes/1" \
  -H "Content-Type: application/json" \
  -d '{
    "stops": [
      {
        "id": 30,
        "address": "Updated Address",
        "type": "Enhanced Bus Station"
      }
    ]
  }'
```

**System Actions:**
1. ✅ Finds stop with ID 30
2. ✅ Updates only specified fields
3. ✅ Preserves existing index and coordinates
4. ✅ No index conflicts since position unchanged

### Example 3: Mixed Update - Route + Stops
**Request:**
```bash
curl -X PUT "http://localhost:8080/api/routes/1" \
  -H "Content-Type: application/json" \
  -d '{
    "routeName": "Enhanced C5 Route",
    "description": "Route with smart stop management",
    "stops": [
      {
        "id": 30,
        "address": "Updated Existing Stop"
      },
      {
        "latitude": -26.20500,
        "longitude": 28.04300,
        "address": "New Terminal",
        "busStopIndex": 1,
        "direction": "Northbound",
        "type": "Terminal"
      }
    ]
  }'
```

## 📊 Response Format

**Successful Response:**
```json
{
  "message": "Route updated successfully",
  "route": {
    "id": 1,
    "company": "Rea Vaya",
    "busNumber": "C5",
    "routeName": "Enhanced C5 Route",
    "description": "Route with smart stop management",
    "active": true,
    "stopsCount": 5
  },
  "stops": [
    {
      "id": 45,
      "latitude": -26.20500,
      "longitude": 28.04300,
      "address": "New Terminal",
      "busStopIndex": 1,
      "direction": "Northbound",
      "type": "Terminal",
      "northboundIndex": null,
      "southboundIndex": null
    },
    {
      "id": 30,
      "latitude": -26.2041,
      "longitude": 28.0473,
      "address": "Updated Existing Stop",
      "busStopIndex": 2,
      "direction": "Northbound",
      "type": "Bus stop",
      "northboundIndex": null,
      "southboundIndex": null
    }
  ]
}
```

## 🛡️ Validation & Error Handling

### Coordinate Validation
```json
{
  "latitude": 95.0,    // ❌ Error: Must be -90 to 90
  "longitude": 200.0   // ❌ Error: Must be -180 to 180
}
```

### Field Length Validation
```json
{
  "address": "Very long address that exceeds 255 characters...",  // ❌ Error
  "direction": "Very long direction name that exceeds 50 chars", // ❌ Error
  "type": "Very long type name that exceeds 50 characters"       // ❌ Error
}
```

### Error Response Example
```json
{
  "error": "Validation failed: Latitude must be between -90 and 90, Address must not exceed 255 characters"
}
```

## 🧪 Testing

Use the enhanced test script to verify all functionality:

```bash
chmod +x test_route_update.sh
./test_route_update.sh
```

**Test Coverage:**
- ✅ Index conflict resolution
- ✅ New stop insertion
- ✅ Existing stop updates
- ✅ Direction-aware management
- ✅ Validation error handling
- ✅ Mixed route + stops updates
- ✅ Edge cases (index 1 insertion)

## 🔍 Logging & Debugging

The system provides comprehensive logging:

```
INFO: Found 3 conflicting stops at index 2 or higher. Shifting indices...
DEBUG: Shifted stop "Main Road Station" from index 2 to 3
DEBUG: Shifted stop "Hospital Stop" from index 3 to 4
DEBUG: Shifted stop "University Campus" from index 4 to 5
INFO: Successfully shifted 3 stops to accommodate new stop at index 2
INFO: Route and 1 stops updated successfully for route ID: 1
```

## 🔗 Integration

### Swagger Documentation
Full OpenAPI documentation available at: `http://localhost:8080/swagger-ui.html`

### Related Endpoints
- `GET /api/routes` - List all routes with stops
- `GET /api/routes/{routeId}` - Get specific route with complete stop information
- `POST /api/routes/import-json` - Bulk import routes with automatic index management

## 🎯 Key Benefits

1. **🚫 No Index Conflicts**: Automatic resolution prevents duplicate indices
2. **🔄 Intelligent Shifting**: Minimal disruption to existing stop order
3. **📍 Direction Awareness**: Separate index management per direction
4. **🔧 Flexible Updates**: Support for partial updates without breaking existing data
5. **📝 Comprehensive Logging**: Full audit trail of all index changes
6. **✅ Robust Validation**: Prevents invalid coordinate and field data
7. **📊 Complete Responses**: Returns full route state after updates

This enhanced system ensures data integrity while providing maximum flexibility for route management!
