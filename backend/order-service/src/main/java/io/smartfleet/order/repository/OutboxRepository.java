package io.smartfleet.order.repository;

import io.smartfleet.order.entity.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for Outbox entity (Transactional Outbox Pattern).
 */
@Repository
public interface OutboxRepository extends JpaRepository<Outbox, UUID> {
}
