import requests
import time
from datetime import datetime, timedelta

# Hardcoded C5 route coordinates (ordered by bus_stop_index for each direction)
# These are the exact coordinates from ReaVayaRoutes.json
northbound_stops = [
    {"lat": -26.173968, "lon": 27.957238, "bus_stop_index": 1, "address": "Main road and 17 street"},
    {"lat": -26.174218504355338, "lon": 27.962761385665004, "bus_stop_index": 2, "address": "105 Main Rd, Newlands, Randburg, 2092"},
    {"lat": -26.175635545564255, "lon": 27.96784838499146, "bus_stop_index": 3, "address": "149 Main Rd, Newlands, Randburg, 2092"},
    {"lat": -26.177320166469546, "lon": 27.9724908735137, "bus_stop_index": 4, "address": "Main Rd, Martindale, Johannesburg, 2092"},
    {"lat": -26.184884497848433, "lon": 27.98219597687166, "bus_stop_index": 5, "address": "17 Perth Rd, Westdene, Johannesburg, 2092"},
    {"lat": -26.18325257720585, "lon": 27.98120626459567, "bus_stop_index": 6, "address": "7-9 Perthweg, Westdene, Johannesburg, 2092"},
    {"lat": -26.183228593047158, "lon": 27.99049990028975, "bus_stop_index": 7, "address": "Helen Joseph Hospital Station (Northbound)"},
    {"lat": -26.18197145871311, "lon": 27.99895649341298, "bus_stop_index": 8, "address": "UJ Kingsway Campus Station (Northbound)"},
    {"lat": -26.1832, "lon": 28.00452, "bus_stop_index": 9, "address": "UJ Sophiatown Residence Bus Station (Northbound)"},
    {"lat": -26.184422, "lon": 28.011154, "bus_stop_index": 10, "address": "SABC Media Park Bus Station (Northbound)"},
    {"lat": -26.183160, "lon": 28.020200, "bus_stop_index": 11, "address": "Milpark Station (Northbound)"},
    {"lat": -26.185220597453053, "lon": 28.028090695335067, "bus_stop_index": 12, "address": "Wits Bus Station (Northbound)"},
    {"lat": -26.185084425447673, "lon": 28.0392013235618, "bus_stop_index": 13, "address": "Parktown Station West (Northbound)"},
    {"lat": -26.19112, "lon": 28.04125, "bus_stop_index": 14, "address": "Constitutional Hill Bus Station"},
    {"lat": -26.19567, "lon": 28.04089, "bus_stop_index": 15, "address": "Park Bus Station (Northbound)"}
]

southbound_stops = [
    {"lat": -26.20282, "lon": 28.04011, "bus_stop_index": 1, "address": "Library Gardens Bus Station"},
    {"lat": -26.20282, "lon": 28.04011, "bus_stop_index": 2, "address": "Harrison Street Bus Station"},
    {"lat": -26.20216, "lon": 28.04178, "bus_stop_index": 3, "address": "Rissik Street Bus Station"},
    {"lat": -26.19567, "lon": 28.04089, "bus_stop_index": 4, "address": "Park Bus Station (Southbound)"},
    {"lat": -26.19163, "lon": 28.03922, "bus_stop_index": 5, "address": "Joburg Theatre Bus Station"},
    {"lat": -26.185084425447673, "lon": 28.0392013235618, "bus_stop_index": 6, "address": "Parktown Station West (Southbound)"},
    {"lat": -26.185220597453053, "lon": 28.028090695335067, "bus_stop_index": 7, "address": "Wits Bus Station (Southbound)"},
    {"lat": -26.183160, "lon": 28.020200, "bus_stop_index": 8, "address": "Milpark Station (Southbound)"},
    {"lat": -26.184327, "lon": 28.009803, "bus_stop_index": 9, "address": "SABC Media Park Bus Station (Southbound)"},
    {"lat": -26.18298, "lon": 28.00357, "bus_stop_index": 10, "address": "UJ Sophiatown Residence Bus Station (Southbound)"},
    {"lat": -26.18197145871311, "lon": 27.99895649341298, "bus_stop_index": 11, "address": "UJ Kingsway Campus Station (Southbound)"},
    {"lat": -26.183228593047158, "lon": 27.99049990028975, "bus_stop_index": 12, "address": "Helen Joseph Hospital Station (Southbound)"},
    {"lat": -26.185182980965855, "lon": 27.98212629738987, "bus_stop_index": 13, "address": "Laurence Wessenaar St, Westbury, Johannesburg, 2093"},
    {"lat": -26.18293584803714, "lon": 27.98072265670588, "bus_stop_index": 14, "address": "Perthweg, Westdene, Johannesburg, 2092"},
    {"lat": -26.181111316139788, "lon": 27.978598293763316, "bus_stop_index": 15, "address": "Westbury, Johannesburg, 2093"},
    {"lat": -26.178307106637636, "lon": 27.973772995629712, "bus_stop_index": 16, "address": "Main Rd, Martindale, Johannesburg, 2092"},
    {"lat": -26.17695764525254, "lon": 27.971516721878604, "bus_stop_index": 17, "address": "Sophia Town Police Station"},
    {"lat": -26.175744596678115, "lon": 27.96747044376172, "bus_stop_index": 18, "address": "140-136 Main Rd, Newlands, Randburg, 2092"},
    {"lat": -26.174565126136343, "lon": 27.963044813663775, "bus_stop_index": 19, "address": "100 Main Rd, Newland"},
    {"lat": -26.1742, "lon": 27.95722, "bus_stop_index": 20, "address": "Main road and 17 street"}
]

API_URL = "http://localhost:8080/api/tracker/payload"
DELAY_SECONDS = 2

# Simulate a bus moving along a path
def simulate_bus(stops, tracker_imei, direction, bus_number):
    print(f"\nSimulating bus {tracker_imei} ({direction})...")
    timestamp = datetime.utcnow()
    for stop in stops:
        payload = {
            "trackerImei": tracker_imei,
            "lat": stop["lat"],
            "lon": stop["lon"],
            "busNumber": bus_number,
            "tripDirection": direction,
            "bus_stop_index": stop["bus_stop_index"],
            "timestamp": timestamp.isoformat() + "Z"
        }
        print(f"Sending: {payload}")
        try:
            resp = requests.post(API_URL, json=payload)
            print(f"  → Status: {resp.status_code}")
        except Exception as e:
            print(f"  → Error: {e}")
        timestamp += timedelta(seconds=DELAY_SECONDS)
        time.sleep(DELAY_SECONDS)
    print(f"Simulation complete for bus {tracker_imei} ({direction})\n")

if __name__ == "__main__":
    # Simulate northbound and southbound buses in parallel
    import threading
    t1 = threading.Thread(target=simulate_bus, args=(northbound_stops, "C5NORTH001", "Northbound", "C5"))
    t2 = threading.Thread(target=simulate_bus, args=(southbound_stops, "C5SOUTH001", "Southbound", "C5"))
    t1.start()
    t2.start()
    t1.join()
    t2.join()
    print("All C5 bus simulations complete!") 