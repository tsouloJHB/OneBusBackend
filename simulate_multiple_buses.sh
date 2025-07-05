#!/bin/bash

echo "🚌 Starting Multi-Bus Simulation..."
echo "This will simulate Bus 101 (Northbound) and Bus 102 (Eastbound) moving simultaneously"
echo ""

# Initial positions
bus101_lat=-26.2241
bus101_lon=28.0473
bus102_lat=-26.2241
bus102_lon=28.0673

# Simulate both buses moving for 10 updates
for i in {1..10}; do
    echo "📡 Sending update $i for both buses..."
    
    # Bus 101 moves north
    bus101_lat=$(echo "$bus101_lat - 0.001" | bc -l)
    
    # Bus 102 moves east
    bus102_lon=$(echo "$bus102_lon + 0.001" | bc -l)
    
    # Send Bus 101 update
    echo "  🚌 Bus 101 (Northbound): lat=$bus101_lat, lon=$bus101_lon"
    curl -s -X POST http://localhost:8080/api/tracker/payload \
      -H "Content-Type: application/json" \
      -d "{
        \"trackerImei\": \"101001\",
        \"lat\": $bus101_lat,
        \"lon\": $bus101_lon,
        \"speedKmh\": $((65 + $i * 2)),
        \"headingDegrees\": 0.0,
        \"tripDirection\": \"Northbound\",
        \"timestamp\": \"2024-01-15T15:$((30 + $i)):00Z\"
      }" > /dev/null
    
    # Send Bus 102 update
    echo "  🚌 Bus 102 (Eastbound): lat=$bus102_lat, lon=$bus102_lon"
    curl -s -X POST http://localhost:8080/api/tracker/payload \
      -H "Content-Type: application/json" \
      -d "{
        \"trackerImei\": \"102002\",
        \"lat\": $bus102_lat,
        \"lon\": $bus102_lon,
        \"speedKmh\": $((55 + $i * 3)),
        \"headingDegrees\": 90.0,
        \"tripDirection\": \"Eastbound\",
        \"timestamp\": \"2024-01-15T15:$((30 + $i)):00Z\"
      }" > /dev/null
    
    echo "  ✅ Update $i sent for both buses"
    echo "  ⏱️  Waiting 3 seconds..."
    echo ""
    sleep 3
done

echo "🎉 Multi-bus simulation complete!"
echo ""
echo "📊 Summary:"
echo "  • Bus 101: Moved northbound with increasing speed"
echo "  • Bus 102: Moved eastbound with increasing speed"
echo "  • Both buses sent updates simultaneously"
echo "  • Multiple clients can subscribe to different buses/directions"
echo ""
echo "🌐 Test with: http://localhost:8080/multi-bus-client.html" 