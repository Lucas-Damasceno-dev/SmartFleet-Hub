package io.smartfleet.geo.repository;

import io.smartfleet.geo.entity.DriverLocation;
import org.junit.jupiter.api.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgisContainerProvider;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Integration tests for DriverLocationRepository with PostGIS.
 * Uses TestContainers to run a real PostGIS database.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class DriverLocationRepositoryIT {

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

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), SRID_WGS84);

    // São Paulo coordinates
    private static final double CENTER_LAT = -23.5629;
    private static final double CENTER_LON = -46.6544;

    @BeforeEach
    void setUp() {
        driverLocationRepository.deleteAll();
    }

    @Test
    @DisplayName("should save and find driver location by driver ID")
    void shouldSaveAndFindByDriverId() {
        // Given
        DriverLocation driver = createDriverLocation(1L, 101L, CENTER_LAT, CENTER_LON, "AVAILABLE");
        driverLocationRepository.save(driver);

        // When
        Optional<DriverLocation> found = driverLocationRepository.findByDriverId(1L);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getDriverId()).isEqualTo(1L);
        assertThat(found.get().getLocation().getY()).isCloseTo(CENTER_LAT, within(0.0001));
        assertThat(found.get().getLocation().getX()).isCloseTo(CENTER_LON, within(0.0001));
    }

    @Test
    @DisplayName("should find drivers within radius using ST_DWithin")
    void shouldFindDriversWithinRadius() {
        // Given: 3 drivers at different distances from center
        // Driver 1: 100m from center
        DriverLocation driver1 = createDriverLocation(1L, 101L, CENTER_LAT + 0.0009, CENTER_LON, "AVAILABLE");
        // Driver 2: 500m from center
        DriverLocation driver2 = createDriverLocation(2L, 102L, CENTER_LAT + 0.0045, CENTER_LON, "AVAILABLE");
        // Driver 3: 2km from center
        DriverLocation driver3 = createDriverLocation(3L, 103L, CENTER_LAT + 0.018, CENTER_LON, "AVAILABLE");
        // Driver 4: 100m but BUSY (should not be included)
        DriverLocation driver4 = createDriverLocation(4L, 104L, CENTER_LAT + 0.0009, CENTER_LON, "BUSY");

        driverLocationRepository.saveAll(List.of(driver1, driver2, driver3, driver4));

        Point center = createPoint(CENTER_LAT, CENTER_LON);

        // When: search within 1000m radius
        List<DriverLocation> nearbyDrivers = driverLocationRepository.findDriversWithinRadius(center, 1000);

        // Then: should find driver1 and driver2 (both within 1km and AVAILABLE)
        assertThat(nearbyDrivers).hasSize(2);
        assertThat(nearbyDrivers.stream().map(DriverLocation::getDriverId))
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("should find all drivers within radius regardless of status")
    void shouldFindAllDriversWithinRadiusIgnoringStatus() {
        // Given
        DriverLocation driver1 = createDriverLocation(1L, 101L, CENTER_LAT + 0.0009, CENTER_LON, "AVAILABLE");
        DriverLocation driver2 = createDriverLocation(2L, 102L, CENTER_LAT + 0.0009, CENTER_LON, "BUSY");

        driverLocationRepository.saveAll(List.of(driver1, driver2));

        Point center = createPoint(CENTER_LAT, CENTER_LON);

        // When
        List<DriverLocation> allNearby = driverLocationRepository.findAllDriversWithinRadius(center, 500);

        // Then
        assertThat(allNearby).hasSize(2);
    }

    @Test
    @DisplayName("should correctly check if driver is within distance of a point")
    void shouldCheckDriverWithinDistance() {
        // Given: driver 100m from center
        DriverLocation driver = createDriverLocation(1L, 101L, CENTER_LAT + 0.0009, CENTER_LON, "AVAILABLE");
        driverLocationRepository.save(driver);

        Point target = createPoint(CENTER_LAT, CENTER_LON);

        // When: check if within 200m (should be true)
        boolean within200m = driverLocationRepository.isDriverWithinDistance(1L, target, 200);
        // When: check if within 50m (should be false - driver is ~100m away)
        boolean within50m = driverLocationRepository.isDriverWithinDistance(1L, target, 50);

        // Then
        assertThat(within200m).isTrue();
        assertThat(within50m).isFalse();
    }

    @Test
    @DisplayName("should calculate distance to point correctly")
    void shouldCalculateDistanceToPoint() {
        // Given: driver at center
        DriverLocation driver = createDriverLocation(1L, 101L, CENTER_LAT, CENTER_LON, "AVAILABLE");
        driverLocationRepository.save(driver);

        // Target point approximately 111m north (0.001 degrees latitude ≈ 111m)
        Point target = createPoint(CENTER_LAT + 0.001, CENTER_LON);

        // When
        Optional<Double> distance = driverLocationRepository.calculateDistanceToPoint(1L, target);

        // Then: distance should be approximately 111 meters
        assertThat(distance).isPresent();
        assertThat(distance.get()).isBetween(100.0, 120.0);
    }

    @Test
    @DisplayName("should return drivers sorted by distance (closest first)")
    void shouldReturnDriversSortedByDistance() {
        // Given: 3 drivers at increasing distances
        DriverLocation closest = createDriverLocation(1L, 101L, CENTER_LAT + 0.001, CENTER_LON, "AVAILABLE");
        DriverLocation medium = createDriverLocation(2L, 102L, CENTER_LAT + 0.003, CENTER_LON, "AVAILABLE");
        DriverLocation farthest = createDriverLocation(3L, 103L, CENTER_LAT + 0.005, CENTER_LON, "AVAILABLE");

        // Save in random order
        driverLocationRepository.saveAll(List.of(farthest, closest, medium));

        Point center = createPoint(CENTER_LAT, CENTER_LON);

        // When
        List<DriverLocation> drivers = driverLocationRepository.findDriversWithinRadius(center, 1000);

        // Then: should be sorted by distance
        assertThat(drivers).hasSize(3);
        assertThat(drivers.get(0).getDriverId()).isEqualTo(1L); // closest
        assertThat(drivers.get(1).getDriverId()).isEqualTo(2L); // medium
        assertThat(drivers.get(2).getDriverId()).isEqualTo(3L); // farthest
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
        // PostGIS uses (lon, lat) order
        return geometryFactory.createPoint(new Coordinate(lon, lat));
    }
}
