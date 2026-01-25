package io.smartfleet.geo.repository;

import io.smartfleet.geo.entity.DriverLocation;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for DriverLocation with PostGIS spatial queries.
 */
@Repository
public interface DriverLocationRepository extends JpaRepository<DriverLocation, Long> {

    Optional<DriverLocation> findByDriverId(Long driverId);

    Optional<DriverLocation> findByVehicleId(Long vehicleId);

    List<DriverLocation> findByStatus(String status);

    /**
     * Find drivers within a radius of a point using PostGIS ST_DWithin.
     * ST_DWithin uses meters for geography type.
     *
     * @param center The center point
     * @param distanceMeters Search radius in meters
     * @return List of driver locations within the radius
     */
    @Query(value = """
            SELECT dl.* FROM driver_locations dl
            WHERE ST_DWithin(
                dl.location::geography,
                :center::geography,
                :distanceMeters
            )
            AND dl.status = 'AVAILABLE'
            ORDER BY ST_Distance(dl.location::geography, :center::geography)
            """, nativeQuery = true)
    List<DriverLocation> findDriversWithinRadius(
            @Param("center") Point center,
            @Param("distanceMeters") double distanceMeters);

    /**
     * Find drivers within radius with any status.
     */
    @Query(value = """
            SELECT dl.* FROM driver_locations dl
            WHERE ST_DWithin(
                dl.location::geography,
                :center::geography,
                :distanceMeters
            )
            ORDER BY ST_Distance(dl.location::geography, :center::geography)
            """, nativeQuery = true)
    List<DriverLocation> findAllDriversWithinRadius(
            @Param("center") Point center,
            @Param("distanceMeters") double distanceMeters);

    /**
     * Calculate distance between driver location and a point.
     * Returns distance in meters.
     */
    @Query(value = """
            SELECT ST_Distance(
                dl.location::geography,
                :target::geography
            ) FROM driver_locations dl
            WHERE dl.driver_id = :driverId
            """, nativeQuery = true)
    Optional<Double> calculateDistanceToPoint(
            @Param("driverId") Long driverId,
            @Param("target") Point target);

    /**
     * Check if driver is within distance of a point.
     */
    @Query(value = """
            SELECT EXISTS(
                SELECT 1 FROM driver_locations dl
                WHERE dl.driver_id = :driverId
                AND ST_DWithin(
                    dl.location::geography,
                    :target::geography,
                    :distanceMeters
                )
            )
            """, nativeQuery = true)
    boolean isDriverWithinDistance(
            @Param("driverId") Long driverId,
            @Param("target") Point target,
            @Param("distanceMeters") double distanceMeters);
}
