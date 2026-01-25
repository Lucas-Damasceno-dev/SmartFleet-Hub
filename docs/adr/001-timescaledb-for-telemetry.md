# 001 - Use TimescaleDB for GPS Telemetry

* **Status:** Accepted
* **Date:** 2025-01-15
* **Deciders:** Architecture Team

## Context
We need to store massive amounts of GPS telemetry data (10k+ points per second) with efficient time-based queries for historical analysis and real-time tracking.

## Decision
We will use **TimescaleDB** (PostgreSQL extension) instead of InfluxDB or pure PostgreSQL because:
1. Native PostgreSQL compatibility - same tooling, drivers, and ORM support
2. Automatic partitioning (hypertables) with time-based chunks
3. Built-in compression and retention policies
4. Can coexist with PostGIS for geospatial queries in the same database
5. Continuous aggregates for efficient downsampling

## Consequences
* **Positive:** Single database for both relational and time-series data, reducing operational complexity
* **Positive:** Familiar SQL interface for all developers
* **Positive:** Automatic data lifecycle management (retention, compression)
* **Negative:** Requires TimescaleDB extension installation and license for some advanced features
* **Negative:** Slightly higher learning curve for hypertable optimization
