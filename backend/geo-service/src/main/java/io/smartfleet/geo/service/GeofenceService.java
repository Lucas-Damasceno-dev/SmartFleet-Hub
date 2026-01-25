package io.smartfleet.geo.service;

import io.smartfleet.geo.dto.RouteInfo;
import io.smartfleet.geo.provider.RoutingProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Geofencing service using PostGIS functions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeofenceService {

    private static final double DEFAULT_ARRIVAL_RADIUS_METERS = 200.0;

    @Value("${geo.arrival-detection-radius-meters:200}")
    private double arrivalRadiusMeters;

    @Value("${geo.eta-recalculation-interval-minutes:5}")
    private int etaRecalculationIntervalMinutes;

    @Value("${geo.route-deviation-threshold-meters:500}")
    private double routeDeviationThresholdMeters;

    private final RoutingProvider routingProvider;

    /**
     * Detect if a driver is within arrival radius of destination.
     * Uses ST_DWithin equivalent calculation.
     *
     * @param driverLat Driver current latitude
     * @param driverLon Driver current longitude
     * @param destLat Destination latitude
     * @param destLon Destination longitude
     * @return true if driver is within arrival radius
     */
    public boolean detectArrival(BigDecimal driverLat, BigDecimal driverLon,
                                  BigDecimal destLat, BigDecimal destLon) {
        double distance = routingProvider.calculateDistance(driverLat, driverLon, destLat, destLon);
        boolean isNearby = distance <= arrivalRadiusMeters;
        
        if (isNearby) {
            log.info("Arrival detected: driver at ({}, {}) is within {}m of destination",
                    driverLat, driverLon, arrivalRadiusMeters);
        }
        
        return isNearby;
    }

    /**
     * Check if driver is within specified distance of a point.
     * Equivalent to PostGIS ST_DWithin function.
     *
     * @param pointLat Point latitude
     * @param pointLon Point longitude
     * @param targetLat Target latitude
     * @param targetLon Target longitude
     * @param radiusMeters Radius in meters
     * @return true if within radius
     */
    public boolean isWithinDistance(BigDecimal pointLat, BigDecimal pointLon,
                                     BigDecimal targetLat, BigDecimal targetLon,
                                     double radiusMeters) {
        double distance = routingProvider.calculateDistance(pointLat, pointLon, targetLat, targetLon);
        return distance <= radiusMeters;
    }

    /**
     * Calculate ETA from current position to destination.
     *
     * @param currentLat Current latitude
     * @param currentLon Current longitude
     * @param destLat Destination latitude
     * @param destLon Destination longitude
     * @return Estimated time of arrival
     */
    public OffsetDateTime calculateEta(BigDecimal currentLat, BigDecimal currentLon,
                                        BigDecimal destLat, BigDecimal destLon) {
        RouteInfo route = routingProvider.calculateRoute(currentLat, currentLon, destLat, destLon);
        Duration travelTime = route.getDuration();
        
        return OffsetDateTime.now().plus(travelTime);
    }

    /**
     * Get route information between two points.
     *
     * @param originLat Origin latitude
     * @param originLon Origin longitude
     * @param destLat Destination latitude
     * @param destLon Destination longitude
     * @return Route information
     */
    public RouteInfo getRoute(BigDecimal originLat, BigDecimal originLon,
                               BigDecimal destLat, BigDecimal destLon) {
        return routingProvider.calculateRoute(originLat, originLon, destLat, destLon);
    }

    /**
     * Check if driver has deviated from expected route.
     *
     * @param driverLat Driver current latitude
     * @param driverLon Driver current longitude
     * @param expectedLat Expected position latitude
     * @param expectedLon Expected position longitude
     * @return true if deviation exceeds threshold
     */
    public boolean hasDeviatedFromRoute(BigDecimal driverLat, BigDecimal driverLon,
                                         BigDecimal expectedLat, BigDecimal expectedLon) {
        double deviation = routingProvider.calculateDistance(driverLat, driverLon, expectedLat, expectedLon);
        return deviation > routeDeviationThresholdMeters;
    }

    /**
     * Calculate distance between two points.
     *
     * @param lat1 First point latitude
     * @param lon1 First point longitude
     * @param lat2 Second point latitude
     * @param lon2 Second point longitude
     * @return Distance in meters
     */
    public double getDistance(BigDecimal lat1, BigDecimal lon1,
                              BigDecimal lat2, BigDecimal lon2) {
        return routingProvider.calculateDistance(lat1, lon1, lat2, lon2);
    }
}
