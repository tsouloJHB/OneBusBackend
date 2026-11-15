-- V7: Seed popular South African locations into geocoding cache
-- These are pre-populated so searches don't always hit the Nominatim API

INSERT INTO geocoding_cache (query_key, original_query, latitude, longitude, display_name, location_type, confidence, created_at, last_accessed_at, access_count) VALUES

-- Johannesburg major areas
('auckland park', 'Auckland Park', -26.1853203, 27.9971831, 'Auckland Park, Johannesburg, Gauteng, South Africa', 'suburb', 0.95, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('sandton', 'Sandton', -26.1032, 28.0552, 'Sandton, Johannesburg, Gauteng, South Africa', 'suburb', 0.95, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('midrand', 'Midrand', -25.9822, 28.1105, 'Midrand, Johannesburg, Gauteng, South Africa', 'suburb', 0.95, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('randburg', 'Randburg', -26.1447, 27.9558, 'Randburg, Johannesburg, Gauteng, South Africa', 'suburb', 0.95, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('melville', 'Melville', -26.1920, 27.8810, 'Melville, Johannesburg, Gauteng, South Africa', 'suburb', 0.95, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('florida', 'Florida', -26.2236, 27.9559, 'Florida, Johannesburg, Gauteng, South Africa', 'suburb', 0.95, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('parktown', 'Parktown', -26.1855, 28.0336, 'Parktown, Johannesburg, Gauteng, South Africa', 'suburb', 0.95, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('braamfontein', 'Braamfontein', -26.1900, 28.0341, 'Braamfontein, Johannesburg, Gauteng, South Africa', 'suburb', 0.95, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('downtown johannesburg', 'Downtown Johannesburg', -26.2041, 28.0473, 'Downtown, Johannesburg, Gauteng, South Africa', 'suburb', 0.90, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- Pretoria
('pretoria', 'Pretoria', -25.7482, 28.2293, 'Pretoria, Gauteng, South Africa', 'city', 0.95, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- Other major SA cities
('cape town', 'Cape Town', -33.9249, 18.4241, 'Cape Town, Western Cape, South Africa', 'city', 0.95, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('durban', 'Durban', -29.8683, 31.0292, 'Durban, KwaZulu-Natal, South Africa', 'city', 0.95, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('bloemfontein', 'Bloemfontein', -29.1199, 25.5276, 'Bloemfontein, Free State, South Africa', 'city', 0.95, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Add more locations as needed (edit this file and re-run)
