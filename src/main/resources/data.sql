-- Insert test bus data
INSERT INTO buses (bus_id, tracker_imei, bus_number, route, bus_company, driver_id, driver_name) 
VALUES ('BUS001', '359339072173799', 'A123', 'Route 1', 'Test Bus Company', 'DRIVER001', 'John Doe')
ON CONFLICT (bus_id) DO NOTHING; 