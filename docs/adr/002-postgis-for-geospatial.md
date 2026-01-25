# 002 - Use PostGIS for Geospatial Operations

* **Status:** Accepted
* **Date:** 2025-01-15
* **Deciders:** Architecture Team

## Context
The fleet management system requires complex geospatial operations including:
- Finding drivers within a radius
- Geofence detection (entry/exit)
- Distance calculations
- Route proximity checks

## Decision
We will use **PostGIS** extension for PostgreSQL instead of external APIs (Google Maps, MapBox) for core geospatial logic because:
1. No per-query costs for internal operations
2. Sub-millisecond performance with spatial indexes (GIST)
3. Full SQL integration for complex joins with business data
4. Industry-standard OGC compliance
5. Reduces external dependencies for critical path operations

## Consequences
* **Positive:** Zero cost for unlimited geospatial queries
* **Positive:** All data stays in our infrastructure (privacy/security)
* **Positive:** Atomic transactions combining business logic with geo operations
* **Negative:** Requires spatial index tuning for optimal performance
* **Negative:** External APIs still needed for routing/directions (Google Maps)
