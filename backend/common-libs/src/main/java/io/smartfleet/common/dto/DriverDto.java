package io.smartfleet.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO for Driver information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverDto {

    private Long driverId;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    @Email(message = "Invalid email format")
    private String email;

    private String phone;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
