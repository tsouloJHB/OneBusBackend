# Simple Route Creation Test Examples

## Prerequisites
- Spring Boot application running on localhost:8080
- jq installed for JSON formatting (optional)

## Test 1: Create Northbound Route
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

Expected Response (HTTP 201):
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

## Test 2: Create Southbound Route
```bash
curl -X POST "http://localhost:8080/api/routes" \
  -H "Content-Type: application/json" \
  -d '{
    "company": "SimulatedCo",
    "busNumber": "C5",
    "routeName": "C5 Working Test Southbound",
    "description": "Testing southbound direction",
    "direction": "Southbound",
    "startPoint": "Sandton City",
    "endPoint": "Johannesburg CBD",
    "active": true
  }'
```

## Test 3: Test Duplicate Prevention (Should Fail)
```bash
curl -X POST "http://localhost:8080/api/routes" \
  -H "Content-Type: application/json" \
  -d '{
    "company": "SimulatedCo",
    "busNumber": "C5",
    "routeName": "C5 Duplicate",
    "direction": "Northbound"
  }'
```

Expected Response (HTTP 400):
```json
{
  "error": "Route already exists with the same company, bus number, and direction",
  "existingRouteId": 1
}
```

## Test 4: Verify Routes
```bash
curl -X GET "http://localhost:8080/api/routes"
```

## Test 5: Get Specific Route
```bash
curl -X GET "http://localhost:8080/api/routes/1"
```

## Test 6: WebSocket Subscription (After Route Creation)
```javascript
// WebSocket subscription payload
{
  "busNumber": "C5",
  "direction": "Northbound",
  "latitude": -26.2041,
  "longitude": 28.0473,
  "busStopIndex": 3
}
```

## Run Comprehensive Tests
```bash
./test_route_creation.sh
```

## Valid Direction Values
- Northbound
- Southbound  
- Eastbound
- Westbound
- Bidirectional

## API Endpoints Summary
- `POST /api/routes` - Create new route with direction
- `GET /api/routes` - List all routes
- `GET /api/routes/{id}` - Get specific route
- `PUT /api/routes/{id}` - Update existing route
- `DELETE /api/routes/{routeId}/stops/{stopId}` - Delete route stop

## WebSocket Integration
After creating routes, clients can subscribe to real-time updates using:
- WebSocket endpoint: `/ws`
- Subscribe message: `/app/subscribe`
- Response topic: `/user/topic/subscription/status`
