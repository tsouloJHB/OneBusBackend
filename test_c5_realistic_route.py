import requests
import time
from datetime import datetime, timedelta

# Real C5 route coordinates (detailed path with curves and turns) - converted to stop format
REALISTIC_C5_COORDINATES = [
    {"lat": -26.204726, "lon": 28.041674, "address": "Start - Walter Sisulu"},
    {"lat": -26.204686, "lon": 28.042042, "address": "Point 2"},
    {"lat": -26.204616, "lon": 28.042108, "address": "Point 3"},
    {"lat": -26.204480, "lon": 28.042096, "address": "Point 4"},
    {"lat": -26.204216, "lon": 28.042063, "address": "Point 5"},
    {"lat": -26.204040, "lon": 28.042030, "address": "Point 6"},
    {"lat": -26.203819, "lon": 28.041997, "address": "Point 7"},
    {"lat": -26.203581, "lon": 28.041965, "address": "Point 8"},
    {"lat": -26.203262, "lon": 28.041916, "address": "Point 9"},
    {"lat": -26.203133, "lon": 28.041903, "address": "Point 10"},
    {"lat": -26.202953, "lon": 28.041875, "address": "Point 11"},
    {"lat": -26.202696, "lon": 28.041838, "address": "Point 12"},
    {"lat": -26.202439, "lon": 28.041805, "address": "Point 13"},
    {"lat": -26.202182, "lon": 28.041772, "address": "Point 14"},
    {"lat": -26.201955, "lon": 28.041723, "address": "Point 15"},
    {"lat": -26.201709, "lon": 28.041687, "address": "Point 16"},
    {"lat": -26.201514, "lon": 28.041678, "address": "Point 17"},
    {"lat": -26.201224, "lon": 28.041633, "address": "Point 18"},
    {"lat": -26.200971, "lon": 28.041601, "address": "Point 19"},
    {"lat": -26.200802, "lon": 28.041584, "address": "Point 20"},
    {"lat": -26.200553, "lon": 28.041556, "address": "Point 21"},
    {"lat": -26.200373, "lon": 28.041539, "address": "Point 22"},
    {"lat": -26.200171, "lon": 28.041519, "address": "Point 23"},
    {"lat": -26.200021, "lon": 28.041503, "address": "Point 24"},
    {"lat": -26.199815, "lon": 28.041445, "address": "Point 25"},
    {"lat": -26.199624, "lon": 28.041429, "address": "Point 26"},
    {"lat": -26.199433, "lon": 28.041392, "address": "Point 27"},
    {"lat": -26.199250, "lon": 28.041372, "address": "Point 28"},
    {"lat": -26.199041, "lon": 28.041363, "address": "Point 29"},
    {"lat": -26.198886, "lon": 28.041323, "address": "Point 30"},
    {"lat": -26.198619, "lon": 28.041294, "address": "Point 31"},
    {"lat": -26.198273, "lon": 28.041253, "address": "Point 32"},
    {"lat": -26.197998, "lon": 28.041196, "address": "Point 33"},
    {"lat": -26.197481, "lon": 28.041130, "address": "Point 34"},
    {"lat": -26.196772, "lon": 28.041032, "address": "Point 35"},
    {"lat": -26.196222, "lon": 28.040942, "address": "Point 36"},
    {"lat": -26.195877, "lon": 28.040856, "address": "Point 37"},
    {"lat": -26.195480, "lon": 28.040783, "address": "Point 38"},
    {"lat": -26.195286, "lon": 28.040754, "address": "Point 39"},
    {"lat": -26.195091, "lon": 28.040717, "address": "Point 40"},
    {"lat": -26.194908, "lon": 28.040680, "address": "Park Station"},
    {"lat": -26.194724, "lon": 28.040648, "address": "Point 42"},
    {"lat": -26.194508, "lon": 28.040590, "address": "Point 43"},
    {"lat": -26.194203, "lon": 28.040545, "address": "Point 44"},
    {"lat": -26.193920, "lon": 28.040488, "address": "Point 45"},
    {"lat": -26.193469, "lon": 28.040418, "address": "Point 46"},
    {"lat": -26.193131, "lon": 28.040267, "address": "Point 47"},
    {"lat": -26.192764, "lon": 28.039842, "address": "Point 48"},
    {"lat": -26.192412, "lon": 28.039371, "address": "Point 49"},
    {"lat": -26.192096, "lon": 28.039187, "address": "Point 50"},
    {"lat": -26.191674, "lon": 28.039146, "address": "Joburg Theatre"},
    {"lat": -26.191219, "lon": 28.039060, "address": "Point 52"},
    {"lat": -26.190756, "lon": 28.038999, "address": "Point 53"},
    {"lat": -26.190250, "lon": 28.038942, "address": "Point 54"},
    {"lat": -26.189941, "lon": 28.039163, "address": "Point 55"},
    {"lat": -26.189875, "lon": 28.039633, "address": "Point 56"},
    {"lat": -26.189831, "lon": 28.040210, "address": "Point 57"},
    {"lat": -26.189687, "lon": 28.040917, "address": "Point 58"},
    {"lat": -26.189436, "lon": 28.041051, "address": "Point 59"},
    {"lat": -26.188883, "lon": 28.040965, "address": "Point 60"},
    {"lat": -26.188339, "lon": 28.040938, "address": "Point 61"},
    {"lat": -26.187674, "lon": 28.040960, "address": "Point 62"},
    {"lat": -26.187102, "lon": 28.040955, "address": "Point 63"},
    {"lat": -26.186485, "lon": 28.040826, "address": "Point 64"},
    {"lat": -26.185850, "lon": 28.040820, "address": "Parktown Station"},
    {"lat": -26.185398, "lon": 28.040869, "address": "Point 66"},
    {"lat": -26.185354, "lon": 28.040295, "address": "Point 67"},
    {"lat": -26.185325, "lon": 28.039383, "address": "Point 68"},
    {"lat": -26.185243, "lon": 28.038782, "address": "Point 69"},
    {"lat": -26.185407, "lon": 28.037967, "address": "Point 70"},
    {"lat": -26.185605, "lon": 28.037312, "address": "Point 71"},
    {"lat": -26.185855, "lon": 28.036878, "address": "Point 72"},
    {"lat": -26.186076, "lon": 28.036733, "address": "Point 73"},
    {"lat": -26.186620, "lon": 28.036352, "address": "Point 74"},
    {"lat": -26.187015, "lon": 28.036014, "address": "Point 75"},
    {"lat": -26.187275, "lon": 28.035676, "address": "Point 76"},
    {"lat": -26.187304, "lon": 28.035118, "address": "Point 77"},
    {"lat": -26.187222, "lon": 28.034346, "address": "Point 78"},
    {"lat": -26.187155, "lon": 28.033621, "address": "Point 79"},
    {"lat": -26.186938, "lon": 28.032720, "address": "Point 80"},
    {"lat": -26.186596, "lon": 28.032098, "address": "Point 81"},
    {"lat": -26.186254, "lon": 28.031567, "address": "Point 82"},
    {"lat": -26.186019, "lon": 28.030848, "address": "Point 83"},
    {"lat": -26.185874, "lon": 28.030371, "address": "Point 84"},
    {"lat": -26.185672, "lon": 28.029480, "address": "Wits Station"},
    {"lat": -26.185537, "lon": 28.029024, "address": "Point 86"},
    {"lat": -26.185450, "lon": 28.028525, "address": "Point 87"},
    {"lat": -26.185364, "lon": 28.028262, "address": "Point 88"},
    {"lat": -26.185239, "lon": 28.027726, "address": "Point 89"},
    {"lat": -26.185018, "lon": 28.026921, "address": "End - Helen Joseph"},
]

API_URL = "http://localhost:8080/api/tracker/payload"
DELAY_SECONDS = 3  # 3 seconds between waypoints for detailed route

# Simulate a bus moving along realistic route coordinates
def simulate_bus(coordinates, tracker_imei, direction, bus_number, delay_seconds=DELAY_SECONDS):
    if not coordinates:
        print(f"⚠️  No coordinates available for {direction} direction - skipping simulation")
        return
        
    print(f"\nStarting C5 Realistic Route Bus {tracker_imei} ({direction}) with {len(coordinates)} waypoints...")
    
    current_point_index = 0
    
    while True:  # Continuous loop simulation
        try:
            point = coordinates[current_point_index]
            timestamp = datetime.now().isoformat()
            
            # Create payload matching the format of simulate_c5_buses.py
            payload = {
                "trackerImei": tracker_imei,
                "busNumber": bus_number,
                "lat": point["lat"],
                "lon": point["lon"],
                "timestamp": timestamp,
                "busCompany": "Rea Vaya",
                "tripDirection": direction
            }
            
            print(f"✓ [{direction}] Bus {bus_number} at waypoint {current_point_index + 1}/{len(coordinates)}: {point['address']}")
            
            try:
                resp = requests.post(API_URL, json=payload, headers={"Content-Type": "application/json"})
                if resp.status_code != 200:
                    print(f"  → API Error: {resp.status_code}")
            except Exception as e:
                print(f"  → Request Error: {e}")
            
            # Move to next waypoint (loop back to start)
            current_point_index = (current_point_index + 1) % len(coordinates)
            
            # Wait before next position update
            time.sleep(delay_seconds)
            
        except Exception as e:
            print(f"Error in {direction} bus simulation: {e}")
            time.sleep(5)  # Wait before retrying

if __name__ == "__main__":
    print("🚌 Starting C5 Bus Realistic Route Simulation...")
    print(f"📍 Total waypoints: {len(REALISTIC_C5_COORDINATES)}")
    
    import threading
    
    # Simulate single bus on realistic route
    t1 = threading.Thread(target=simulate_bus, args=(REALISTIC_C5_COORDINATES, "Rea1234567789", "Southbound", "C5", DELAY_SECONDS))
    
    print("🚀 Starting bus simulation on realistic route...")
    print(f"   Bus: C5 with {len(REALISTIC_C5_COORDINATES)} detailed waypoints")
    print(f"   Update interval: {DELAY_SECONDS}s between waypoints")
    
    t1.start()
    
    try:
        t1.join()
    except KeyboardInterrupt:
        print("\n🛑 Simulation stopped by user")
    
    print("C5 realistic route simulation complete!")
