package io.smartfleet.geo.provider;

import io.smartfleet.geo.dto.RouteInfo;

import java.math.BigDecimal;

/**
 * Interface for routing providers.
 * Allows swapping between Google Maps, OSRM, etc.
 */
public interface RoutingProvider {

    /**
     * Calculate route between two points.
     *
     * @param originLat Origin latitude
     * @param originLon Origin longitude
     * @param destLat Destination latitude
     * @param destLon Destination longitude
     * @return Route information including distance and duration
     */
    RouteInfo calculateRoute(BigDecimal originLat, BigDecimal originLon,
                             BigDecimal destLat, BigDecimal destLon);

    /**
     * Calculate distance between two points (straight line).
     *
     * @param lat1 First point latitude
     * @param lon1 First point longitude
     * @param lat2 Second point latitude
     * @param lon2 Second point longitude
     * @return Distance in meters
     */
    double calculateDistance(BigDecimal lat1, BigDecimal lon1,
                             BigDecimal lat2, BigDecimal lon2);
}
