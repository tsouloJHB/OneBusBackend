#!/usr/bin/env python3
"""
Verify that C5 buses are registered and accepting tracker data
"""

import requests
import json
from datetime import datetime

API_BASE_URL = "http://localhost:8080/api"

def check_registered_buses():
    """Check which buses are registered"""
    print("🔍 Checking registered buses...")
    try:
        response = requests.get(f"{API_BASE_URL}/buses")
        if response.status_code == 200:
            buses = response.json()
            print(f"\n📋 Found {len(buses)} buses in database:")
            for bus in buses:
                print(f"   Bus ID: {bus.get('busId')}")
                print(f"      Bus Number: {bus.get('busNumber')}")
                print(f"      Tracker IMEI: {bus.get('trackerImei')}")
                print(f"      Company: {bus.get('busCompanyName')}")
                print(f"      Status: {bus.get('status', 'N/A')}")
                print()
            return buses
        else:
            print(f"❌ Failed to fetch buses: {response.status_code}")
            return []
    except Exception as e:
        print(f"❌ Error: {e}")
        return []

def check_active_buses():
    """Check which buses are currently active (tracked)"""
    print("\n🟢 Checking active buses (last 10 minutes)...")
    try:
        response = requests.get(f"{API_BASE_URL}/buses/active")
        if response.status_code == 200:
            active_buses = response.json()
            print(f"\n✓ Found {len(active_buses)} active buses:")
            for bus in active_buses:
                bus_info = bus.get('bus', {})
                location = bus.get('currentLocation', {})
                print(f"   - {bus_info.get('busNumber')} (IMEI: {bus_info.get('trackerImei')})")
                print(f"      Lat: {location.get('lat')}, Lon: {location.get('lng')}")
                print(f"      Status: {bus.get('status')}")
                print()
            return active_buses
        else:
            print(f"ℹ️  No active buses or endpoint returned {response.status_code}")
            print(f"   (This is normal if simulator hasn't sent data recently)")
            return []
    except Exception as e:
        print(f"❌ Error: {e}")
        return []

def send_test_payload():
    """Send a test tracker payload"""
    print("\n🧪 Sending test tracker payload...")
    payload = {
        "trackerImei": "C5NORTH001",
        "busNumber": "C5",
        "lat": -26.2041,
        "lon": 28.0416,
        "timestamp": datetime.now().isoformat(),
        "busStopIndex": 1,
        "busCompany": "Rea Vaya",
        "tripDirection": "Northbound"
    }
    
    try:
        response = requests.post(
            f"{API_BASE_URL}/tracker/payload",
            json=payload,
            headers={"Content-Type": "application/json"}
        )
        
        if response.status_code == 200:
            print(f"✅ Payload accepted!")
            print(f"   Response: {response.json()}")
            return True
        else:
            print(f"❌ Payload rejected with status {response.status_code}")
            print(f"   Response: {response.text}")
            return False
    except Exception as e:
        print(f"❌ Error sending payload: {e}")
        return False

def main():
    print("=" * 60)
    print("🚌 C5 Bus Verification Script")
    print("=" * 60)
    
    # 1. Check registered buses
    buses = check_registered_buses()
    
    # 2. Check active buses
    active_buses = check_active_buses()
    
    # 3. Send test payload
    success = send_test_payload()
    
    # Summary
    print("\n" + "=" * 60)
    print("📊 Summary:")
    print(f"   Total registered buses: {len(buses)}")
    print(f"   Active buses (recently tracked): {len(active_buses)}")
    print(f"   Test payload: {'✅ Accepted' if success else '❌ Rejected'}")
    print("=" * 60)
    
    # Recommendations
    print("\n💡 Recommendations:")
    if len(buses) == 0:
        print("   ⚠️  No buses registered! Run register_c5_buses.py first")
    elif len(buses) > 0 and len(active_buses) == 0:
        print("   ℹ️  Buses registered but not yet tracked")
        print("      Make sure simulator is running: python3 simulate_c5_buses.py")
    else:
        print("   ✅ System appears to be working correctly!")
        print("      Buses are registered and being tracked")

if __name__ == "__main__":
    main()
