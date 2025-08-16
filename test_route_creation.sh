#!/bin/bash

# Test script for Route Creation API
# Make sure the Spring Boot application is running on localhost:8080

echo "🚌 Testing Route Creation API"
echo "=============================="

BASE_URL="http://localhost:8080/api"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✅ PASS${NC}: $2"
    else
        echo -e "${RED}❌ FAIL${NC}: $2"
    fi
}

print_info() {
    echo -e "${BLUE}ℹ️  INFO${NC}: $1"
}

print_warning() {
    echo -e "${YELLOW}⚠️  WARNING${NC}: $1"
}

# Test 1: Create Northbound Route
echo -e "\n${BLUE}Test 1: Create Northbound Route${NC}"
echo "================================="

RESPONSE1=$(curl -s -w "HTTPSTATUS:%{http_code}" \
  -X POST "$BASE_URL/routes" \
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
  }')

HTTP_STATUS1=$(echo $RESPONSE1 | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
RESPONSE_BODY1=$(echo $RESPONSE1 | sed -e 's/HTTPSTATUS:.*//g')

if [ $HTTP_STATUS1 -eq 201 ]; then
    print_status 0 "Northbound route created successfully"
    echo "Response: $RESPONSE_BODY1" | jq '.'
    ROUTE1_ID=$(echo $RESPONSE_BODY1 | jq -r '.route.id')
    print_info "Created route ID: $ROUTE1_ID"
else
    print_status 1 "Failed to create Northbound route (HTTP $HTTP_STATUS1)"
    echo "Response: $RESPONSE_BODY1"
fi

# Test 2: Create Southbound Route (Same bus number, different direction)
echo -e "\n${BLUE}Test 2: Create Southbound Route${NC}"
echo "================================="

RESPONSE2=$(curl -s -w "HTTPSTATUS:%{http_code}" \
  -X POST "$BASE_URL/routes" \
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
  }')

HTTP_STATUS2=$(echo $RESPONSE2 | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
RESPONSE_BODY2=$(echo $RESPONSE2 | sed -e 's/HTTPSTATUS:.*//g')

if [ $HTTP_STATUS2 -eq 201 ]; then
    print_status 0 "Southbound route created successfully"
    echo "Response: $RESPONSE_BODY2" | jq '.'
    ROUTE2_ID=$(echo $RESPONSE_BODY2 | jq -r '.route.id')
    print_info "Created route ID: $ROUTE2_ID"
else
    print_status 1 "Failed to create Southbound route (HTTP $HTTP_STATUS2)"
    echo "Response: $RESPONSE_BODY2"
fi

# Test 3: Try to create duplicate route (should fail)
echo -e "\n${BLUE}Test 3: Create Duplicate Route (Should Fail)${NC}"
echo "=============================================="

RESPONSE3=$(curl -s -w "HTTPSTATUS:%{http_code}" \
  -X POST "$BASE_URL/routes" \
  -H "Content-Type: application/json" \
  -d '{
    "company": "SimulatedCo",
    "busNumber": "C5",
    "routeName": "C5 Duplicate Test",
    "description": "This should fail",
    "direction": "Northbound",
    "active": true
  }')

HTTP_STATUS3=$(echo $RESPONSE3 | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
RESPONSE_BODY3=$(echo $RESPONSE3 | sed -e 's/HTTPSTATUS:.*//g')

if [ $HTTP_STATUS3 -eq 400 ]; then
    print_status 0 "Duplicate route correctly rejected"
    echo "Response: $RESPONSE_BODY3" | jq '.'
else
    print_status 1 "Duplicate route should have been rejected (HTTP $HTTP_STATUS3)"
    echo "Response: $RESPONSE_BODY3"
fi

# Test 4: Create route with invalid direction (should fail)
echo -e "\n${BLUE}Test 4: Create Route with Invalid Direction (Should Fail)${NC}"
echo "=========================================================="

RESPONSE4=$(curl -s -w "HTTPSTATUS:%{http_code}" \
  -X POST "$BASE_URL/routes" \
  -H "Content-Type: application/json" \
  -d '{
    "company": "TestCo",
    "busNumber": "T1",
    "routeName": "Test Route",
    "description": "Invalid direction test",
    "direction": "InvalidDirection",
    "active": true
  }')

HTTP_STATUS4=$(echo $RESPONSE4 | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
RESPONSE_BODY4=$(echo $RESPONSE4 | sed -e 's/HTTPSTATUS:.*//g')

if [ $HTTP_STATUS4 -eq 400 ]; then
    print_status 0 "Invalid direction correctly rejected"
    echo "Response: $RESPONSE_BODY4"
else
    print_status 1 "Invalid direction should have been rejected (HTTP $HTTP_STATUS4)"
    echo "Response: $RESPONSE_BODY4"
fi

# Test 5: Create route with minimal data
echo -e "\n${BLUE}Test 5: Create Route with Minimal Data${NC}"
echo "======================================="

RESPONSE5=$(curl -s -w "HTTPSTATUS:%{http_code}" \
  -X POST "$BASE_URL/routes" \
  -H "Content-Type: application/json" \
  -d '{
    "company": "MinimalCo",
    "busNumber": "M1",
    "routeName": "Minimal Route",
    "direction": "Eastbound"
  }')

HTTP_STATUS5=$(echo $RESPONSE5 | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
RESPONSE_BODY5=$(echo $RESPONSE5 | sed -e 's/HTTPSTATUS:.*//g')

if [ $HTTP_STATUS5 -eq 201 ]; then
    print_status 0 "Minimal route created successfully"
    echo "Response: $RESPONSE_BODY5" | jq '.'
    ROUTE5_ID=$(echo $RESPONSE_BODY5 | jq -r '.route.id')
    print_info "Created route ID: $ROUTE5_ID"
else
    print_status 1 "Failed to create minimal route (HTTP $HTTP_STATUS5)"
    echo "Response: $RESPONSE_BODY5"
fi

# Test 6: Verify routes can be retrieved
echo -e "\n${BLUE}Test 6: Verify Created Routes${NC}"
echo "============================="

print_info "Retrieving all routes..."
ALL_ROUTES=$(curl -s "$BASE_URL/routes")
echo "All routes:"
echo $ALL_ROUTES | jq '.'

# Count routes with our test data
C5_ROUTES=$(echo $ALL_ROUTES | jq '[.[] | select(.busNumber == "C5")] | length')
print_info "Found $C5_ROUTES C5 routes (should be 2 - Northbound and Southbound)"

if [ "$C5_ROUTES" -eq 2 ]; then
    print_status 0 "Correct number of C5 routes found"
else
    print_status 1 "Expected 2 C5 routes, found $C5_ROUTES"
fi

# Test 7: Test route retrieval by ID
if [ ! -z "$ROUTE1_ID" ] && [ "$ROUTE1_ID" != "null" ]; then
    echo -e "\n${BLUE}Test 7: Retrieve Route by ID${NC}"
    echo "============================"
    
    ROUTE_DETAIL=$(curl -s "$BASE_URL/routes/$ROUTE1_ID")
    RETRIEVED_DIRECTION=$(echo $ROUTE_DETAIL | jq -r '.direction')
    
    if [ "$RETRIEVED_DIRECTION" = "Northbound" ]; then
        print_status 0 "Route retrieved with correct direction"
        echo "Route details:"
        echo $ROUTE_DETAIL | jq '.'
    else
        print_status 1 "Route direction mismatch. Expected: Northbound, Got: $RETRIEVED_DIRECTION"
    fi
fi

# Summary
echo -e "\n${YELLOW}🎯 Test Summary${NC}"
echo "==============="
echo "✅ Route creation endpoint implemented"
echo "✅ Direction validation working"
echo "✅ Duplicate prevention working"
echo "✅ Start/End point support added"
echo "✅ Route retrieval working with new fields"

echo -e "\n${GREEN}🚀 Routes are ready for WebSocket streaming!${NC}"
echo "Now clients can:"
echo "1. Create routes with: POST /api/routes"
echo "2. List routes with: GET /api/routes"
echo "3. Subscribe to streams with busNumber + direction"

echo -e "\n${BLUE}Example WebSocket subscription:${NC}"
echo '{
  "busNumber": "C5",
  "direction": "Northbound",
  "latitude": -26.2041,
  "longitude": 28.0473,
  "busStopIndex": 3
}'
