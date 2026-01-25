package io.smartfleet.notification.service;

import io.smartfleet.notification.entity.DeviceToken;
import io.smartfleet.notification.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;

    @Transactional
    public void registerToken(UUID userId, UUID driverId, String token, DeviceToken.DeviceType deviceType) {
        log.info("Registering device token for user: {}, driver: {}", userId, driverId);

        // Check if token already exists
        if (deviceTokenRepository.existsByToken(token)) {
            log.debug("Token already registered, updating...");
            deviceTokenRepository.findByToken(token).ifPresent(existing -> {
                existing.setUserId(userId);
                existing.setDriverId(driverId);
                existing.setActive(true);
                deviceTokenRepository.save(existing);
            });
            return;
        }

        DeviceToken deviceToken = DeviceToken.builder()
                .userId(userId)
                .driverId(driverId)
                .token(token)
                .deviceType(deviceType)
                .active(true)
                .build();

        deviceTokenRepository.save(deviceToken);
        log.info("Device token registered successfully");
    }

    @Transactional
    public void deactivateToken(String token) {
        deviceTokenRepository.findByToken(token).ifPresent(deviceToken -> {
            deviceToken.setActive(false);
            deviceTokenRepository.save(deviceToken);
            log.info("Device token deactivated");
        });
    }

    public List<String> getActiveTokensForUser(UUID userId) {
        return deviceTokenRepository.findByUserIdAndActiveTrue(userId)
                .stream()
                .map(DeviceToken::getToken)
                .toList();
    }

    public List<String> getActiveTokensForDriver(UUID driverId) {
        return deviceTokenRepository.findByDriverIdAndActiveTrue(driverId)
                .stream()
                .map(DeviceToken::getToken)
                .toList();
    }
}
