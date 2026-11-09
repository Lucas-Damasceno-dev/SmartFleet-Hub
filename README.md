# SmartFleet Hub 🚚

**Fleet Management & Logistics Platform**

A production-ready, cloud-native fleet management system built with modern microservices architecture.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Running the Application](#running-the-application)
- [Development](#development)
- [Testing](#testing)
- [Observability](#observability)
- [Deployment](#deployment)
- [Contributing](#contributing)

---

## Overview

SmartFleet Hub is a comprehensive fleet management platform that provides:

- **Real-time Vehicle Tracking** - GPS telemetry ingestion at high throughput
- **Order Management** - Full delivery lifecycle with state machine workflow
- **Geofencing** - PostGIS-powered arrival detection and zone management
- **Dynamic Pricing** - Configurable pricing algorithms with surge support
- **Push Notifications** - FCM integration for driver alerts
- **Web Dashboard** - Angular-based real-time monitoring
- **Driver Mobile App** - Ionic/Capacitor cross-platform app

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        API Gateway                              │
│                   (Spring Cloud Gateway)                        │
└──────────────────────────┬──────────────────────────────────────┘
                           │
    ┌──────────────────────┼──────────────────────┐
    │                      │                      │
    ▼                      ▼                      ▼
┌─────────┐         ┌──────────┐          ┌───────────┐
│  Auth   │         │  Order   │          │ Tracking  │
│ Service │         │ Service  │          │  Service  │
└────┬────┘         └────┬─────┘          └─────┬─────┘
     │                   │                      │
     │         ┌─────────┼─────────┐            │
     │         │         │         │            │
     ▼         ▼         ▼         ▼            ▼
┌─────────┐ ┌─────┐ ┌────────┐ ┌───────┐ ┌─────────────┐
│Keycloak │ │Kafka│ │PostgreS│ │ Redis │ │ TimescaleDB │
└─────────┘ └─────┘ └────────┘ └───────┘ └─────────────┘
```

### Key Patterns

- **CQRS** - Separate read/write paths for telemetry
- **Transactional Outbox** - Reliable event publishing via Debezium
- **Event Sourcing** - Order state changes as events
- **Virtual Threads** - Java 21 lightweight concurrency

---

## Tech Stack

### Backend
| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Primary language with Virtual Threads |
| Spring Boot | 3.4.x | Microservices framework |
| PostgreSQL | 16 | Primary database |
| TimescaleDB | 2.x | Time-series telemetry storage |
| PostGIS | 3.x | Geospatial queries |
| Apache Kafka | 3.x | Event streaming |
| Redis | 7.x | Hot storage cache |
| Keycloak | 24.x | Identity & Access Management |

### Frontend
| Technology | Version | Purpose |
|------------|---------|---------|
| Angular | 19 | Web Dashboard |
| Ionic/Capacitor | 8 | Mobile Driver App |
| Leaflet | 1.9 | Map visualization |
| TailwindCSS | 3.x | UI styling |
| NGRX | 18 | State management |

### Infrastructure
| Technology | Purpose |
|------------|---------|
| Docker Compose | Local development |
| GitHub Actions | CI/CD pipeline |
| OpenTelemetry | Distributed tracing |
| Grafana/Loki/Tempo | Observability stack |

---

## Quick Start

### Prerequisites

- **JDK 21** (Temurin recommended)
- **Node.js 20+** & npm
- **Docker Desktop** (or Docker + Docker Compose v2)
- **Maven 3.9+** (or use included wrapper)

### 1. Clone & Setup

```bash
git clone https://github.com/Lucas-Damasceno-dev/SmartFleet-Hub.git
cd SmartFleet-Hub

# Copy environment template
cp .env.example .env
```

### 2. Start Infrastructure

```bash
# Start all required services (Postgres, Kafka, Keycloak, Redis, etc.)
docker compose -f docker-compose.dev.yml up -d

# Wait for services to be healthy (~2-3 minutes)
docker compose -f docker-compose.dev.yml ps
```

### 3. Register Debezium Connector

```bash
# Wait for Debezium to be ready, then register the outbox connector
./infra/docker/register-connector.sh
```

### 4. Build & Run Backend

```bash
cd backend

# Build all modules
./mvnw clean install -DskipTests

# Run services (in separate terminals or use IDE)
./mvnw -pl api-gateway spring-boot:run &
./mvnw -pl auth-service spring-boot:run &
./mvnw -pl order-service spring-boot:run &
./mvnw -pl tracking-service spring-boot:run &
./mvnw -pl geo-service spring-boot:run &
./mvnw -pl payment-service spring-boot:run &
./mvnw -pl notification-service spring-boot:run &
```

### 5. Build & Run Frontend

```bash
cd frontend

# Install dependencies
npm ci

# Run Dashboard in development mode
npx nx serve dashboard
```

### 6. Access Applications

| Application | URL | Credentials |
|-------------|-----|-------------|
| Web Dashboard | http://localhost:4200 | (Keycloak login) |
| Keycloak Admin | http://localhost:8081 | admin / admin |
| Kafka UI | http://localhost:8090 | - |
| Grafana | http://localhost:3000 | admin / admin |
| API Gateway | http://localhost:8080 | - |

---

## Project Structure

```
smartfleet-hub/
├── backend/                    # Java microservices
│   ├── api-gateway/            # Spring Cloud Gateway
│   ├── auth-service/           # Keycloak integration
│   ├── common-libs/            # Shared DTOs & utilities
│   ├── geo-service/            # PostGIS geofencing
│   ├── notification-service/   # FCM push notifications
│   ├── order-service/          # Order management & dispatch
│   ├── payment-service/        # Pricing & billing
│   └── tracking-service/       # GPS telemetry ingestion
├── frontend/                   # Nx monorepo
│   ├── apps/
│   │   ├── dashboard/          # Angular web app
│   │   └── driver-app/         # Ionic mobile app
│   └── libs/
│       ├── shared-ui/          # Reusable components
│       └── shared-types/       # TypeScript interfaces
├── infra/
│   ├── docker/                 # Docker configs & init scripts
│   ├── grafana/                # Grafana provisioning
│   ├── k8s/                    # Kubernetes manifests (future)
│   └── keycloak/               # Realm export
├── docs/                       # Documentation
│   ├── adr/                    # Architecture Decision Records
│   └── diagrams/               # System diagrams
├── docker-compose.dev.yml      # Development environment
├── docker-compose.prod.yml     # Production environment
└── .github/workflows/          # CI/CD pipelines
```

---

## Running the Application

### Development Mode with Ghost Drivers

To see live data flowing through the system, activate the Ghost Driver simulation:

```bash
# The tracking-service includes a dev-only simulation component
# It spawns 10 virtual drivers around São Paulo that emit GPS every 5 seconds

# Make sure tracking-service is running with dev profile
cd backend
SPRING_PROFILES_ACTIVE=dev ./mvnw -pl tracking-service spring-boot:run
```

Open the Dashboard at http://localhost:4200 to see drivers moving on the map.

### Creating Test Orders

```bash
# Via API (requires valid JWT token)
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "pickupAddress": "Rua Augusta, 100, São Paulo",
    "dropoffAddress": "Av. Paulista, 1000, São Paulo",
    "pickupLatitude": -23.5505,
    "pickupLongitude": -46.6333,
    "dropoffLatitude": -23.5617,
    "dropoffLongitude": -46.6544,
    "vehicleType": "MOTORCYCLE"
  }'
```

---

## Testing

### Backend Tests

```bash
cd backend

# Run all tests
./mvnw verify

# Run specific module tests
./mvnw -pl order-service test

# Run integration tests only
./mvnw verify -Pintegration-tests

# Run contract tests (Pact)
./mvnw test -Dtest=*PactTest
```

### Frontend Tests

```bash
cd frontend

# Unit tests
npx nx test dashboard

# E2E tests
npx nx e2e dashboard-e2e
```

---

## Observability

### Accessing Metrics & Traces

1. **Grafana Dashboard**: http://localhost:3000
   - Pre-configured datasources for Tempo (traces) and Loki (logs)
   
2. **View Distributed Traces**: 
   - Go to Grafana → Explore → Select "Tempo"
   - Search by service name or trace ID

3. **View Logs**:
   - Go to Grafana → Explore → Select "Loki"
   - Query: `{service="order-service"}`

### Health Checks

All services expose actuator endpoints:

```bash
# Check service health
curl http://localhost:8085/actuator/health

# View metrics
curl http://localhost:8085/actuator/metrics
```

---

## Deployment

### Production Checklist

1. **Environment Variables**: Set all secrets in `.env` (never commit!)
2. **SSL/TLS**: Configure certificates in `infra/nginx/certs/`
3. **Database**: Use managed PostgreSQL with replication
4. **Kafka**: Use managed Kafka cluster (Confluent, MSK)
5. **Keycloak**: Configure production realm, disable dev mode

### Using Production Compose

```bash
# Build production images
docker compose -f docker-compose.prod.yml build

# Deploy
docker compose -f docker-compose.prod.yml up -d
```

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feat/amazing-feature`
3. Commit changes: `git commit -m 'feat: add amazing feature'`
4. Push to the branch: `git push origin feat/amazing-feature`
5. Open a Pull Request

### Commit Convention

We follow [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation
- `refactor:` Code refactoring
- `test:` Adding tests
- `chore:` Maintenance

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Support

- 📖 Documentation: [docs/](./docs/)
- 🐛 Issues: [GitHub Issues](https://github.com/Lucas-Damasceno-dev/SmartFleet-Hub/issues)
