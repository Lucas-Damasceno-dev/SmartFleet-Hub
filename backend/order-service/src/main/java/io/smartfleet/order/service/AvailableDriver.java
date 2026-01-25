package io.smartfleet.order.service;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO representing a driver available for dispatch.
 */
@Data
@Builder
public class AvailableDriver {
    private Long driverId;
    private String driverName;
    private Long vehicleId;
    private String vehiclePlate;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Double distanceKm;
    private String status;
}
