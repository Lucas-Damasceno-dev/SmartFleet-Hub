package io.smartfleet.notification.controller;

import io.smartfleet.notification.entity.DeviceToken;
import io.smartfleet.notification.service.DeviceTokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping("/register-device")
    public ResponseEntity<Void> registerDevice(
            @Valid @RequestBody RegisterDeviceRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID userId = UUID.fromString(jwt.getSubject());
        UUID driverId = jwt.hasClaim("driver_id") 
                ? UUID.fromString(jwt.getClaimAsString("driver_id")) 
                : null;

        log.info("Registering device token for user: {}", userId);

        deviceTokenService.registerToken(
                userId,
                driverId,
                request.token(),
                request.deviceType()
        );

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/unregister-device")
    public ResponseEntity<Void> unregisterDevice(
            @RequestParam String token,
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Unregistering device token for user: {}", userId);

        deviceTokenService.deactivateToken(token);

        return ResponseEntity.ok().build();
    }

    public record RegisterDeviceRequest(
            @NotBlank String token,
            @NotNull DeviceToken.DeviceType deviceType
    ) {}
}
