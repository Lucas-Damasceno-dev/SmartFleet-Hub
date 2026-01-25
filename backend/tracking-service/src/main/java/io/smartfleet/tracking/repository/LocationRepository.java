package io.smartfleet.tracking.repository;

import io.smartfleet.tracking.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Repository for Location entity (TimescaleDB Hypertable).
 */
@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    @Query(value = """
        SELECT * FROM locations 
        WHERE vehicle_id = :vehicleId 
        AND time >= :startTime 
        AND time <= :endTime 
        ORDER BY time DESC
        """, nativeQuery = true)
    List<Location> findByVehicleIdAndTimeRange(
            @Param("vehicleId") Long vehicleId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime);

    @Query(value = """
        SELECT * FROM locations 
        WHERE vehicle_id = :vehicleId 
        ORDER BY time DESC 
        LIMIT 1
        """, nativeQuery = true)
    Location findLatestByVehicleId(@Param("vehicleId") Long vehicleId);
    
    @Query(value = """
        SELECT * FROM locations 
        WHERE trip_id = :tripId 
        ORDER BY time ASC
        """, nativeQuery = true)
    List<Location> findByTripId(@Param("tripId") Long tripId);
}
