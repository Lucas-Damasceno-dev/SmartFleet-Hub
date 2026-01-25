package io.smartfleet.geo.repository;

import io.smartfleet.geo.entity.OrderLocation;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for OrderLocation with PostGIS spatial queries.
 */
@Repository
public interface OrderLocationRepository extends JpaRepository<OrderLocation, Long> {

    List<OrderLocation> findByAssignedDriverId(Long driverId);

    List<OrderLocation> findByStatus(String status);

    /**
     * Find orders with pickup location within radius.
     */
    @Query(value = """
            SELECT ol.* FROM order_locations ol
            WHERE ST_DWithin(
                ol.pickup_location::geography,
                :center::geography,
                :distanceMeters
            )
            AND ol.status IN ('PENDING', 'ASSIGNED')
            ORDER BY ST_Distance(ol.pickup_location::geography, :center::geography)
            """, nativeQuery = true)
    List<OrderLocation> findOrdersWithPickupWithinRadius(
            @Param("center") Point center,
            @Param("distanceMeters") double distanceMeters);

    /**
     * Get active orders for a driver where driver is near pickup.
     */
    @Query(value = """
            SELECT ol.* FROM order_locations ol
            JOIN driver_locations dl ON ol.assigned_driver_id = dl.driver_id
            WHERE ol.assigned_driver_id = :driverId
            AND ol.status IN ('ASSIGNED', 'DRIVER_EN_ROUTE')
            AND ST_DWithin(
                dl.location::geography,
                ol.pickup_location::geography,
                :distanceMeters
            )
            """, nativeQuery = true)
    List<OrderLocation> findDriverNearPickupOrders(
            @Param("driverId") Long driverId,
            @Param("distanceMeters") double distanceMeters);
}
