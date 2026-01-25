package io.smartfleet.order.repository;

import io.smartfleet.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Order entity.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatus(String status);
    
    List<Order> findByAssignedDriverId(Long driverId);
    
    List<Order> findByAssignedVehicleId(Long vehicleId);
}
