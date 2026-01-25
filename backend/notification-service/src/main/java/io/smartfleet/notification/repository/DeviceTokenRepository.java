package io.smartfleet.notification.repository;

import io.smartfleet.notification.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findByUserIdAndActiveTrue(UUID userId);

    List<DeviceToken> findByDriverIdAndActiveTrue(UUID driverId);

    void deleteByToken(String token);

    boolean existsByToken(String token);
}
