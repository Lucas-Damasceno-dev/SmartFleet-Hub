package io.smartfleet.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO for Location/GPS telemetry data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {

    @NotNull(message = "Time is required")
    private OffsetDateTime time;

    @NotNull(message = "Vehicle ID is required")
    private Long vehicleId;

    private Long tripId;

    @NotNull(message = "Latitude is required")
    private BigDecimal latitude;

    @NotNull(message = "Longitude is required")
    private BigDecimal longitude;

    private BigDecimal speedKmh;
    private BigDecimal headingDegrees;
    private BigDecimal batteryLevel;
    private Boolean ignitionOn;
    private BigDecimal accuracyMeters;
}
