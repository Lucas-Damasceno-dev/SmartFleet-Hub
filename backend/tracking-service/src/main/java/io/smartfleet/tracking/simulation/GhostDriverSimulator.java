package io.smartfleet.tracking.simulation;

import io.smartfleet.common.dto.LocationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ghost Driver Simulation component for development.
 * Spawns 10 simulated drivers that move with Brownian motion around a central point.
 * 
 * Only active in 'dev' profile.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class GhostDriverSimulator {

    private static final int NUM_GHOST_DRIVERS = 10;
    private static final double MOVEMENT_DISTANCE_METERS = 500.0;  // Move 500m per tick
    private static final long TICK_INTERVAL_MS = 5000;  // Every 5 seconds
    private static final double EARTH_RADIUS_METERS = 6371000;

    // Central point: São Paulo, Brazil (Paulista Avenue)
    @Value("${simulation.center-latitude:-23.5629}")
    private double centerLatitude;

    @Value("${simulation.center-longitude:-46.6544}")
    private double centerLongitude;

    @Value("${simulation.enabled:true}")
    private boolean simulationEnabled;

    @Value("${kafka.topics.telemetry:telemetry-topic}")
    private String telemetryTopic;

    private final KafkaTemplate<String, LocationDto> kafkaTemplate;
    private final Random random = new Random();
    private final List<GhostDriver> ghostDrivers = new ArrayList<>();
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        if (!simulationEnabled) {
            log.info("Ghost driver simulation is disabled");
            return;
        }

        log.info("Initializing Ghost Driver Simulation with {} drivers around ({}, {})",
                NUM_GHOST_DRIVERS, centerLatitude, centerLongitude);

        // Spawn ghost drivers around the central point
        for (int i = 1; i <= NUM_GHOST_DRIVERS; i++) {
            // Random initial position within 2km of center
            double randomAngle = random.nextDouble() * 2 * Math.PI;
            double randomDistance = random.nextDouble() * 2000; // 0-2km

            double[] offset = calculateOffset(randomDistance, randomAngle);
            double lat = centerLatitude + offset[0];
            double lon = centerLongitude + offset[1];

            GhostDriver driver = new GhostDriver(
                    (long) i,
                    100L + i,  // Vehicle ID
                    BigDecimal.valueOf(lat).setScale(7, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(lon).setScale(7, RoundingMode.HALF_UP),
                    random.nextBoolean() ? "AVAILABLE" : "BUSY"
            );

            ghostDrivers.add(driver);
            log.debug("Spawned ghost driver {} at ({}, {})", i, lat, lon);
        }

        initialized.set(true);
        log.info("Ghost Driver Simulation initialized with {} drivers", ghostDrivers.size());
    }

    /**
     * Scheduled task to move ghost drivers with Brownian motion.
     * Runs every 5 seconds.
     */
    @Scheduled(fixedRate = TICK_INTERVAL_MS)
    public void simulationTick() {
        if (!simulationEnabled || !initialized.get()) {
            return;
        }

        log.debug("Simulation tick - moving {} ghost drivers", ghostDrivers.size());

        for (GhostDriver driver : ghostDrivers) {
            moveDriverBrownian(driver);
            publishTelemetry(driver);
        }
    }

    /**
     * Move driver with Brownian motion (random walk).
     * Moves approximately 500m in a random direction.
     */
    private void moveDriverBrownian(GhostDriver driver) {
        // Random direction (0 to 2π)
        double randomAngle = random.nextDouble() * 2 * Math.PI;

        // Calculate new position
        double[] offset = calculateOffset(MOVEMENT_DISTANCE_METERS, randomAngle);
        
        double newLat = driver.getLatitude().doubleValue() + offset[0];
        double newLon = driver.getLongitude().doubleValue() + offset[1];

        // Keep within reasonable bounds (within 10km of center)
        double distanceFromCenter = haversineDistance(centerLatitude, centerLongitude, newLat, newLon);
        if (distanceFromCenter > 10000) {
            // Move back towards center
            double angleToCenter = Math.atan2(
                    centerLatitude - newLat,
                    centerLongitude - newLon
            );
            offset = calculateOffset(MOVEMENT_DISTANCE_METERS * 2, angleToCenter);
            newLat = driver.getLatitude().doubleValue() + offset[0];
            newLon = driver.getLongitude().doubleValue() + offset[1];
        }

        driver.setLatitude(BigDecimal.valueOf(newLat).setScale(7, RoundingMode.HALF_UP));
        driver.setLongitude(BigDecimal.valueOf(newLon).setScale(7, RoundingMode.HALF_UP));

        // Random speed (0-60 km/h)
        driver.setSpeedKmh(BigDecimal.valueOf(random.nextDouble() * 60).setScale(2, RoundingMode.HALF_UP));

        // Random heading (0-360 degrees)
        driver.setHeading(BigDecimal.valueOf(Math.toDegrees(randomAngle)).setScale(2, RoundingMode.HALF_UP));

        // Toggle status occasionally
        if (random.nextDouble() < 0.05) { // 5% chance to toggle
            driver.setStatus(driver.getStatus().equals("AVAILABLE") ? "BUSY" : "AVAILABLE");
        }
    }

    /**
     * Publish telemetry event to Kafka.
     */
    private void publishTelemetry(GhostDriver driver) {
        LocationDto telemetry = LocationDto.builder()
                .time(OffsetDateTime.now())
                .vehicleId(driver.getVehicleId())
                .tripId(driver.getCurrentTripId())
                .latitude(driver.getLatitude())
                .longitude(driver.getLongitude())
                .speedKmh(driver.getSpeedKmh())
                .headingDegrees(driver.getHeading())
                .batteryLevel(BigDecimal.valueOf(random.nextDouble() * 50 + 50).setScale(2, RoundingMode.HALF_UP))
                .ignitionOn(true)
                .accuracyMeters(BigDecimal.valueOf(random.nextDouble() * 10 + 5).setScale(2, RoundingMode.HALF_UP))
                .build();

        try {
            kafkaTemplate.send(telemetryTopic, driver.getVehicleId().toString(), telemetry);
            log.trace("Published telemetry for vehicle {}: ({}, {})",
                    driver.getVehicleId(), driver.getLatitude(), driver.getLongitude());
        } catch (Exception e) {
            log.warn("Failed to publish telemetry for vehicle {}: {}", driver.getVehicleId(), e.getMessage());
        }
    }

    /**
     * Calculate latitude/longitude offset for a given distance and bearing.
     */
    private double[] calculateOffset(double distanceMeters, double bearingRadians) {
        double latOffset = (distanceMeters / EARTH_RADIUS_METERS) * (180.0 / Math.PI);
        double lonOffset = (distanceMeters / (EARTH_RADIUS_METERS * Math.cos(Math.toRadians(centerLatitude))))
                * (180.0 / Math.PI);

        return new double[]{
                latOffset * Math.cos(bearingRadians),
                lonOffset * Math.sin(bearingRadians)
        };
    }

    /**
     * Calculate Haversine distance between two points in meters.
     */
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Get current ghost drivers for debugging/monitoring.
     */
    public List<GhostDriver> getGhostDrivers() {
        return new ArrayList<>(ghostDrivers);
    }

    /**
     * Internal class representing a ghost driver.
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class GhostDriver {
        private Long driverId;
        private Long vehicleId;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String status;
        private BigDecimal speedKmh = BigDecimal.ZERO;
        private BigDecimal heading = BigDecimal.ZERO;
        private Long currentTripId = null;

        public GhostDriver(Long driverId, Long vehicleId, BigDecimal latitude, BigDecimal longitude, String status) {
            this.driverId = driverId;
            this.vehicleId = vehicleId;
            this.latitude = latitude;
            this.longitude = longitude;
            this.status = status;
        }
    }
}
