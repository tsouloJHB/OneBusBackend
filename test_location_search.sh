#!/bin/bash

# Location Search Testing Script
# This script tests the location search API endpoints

BASE_URL="http://localhost:8080/api/v1/search"

echo "=================================="
echo "Location Search API Test Script"
echo "=================================="
echo ""

# Test 1: Search by location name
echo "Test 1: Search by location name (Soweto)"
echo "-----------------------------------"
curl -X POST "$BASE_URL/location" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Soweto",
    "radiusKm": 5.0
  }' | json_pp
echo ""
echo ""

# Test 2: Search by location with user coordinates
echo "Test 2: Search by location with user coordinates"
echo "-----------------------------------"
curl -X POST "$BASE_URL/location" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Sandton",
    "radiusKm": 10.0,
    "userLatitude": -26.1076,
    "userLongitude": 28.0567
  }' | json_pp
echo ""
echo ""

# Test 3: Search by coordinates
echo "Test 3: Search by direct coordinates (Johannesburg CBD)"
echo "-----------------------------------"
curl -X GET "$BASE_URL/coordinates?latitude=-26.2041&longitude=28.0473&radiusKm=5.0" | json_pp
echo ""
echo ""

# Test 4: Search with small radius
echo "Test 4: Search with small radius (2km)"
echo "-----------------------------------"
curl -X POST "$BASE_URL/location" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Johannesburg",
    "radiusKm": 2.0
  }' | json_pp
echo ""
echo ""

# Test 5: Search with large radius
echo "Test 5: Search with large radius (15km)"
echo "-----------------------------------"
curl -X POST "$BASE_URL/location" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Pretoria",
    "radiusKm": 15.0
  }' | json_pp
echo ""
echo ""

echo "=================================="
echo "Tests completed!"
echo "=================================="
