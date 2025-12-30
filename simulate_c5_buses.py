import requests
import time
from datetime import datetime, timedelta

# Florida Route stops from the backend database (fetched dynamically)
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
                
                # Sort stops by busStopIndex to ensure correct order
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

# Fallback hardcoded stops (in case API is unavailable)
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
DELAY_SECONDS = 30  # 30 seconds between stops for realistic simulation
FAST_DELAY_SECONDS = 15  # 15 seconds for faster bus

# Simulate a bus moving along a path
def simulate_bus(stops, tracker_imei, direction, bus_number, delay_seconds=DELAY_SECONDS):
    if not stops:
        print(f"⚠️  No stops available for {direction} direction - skipping simulation")
        return
        
    speed_label = "FAST" if delay_seconds == FAST_DELAY_SECONDS else "NORMAL"
    print(f"\nStarting Florida Route Bus {tracker_imei} ({direction}) [{speed_label}] with {len(stops)} stops...")
    
    current_stop_index = 0
    
    while True:  # Continuous loop simulation
        try:
            stop = stops[current_stop_index]
            timestamp = datetime.now().isoformat()
            
            # Ensure tripDirection uses the expected capitalized format (e.g. "Northbound")
            payload = {
                "trackerImei": tracker_imei,
                "busNumber": bus_number,
                "lat": stop["lat"],
                "lon": stop["lon"],
                "timestamp": timestamp,
                #"busStopIndex": stop["bus_stop_index"],
                "busCompany": "Rea Vaya",
                #"tripDirection": direction  # use original case (e.g. 'Northbound' / 'Southbound')
            }
            
            print(f"✓ [{speed_label}] {direction} Bus {bus_number} at stop {current_stop_index + 1}/{len(stops)}: {stop['address']}")
            
            try:
                resp = requests.post(API_URL, json=payload, headers={"Content-Type": "application/json"})
                if resp.status_code != 200:
                    print(f"  → API Error: {resp.status_code}")
            except Exception as e:
                print(f"  → Request Error: {e}")
            
            # Move to next stop (loop back to start)
            current_stop_index = (current_stop_index + 1) % len(stops)
            
            # Wait before next position update
            time.sleep(delay_seconds)
            
        except Exception as e:
            print(f"Error in {direction} bus simulation: {e}")
            time.sleep(5)  # Wait before retrying

if __name__ == "__main__":
    print("🚌 Starting Florida Route Bus Simulation...")
    print("Fetching route data from backend...")
    
    # Load stops from backend API (with fallback)
    northbound_stops, southbound_stops = fetch_florida_route_stops()
    
    # Use fallback data if API failed
    if not northbound_stops and not southbound_stops:
        print("⚠️  Using fallback route data")
        northbound_stops = fallback_northbound_stops
        southbound_stops = fallback_southbound_stops
    
    print(f"📍 Loaded {len(northbound_stops)} Northbound stops, {len(southbound_stops)} Southbound stops")
    
    # Simulate two buses on the same route with different speeds
    import threading
    # Bus 1 (C5-A) - Normal speed on Northbound route
    t1 = threading.Thread(target=simulate_bus, args=(southbound_stops, "123456789012345", "Southbound", "C5", DELAY_SECONDS))
    # Bus 2 (C5-B) - Fast speed on Northbound route (same route, faster bus)
    t2 = threading.Thread(target=simulate_bus, args=(southbound_stops, "Rea1234567789", "Southbound", "C5", FAST_DELAY_SECONDS))
    
    print("🚀 Starting parallel bus simulations on the same route...")
    print(f"   C5-A: Normal speed ({DELAY_SECONDS}s between stops)")
    print(f"   C5-B: Fast speed ({FAST_DELAY_SECONDS}s between stops)")
    t1.start()
    t2.start()
    
    try:
        t1.join()
        t2.join()
    except KeyboardInterrupt:
        print("\n🛑 Simulation stopped by user")
    
    print("All Florida Route bus simulations complete!") 