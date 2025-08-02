#!/bin/bash

# Test script for the enhanced PUT /api/routes/{routeId} endpoint with smart index management

BASE_URL="http://localhost:8080/api"

echo "🧪 Testing Enhanced Route Update Endpoint with Index Management"
echo "=============================================================="

# First, let's get all routes to see what exists
echo -e "\n1. Getting all routes:"
curl -X GET "$BASE_URL/routes" \
  -H "Content-Type: application/json" \
  | jq '.' 2>/dev/null || echo "Failed to parse JSON response"

echo -e "\n2. Testing route update with new stop at index 2 (should shift existing stops):"
# This will demonstrate index conflict resolution
curl -X PUT "$BASE_URL/routes/1" \
  -H "Content-Type: application/json" \
  -d '{
    "company": "Enhanced Rea Vaya",
    "busNumber": "C5",
    "routeName": "Enhanced Thokoza to Johannesburg CBD",
    "description": "Enhanced route with smart index management",
    "active": true,
    "stops": [
      {
        "latitude": -26.20282,
        "longitude": 28.04011,
        "address": "NEW: Library Gardens Bus Station",
        "busStopIndex": 2,
        "direction": "Southbound",
        "type": "Bus station"
      }
    ]
  }' \
  | jq '.' 2>/dev/null || echo "Failed to parse JSON response"

echo -e "\n3. Adding multiple stops with various indices:"
curl -X PUT "$BASE_URL/routes/1" \
  -H "Content-Type: application/json" \
  -d '{
    "stops": [
      {
        "latitude": -26.20300,
        "longitude": 28.04100,
        "address": "Shopping Center Stop",
        "busStopIndex": 1,
        "direction": "Northbound",
        "type": "Bus stop"
      },
      {
        "latitude": -26.20400,
        "longitude": 28.04200,
        "address": "Hospital Stop",
        "busStopIndex": 3,
        "direction": "Southbound",
        "type": "Bus stop"
      },
      {
        "latitude": -26.20500,
        "longitude": 28.04300,
        "address": "University Campus",
        "busStopIndex": 5,
        "direction": "Bidirectional",
        "type": "Bus station"
      }
    ]
  }' \
  | jq '.' 2>/dev/null || echo "Failed to parse JSON response"

echo -e "\n4. Updating existing stop (by ID) without affecting indices:"
# Assuming stop ID 30 exists from previous operations
curl -X PUT "$BASE_URL/routes/1" \
  -H "Content-Type: application/json" \
  -d '{
    "stops": [
      {
        "id": 30,
        "address": "UPDATED: Library Gardens (Modified Address)",
        "type": "Enhanced Bus Station"
      }
    ]
  }' \
  | jq '.' 2>/dev/null || echo "Failed to parse JSON response"

echo -e "\n5. Testing edge case - adding stop at index 1 (should shift all existing stops):"
curl -X PUT "$BASE_URL/routes/1" \
  -H "Content-Type: application/json" \
  -d '{
    "stops": [
      {
        "latitude": -26.20100,
        "longitude": 28.03900,
        "address": "NEW FIRST STOP: Origin Terminal",
        "busStopIndex": 1,
        "direction": "Northbound",
        "type": "Terminal"
      }
    ]
  }' \
  | jq '.' 2>/dev/null || echo "Failed to parse JSON response"

echo -e "\n6. Retrieving final route state to verify index management:"
curl -X GET "$BASE_URL/routes/1" \
  -H "Content-Type: application/json" \
  | jq '.' 2>/dev/null || echo "Failed to parse JSON response"

echo -e "\n7. Testing validation - invalid coordinates:"
curl -X PUT "$BASE_URL/routes/1" \
  -H "Content-Type: application/json" \
  -d '{
    "stops": [
      {
        "latitude": 95.0,
        "longitude": 200.0,
        "address": "Invalid Coordinates Stop",
        "busStopIndex": 10
      }
    ]
  }' \
  -w "\nHTTP Status: %{http_code}\n" \
  | jq '.' 2>/dev/null || echo "Failed to parse JSON response"

echo -e "\n8. Mixed update - route info + stops management:"
curl -X PUT "$BASE_URL/routes/1" \
  -H "Content-Type: application/json" \
  -d '{
    "routeName": "Smart Index Managed Route",
    "description": "Route with intelligent stop index management system",
    "stops": [
      {
        "latitude": -26.20600,
        "longitude": 28.04400,
        "address": "Final Test Stop",
        "busStopIndex": 100,
        "direction": "Southbound",
        "type": "Bus stop"
      }
    ]
  }' \
  | jq '.' 2>/dev/null || echo "Failed to parse JSON response"

echo -e "\n✅ Enhanced Route Update with Index Management Tests Completed!"
echo -e "\n📊 Key Features Tested:"
echo "   • Index conflict resolution (automatic shifting)"
echo "   • New stop insertion at any index"
echo "   • Existing stop updates by ID"
echo "   • Direction-aware index management"
echo "   • Validation error handling"
echo "   • Mixed route + stops updates"
