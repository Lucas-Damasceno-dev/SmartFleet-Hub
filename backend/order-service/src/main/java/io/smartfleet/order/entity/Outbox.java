package io.smartfleet.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Transactional Outbox entity for CDC (Change Data Capture).
 * Events are inserted here within the business transaction and captured by Debezium.
 */
@Entity
@Table(name = "outbox")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Outbox {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
