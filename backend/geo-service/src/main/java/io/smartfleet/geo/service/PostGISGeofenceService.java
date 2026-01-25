package io.smartfleet.geo.service;

import io.smartfleet.geo.dto.RouteInfo;
import io.smartfleet.geo.entity.DriverLocation;
import io.smartfleet.geo.entity.OrderLocation;
import io.smartfleet.geo.provider.RoutingProvider;
import io.smartfleet.geo.repository.DriverLocationRepository;
import io.smartfleet.geo.repository.OrderLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Geofencing service using PostGIS functions via repository.
 * Uses ST_DWithin for radius searches and ST_Distance for distance calculations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostGISGeofenceService {

    private static final int SRID_WGS84 = 4326;
    private static final double DEFAULT_ARRIVAL_RADIUS_METERS = 200.0;

    @Value("${geo.arrival-detection-radius-meters:200}")
    private double arrivalRadiusMeters;

    @Value("${geo.eta-recalculation-interval-minutes:5}")
    private int etaRecalculationIntervalMinutes;

    @Value("${geo.driver-search-radius-km:5}")
    private double driverSearchRadiusKm;

    private final DriverLocationRepository driverLocationRepository;
    private final OrderLocationRepository orderLocationRepository;
    private final RoutingProvider routingProvider;
    private final GeometryFactory geometryFactory;

    public PostGISGeofenceService(DriverLocationRepository driverLocationRepository,
                                   OrderLocationRepository orderLocationRepository,
                                   RoutingProvider routingProvider) {
        this.driverLocationRepository = driverLocationRepository;
        this.orderLocationRepository = orderLocationRepository;
        this.routingProvider = routingProvider;
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), SRID_WGS84);
    }

    /**
     * Find available drivers within radius of a point using PostGIS ST_DWithin.
     *
     * @param lat Center latitude
     * @param lon Center longitude
     * @param radiusKm Radius in kilometers
     * @return List of drivers within radius, sorted by distance
     */
    @Transactional(readOnly = true)
    public List<DriverLocation> findDriversWithinRadius(BigDecimal lat, BigDecimal lon, double radiusKm) {
        Point center = createPoint(lat, lon);
        double radiusMeters = radiusKm * 1000;

        log.debug("Finding drivers within {}km of ({}, {})", radiusKm, lat, lon);

        List<DriverLocation> drivers = driverLocationRepository.findDriversWithinRadius(center, radiusMeters);

        log.info("Found {} available drivers within {}km", drivers.size(), radiusKm);
        return drivers;
    }

    /**
     * Detect if driver has arrived at destination (within 200m).
     * Uses PostGIS ST_DWithin for accurate geographic distance.
     *
     * @param driverId Driver ID
     * @param destLat Destination latitude
     * @param destLon Destination longitude
     * @return true if driver is within arrival radius
     */
    @Transactional(readOnly = true)
    public boolean detectDriverArrival(Long driverId, BigDecimal destLat, BigDecimal destLon) {
        Point destination = createPoint(destLat, destLon);

        boolean isNearby = driverLocationRepository.isDriverWithinDistance(
                driverId, destination, arrivalRadiusMeters);

        if (isNearby) {
            log.info("Driver {} has arrived at destination (within {}m)", driverId, arrivalRadiusMeters);
        }

        return isNearby;
    }

    /**
     * Check if driver is near any assigned order's pickup location.
     *
     * @param driverId Driver ID
     * @return List of orders where driver is near pickup
     */
    @Transactional(readOnly = true)
    public List<OrderLocation> findOrdersWhereDriverNearPickup(Long driverId) {
        return orderLocationRepository.findDriverNearPickupOrders(driverId, arrivalRadiusMeters);
    }

    /**
     * Calculate distance from driver to a point using PostGIS.
     *
     * @param driverId Driver ID
     * @param targetLat Target latitude
     * @param targetLon Target longitude
     * @return Distance in meters, or empty if driver not found
     */
    @Transactional(readOnly = true)
    public Optional<Double> getDistanceToPoint(Long driverId, BigDecimal targetLat, BigDecimal targetLon) {
        Point target = createPoint(targetLat, targetLon);
        return driverLocationRepository.calculateDistanceToPoint(driverId, target);
    }

    /**
     * Update driver location in database.
     *
     * @param driverId Driver ID
     * @param vehicleId Vehicle ID
     * @param lat Latitude
     * @param lon Longitude
     * @param speedKmh Speed in km/h
     * @param heading Heading in degrees
     * @param status Driver status
     * @return Updated driver location
     */
    @Transactional
    public DriverLocation updateDriverLocation(Long driverId, Long vehicleId,
                                                 BigDecimal lat, BigDecimal lon,
                                                 Double speedKmh, Double heading,
                                                 String status) {
        Point location = createPoint(lat, lon);

        DriverLocation driverLocation = driverLocationRepository.findByDriverId(driverId)
                .orElse(DriverLocation.builder().driverId(driverId).build());

        driverLocation.setVehicleId(vehicleId);
        driverLocation.setLocation(location);
        driverLocation.setSpeedKmh(speedKmh);
        driverLocation.setHeadingDegrees(heading);
        driverLocation.setStatus(status != null ? status : "AVAILABLE");
        driverLocation.setLastUpdated(OffsetDateTime.now());

        return driverLocationRepository.save(driverLocation);
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
     */
    public RouteInfo getRoute(BigDecimal originLat, BigDecimal originLon,
                               BigDecimal destLat, BigDecimal destLon) {
        return routingProvider.calculateRoute(originLat, originLon, destLat, destLon);
    }

    /**
     * Create a PostGIS Point from lat/lon coordinates.
     * Note: PostGIS uses (lon, lat) order (X, Y).
     */
    private Point createPoint(BigDecimal lat, BigDecimal lon) {
        return geometryFactory.createPoint(new Coordinate(lon.doubleValue(), lat.doubleValue()));
    }
}
