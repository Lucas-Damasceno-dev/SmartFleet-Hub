-- ============================================
-- SmartFleet Hub - Seed Data
-- Initial data for development and testing
-- ============================================

-- Vehicle Types
INSERT INTO vehicle_types (id, name, capacity_kg) VALUES 
(1, 'Motorcycle', 20),
(2, 'Van', 800),
(3, 'Truck', 5000)
ON CONFLICT (id) DO NOTHING;

-- Initial Pricing Rules (per vehicle type)
INSERT INTO pricing_rules (vehicle_type_id, base_fee, rate_km, rate_min, minimum_fare, multiplier) VALUES
(1, 3.00, 1.00, 0.20, 8.00, 1.00),   -- Motorcycle
(2, 5.00, 1.50, 0.50, 10.00, 1.00),  -- Van
(3, 15.00, 2.50, 0.80, 25.00, 1.00)  -- Truck
ON CONFLICT DO NOTHING;

-- Admin User (password: admin123 - BCrypt hashed)
-- Note: In production, use Keycloak for user management
INSERT INTO app_users (id, email, role, created_at) VALUES
('admin-001', 'admin@smartfleet.io', 'ADMIN', NOW())
ON CONFLICT DO NOTHING;

-- Sample Drivers (for development)
INSERT INTO drivers (id, first_name, last_name, license_number, email, phone, status, fcm_token) VALUES
('driver-001', 'João', 'Silva', 'CNH123456789', 'joao.silva@smartfleet.dev', '+5511999990001', 'active', 'fcm-token-001'),
('driver-002', 'Maria', 'Santos', 'CNH987654321', 'maria.santos@smartfleet.dev', '+5511999990002', 'active', 'fcm-token-002'),
('driver-003', 'Pedro', 'Oliveira', 'CNH456789123', 'pedro.oliveira@smartfleet.dev', '+5511999990003', 'active', 'fcm-token-003'),
('driver-004', 'Ana', 'Costa', 'CNH789123456', 'ana.costa@smartfleet.dev', '+5511999990004', 'active', 'fcm-token-004'),
('driver-005', 'Carlos', 'Ferreira', 'CNH321654987', 'carlos.ferreira@smartfleet.dev', '+5511999990005', 'active', 'fcm-token-005')
ON CONFLICT DO NOTHING;

-- Sample Vehicles (for development)
INSERT INTO vehicles (id, license_plate, vin, make, model, year, vehicle_type_id, current_driver_id, status) VALUES
('vehicle-001', 'ABC-1234', '1HGBH41JXMN109186', 'Honda', 'CG 160', 2023, 1, 'driver-001', 'active'),
('vehicle-002', 'DEF-5678', '2T1BURHE1HC812345', 'Fiat', 'Fiorino', 2022, 2, 'driver-002', 'active'),
('vehicle-003', 'GHI-9012', '3FAHP0HA9CR123456', 'Mercedes', 'Sprinter', 2023, 2, 'driver-003', 'active'),
('vehicle-004', 'JKL-3456', '5YJSA1E24HF000001', 'Volkswagen', 'Delivery', 2021, 3, 'driver-004', 'active'),
('vehicle-005', 'MNO-7890', 'WVWZZZ3CZWE123456', 'Yamaha', 'Factor 150', 2023, 1, 'driver-005', 'active')
ON CONFLICT DO NOTHING;

-- Sample Geofences (São Paulo area landmarks)
INSERT INTO geofences (id, name, center, radius_meters, type) VALUES
('geofence-001', 'Centro SP', ST_SetSRID(ST_MakePoint(-46.6333, -23.5505), 4326), 5000, 'zone'),
('geofence-002', 'Av. Paulista', ST_SetSRID(ST_MakePoint(-46.6544, -23.5617), 4326), 1000, 'zone'),
('geofence-003', 'Aeroporto Congonhas', ST_SetSRID(ST_MakePoint(-46.6560, -23.6275), 4326), 2000, 'restricted'),
('geofence-004', 'Shopping Ibirapuera', ST_SetSRID(ST_MakePoint(-46.6658, -23.6100), 4326), 500, 'pickup')
ON CONFLICT DO NOTHING;

-- Sample Trips (for development - São Paulo area)
INSERT INTO trips (vehicle_id, driver_id, start_time, end_time, start_location, end_location, distance_km, status) VALUES
('vehicle-001', 'driver-001', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 
 ST_SetSRID(ST_MakePoint(-46.6333, -23.5505), 4326), 
 ST_SetSRID(ST_MakePoint(-46.6544, -23.5617), 4326), 
 5.2, 'completed'),
('vehicle-002', 'driver-002', NOW() - INTERVAL '1 hour', NULL, 
 ST_SetSRID(ST_MakePoint(-46.6388, -23.5489), 4326), 
 ST_SetSRID(ST_MakePoint(-46.6712, -23.5823), 4326), 
 NULL, 'in_progress'),
('vehicle-003', 'driver-003', NOW() + INTERVAL '1 hour', NULL, 
 ST_SetSRID(ST_MakePoint(-46.6250, -23.5300), 4326), 
 ST_SetSRID(ST_MakePoint(-46.6900, -23.5900), 4326), 
 NULL, 'planned')
ON CONFLICT DO NOTHING;
