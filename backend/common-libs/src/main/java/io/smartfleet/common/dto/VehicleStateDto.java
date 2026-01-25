package io.smartfleet.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Redis cache DTO for vehicle state (CQRS Hot Storage).
 * Stored as JSON in Redis key: vehicle:{vehicleId}:state
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VehicleStateDto {

    private Long vehicleId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal speedKmh;
    private String status;
    private Boolean ignitionOn;
    private Long currentTripId;
    private OffsetDateTime lastUpdate;
}
