# SmartFleet Hub - Backend

## Overview
Maven multi-module backend project for the SmartFleet Hub fleet management system.

## Tech Stack
* **Language:** Java 21
* **Framework:** Spring Boot 3.4.1
* **Concurrency:** Virtual Threads (Project Loom)
* **Auth:** Keycloak (OAuth2 / OpenID Connect)
* **Database:** PostgreSQL with PostGIS & TimescaleDB
* **Messaging:** Apache Kafka
* **Cache:** Redis

## Modules

| Module | Port | Description |
|--------|------|-------------|
| `common-libs` | - | Shared DTOs, utilities |
| `auth-service` | 8080 | Identity Provider, SSO |
| `tracking-service` | 8082 | GPS Ingestion, Telemetry |
| `geo-service` | 8083 | Geofencing, Routes |
| `payment-service` | 8084 | Billing, Payouts |
| `order-service` | 8085 | Delivery Management |

## Prerequisites
* JDK 21
* Maven 3.9+
* Docker & Docker Compose

## Running Locally

1. Start infrastructure:
```bash
docker-compose -f docker-compose.dev.yml up -d
```

2. Build all modules:
```bash
cd backend
./mvnw clean install
```

3. Run a specific service:
```bash
./mvnw -pl auth-service spring-boot:run
```

## Testing
```bash
# Run all tests
./mvnw verify

# Run tests for a specific module
./mvnw -pl tracking-service verify
```

## Configuration
Environment variables are documented in each service's `application.yml`.
