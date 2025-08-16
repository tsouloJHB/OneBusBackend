#!/bin/bash

# Test script for Bus Number Management API
# This script demonstrates all the CRUD operations for bus numbers

BASE_URL="http://localhost:8080/api/bus-numbers"

echo "🚍 Bus Number Management API Test Script"
echo "========================================"

# Function to make HTTP requests with error handling
make_request() {
    local method=$1
    local url=$2
    local data=$3
    local description=$4
    
    echo ""
    echo "📋 $description"
    echo "🔗 $method $url"
    
    if [ -n "$data" ]; then
        echo "📄 Request Body: $data"
        response=$(curl -s -w "\n%{http_code}" -X $method "$url" \
                   -H "Content-Type: application/json" \
                   -d "$data")
    else
        response=$(curl -s -w "\n%{http_code}" -X $method "$url" \
                   -H "Accept: application/json")
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)
    
    echo "📊 Response Code: $http_code"
    echo "📃 Response Body: $body"
    echo "----------------------------------------"
}

# Test 1: Create Bus Numbers for different companies
echo ""
echo "🔨 Creating Bus Numbers..."

# Rea Vaya Bus Numbers
make_request "POST" "$BASE_URL" '{
    "busNumber": "C1",
    "companyName": "Rea Vaya",
    "routeName": "Central Line",
    "description": "Main route through Johannesburg CBD",
    "startDestination": "Thokoza Park",
    "endDestination": "University of Witwatersrand",
    "direction": "Bidirectional",
    "distanceKm": 15.5,
    "estimatedDurationMinutes": 45,
    "frequencyMinutes": 10,
    "isActive": true
}' "Create Rea Vaya C1 Bus Number"

make_request "POST" "$BASE_URL" '{
    "busNumber": "C2",
    "companyName": "Rea Vaya",
    "routeName": "Eastern Route",
    "description": "Route to eastern suburbs",
    "startDestination": "Ellis Park",
    "endDestination": "Kensington",
    "direction": "Bidirectional",
    "distanceKm": 12.3,
    "estimatedDurationMinutes": 35,
    "frequencyMinutes": 15,
    "isActive": true
}' "Create Rea Vaya C2 Bus Number"

# Metrobus Bus Numbers
make_request "POST" "$BASE_URL" '{
    "busNumber": "85",
    "companyName": "Metrobus",
    "routeName": "Soweto Express",
    "description": "Express service to Soweto",
    "startDestination": "Johannesburg CBD",
    "endDestination": "Maponya Mall",
    "direction": "Bidirectional",
    "distanceKm": 25.7,
    "estimatedDurationMinutes": 60,
    "frequencyMinutes": 20,
    "isActive": true
}' "Create Metrobus 85 Bus Number"

make_request "POST" "$BASE_URL" '{
    "busNumber": "12A",
    "companyName": "Metrobus",
    "routeName": "Northern Route",
    "description": "Service to northern suburbs",
    "startDestination": "Park Station",
    "endDestination": "Sandton City",
    "direction": "Bidirectional",
    "distanceKm": 18.2,
    "estimatedDurationMinutes": 50,
    "frequencyMinutes": 12,
    "isActive": true
}' "Create Metrobus 12A Bus Number"

# Test 2: Get all bus numbers
make_request "GET" "$BASE_URL" "" "Get All Bus Numbers"

# Test 3: Get active bus numbers
make_request "GET" "$BASE_URL/active" "" "Get All Active Bus Numbers"

# Test 4: Get bus numbers by company
make_request "GET" "$BASE_URL/company/Rea%20Vaya" "" "Get Bus Numbers for Rea Vaya"

make_request "GET" "$BASE_URL/company/Metrobus" "" "Get Bus Numbers for Metrobus"

# Test 5: Get bus numbers grouped by company
make_request "GET" "$BASE_URL/grouped-by-company" "" "Get Bus Numbers Grouped by Company"

# Test 6: Search by route
make_request "GET" "$BASE_URL/route/Central" "" "Search Bus Numbers by Route (Central)"

# Test 7: Search by destination
make_request "GET" "$BASE_URL/destination/Sandton" "" "Search Bus Numbers by Destination (Sandton)"

# Test 8: Get count by company
make_request "GET" "$BASE_URL/company/Rea%20Vaya/count" "" "Get Count of Bus Numbers for Rea Vaya"

# Test 9: Search companies
make_request "GET" "$BASE_URL/search/company?companyName=Metro" "" "Search Bus Numbers by Company Name (Metro)"

# Test 10: Update a bus number (assuming ID 1 exists)
make_request "PUT" "$BASE_URL/1" '{
    "busNumber": "C1",
    "companyName": "Rea Vaya",
    "routeName": "Central Line Express",
    "description": "Updated main route through Johannesburg CBD with express service",
    "startDestination": "Thokoza Park",
    "endDestination": "University of Witwatersrand",
    "direction": "Bidirectional",
    "distanceKm": 15.5,
    "estimatedDurationMinutes": 40,
    "frequencyMinutes": 8,
    "isActive": true
}' "Update Bus Number ID 1"

# Test 11: Get specific bus number by ID
make_request "GET" "$BASE_URL/1" "" "Get Bus Number by ID (1)"

# Test 12: Get active bus numbers by company
make_request "GET" "$BASE_URL/company/Rea%20Vaya/active" "" "Get Active Bus Numbers for Rea Vaya"

# Test 13: Deactivate a bus number
make_request "PATCH" "$BASE_URL/2/deactivate" "" "Deactivate Bus Number ID 2"

# Test 14: Activate a bus number
make_request "PATCH" "$BASE_URL/2/activate" "" "Activate Bus Number ID 2"

# Test 15: Try to create duplicate (should fail)
make_request "POST" "$BASE_URL" '{
    "busNumber": "C1",
    "companyName": "Rea Vaya",
    "routeName": "Duplicate Route",
    "description": "This should fail due to duplicate bus number",
    "startDestination": "Test Start",
    "endDestination": "Test End",
    "direction": "Northbound",
    "isActive": true
}' "Try to Create Duplicate Bus Number (Should Fail)"

echo ""
echo "✅ Bus Number Management API Testing Complete!"
echo ""
echo "📊 Summary of Available Endpoints:"
echo "   • POST   /api/bus-numbers                          - Create bus number"
echo "   • GET    /api/bus-numbers                          - Get all bus numbers"
echo "   • GET    /api/bus-numbers/active                   - Get active bus numbers"
echo "   • GET    /api/bus-numbers/{id}                     - Get bus number by ID"
echo "   • GET    /api/bus-numbers/company/{name}           - Get by company"
echo "   • GET    /api/bus-numbers/company/{name}/active    - Get active by company"
echo "   • GET    /api/bus-numbers/grouped-by-company       - Get grouped by company"
echo "   • GET    /api/bus-numbers/route/{name}             - Get by route name"
echo "   • GET    /api/bus-numbers/destination/{name}       - Get by destination"
echo "   • GET    /api/bus-numbers/search/company?name=     - Search by company"
echo "   • GET    /api/bus-numbers/company/{name}/count     - Count by company"
echo "   • PUT    /api/bus-numbers/{id}                     - Update bus number"
echo "   • PATCH  /api/bus-numbers/{id}/activate            - Activate bus number"
echo "   • PATCH  /api/bus-numbers/{id}/deactivate          - Deactivate bus number"
echo "   • DELETE /api/bus-numbers/{id}                     - Delete bus number"
echo ""
