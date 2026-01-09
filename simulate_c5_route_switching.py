import requests
import time
from datetime import datetime

# Florida Route stops
def fetch_florida_route_stops():
    """Fetch the actual Florida Route stops from the backend API"""
    try:
        response = requests.get("http://localhost:8080/api/routes/c5/Rea%20Vaya")
        if response.status_code == 200:
            data = response.json()
            routes = data.get("routes", [])
            
            northbound_stops = []
            southbound_stops = []
            
            for route in routes:
                direction = route.get("direction", "").lower()
                stops = route.get("stops", [])
                
                # Sort stops by busStopIndex
                sorted_stops = sorted(stops, key=lambda x: x.get("busStopIndex", 0))
                
                converted_stops = []
                for stop in sorted_stops:
                    converted_stops.append({
                        "lat": stop.get("latitude"),
                        "lon": stop.get("longitude"), 
                        "bus_stop_index": stop.get("busStopIndex"),
                        "address": stop.get("address", "Unknown")
                    })
                
                if "northbound" in direction:
                    northbound_stops = converted_stops
                    print(f"Loaded {len(northbound_stops)} Northbound stops from Florida Route")
                elif "southbound" in direction:
                    southbound_stops = converted_stops
                    print(f"Loaded {len(southbound_stops)} Southbound stops from Florida Route")
            
            return northbound_stops, southbound_stops
        else:
            print(f"Failed to fetch route data: {response.status_code}")
            return [], []
    except Exception as e:
        print(f"Error fetching route data: {e}")
        return [], []

# Fallback hardcoded stops
fallback_northbound_stops = [
    {"lat": -26.204750194269504, "lon": 28.041655462298205, "bus_stop_index": 1, "address": "Walter sisulu"},
    {"lat": -26.202070040405285, "lon": 28.041742926367114, "bus_stop_index": 2, "address": "Rissk bus station"},
    {"lat": -26.1955479385153, "lon": 28.0408175137073, "bus_stop_index": 3, "address": "Park Station"},
    {"lat": -26.19170671353952, "lon": 28.039145181661407, "bus_stop_index": 4, "address": "Joburg Theater station"},
    {"lat": -26.185220433203423, "lon": 28.03956627845764, "bus_stop_index": 5, "address": "Parktown station east"},
    {"lat": -26.18521561932357, "lon": 28.0280864238739, "bus_stop_index": 6, "address": "Wits station"},
    {"lat": -26.184391206069318, "lon": 28.011226975621028, "bus_stop_index": 7, "address": "Sabc station"},
    {"lat": -26.18294825980525, "lon": 28.003522753715515, "bus_stop_index": 8, "address": "Campus square station"},
    {"lat": -26.18202044726989, "lon": 27.998989839947573, "bus_stop_index": 9, "address": "Uj station"},
    {"lat": -26.18333710476769, "lon": 27.990371730972477, "bus_stop_index": 10, "address": "Helen joseph station"}
]

fallback_southbound_stops = [
    {"lat": -26.20476288476923, "lon": 28.041700626853302, "bus_stop_index": 1, "address": "Walter"},
    {"lat": -26.202079223006173, "lon": 28.041751924100712, "bus_stop_index": 2, "address": "rissk"},
    {"lat": -26.1955368917695, "lon": 28.040835782084063, "bus_stop_index": 3, "address": "park station"},
    {"lat": -26.191625078420532, "lon": 28.039169543552593, "bus_stop_index": 4, "address": "Joburg theatre station"},
    {"lat": -26.185220433203423, "lon": 28.03956627845764, "bus_stop_index": 5, "address": "Parktown station east"},
    {"lat": -26.18548594967584, "lon": 28.02897168256841, "bus_stop_index": 6, "address": "wits station"},
    {"lat": -26.183159594025742, "lon": 28.02018089034793, "bus_stop_index": 7, "address": "Milpark station"}
]

API_URL = "http://localhost:8080/api/tracker/payload"
DELAY_SECONDS = 10  # Faster for testing route switching

def simulate_bus_with_route_switch(
    northbound_stops,
    southbound_stops,
    tracker_imei,
    bus_number,
    start_direction="Southbound",
    max_cycles=2,
):
    """
    Simulate a bus that travels from start to end of one direction,
    then automatically switches to the opposite direction and travels back.
    This tests the route switching functionality.
    """
    
    print(f"\n🚌 [ROUTE-SWITCH-TEST] Starting C5 Bus {tracker_imei} - {bus_number}")
    print(f"   Initial direction: {start_direction}")
    print(f"   This bus will travel its route and test automatic direction switching\n")
    
    # Determine initial route
    current_direction = start_direction
    current_stops = northbound_stops if start_direction == "Northbound" else southbound_stops
    opposite_stops = southbound_stops if start_direction == "Northbound" else northbound_stops
    
    cycle = 0
    
    while cycle < max_cycles:
        try:
            print(f"\n{'='*70}")
            print(f"[CYCLE {cycle + 1}] Bus traveling {current_direction}")
            print(f"{'='*70}")
            
            # Travel through current direction
            for idx, stop in enumerate(current_stops):
                timestamp = datetime.now().isoformat()
                
                payload = {
                    "trackerImei": tracker_imei,
                    "busNumber": bus_number,
                    "lat": stop["lat"],
                    "lon": stop["lon"],
                    "timestamp": timestamp,
                    "busCompany": "Rea Vaya",
                }
                
                print(f"  [{current_direction}] Stop {idx + 1}/{len(current_stops)}: {stop['address']}")
                
                try:
                    resp = requests.post(API_URL, json=payload, headers={"Content-Type": "application/json"})
                    if resp.status_code == 200:
                        print(f"    ✓ Sent (Stop index: {idx + 1})")
                    else:
                        print(f"    ✗ Error: {resp.status_code}")
                except Exception as e:
                    print(f"    ✗ Request failed: {e}")
                
                time.sleep(DELAY_SECONDS)
            
            # Route switch!
            print(f"\n{'='*70}")
            print(f"[ROUTE-SWITCH] Bus {tracker_imei} reached end of {current_direction}")
            print(f"[ROUTE-SWITCH] Switching direction: {current_direction} → ", end="")
            
            # Send PAUSE update at end of current direction to clear old position
            last_stop = current_stops[-1]
            timestamp = datetime.now().isoformat()
            pause_payload = {
                "trackerImei": tracker_imei,
                "busNumber": bus_number,
                "lat": last_stop["lat"],
                "lon": last_stop["lon"],
                "timestamp": timestamp,
                "busCompany": "Rea Vaya",
                "status": "PAUSE_FOR_DIRECTION_SWITCH",
            }
            print(f"\n  [PAUSE] Sending pause status at end of {current_direction}")
            try:
                resp = requests.post(API_URL, json=pause_payload, headers={"Content-Type": "application/json"})
                if resp.status_code == 200:
                    print(f"    ✓ Pause status sent")
            except Exception as e:
                print(f"    ✗ Error: {e}")
            
            # Wait before switching to allow backend to process
            time.sleep(5)
            
            # Switch direction
            if current_direction == "Northbound":
                current_direction = "Southbound"
                current_stops = southbound_stops
            else:
                current_direction = "Northbound"
                current_stops = northbound_stops
            
            print(f"  [SWITCH] Direction changed to: {current_direction}")
            print(f"{'='*70}\n")
            
            # Send one more update with new direction to confirm switch
            time.sleep(3)
            first_stop = current_stops[0]
            timestamp = datetime.now().isoformat()
            
            payload = {
                "trackerImei": tracker_imei,
                "busNumber": bus_number,
                "lat": first_stop["lat"],
                "lon": first_stop["lon"],
                "timestamp": timestamp,
                "busCompany": "Rea Vaya",
                "status": "DIRECTION_SWITCHED",
            }
            
            print(f"  [CONFIRM] Sending first stop of {current_direction} to confirm route switch")
            try:
                resp = requests.post(API_URL, json=payload, headers={"Content-Type": "application/json"})
                if resp.status_code == 200:
                    print(f"    ✓ Direction confirmed!")
            except Exception as e:
                print(f"    ✗ Error: {e}")
            
            cycle += 1
            time.sleep(DELAY_SECONDS)
            
        except Exception as e:
            print(f"Error in bus simulation: {e}")
            time.sleep(5)
    
    print(f"\n✅ [ROUTE-SWITCH-TEST] Bus {tracker_imei} completed {cycle} cycles")

if __name__ == "__main__":
    print("🚌 Route Switching Test Simulator for C5 Bus")
    print("=" * 70)
    print("This simulator tests the automatic route switching functionality.")
    print("When a bus reaches the end of its route, it should automatically")
    print("switch to the opposite direction and return.")
    print("=" * 70)
    
    print("\nFetching route data from backend...")
    northbound_stops, southbound_stops = fetch_florida_route_stops()
    
    # Use fallback if needed
    if not northbound_stops and not southbound_stops:
        print("⚠️  Using fallback route data")
        northbound_stops = fallback_northbound_stops
        southbound_stops = fallback_southbound_stops
    
    print(f"\n📍 Loaded {len(northbound_stops)} Northbound stops, {len(southbound_stops)} Southbound stops")
    
    # Simulate a bus starting Northbound
    print("\n" + "=" * 70)
    print("Starting Route Switch Test")
    print("=" * 70)
    
    simulate_bus_with_route_switch(
        northbound_stops, 
        southbound_stops, 
        tracker_imei="Rea1234567789",  # Use real registered tracker IMEI
        bus_number="C5",
        start_direction="Northbound",
        max_cycles=1,  # Single round trip (Northbound then Southbound)
    )
    
    print("\n" + "=" * 70)
    print("🛑 Route Switching Test Complete!")
    print("=" * 70)
    print("\nCheck the backend logs for:")
    print("  1. [ROUTE-SWITCH] messages confirming direction switches")
    print("  2. Direction changing from Northbound to Southbound and back")
    print("  3. Stop index resetting to 0 after direction switch")
