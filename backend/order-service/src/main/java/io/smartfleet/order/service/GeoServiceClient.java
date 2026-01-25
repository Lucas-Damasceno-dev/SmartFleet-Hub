package io.smartfleet.order.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interface for geo service client.
 * Used for finding nearby drivers and calculating distances.
 */
public interface GeoServiceClient {

    /**
     * Find drivers within a radius of a given point.
     *
     * @param latitude Center point latitude
     * @param longitude Center point longitude
     * @param radiusKm Search radius in kilometers
     * @return List of available drivers sorted by distance
     */
    List<AvailableDriver> findDriversWithinRadius(BigDecimal latitude, BigDecimal longitude, double radiusKm);

    /**
     * Calculate distance between two points.
     *
     * @param lat1 First point latitude
     * @param lon1 First point longitude
     * @param lat2 Second point latitude
     * @param lon2 Second point longitude
     * @return Distance in kilometers
     */
    double calculateDistance(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2);
}
