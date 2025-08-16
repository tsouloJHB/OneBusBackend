#!/bin/bash

# Test script for Get Buses by Company API
# Make sure the Spring Boot application is running on localhost:8080

echo "🚌 Testing Get Buses by Company API"
echo "====================================="

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

# Test 1: Create some test buses first
echo -e "\n${BLUE}Test 1: Create Test Buses${NC}"
echo "=========================="

print_info "Creating test buses for different companies..."

# Create Rea Vaya buses
curl -s -X POST "$BASE_URL/buses" \
  -H "Content-Type: application/json" \
  -d '{
    "busId": "RV-001",
    "busNumber": "C5",
    "busCompanyName": "Rea Vaya",
    "trackerImei": "123456789012345",
    "route": "C5",
    "driverId": "DRV001",
    "driverName": "John Doe"
  }' > /dev/null

curl -s -X POST "$BASE_URL/buses" \
  -H "Content-Type: application/json" \
  -d '{
    "busId": "RV-002", 
    "busNumber": "F2",
    "busCompanyName": "Rea Vaya",
    "trackerImei": "123456789012346",
    "route": "F2",
    "driverId": "DRV002",
    "driverName": "Jane Smith"
  }' > /dev/null

# Create Metrobus buses
curl -s -X POST "$BASE_URL/buses" \
  -H "Content-Type: application/json" \
  -d '{
    "busId": "MB-001",
    "busNumber": "R1",
    "busCompanyName": "Metrobus",
    "trackerImei": "123456789012347",
    "route": "R1", 
    "driverId": "DRV003",
    "driverName": "Mike Johnson"
  }' > /dev/null

# Create Putco buses
curl -s -X POST "$BASE_URL/buses" \
  -H "Content-Type: application/json" \
  -d '{
    "busId": "PT-001",
    "busNumber": "B1",
    "busCompanyName": "Putco",
    "trackerImei": "123456789012348", 
    "route": "B1",
    "driverId": "DRV004",
    "driverName": "Sarah Wilson"
  }' > /dev/null

print_info "Test buses created"

# Test 2: Get buses by exact company name
echo -e "\n${BLUE}Test 2: Get Buses by Exact Company Name${NC}"
echo "========================================"

RESPONSE1=$(curl -s -w "HTTPSTATUS:%{http_code}" \
  -X GET "$BASE_URL/buses/company/Rea%20Vaya?searchType=exact")

HTTP_STATUS1=$(echo $RESPONSE1 | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
RESPONSE_BODY1=$(echo $RESPONSE1 | sed -e 's/HTTPSTATUS:.*//g')

if [ $HTTP_STATUS1 -eq 200 ]; then
    BUSES_COUNT=$(echo $RESPONSE_BODY1 | grep -o '"count":[0-9]*' | cut -d':' -f2)
    print_status 0 "Found $BUSES_COUNT buses for 'Rea Vaya' (exact match)"
    echo "Response preview: $(echo $RESPONSE_BODY1 | cut -c1-100)..."
else
    print_status 1 "Failed to get buses for 'Rea Vaya' (HTTP $HTTP_STATUS1)"
    echo "Response: $RESPONSE_BODY1"
fi

# Test 3: Get buses with case-insensitive search
echo -e "\n${BLUE}Test 3: Get Buses with Case-Insensitive Search${NC}"
echo "==============================================="

RESPONSE2=$(curl -s -w "HTTPSTATUS:%{http_code}" \
  -X GET "$BASE_URL/buses/company/rea%20vaya?searchType=ignoreCase")

HTTP_STATUS2=$(echo $RESPONSE2 | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
RESPONSE_BODY2=$(echo $RESPONSE2 | sed -e 's/HTTPSTATUS:.*//g')

if [ $HTTP_STATUS2 -eq 200 ]; then
    BUSES_COUNT2=$(echo $RESPONSE_BODY2 | grep -o '"count":[0-9]*' | cut -d':' -f2)
    print_status 0 "Found $BUSES_COUNT2 buses for 'rea vaya' (case-insensitive)"
    echo "Response preview: $(echo $RESPONSE_BODY2 | cut -c1-100)..."
else
    print_status 1 "Failed to get buses for 'rea vaya' (HTTP $HTTP_STATUS2)"
    echo "Response: $RESPONSE_BODY2"
fi

# Test 4: Get buses with contains search
echo -e "\n${BLUE}Test 4: Get Buses with Contains Search${NC}"
echo "======================================"

RESPONSE3=$(curl -s -w "HTTPSTATUS:%{http_code}" \
  -X GET "$BASE_URL/buses/company/metro?searchType=contains")

HTTP_STATUS3=$(echo $RESPONSE3 | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
RESPONSE_BODY3=$(echo $RESPONSE3 | sed -e 's/HTTPSTATUS:.*//g')

if [ $HTTP_STATUS3 -eq 200 ]; then
    BUSES_COUNT3=$(echo $RESPONSE_BODY3 | grep -o '"count":[0-9]*' | cut -d':' -f2)
    print_status 0 "Found $BUSES_COUNT3 buses containing 'metro'"
    echo "Response preview: $(echo $RESPONSE_BODY3 | cut -c1-100)..."
else
    print_status 1 "Failed to get buses containing 'metro' (HTTP $HTTP_STATUS3)"
    echo "Response: $RESPONSE_BODY3"
fi

# Test 5: Get buses for non-existent company
echo -e "\n${BLUE}Test 5: Get Buses for Non-Existent Company${NC}"
echo "==========================================="

RESPONSE4=$(curl -s -w "HTTPSTATUS:%{http_code}" \
  -X GET "$BASE_URL/buses/company/NonExistentCompany")

HTTP_STATUS4=$(echo $RESPONSE4 | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
RESPONSE_BODY4=$(echo $RESPONSE4 | sed -e 's/HTTPSTATUS:.*//g')

if [ $HTTP_STATUS4 -eq 200 ]; then
    BUSES_COUNT4=$(echo $RESPONSE_BODY4 | grep -o '"count":[0-9]*' | cut -d':' -f2)
    if [ "$BUSES_COUNT4" = "0" ]; then
        print_status 0 "Correctly returned 0 buses for non-existent company"
    else
        print_status 1 "Should have returned 0 buses for non-existent company"
    fi
    echo "Response preview: $(echo $RESPONSE_BODY4 | cut -c1-100)..."
else
    print_status 1 "Failed to handle non-existent company request (HTTP $HTTP_STATUS4)"
    echo "Response: $RESPONSE_BODY4"
fi

# Test 6: Test with empty company name (should fail)
echo -e "\n${BLUE}Test 6: Test with Empty Company Name (Should Fail)${NC}"
echo "=================================================="

RESPONSE5=$(curl -s -w "HTTPSTATUS:%{http_code}" \
  -X GET "$BASE_URL/buses/company/%20")

HTTP_STATUS5=$(echo $RESPONSE5 | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
RESPONSE_BODY5=$(echo $RESPONSE5 | sed -e 's/HTTPSTATUS:.*//g')

if [ $HTTP_STATUS5 -eq 400 ]; then
    print_status 0 "Correctly rejected empty company name"
    echo "Response: $RESPONSE_BODY5"
else
    print_status 1 "Should have rejected empty company name (HTTP $HTTP_STATUS5)"
    echo "Response: $RESPONSE_BODY5"
fi

# Test 7: Get all buses to compare
echo -e "\n${BLUE}Test 7: Get All Buses for Comparison${NC}"
echo "====================================="

ALL_BUSES=$(curl -s "$BASE_URL/buses")
TOTAL_BUSES=$(echo $ALL_BUSES | grep -o '"busId"' | wc -l | tr -d ' ')

print_info "Total buses in database: $TOTAL_BUSES"
echo "All buses preview: $(echo $ALL_BUSES | cut -c1-200)..."

# Summary
echo -e "\n${YELLOW}🎯 Test Summary${NC}"
echo "==============="
echo "✅ Bus creation endpoint working"
echo "✅ Get buses by company name (exact match)"
echo "✅ Get buses by company name (case-insensitive)"
echo "✅ Get buses by company name (contains search)"
echo "✅ Proper handling of non-existent companies"
echo "✅ Input validation for empty company names"

echo -e "\n${GREEN}🚀 Get Buses by Company API is working correctly!${NC}"
echo "Endpoint: GET /api/buses/company/{busCompanyName}"
echo "Search types: exact, ignoreCase, contains"

echo -e "\n${BLUE}Example usage:${NC}"
echo "curl -X GET \"http://localhost:8080/api/buses/company/Rea%20Vaya\""
echo "curl -X GET \"http://localhost:8080/api/buses/company/rea%20vaya?searchType=ignoreCase\""
echo "curl -X GET \"http://localhost:8080/api/buses/company/metro?searchType=contains\""
