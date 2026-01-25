-- ============================================
-- SmartFleet Hub - Seed Data
-- Initial data for development and testing
-- ============================================

-- Vehicle Types
INSERT INTO vehicle_types (id, name, capacity_kg) VALUES 
(1, 'Motorcycle', 20),
(2, 'Van', 800),
(3, 'Truck', 5000);

-- Initial Pricing Rules (per vehicle type)
INSERT INTO pricing_rules (vehicle_type_id, base_fee, rate_km, rate_min, minimum_fare, multiplier) VALUES
(1, 3.00, 1.00, 0.20, 8.00, 1.00),   -- Motorcycle
(2, 5.00, 1.50, 0.50, 10.00, 1.00),  -- Van
(3, 15.00, 2.50, 0.80, 25.00, 1.00); -- Truck

-- Sample Drivers (for development)
INSERT INTO drivers (first_name, last_name, license_number, email, phone, status) VALUES
('João', 'Silva', 'CNH123456789', 'joao.silva@smartfleet.dev', '+5511999990001', 'active'),
('Maria', 'Santos', 'CNH987654321', 'maria.santos@smartfleet.dev', '+5511999990002', 'active'),
('Pedro', 'Oliveira', 'CNH456789123', 'pedro.oliveira@smartfleet.dev', '+5511999990003', 'active'),
('Ana', 'Costa', 'CNH789123456', 'ana.costa@smartfleet.dev', '+5511999990004', 'active'),
('Carlos', 'Ferreira', 'CNH321654987', 'carlos.ferreira@smartfleet.dev', '+5511999990005', 'active');

-- Sample Vehicles (for development)
INSERT INTO vehicles (license_plate, vin, make, model, year, current_driver_id, status) VALUES
('ABC-1234', '1HGBH41JXMN109186', 'Honda', 'CG 160', 2023, 1, 'active'),
('DEF-5678', '2T1BURHE1HC812345', 'Fiat', 'Fiorino', 2022, 2, 'active'),
('GHI-9012', '3FAHP0HA9CR123456', 'Mercedes', 'Sprinter', 2023, 3, 'active'),
('JKL-3456', '5YJSA1E24HF000001', 'Volkswagen', 'Delivery', 2021, 4, 'active'),
('MNO-7890', 'WVWZZZ3CZWE123456', 'Yamaha', 'Factor 150', 2023, 5, 'active');

-- Sample Trips (for development - São Paulo area)
INSERT INTO trips (vehicle_id, driver_id, start_time, end_time, start_location, end_location, distance_km, status) VALUES
(1, 1, NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 
 ST_SetSRID(ST_MakePoint(-46.6333, -23.5505), 4326), 
 ST_SetSRID(ST_MakePoint(-46.6544, -23.5617), 4326), 
 5.2, 'completed'),
(2, 2, NOW() - INTERVAL '1 hour', NULL, 
 ST_SetSRID(ST_MakePoint(-46.6388, -23.5489), 4326), 
 ST_SetSRID(ST_MakePoint(-46.6712, -23.5823), 4326), 
 NULL, 'in_progress'),
(3, 3, NOW() + INTERVAL '1 hour', NULL, 
 ST_SetSRID(ST_MakePoint(-46.6250, -23.5300), 4326), 
 ST_SetSRID(ST_MakePoint(-46.6900, -23.5900), 4326), 
 NULL, 'planned');
