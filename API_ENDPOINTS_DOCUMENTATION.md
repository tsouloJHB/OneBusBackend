# OneBus Backend API Documentation

## Overview
This document provides comprehensive documentation for all route management endpoints in the OneBus backend API, including GET, POST, PUT, and DELETE operations.

---

## 🛣️ Route Management Endpoints

### 1. GET /api/routes
**Description:** Retrieve all active bus routes

**Method:** `GET`  
**URL:** `http://localhost:8080/api/routes`

**Parameters:** None

**Response Codes:**
- `200 OK`: Routes retrieved successfully

**Example Request:**
```bash
curl -X GET "http://localhost:8080/api/routes" \
  -H "Accept: application/json"
```

**Example Response:**
```json
[
  {
    "id": 1,
    "company": "Rea Vaya",
    "busNumber": "C5",
    "routeName": "Johannesburg CBD to Sandton",
    "description": "Main route connecting CBD to Sandton",
    "active": true
  },
  {
    "id": 2,
    "company": "Rea Vaya", 
    "busNumber": "A1",
    "routeName": "Airport Route",
    "description": "OR Tambo to Sandton",
    "active": true
  }
]
```

---

### 2. GET /api/routes/{routeId}
**Description:** Retrieve a specific route by ID with all stops

**Method:** `GET`  
**URL:** `http://localhost:8080/api/routes/{routeId}`

**Parameters:**
- `routeId` (path parameter): The ID of the route to retrieve (required)

**Response Codes:**
- `200 OK`: Route found and returned
- `404 Not Found`: Route with specified ID does not exist

**Example Request:**
```bash
curl -X GET "http://localhost:8080/api/routes/1" \
  -H "Accept: application/json"
```

**Example Response:**
```json
{
  "id": 1,
  "company": "Rea Vaya",
  "busNumber": "C5",
  "routeName": "Johannesburg CBD to Sandton",
  "description": "Main route connecting CBD to Sandton",
  "active": true,
  "stops": [
    {
      "id": 30,
      "latitude": -26.2041,
      "longitude": 28.0473,
      "address": "Wits Bus Station",
      "busStopIndex": 0,
      "direction": "Northbound",
      "type": "Bus station",
      "northboundIndex": null,
      "southboundIndex": null
    },
    {
      "id": 31,
      "latitude": -26.1951,
      "longitude": 28.0312,
      "address": "Helen Joseph Hospital",
      "busStopIndex": 1,
      "direction": "Northbound",
      "type": "Bus stop",
      "northboundIndex": null,
      "southboundIndex": null
    }
  ]
}
```

---

### 3. POST /api/routes/import-json
**Description:** Import routes and stops from JSON data

**Method:** `POST`  
**URL:** `http://localhost:8080/api/routes/import-json`

**Content-Type:** `application/json`

**Request Body:** Array of route objects
```json
[
  {
    "company": "string (required, max 100 chars)",
    "busNumber": "string (required, max 20 chars)",
    "routeName": "string (required, max 200 chars)",
    "description": "string (optional, max 500 chars)",
    "stops": [
      {
        "coordinates": {
          "latitude": "number (required, -90 to 90)",
          "longitude": "number (required, -180 to 180)"
        },
        "address": "string (optional, max 255 chars)",
        "bus_stop_index": "number (optional)",
        "direction": "string (optional, max 50 chars)",
        "type": "string (optional, max 50 chars)",
        "bus_stop_indices": {
          "northbound": "number (optional)",
          "southbound": "number (optional)"
        }
      }
    ]
  }
]
```

**Response Codes:**
- `200 OK`: Routes imported successfully
- `400 Bad Request`: Invalid JSON data or import failed

**Example Request:**
```bash
curl -X POST "http://localhost:8080/api/routes/import-json" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "company": "Rea Vaya",
      "busNumber": "C5",
      "routeName": "Test Route",
      "description": "Test route for demonstration",
      "stops": [
        {
          "coordinates": {
            "latitude": -26.2041,
            "longitude": 28.0473
          },
          "address": "Wits Bus Station",
          "bus_stop_index": 0,
          "direction": "Northbound",
          "type": "Bus station"
        }
      ]
    }
  ]'
```

**Example Response:**
```json
{
  "message": "Routes imported successfully"
}
```

---

### 4. POST /api/routes
**Description:** Create a new route with direction support

**Method:** `POST`  
**URL:** `http://localhost:8080/api/routes`

**Content-Type:** `application/json`

**Request Body:** `RouteCreateDTO`
```json
{
  "company": "string (required, max 100 chars)",
  "busNumber": "string (required, max 20 chars)",
  "routeName": "string (required, max 200 chars)",
  "description": "string (optional, max 500 chars)",
  "direction": "string (required, Northbound|Southbound|Eastbound|Westbound|Bidirectional)",
  "startPoint": "string (optional, max 255 chars)",
  "endPoint": "string (optional, max 255 chars)",
  "active": "boolean (optional, default: true)"
}
```

**Response Codes:**
- `201 Created`: Route created successfully
- `400 Bad Request`: Invalid input data or route already exists
- `500 Internal Server Error`: Server error occurred

**🔧 Key Features:**
- **Direction Support**: Routes must specify direction (Northbound, Southbound, etc.)
- **Duplicate Prevention**: Same company + busNumber + direction combination not allowed
- **Optional Start/End Points**: Can specify route terminals
- **Validation**: Comprehensive input validation with meaningful error messages

**Example Request - Create Northbound Route:**
```bash
curl -X POST "http://localhost:8080/api/routes" \
  -H "Content-Type: application/json" \
  -d '{
    "company": "SimulatedCo",
    "busNumber": "C5",
    "routeName": "C5 Working Test",
    "description": "Testing after fixing H2 scope issue",
    "direction": "Northbound",
    "startPoint": "Johannesburg CBD",
    "endPoint": "Sandton City",
    "active": true
  }'
```

**Example Request - Create Southbound Route:**
```bash
curl -X POST "http://localhost:8080/api/routes" \
  -H "Content-Type: application/json" \
  -d '{
    "company": "SimulatedCo",
    "busNumber": "C5",
    "routeName": "C5 Working Test Southbound",
    "direction": "Southbound",
    "startPoint": "Sandton City",
    "endPoint": "Johannesburg CBD"
  }'
```

**Example Success Response:**
```json
{
  "message": "Route created successfully",
  "route": {
    "id": 1,
    "company": "SimulatedCo",
    "busNumber": "C5",
    "routeName": "C5 Working Test",
    "description": "Testing after fixing H2 scope issue",
    "direction": "Northbound",
    "startPoint": "Johannesburg CBD",
    "endPoint": "Sandton City",
    "active": true
  }
}
```

**Example Error Response (Duplicate Route):**
```json
{
  "error": "Route already exists with the same company, bus number, and direction",
  "existingRouteId": 1
}
```

**Valid Direction Values:**
- `Northbound` - North direction travel
- `Southbound` - South direction travel  
- `Eastbound` - East direction travel
- `Westbound` - West direction travel
- `Bidirectional` - Both directions on same route

---

### 5. PUT /api/routes/{routeId}
**Description:** Update an existing route with intelligent stop management

**Method:** `PUT`  
**URL:** `http://localhost:8080/api/routes/{routeId}`

**Parameters:**
- `routeId` (path parameter): The ID of the route to update (required)

**Content-Type:** `application/json`

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
      "busStopIndex": "number (optional - auto-managed with smart conflict resolution)",
      "direction": "string (optional, max 50 chars)",
      "type": "string (optional, max 50 chars)",
      "northboundIndex": "number (optional)",
      "southboundIndex": "number (optional)"
    }
  ]
}
```

**Response Codes:**
- `200 OK`: Route updated successfully
- `400 Bad Request`: Invalid input data (validation errors)
- `404 Not Found`: Route with specified ID does not exist
- `500 Internal Server Error`: Server error occurred

**🔧 Smart Index Management Features:**

1. **Automatic Conflict Resolution**: When adding a stop at an existing index, conflicting stops are automatically shifted
2. **Direction-Aware Management**: Index conflicts resolved per direction
3. **Update vs Insert Logic**: 
   - With `id`: Updates existing stop
   - Without `id`: Creates new stop with conflict resolution
4. **Bidirectional Support**: Handles complex routing with direction-specific indices

**Example Request - Add New Stop with Smart Index Management:**
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

**Example Request - Update Existing Stop:**
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

**Example Request - Update Route Metadata + Add Stops:**
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

**Example Response:**
```json
{
  "message": "Route updated successfully",
  "routeId": 1
}
```

---

### 6. DELETE /api/routes/{routeId}/stops/{stopId}
**Description:** Delete a specific stop from a route

**Method:** `DELETE`  
**URL:** `http://localhost:8080/api/routes/{routeId}/stops/{stopId}`

**Parameters:**
- `routeId` (path parameter): The ID of the route (required)
- `stopId` (path parameter): The ID of the stop to delete (required)

**Response Codes:**
- `200 OK`: Stop deleted successfully
- `400 Bad Request`: Stop does not belong to the specified route
- `404 Not Found`: Route or stop not found
- `500 Internal Server Error`: Server error occurred

**Example Request:**
```bash
curl -X DELETE "http://localhost:8080/api/routes/1/stops/30" \
  -H "Accept: application/json"
```

**Example Response:**
```json
{
  "message": "Stop deleted successfully",
  "deletedStopId": 30,
  "routeId": 1
}
```

---

## 🛡️ Validation Rules

### Route Fields
- `company`: Maximum 100 characters
- `busNumber`: Maximum 20 characters
- `routeName`: Maximum 200 characters
- `description`: Maximum 500 characters
- `active`: Boolean value

### Stop Fields
- `latitude`: Must be between -90 and 90
- `longitude`: Must be between -180 and 180
- `address`: Maximum 255 characters
- `direction`: Maximum 50 characters
- `type`: Maximum 50 characters
- `busStopIndex`: Positive integer
- `northboundIndex`: Positive integer
- `southboundIndex`: Positive integer

### Required Fields for New Stops
- `latitude` and `longitude` are required when creating new stops
- Existing stops can be updated with partial data

---

## 🔍 Error Handling

### Common Error Responses

**400 Bad Request - Validation Error:**
```json
{
  "error": "Validation failed: Latitude must be between -90 and 90, Address must not exceed 255 characters"
}
```

**404 Not Found - Route Not Found:**
```json
{
  "error": "Route with ID 999 not found"
}
```

**404 Not Found - Stop Not Found:**
```json
{
  "error": "Stop with ID 999 not found"
}
```

**400 Bad Request - Stop/Route Mismatch:**
```json
{
  "error": "Stop does not belong to the specified route"
}
```

**500 Internal Server Error:**
```json
{
  "error": "Failed to update route: Database connection error"
}
```

---

## 🧪 Testing Examples

### Test Complete Route Management Workflow

1. **Create a new route:**
```bash
curl -X POST "http://localhost:8080/api/routes/import-json" \
  -H "Content-Type: application/json" \
  -d '[{"company":"Test Co","busNumber":"T1","routeName":"Test Route","stops":[]}]'
```

2. **Get all routes:**
```bash
curl -X GET "http://localhost:8080/api/routes"
```

3. **Add stops to the route:**
```bash
curl -X PUT "http://localhost:8080/api/routes/1" \
  -H "Content-Type: application/json" \
  -d '{
    "stops": [
      {
        "latitude": -26.2041,
        "longitude": 28.0473,
        "address": "First Stop",
        "busStopIndex": 0,
        "direction": "Northbound"
      },
      {
        "latitude": -26.1951,
        "longitude": 28.0312,
        "address": "Second Stop", 
        "busStopIndex": 1,
        "direction": "Northbound"
      }
    ]
  }'
```

4. **Get specific route with stops:**
```bash
curl -X GET "http://localhost:8080/api/routes/1"
```

5. **Update a specific stop:**
```bash
curl -X PUT "http://localhost:8080/api/routes/1" \
  -H "Content-Type: application/json" \
  -d '{
    "stops": [
      {
        "id": 30,
        "address": "Updated Stop Name"
      }
    ]
  }'
```

6. **Delete a stop:**
```bash
curl -X DELETE "http://localhost:8080/api/routes/1/stops/31"
```

---

## 📊 Smart Index Management Examples

### Scenario 1: Insert Stop at Beginning
**Before:** Stop A(0) → Stop B(1) → Stop C(2)  
**Action:** Add new stop with index 0  
**After:** New Stop(0) → Stop A(1) → Stop B(2) → Stop C(3)

```bash
curl -X PUT "http://localhost:8080/api/routes/1" \
  -H "Content-Type: application/json" \
  -d '{
    "stops": [
      {
        "latitude": -26.2000,
        "longitude": 28.0400,
        "address": "New First Stop",
        "busStopIndex": 0,
        "direction": "Northbound"
      }
    ]
  }'
```

### Scenario 2: Insert Stop in Middle
**Before:** Stop A(0) → Stop B(1) → Stop C(2) → Stop D(3)  
**Action:** Add new stop with index 2  
**After:** Stop A(0) → Stop B(1) → New Stop(2) → Stop C(3) → Stop D(4)

```bash
curl -X PUT "http://localhost:8080/api/routes/1" \
  -H "Content-Type: application/json" \
  -d '{
    "stops": [
      {
        "latitude": -26.1980,
        "longitude": 28.0350,
        "address": "Middle Stop",
        "busStopIndex": 2,
        "direction": "Northbound"
      }
    ]
  }'
```

### Scenario 3: Direction-Specific Updates
**Action:** Add stop that only affects Southbound direction

```bash
curl -X PUT "http://localhost:8080/api/routes/1" \
  -H "Content-Type: application/json" \
  -d '{
    "stops": [
      {
        "latitude": -26.1970,
        "longitude": 28.0320,
        "address": "Southbound Only Stop",
        "busStopIndex": 1,
        "direction": "Southbound"
      }
    ]
  }'
```

---

## 🔗 Related Endpoints

### Bus Tracking Endpoints
- `POST /api/tracker/payload` - Receive GPS tracking data
- `GET /api/buses/nearest` - Find nearest bus to location
- `GET /api/buses/active` - Get all active buses
- `GET /api/buses` - Get all buses
- `POST /api/buses` - Create new bus

### Utility Endpoints  
- `GET /api/routes/{busNumber}/{direction}/buses` - Get available buses for route
- `GET /api/debug/subscriptions` - Debug subscription status
- `GET /api/debug/websocket-test` - Test WebSocket connectivity

---

## 📝 API Integration Notes

1. **Content-Type**: Always use `application/json` for POST/PUT requests
2. **Base URL**: Default is `http://localhost:8080` (configurable via application.properties)
3. **Swagger Documentation**: Available at `http://localhost:8080/swagger-ui.html`
4. **CORS**: Configured to allow cross-origin requests for development
5. **Database**: Uses H2 in-memory database for development/testing
6. **Logging**: Comprehensive logging available for debugging route operations

---

## 🎯 Best Practices

1. **Always validate coordinates** before sending requests
2. **Use specific stop IDs** when updating existing stops
3. **Leverage smart index management** instead of manual index calculation
4. **Check route existence** before attempting stop operations
5. **Use direction-specific operations** for complex routes
6. **Test with curl** before integrating into applications
7. **Monitor logs** for index conflict resolution details
8. **Handle HTTP error codes** appropriately in client applications

This comprehensive API documentation covers all route management operations with practical examples and best practices for successful integration!