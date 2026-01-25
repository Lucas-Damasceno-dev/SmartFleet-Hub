package io.smartfleet.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO for Vehicle information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDto {

    private Long vehicleId;

    @NotBlank(message = "License plate is required")
    private String licensePlate;

    private String vin;
    private String make;
    private String model;
    private Integer year;
    private Long currentDriverId;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
