-- Flyway Migration: Create pricing_rules table
-- Version: V1__create_pricing_rules_table.sql

CREATE TABLE IF NOT EXISTS pricing_rules (
    id BIGSERIAL PRIMARY KEY,
    vehicle_type_id INTEGER,
    base_fee DECIMAL(10, 2) NOT NULL DEFAULT 5.00,
    rate_km DECIMAL(10, 2) NOT NULL DEFAULT 1.50,
    rate_min DECIMAL(10, 2) NOT NULL DEFAULT 0.50,
    minimum_fare DECIMAL(10, 2) NOT NULL DEFAULT 10.00,
    multiplier DECIMAL(5, 2) NOT NULL DEFAULT 1.00,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    
    CONSTRAINT unique_vehicle_type_active UNIQUE (vehicle_type_id, active)
);

-- Create index for faster lookups
CREATE INDEX idx_pricing_rules_vehicle_type ON pricing_rules(vehicle_type_id) WHERE active = true;

-- Insert default pricing rule
INSERT INTO pricing_rules (vehicle_type_id, base_fee, rate_km, rate_min, minimum_fare, multiplier, active)
VALUES 
    (NULL, 5.00, 1.50, 0.50, 10.00, 1.00, true),  -- Default rule
    (1, 5.00, 1.50, 0.50, 10.00, 1.00, true),     -- Standard vehicle
    (2, 7.00, 2.00, 0.70, 15.00, 1.00, true),     -- Premium vehicle
    (3, 10.00, 2.50, 1.00, 20.00, 1.00, true);    -- Van/Truck

COMMENT ON TABLE pricing_rules IS 'Pricing rules per vehicle type for fare calculation';
COMMENT ON COLUMN pricing_rules.base_fee IS 'Base fee in BRL';
COMMENT ON COLUMN pricing_rules.rate_km IS 'Rate per kilometer in BRL';
COMMENT ON COLUMN pricing_rules.rate_min IS 'Rate per minute in BRL';
COMMENT ON COLUMN pricing_rules.minimum_fare IS 'Minimum fare in BRL';
COMMENT ON COLUMN pricing_rules.multiplier IS 'Surge pricing multiplier (1.0 = no surge)';
