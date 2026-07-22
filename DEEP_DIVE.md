# 🚚 SmartFleet Hub (Grandão) - Deep Dive Técnico

## 📋 Visão Geral do Projeto

O **SmartFleet Hub** é uma plataforma de gestão de frotas e logística de nível enterprise, construída com arquitetura de microserviços. Este é o projeto mais completo e ambicioso do portfólio, demonstrando domínio de tecnologias avançadas como CQRS, Event Sourcing, Transactional Outbox, Virtual Threads do Java 21 e observabilidade completa.

---

## 🏗️ Arquitetura de Microserviços

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

### Serviços

| Serviço | Porta | Responsabilidade |
|---------|-------|------------------|
| api-gateway | 8080 | Roteamento, rate limiting, autenticação |
| auth-service | 8081 | Integração Keycloak, JWT |
| order-service | 8082 | Gestão de pedidos, state machine |
| tracking-service | 8083 | Ingestão de telemetria GPS |
| geo-service | 8084 | Geofencing, PostGIS |
| payment-service | 8085 | Precificação dinâmica, billing |
| notification-service | 8086 | Push notifications (FCM) |

---

## 🔧 Padrões Arquiteturais Avançados

### 1. CQRS (Command Query Responsibility Segregation)

**Problema**: Telemetria GPS gera milhares de writes por segundo, mas reads são menos frequentes e precisam de agregações diferentes.

**Solução Implementada**:

```java
// tracking-service - Write Side
@Service
public class TelemetryCommandService {
    private final KafkaTemplate<String, TelemetryEvent> kafkaTemplate;
    
    public void ingestTelemetry(TelemetryDTO dto) {
        TelemetryEvent event = TelemetryEvent.builder()
            .vehicleId(dto.getVehicleId())
            .latitude(dto.getLatitude())
            .longitude(dto.getLongitude())
            .speed(dto.getSpeed())
            .heading(dto.getHeading())
            .timestamp(Instant.now())
            .build();
        
        // Write to Kafka (high throughput)
        kafkaTemplate.send("vehicle-telemetry", dto.getVehicleId(), event);
    }
}

// tracking-service - Read Side (Consumer)
@KafkaListener(topics = "vehicle-telemetry")
public void consumeTelemetry(TelemetryEvent event) {
    // Write to TimescaleDB for time-series queries
    timescaleRepository.save(toTimescaleEntity(event));
    
    // Update Redis for real-time position (hot storage)
    redisTemplate.opsForValue().set(
        "vehicle:position:" + event.getVehicleId(),
        toPositionJson(event),
        Duration.ofMinutes(5)
    );
}
```

**Por que CQRS aqui?**
- Writes podem escalar independentemente (Kafka partitions)
- Reads otimizados para diferentes padrões (time-series vs real-time)
- Eventual consistency aceitável para posições de veículos

---

### 2. Transactional Outbox Pattern

**Problema**: Publicar eventos no Kafka e salvar no banco precisa ser atômico. Se Kafka falhar após o commit do banco, perdem-se eventos.

**Solução com Debezium**:

```java
// order-service - Transactional Outbox
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    private UUID id;
    
    @Column(name = "aggregate_type")
    private String aggregateType;
    
    @Column(name = "aggregate_id")
    private String aggregateId;
    
    @Column(name = "event_type")
    private String eventType;
    
    @Column(columnDefinition = "jsonb")
    private String payload;
    
    @CreatedDate
    private Instant createdAt;
}

// No service, salvamos na mesma transação
@Transactional
public Order createOrder(CreateOrderRequest request) {
    Order order = orderRepository.save(buildOrder(request));
    
    // Salva evento no outbox (mesma transação)
    outboxRepository.save(OutboxEvent.builder()
        .aggregateType("Order")
        .aggregateId(order.getId().toString())
        .eventType("OrderCreated")
        .payload(objectMapper.writeValueAsString(toOrderEvent(order)))
        .build());
    
    return order;
}
```

**Debezium captura mudanças no outbox table e publica no Kafka**:
```json
// debezium-connector.json
{
  "name": "outbox-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "transforms": "outbox",
    "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
    "transforms.outbox.table.field.event.key": "aggregate_id",
    "transforms.outbox.table.field.event.type": "event_type",
    "transforms.outbox.table.field.event.payload": "payload"
  }
}
```

---

### 3. Event Sourcing para Pedidos

**Problema**: Pedidos passam por muitos estados. Precisamos de histórico completo para auditoria e replay.

**State Machine**:
```java
// order-service - State Machine
public enum OrderState {
    CREATED,
    AWAITING_PAYMENT,
    PAYMENT_CONFIRMED,
    DISPATCHED,
    DRIVER_ASSIGNED,
    PICKED_UP,
    IN_TRANSIT,
    ARRIVED,
    DELIVERED,
    CANCELLED
}

@Entity
public class OrderEvent {
    @Id
    private UUID id;
    
    @Column(name = "order_id")
    private UUID orderId;
    
    @Enumerated(EnumType.STRING)
    private OrderState fromState;
    
    @Enumerated(EnumType.STRING)
    private OrderState toState;
    
    private String eventType;
    private String payload;
    private String triggeredBy;
    
    @CreatedDate
    private Instant occurredAt;
}

@Service
public class OrderStateMachine {
    
    private static final Map<OrderState, Set<OrderState>> VALID_TRANSITIONS = Map.of(
        OrderState.CREATED, Set.of(OrderState.AWAITING_PAYMENT, OrderState.CANCELLED),
        OrderState.AWAITING_PAYMENT, Set.of(OrderState.PAYMENT_CONFIRMED, OrderState.CANCELLED),
        OrderState.PAYMENT_CONFIRMED, Set.of(OrderState.DISPATCHED, OrderState.CANCELLED),
        OrderState.DISPATCHED, Set.of(OrderState.DRIVER_ASSIGNED),
        OrderState.DRIVER_ASSIGNED, Set.of(OrderState.PICKED_UP, OrderState.DISPATCHED),
        OrderState.PICKED_UP, Set.of(OrderState.IN_TRANSIT),
        OrderState.IN_TRANSIT, Set.of(OrderState.ARRIVED),
        OrderState.ARRIVED, Set.of(OrderState.DELIVERED)
    );
    
    @Transactional
    public Order transition(UUID orderId, OrderState newState, String triggeredBy) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        
        if (!canTransition(order.getState(), newState)) {
            throw new InvalidStateTransitionException(order.getState(), newState);
        }
        
        OrderState oldState = order.getState();
        order.setState(newState);
        orderRepository.save(order);
        
        // Record event
        orderEventRepository.save(OrderEvent.builder()
            .orderId(orderId)
            .fromState(oldState)
            .toState(newState)
            .eventType(newState.name())
            .triggeredBy(triggeredBy)
            .build());
        
        return order;
    }
    
    public List<OrderEvent> getOrderHistory(UUID orderId) {
        return orderEventRepository.findByOrderIdOrderByOccurredAtAsc(orderId);
    }
    
    public Order rebuildOrder(UUID orderId) {
        // Replay all events to rebuild state
        List<OrderEvent> events = getOrderHistory(orderId);
        Order order = new Order();
        order.setId(orderId);
        
        for (OrderEvent event : events) {
            applyEvent(order, event);
        }
        
        return order;
    }
}
```

---

### 4. Geofencing com PostGIS

**Problema**: Detectar quando veículo entra/sai de uma zona (cliente, armazém).

**Solução**:

```java
// geo-service
@Repository
public interface GeofenceRepository extends JpaRepository<Geofence, UUID> {
    
    @Query(value = """
        SELECT g.* FROM geofences g
        WHERE ST_Contains(g.polygon, ST_SetSRID(ST_Point(:lng, :lat), 4326))
        AND g.active = true
        """, nativeQuery = true)
    List<Geofence> findContainingPoint(
        @Param("lat") double latitude, 
        @Param("lng") double longitude);
    
    @Query(value = """
        SELECT ST_Distance(
            ST_SetSRID(ST_Point(:lng1, :lat1), 4326)::geography,
            ST_SetSRID(ST_Point(:lng2, :lat2), 4326)::geography
        )
        """, nativeQuery = true)
    double calculateDistance(
        @Param("lat1") double lat1, @Param("lng1") double lng1,
        @Param("lat2") double lat2, @Param("lng2") double lng2);
}

@Service
public class GeofenceService {
    
    @KafkaListener(topics = "vehicle-telemetry")
    public void checkGeofences(TelemetryEvent event) {
        List<Geofence> zones = geofenceRepository.findContainingPoint(
            event.getLatitude(), event.getLongitude());
        
        String cacheKey = "vehicle:zones:" + event.getVehicleId();
        Set<String> previousZones = getCachedZones(cacheKey);
        Set<String> currentZones = zones.stream()
            .map(z -> z.getId().toString())
            .collect(Collectors.toSet());
        
        // Detect entries
        for (String zoneId : currentZones) {
            if (!previousZones.contains(zoneId)) {
                publishEvent(new GeofenceEntryEvent(event.getVehicleId(), zoneId));
            }
        }
        
        // Detect exits
        for (String zoneId : previousZones) {
            if (!currentZones.contains(zoneId)) {
                publishEvent(new GeofenceExitEvent(event.getVehicleId(), zoneId));
            }
        }
        
        // Update cache
        redisTemplate.opsForSet().add(cacheKey, currentZones.toArray(new String[0]));
    }
}
```

---

### 5. Virtual Threads (Java 21)

**Problema**: Alta concorrência de conexões WebSocket para tracking em tempo real.

**Configuração**:
```java
// application.yml
spring:
  threads:
    virtual:
      enabled: true

server:
  tomcat:
    threads:
      max: 200  # Virtual threads handle scaling

// TrackingService.java
@Async
public CompletableFuture<Void> processHighVolumeTelemetry(List<TelemetryDTO> batch) {
    // With virtual threads, this scales to thousands of concurrent operations
    return CompletableFuture.allOf(
        batch.stream()
            .map(dto -> CompletableFuture.runAsync(() -> processSingle(dto)))
            .toArray(CompletableFuture[]::new)
    );
}
```

---

### 6. Precificação Dinâmica

```java
// payment-service
@Service
public class DynamicPricingService {
    
    public PriceQuote calculatePrice(PriceRequest request) {
        BigDecimal basePrice = getBasePrice(request.getVehicleType());
        
        // Distance-based pricing
        double distanceKm = geoService.calculateDistance(
            request.getPickupLat(), request.getPickupLng(),
            request.getDropoffLat(), request.getDropoffLng()) / 1000;
        
        BigDecimal distancePrice = basePrice.multiply(BigDecimal.valueOf(distanceKm));
        
        // Surge pricing (demand-based)
        BigDecimal surgeMultiplier = calculateSurgeMultiplier(
            request.getPickupLat(), request.getPickupLng());
        
        // Time-based pricing (rush hour)
        BigDecimal timeMultiplier = calculateTimeMultiplier(LocalTime.now());
        
        // Weather impact
        BigDecimal weatherMultiplier = getWeatherMultiplier(
            request.getPickupLat(), request.getPickupLng());
        
        BigDecimal totalPrice = distancePrice
            .multiply(surgeMultiplier)
            .multiply(timeMultiplier)
            .multiply(weatherMultiplier);
        
        return PriceQuote.builder()
            .basePrice(basePrice)
            .distanceKm(distanceKm)
            .distancePrice(distancePrice)
            .surgeMultiplier(surgeMultiplier)
            .timeMultiplier(timeMultiplier)
            .weatherMultiplier(weatherMultiplier)
            .totalPrice(totalPrice)
            .validUntil(Instant.now().plusSeconds(300)) // 5 min validity
            .build();
    }
    
    private BigDecimal calculateSurgeMultiplier(double lat, double lng) {
        // Count active orders in area
        int activeOrders = orderService.countActiveInRadius(lat, lng, 5); // 5km radius
        int availableDrivers = driverService.countAvailableInRadius(lat, lng, 5);
        
        if (availableDrivers == 0) return new BigDecimal("2.5"); // Max surge
        
        double ratio = (double) activeOrders / availableDrivers;
        
        if (ratio > 2) return new BigDecimal("1.8");
        if (ratio > 1.5) return new BigDecimal("1.5");
        if (ratio > 1) return new BigDecimal("1.2");
        return BigDecimal.ONE;
    }
}
```

---

## 🔭 Observabilidade

### OpenTelemetry + Grafana Stack

```yaml
# docker-compose.dev.yml (excerpt)
services:
  tempo:
    image: grafana/tempo:latest
    
  loki:
    image: grafana/loki:latest
    
  grafana:
    image: grafana/grafana:latest
    volumes:
      - ./infra/grafana/provisioning:/etc/grafana/provisioning
```

**Correlação de Traces**:
```java
// Propagação automática de trace context
@RestController
public class OrderController {
    
    private final Tracer tracer;
    
    @PostMapping("/orders")
    public Order createOrder(@RequestBody CreateOrderRequest request) {
        Span span = tracer.spanBuilder("createOrder").startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("order.customer_id", request.getCustomerId());
            span.setAttribute("order.vehicle_type", request.getVehicleType());
            
            return orderService.create(request);
        } finally {
            span.end();
        }
    }
}
```

---

## 📱 Frontend

### Dashboard Angular + NGRX

```typescript
// store/tracking.effects.ts
@Injectable()
export class TrackingEffects {
    
    connectWebSocket$ = createEffect(() => this.actions$.pipe(
        ofType(TrackingActions.connect),
        switchMap(() => this.websocket.connect('/ws/tracking').pipe(
            map(message => TrackingActions.positionUpdated({ vehicle: message })),
            catchError(error => of(TrackingActions.connectionError({ error })))
        ))
    ));
    
    updateMapMarkers$ = createEffect(() => this.actions$.pipe(
        ofType(TrackingActions.positionUpdated),
        tap(({ vehicle }) => {
            this.mapService.updateMarker(vehicle.id, vehicle.position);
        })
    ), { dispatch: false });
}
```

### Driver App (Ionic)

```typescript
// driver-app/src/services/location.service.ts
@Injectable()
export class LocationService {
    
    startTracking() {
        return Geolocation.watchPosition({
            enableHighAccuracy: true,
            timeout: 10000,
            maximumAge: 0
        }, (position, error) => {
            if (position) {
                this.sendTelemetry({
                    latitude: position.coords.latitude,
                    longitude: position.coords.longitude,
                    speed: position.coords.speed,
                    heading: position.coords.heading,
                    accuracy: position.coords.accuracy
                });
            }
        });
    }
    
    private sendTelemetry(data: TelemetryData) {
        this.http.post('/api/v1/telemetry', data).subscribe();
    }
}
```

---

## 📊 Modelo de Domínio Global

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              SmartFleet Hub                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐                    │
│  │   Customer  │────>│    Order    │<────│   Driver    │                    │
│  └─────────────┘     └─────────────┘     └─────────────┘                    │
│                             │                   │                            │
│                             │                   │                            │
│                      ┌──────┴──────┐     ┌──────┴──────┐                    │
│                      │             │     │             │                    │
│                      ▼             ▼     ▼             │                    │
│               ┌─────────────┐ ┌─────────────┐  ┌───────────────┐            │
│               │OrderEvent   │ │  Payment    │  │   Vehicle     │            │
│               │(Event Src)  │ └─────────────┘  └───────────────┘            │
│               └─────────────┘                          │                    │
│                                                        ▼                    │
│                                               ┌───────────────┐             │
│                                               │   Telemetry   │             │
│                                               │ (TimescaleDB) │             │
│                                               └───────────────┘             │
│                                                        │                    │
│                                                        ▼                    │
│                                               ┌───────────────┐             │
│                                               │   Geofence    │             │
│                                               │   (PostGIS)   │             │
│                                               └───────────────┘             │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🧪 Estratégia de Testes

### Tipos de Testes
- **Unit Tests**: JUnit 5 + Mockito
- **Integration Tests**: TestContainers
- **Contract Tests**: Pact
- **E2E Tests**: Playwright (frontend)

```java
// Contract Test Example
@Pact(consumer = "order-service", provider = "tracking-service")
public RequestResponsePact createPact(PactDslWithProvider builder) {
    return builder
        .given("vehicle 123 exists")
        .uponReceiving("get vehicle position")
        .path("/api/v1/vehicles/123/position")
        .method("GET")
        .willRespondWith()
        .status(200)
        .body(new PactDslJsonBody()
            .stringValue("vehicleId", "123")
            .numberType("latitude", -23.5505)
            .numberType("longitude", -46.6333))
        .toPact();
}
```

---

## 🚀 CI/CD

```yaml
# .github/workflows/backend-ci.yml
name: Backend CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgis/postgis:16-3.4
      kafka:
        image: confluentinc/cp-kafka:7.5.0
      redis:
        image: redis:7
    
    steps:
      - uses: actions/checkout@v4
      
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'maven'
      
      - name: Build & Test
        run: ./mvnw verify
      
      - name: SonarQube Analysis
        run: ./mvnw sonar:sonar
```

---

## 📝 Conclusão

O SmartFleet Hub é o projeto mais complexo e demonstra domínio de:

- **Arquitetura de Microserviços**: 7 serviços independentes
- **CQRS**: Separação de write (Kafka) e read (TimescaleDB)
- **Event Sourcing**: Estado reconstruível por eventos
- **Transactional Outbox**: Consistência com Debezium
- **Geospatial**: PostGIS para geofencing
- **Java 21**: Virtual Threads para alta concorrência
- **Observabilidade**: OpenTelemetry + Grafana Stack
- **CI/CD**: GitHub Actions com TestContainers

Este é um sistema de nível enterprise, pronto para operação em produção com milhares de veículos simultâneos.
