-- ============================================
-- SmartFleet Hub - Database Initialization Script
-- PostgreSQL with PostGIS & TimescaleDB Extensions
-- ============================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- ============================================
-- RELATIONAL TABLES
-- ============================================

-- Drivers Table
-- Stores driver profiles
CREATE TABLE drivers (
    driver_id SERIAL PRIMARY KEY,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    license_number TEXT UNIQUE NOT NULL,
    email TEXT UNIQUE,
    phone TEXT,
    status TEXT DEFAULT 'active' CHECK (status IN ('active', 'inactive', 'suspended')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Create index for driver status queries
CREATE INDEX idx_drivers_status ON drivers(status);

-- Vehicles Table
-- Stores static fleet information
CREATE TABLE vehicles (
    vehicle_id SERIAL PRIMARY KEY,
    license_plate TEXT UNIQUE NOT NULL,
    vin TEXT UNIQUE,
    make TEXT,
    model TEXT,
    year INTEGER,
    current_driver_id INTEGER REFERENCES drivers(driver_id),
    status TEXT DEFAULT 'active' CHECK (status IN ('active', 'maintenance', 'retired')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Create indexes for vehicle queries
CREATE INDEX idx_vehicles_driver ON vehicles(current_driver_id);
CREATE INDEX idx_vehicles_status ON vehicles(status);

-- Trips Table
-- Records individual journeys with geospatial data
CREATE TABLE trips (
    trip_id SERIAL PRIMARY KEY,
    vehicle_id INTEGER NOT NULL REFERENCES vehicles(vehicle_id),
    driver_id INTEGER REFERENCES drivers(driver_id),
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ,
    start_location GEOMETRY(Point, 4326), -- SRID 4326 (WGS 84)
    end_location GEOMETRY(Point, 4326),
    distance_km NUMERIC,
    status TEXT DEFAULT 'planned' CHECK (status IN ('planned', 'in_progress', 'completed', 'cancelled')),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Create indexes for trip queries
CREATE INDEX idx_trips_vehicle ON trips(vehicle_id);
CREATE INDEX idx_trips_driver ON trips(driver_id);
CREATE INDEX idx_trips_start_time ON trips(start_time);
CREATE INDEX idx_trips_status ON trips(status);

-- Spatial indexes for geospatial queries
CREATE INDEX idx_trips_start_loc ON trips USING GIST (start_location);
CREATE INDEX idx_trips_end_loc ON trips USING GIST (end_location);

-- ============================================
-- TIMESCALEDB HYPERTABLE - Telemetry Data
-- ============================================

-- Locations Table
-- High-frequency GPS telemetry ingestion
CREATE TABLE locations (
    time TIMESTAMPTZ NOT NULL,
    vehicle_id INTEGER NOT NULL REFERENCES vehicles(vehicle_id),
    trip_id INTEGER REFERENCES trips(trip_id),
    location GEOMETRY(Point, 4326) NOT NULL,
    speed_kmh NUMERIC,
    heading_degrees NUMERIC,
    battery_level NUMERIC,
    ignition_on BOOLEAN,
    accuracy_meters NUMERIC
);

-- Convert to Hypertable with time partitioning (1 day chunks)
-- and space partitioning by vehicle_id (4 partitions)
SELECT create_hypertable(
    'locations', 
    'time', 
    chunk_time_interval => INTERVAL '1 day', 
    partitioning_column => 'vehicle_id', 
    number_partitions => 4
);

-- Create indexes for hypertable
CREATE INDEX idx_locations_vehicle_time ON locations (vehicle_id, time DESC);
CREATE INDEX idx_locations_spatial ON locations USING GIST (location);

-- ============================================
-- DATA RETENTION POLICY
-- Drop raw telemetry after 90 days
-- ============================================
SELECT add_retention_policy('locations', INTERVAL '90 days');

-- ============================================
-- COMPRESSION POLICY
-- Compress chunks older than 7 days
-- ============================================
ALTER TABLE locations SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'vehicle_id',
    timescaledb.compress_orderby = 'time DESC'
);

SELECT add_compression_policy('locations', INTERVAL '7 days');

-- ============================================
-- CONTINUOUS AGGREGATES (Performance Optimization)
-- Hourly aggregated fleet statistics
-- ============================================
CREATE MATERIALIZED VIEW hourly_fleet_stats
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', time) AS bucket,
    vehicle_id,
    AVG(speed_kmh) as avg_speed,
    MAX(speed_kmh) as max_speed,
    COUNT(*) as point_count,
    LAST(location, time) as last_location
FROM locations
GROUP BY bucket, vehicle_id
WITH NO DATA;

-- Add refresh policy for continuous aggregate
SELECT add_continuous_aggregate_policy('hourly_fleet_stats',
    start_offset => INTERVAL '1 month',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour'
);

-- ============================================
-- OUTBOX TABLE (Transactional Outbox Pattern)
-- For Debezium CDC
-- ============================================
CREATE TABLE outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(255) NOT NULL,    -- e.g., 'ORDER', 'VEHICLE'
    aggregate_id VARCHAR(255) NOT NULL,      -- e.g., Order ID
    type VARCHAR(255) NOT NULL,              -- e.g., 'ORDER_CREATED'
    payload JSONB NOT NULL,                  -- The event data
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index for Debezium polling
CREATE INDEX idx_outbox_created_at ON outbox(created_at);

-- ============================================
-- VEHICLE TYPES TABLE (Pricing Support)
-- ============================================
CREATE TABLE vehicle_types (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    capacity_kg NUMERIC NOT NULL
);

-- ============================================
-- PRICING RULES TABLE
-- ============================================
CREATE TABLE pricing_rules (
    id SERIAL PRIMARY KEY,
    vehicle_type_id INTEGER REFERENCES vehicle_types(id),
    base_fee NUMERIC(10,2) NOT NULL DEFAULT 5.00,
    rate_km NUMERIC(10,2) NOT NULL DEFAULT 1.50,
    rate_min NUMERIC(10,2) NOT NULL DEFAULT 0.50,
    minimum_fare NUMERIC(10,2) NOT NULL DEFAULT 10.00,
    multiplier NUMERIC(5,2) NOT NULL DEFAULT 1.00,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- TRIGGER FUNCTION - Auto-update timestamps
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply triggers to tables with updated_at
CREATE TRIGGER update_drivers_updated_at
    BEFORE UPDATE ON drivers
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_vehicles_updated_at
    BEFORE UPDATE ON vehicles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_pricing_rules_updated_at
    BEFORE UPDATE ON pricing_rules
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
