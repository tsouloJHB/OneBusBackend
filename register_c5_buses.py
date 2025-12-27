#!/usr/bin/env python3
"""
Register C5 Buses in the OneBus Backend
This script registers the simulated C5 buses so their tracker data will be accepted.
"""

import requests
import json

API_BASE_URL = "http://localhost:8080/api"

# Buses to register
buses_to_register = [
    {
        "busId": "C5-NORTH-001",
        "trackerImei": "C5NORTH001",
        "busNumber": "C5",
        "route": "Florida Route",
        "busCompanyName": "Rea Vaya",
        "driverId": "DRV001",
        "driverName": "Simulated Driver North"
    },
    {
        "busId": "C5-SOUTH-001",
        "trackerImei": "C5SOUTH001",
        "busNumber": "C5",
        "route": "Florida Route",
        "busCompanyName": "Rea Vaya",
        "driverId": "DRV002",
        "driverName": "Simulated Driver South"
    }
]

def register_bus(bus_data):
    """Register a single bus in the backend"""
    url = f"{API_BASE_URL}/buses"
    headers = {"Content-Type": "application/json"}
    
    try:
        response = requests.post(url, json=bus_data, headers=headers)
        if response.status_code == 200:
            print(f"✓ Registered bus: {bus_data['busNumber']} ({bus_data['trackerImei']})")
            return True
        else:
            print(f"✗ Failed to register {bus_data['busNumber']}: {response.status_code}")
            print(f"  Response: {response.text}")
            return False
    except Exception as e:
        print(f"✗ Error registering {bus_data['busNumber']}: {e}")
        return False

def check_existing_buses():
    """Check what buses already exist"""
    url = f"{API_BASE_URL}/buses"
    try:
        response = requests.get(url)
        if response.status_code == 200:
            buses = response.json()
            print(f"\n📋 Found {len(buses)} existing buses in database:")
            for bus in buses:
                print(f"   - {bus.get('busNumber')} (IMEI: {bus.get('trackerImei')}, ID: {bus.get('busId')})")
            return buses
        else:
            print(f"Could not fetch existing buses: {response.status_code}")
            return []
    except Exception as e:
        print(f"Error fetching buses: {e}")
        return []

def main():
    print("🚌 C5 Bus Registration Script")
    print("=" * 50)
    
    # Check existing buses
    existing_buses = check_existing_buses()
    existing_imeis = {bus.get('trackerImei') for bus in existing_buses}
    
    print(f"\n📝 Registering {len(buses_to_register)} C5 buses...")
    print("-" * 50)
    
    success_count = 0
    skip_count = 0
    
    for bus in buses_to_register:
        if bus['trackerImei'] in existing_imeis:
            print(f"⊘ Skipping {bus['busNumber']} ({bus['trackerImei']}) - already registered")
            skip_count += 1
        else:
            if register_bus(bus):
                success_count += 1
    
    print("\n" + "=" * 50)
    print(f"✓ Successfully registered: {success_count}")
    print(f"⊘ Already existed (skipped): {skip_count}")
    print(f"Total: {success_count + skip_count}/{len(buses_to_register)}")
    print("\n🎉 Bus registration complete!")
    print("You can now run simulate_c5_buses.py to start sending location data.")

if __name__ == "__main__":
    main()
