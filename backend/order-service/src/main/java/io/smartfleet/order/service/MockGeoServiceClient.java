package io.smartfleet.order.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Mock implementation of GeoServiceClient for development.
 * Simulates drivers in the area for testing dispatch logic.
 */
@Component
@Slf4j
public class MockGeoServiceClient implements GeoServiceClient {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private final Random random = new Random();

    // Simulated drivers (in production, this would query tracking-service via Redis or API)
    private final List<AvailableDriver> simulatedDrivers = List.of(
            AvailableDriver.builder()
                    .driverId(1L).driverName("João Silva").vehicleId(1L).vehiclePlate("ABC-1234")
                    .latitude(new BigDecimal("-23.5505")).longitude(new BigDecimal("-46.6333")).status("AVAILABLE")
                    .build(),
            AvailableDriver.builder()
                    .driverId(2L).driverName("Maria Santos").vehicleId(2L).vehiclePlate("DEF-5678")
                    .latitude(new BigDecimal("-23.5520")).longitude(new BigDecimal("-46.6350")).status("AVAILABLE")
                    .build(),
            AvailableDriver.builder()
                    .driverId(3L).driverName("Pedro Costa").vehicleId(3L).vehiclePlate("GHI-9012")
                    .latitude(new BigDecimal("-23.5480")).longitude(new BigDecimal("-46.6300")).status("AVAILABLE")
                    .build()
    );

    @Override
    public List<AvailableDriver> findDriversWithinRadius(BigDecimal latitude, BigDecimal longitude, double radiusKm) {
        log.debug("Mock: Finding drivers within {}km of ({}, {})", radiusKm, latitude, longitude);

        List<AvailableDriver> nearbyDrivers = new ArrayList<>();

        for (AvailableDriver driver : simulatedDrivers) {
            double distance = calculateDistance(latitude, longitude, driver.getLatitude(), driver.getLongitude());
            
            if (distance <= radiusKm && "AVAILABLE".equals(driver.getStatus())) {
                AvailableDriver driverWithDistance = AvailableDriver.builder()
                        .driverId(driver.getDriverId())
                        .driverName(driver.getDriverName())
                        .vehicleId(driver.getVehicleId())
                        .vehiclePlate(driver.getVehiclePlate())
                        .latitude(driver.getLatitude())
                        .longitude(driver.getLongitude())
                        .distanceKm(distance)
                        .status(driver.getStatus())
                        .build();
                nearbyDrivers.add(driverWithDistance);
            }
        }

        // Sort by distance (greedy-proximity)
        nearbyDrivers.sort(Comparator.comparing(AvailableDriver::getDistanceKm));

        log.info("Mock: Found {} drivers within {}km", nearbyDrivers.size(), radiusKm);
        return nearbyDrivers;
    }

    @Override
    public double calculateDistance(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        return haversineDistance(
                lat1.doubleValue(), lon1.doubleValue(),
                lat2.doubleValue(), lon2.doubleValue()
        );
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}
