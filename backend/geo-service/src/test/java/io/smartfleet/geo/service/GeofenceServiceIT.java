package io.smartfleet.geo.service;

import io.smartfleet.geo.entity.DriverLocation;
import io.smartfleet.geo.entity.OrderLocation;
import io.smartfleet.geo.provider.MockRoutingProvider;
import io.smartfleet.geo.repository.DriverLocationRepository;
import io.smartfleet.geo.repository.OrderLocationRepository;
import org.junit.jupiter.api.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgisContainerProvider;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for geofencing logic with PostGIS.
 * Tests the geofence trigger for DRIVER_NEARBY event.
 */
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class GeofenceServiceIT {

    private static final int SRID_WGS84 = 4326;

    @Container
    static PostgreSQLContainer<?> postgisContainer = new PostgreSQLContainer<>(
            new PostgisContainerProvider().newInstance()
                    .getDockerImageName()
    )
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init-postgis.sql");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgisContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgisContainer::getUsername);
        registry.add("spring.datasource.password", postgisContainer::getPassword);
    }

    @Autowired
    private DriverLocationRepository driverLocationRepository;

    @Autowired
    private OrderLocationRepository orderLocationRepository;

    @Autowired
    private PostGISGeofenceService postGISGeofenceService;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), SRID_WGS84);

    // São Paulo - Paulista Avenue
    private static final double PICKUP_LAT = -23.5629;
    private static final double PICKUP_LON = -46.6544;

    @BeforeEach
    void setUp() {
        driverLocationRepository.deleteAll();
        orderLocationRepository.deleteAll();
    }

    @Test
    @DisplayName("should detect driver arrival when within 200m of pickup location")
    void shouldDetectDriverArrivalWithin200m() {
        // Given: Driver at 100m from pickup location
        double driverLat = PICKUP_LAT + 0.0009; // ~100m north
        DriverLocation driver = createDriverLocation(1L, 101L, driverLat, PICKUP_LON, "ON_DELIVERY");
        driverLocationRepository.save(driver);

        // When
        boolean isArrived = postGISGeofenceService.detectDriverArrival(
                1L,
                BigDecimal.valueOf(PICKUP_LAT),
                BigDecimal.valueOf(PICKUP_LON)
        );

        // Then
        assertThat(isArrived).isTrue();
    }

    @Test
    @DisplayName("should not detect arrival when driver is more than 200m away")
    void shouldNotDetectArrivalWhenDriverFarAway() {
        // Given: Driver at 500m from pickup location
        double driverLat = PICKUP_LAT + 0.0045; // ~500m north
        DriverLocation driver = createDriverLocation(1L, 101L, driverLat, PICKUP_LON, "ON_DELIVERY");
        driverLocationRepository.save(driver);

        // When
        boolean isArrived = postGISGeofenceService.detectDriverArrival(
                1L,
                BigDecimal.valueOf(PICKUP_LAT),
                BigDecimal.valueOf(PICKUP_LON)
        );

        // Then
        assertThat(isArrived).isFalse();
    }

    @Test
    @DisplayName("should find drivers within 5km radius for dispatch")
    void shouldFindDriversWithin5kmForDispatch() {
        // Given: Multiple drivers at various distances
        // Driver 1: 1km away
        DriverLocation driver1 = createDriverLocation(1L, 101L, PICKUP_LAT + 0.009, PICKUP_LON, "AVAILABLE");
        // Driver 2: 3km away
        DriverLocation driver2 = createDriverLocation(2L, 102L, PICKUP_LAT + 0.027, PICKUP_LON, "AVAILABLE");
        // Driver 3: 4km away
        DriverLocation driver3 = createDriverLocation(3L, 103L, PICKUP_LAT + 0.036, PICKUP_LON, "AVAILABLE");
        // Driver 4: 10km away (should NOT be found)
        DriverLocation driver4 = createDriverLocation(4L, 104L, PICKUP_LAT + 0.09, PICKUP_LON, "AVAILABLE");

        driverLocationRepository.saveAll(List.of(driver1, driver2, driver3, driver4));

        // When
        List<DriverLocation> nearbyDrivers = postGISGeofenceService.findDriversWithinRadius(
                BigDecimal.valueOf(PICKUP_LAT),
                BigDecimal.valueOf(PICKUP_LON),
                5.0 // 5km radius
        );

        // Then
        assertThat(nearbyDrivers).hasSize(3);
        assertThat(nearbyDrivers.stream().map(DriverLocation::getDriverId))
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DisplayName("should update driver location and query successfully")
    void shouldUpdateDriverLocationAndQuery() {
        // When: Update driver location through service
        DriverLocation updated = postGISGeofenceService.updateDriverLocation(
                1L, 101L,
                BigDecimal.valueOf(PICKUP_LAT),
                BigDecimal.valueOf(PICKUP_LON),
                30.0, 90.0, "AVAILABLE"
        );

        // Then
        assertThat(updated.getId()).isNotNull();
        assertThat(updated.getDriverId()).isEqualTo(1L);
        assertThat(updated.getLocation().getY()).isCloseTo(PICKUP_LAT, Assertions.within(0.0001));
        assertThat(updated.getLocation().getX()).isCloseTo(PICKUP_LON, Assertions.within(0.0001));

        // And can be found in radius search
        List<DriverLocation> found = postGISGeofenceService.findDriversWithinRadius(
                BigDecimal.valueOf(PICKUP_LAT),
                BigDecimal.valueOf(PICKUP_LON),
                1.0
        );
        assertThat(found).hasSize(1);
    }

    @Test
    @DisplayName("should get accurate distance between driver and destination")
    void shouldGetAccurateDistance() {
        // Given: Driver at known distance from pickup
        // 0.001 degrees latitude ≈ 111 meters
        double driverLat = PICKUP_LAT + 0.001;
        DriverLocation driver = createDriverLocation(1L, 101L, driverLat, PICKUP_LON, "ON_DELIVERY");
        driverLocationRepository.save(driver);

        // When
        var distance = postGISGeofenceService.getDistanceToPoint(
                1L,
                BigDecimal.valueOf(PICKUP_LAT),
                BigDecimal.valueOf(PICKUP_LON)
        );

        // Then
        assertThat(distance).isPresent();
        assertThat(distance.get()).isBetween(100.0, 120.0); // ~111m
    }

    private DriverLocation createDriverLocation(Long driverId, Long vehicleId,
                                                  double lat, double lon, String status) {
        return DriverLocation.builder()
                .driverId(driverId)
                .vehicleId(vehicleId)
                .location(createPoint(lat, lon))
                .status(status)
                .speedKmh(0.0)
                .headingDegrees(0.0)
                .lastUpdated(OffsetDateTime.now())
                .build();
    }

    private Point createPoint(double lat, double lon) {
        return geometryFactory.createPoint(new Coordinate(lon, lat));
    }
}
