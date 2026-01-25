package io.smartfleet.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Base event class for all domain events.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainEvent {

    private String eventId;
    private String eventType;
    private String aggregateType;
    private String aggregateId;
    private OffsetDateTime timestamp;
    private Object payload;

    public static DomainEvent create(String eventType, String aggregateType, String aggregateId, Object payload) {
        return DomainEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .timestamp(OffsetDateTime.now())
                .payload(payload)
                .build();
    }
}
