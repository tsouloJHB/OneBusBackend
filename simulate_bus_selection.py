import sys
from typing import List, Dict

def select_best_bus(buses: List[Dict], direction: str, client_index: int):
    # 1. Prefer buses in requested direction, ahead of or at client
    suitable_requested = [b for b in buses if b['direction'] == direction and b['index'] >= client_index]
    if suitable_requested:
        suitable_requested.sort(key=lambda b: abs(b['index'] - client_index))
        return suitable_requested[0]
    # 2. Fallback: closest bus in opposite direction (no negative index)
    opposite = 'Southbound' if direction == 'Northbound' else 'Northbound'
    suitable_opposite = [b for b in buses if b['direction'] == opposite and b['index'] >= 0]
    if suitable_opposite:
        suitable_opposite.sort(key=lambda b: abs(b['index'] - client_index))
        return suitable_opposite[0]
    # 3. No buses available
    return None

def print_scenario(buses, direction, client_index, label):
    print(f"\n--- {label} ---")
    print(f"Client requests {direction} at stop index {client_index}")
    print("Available buses:")
    for b in buses:
        print(f"  {b['id']} - {b['direction']} at index {b['index']}")
    selected = select_best_bus(buses, direction, client_index)
    if selected:
        print(f"Selected bus: {selected['id']} (index: {selected['index']}, direction: {selected['direction']})")
    else:
        print("No buses available for this route in either direction.")

def main():
    # Scenario 1: Bus ahead in requested direction
    buses1 = [
        {'id': 'bus1', 'direction': 'Northbound', 'index': 5},
        {'id': 'bus2', 'direction': 'Northbound', 'index': 7},
    ]
    print_scenario(buses1, 'Northbound', 4, 'Bus ahead in requested direction')

    # Scenario 2: Bus at client in requested direction
    buses2 = [
        {'id': 'bus1', 'direction': 'Northbound', 'index': 3},
    ]
    print_scenario(buses2, 'Northbound', 3, 'Bus at client in requested direction')

    # Scenario 3: Fallback to opposite direction
    buses3 = [
        {'id': 'bus1', 'direction': 'Southbound', 'index': 10},
    ]
    print_scenario(buses3, 'Northbound', 1, 'Fallback to opposite direction')

    # Scenario 4: No negative index bus suggested
    buses4 = [
        {'id': 'bus1', 'direction': 'Southbound', 'index': -1},
    ]
    print_scenario(buses4, 'Northbound', 1, 'No negative index bus suggested')

    # Scenario 5: Multiple buses in opposite direction, closest selected
    buses5 = [
        {'id': 'bus1', 'direction': 'Southbound', 'index': 2},
        {'id': 'bus2', 'direction': 'Southbound', 'index': 8},
    ]
    print_scenario(buses5, 'Northbound', 5, 'Multiple buses in opposite direction, closest selected')

    # Scenario 6: No buses at all
    buses6 = []
    print_scenario(buses6, 'Northbound', 1, 'No buses at all')

if __name__ == '__main__':
    main() 