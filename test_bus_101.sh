#!/bin/bash

echo "Starting bus 101 simulation..."

# Initial position
lat=-26.2041
lon=28.0473

# Simulate bus moving northbound
for i in {1..10}; do
    echo "Sending update $i for bus 101 Northbound..."
    
    # Move slightly north
    lat=$(echo "$lat - 0.001" | bc -l)
    
    curl -X POST http://localhost:8080/api/tracker/payload \
      -H "Content-Type: application/json" \
      -d "{
        \"trackerImei\": \"101001\",
        \"lat\": $lat,
        \"lon\": $lon,
        \"speedKmh\": $((30 + $i * 2)),
        \"headingDegrees\": 0.0,
        \"tripDirection\": \"Northbound\",
        \"timestamp\": \"2024-01-15T15:$((10 + $i)):00Z\"
      }"
    
    echo -e "\n--- Update $i sent ---"
    sleep 3
done

echo "Bus 101 Northbound simulation complete!" 