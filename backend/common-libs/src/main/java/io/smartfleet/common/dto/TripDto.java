package io.smartfleet.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO for Trip information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripDto {

    private Long tripId;

    @NotNull(message = "Vehicle ID is required")
    private Long vehicleId;

    private Long driverId;

    @NotNull(message = "Start time is required")
    private OffsetDateTime startTime;

    private OffsetDateTime endTime;

    private BigDecimal startLatitude;
    private BigDecimal startLongitude;
    private BigDecimal endLatitude;
    private BigDecimal endLongitude;
    private BigDecimal distanceKm;
    private String status;
    private OffsetDateTime createdAt;
}
