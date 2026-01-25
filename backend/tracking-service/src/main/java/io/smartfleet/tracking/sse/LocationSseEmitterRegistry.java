package io.smartfleet.tracking.sse;

import io.smartfleet.common.dto.LocationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
public class LocationSseEmitterRegistry {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> vehicleSubscribers = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(long timeout) {
        String emitterId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(timeout);

        emitter.onCompletion(() -> removeEmitter(emitterId));
        emitter.onTimeout(() -> removeEmitter(emitterId));
        emitter.onError(e -> removeEmitter(emitterId));

        emitters.put(emitterId, emitter);
        log.info("Created SSE emitter: {}", emitterId);
        
        return emitter;
    }

    public String registerEmitter(SseEmitter emitter) {
        String emitterId = UUID.randomUUID().toString();
        
        emitter.onCompletion(() -> removeEmitter(emitterId));
        emitter.onTimeout(() -> removeEmitter(emitterId));
        emitter.onError(e -> removeEmitter(emitterId));
        
        emitters.put(emitterId, emitter);
        log.info("Registered SSE emitter: {}", emitterId);
        
        return emitterId;
    }

    public void subscribeToVehicle(String emitterId, UUID vehicleId) {
        vehicleSubscribers
                .computeIfAbsent(vehicleId, k -> new CopyOnWriteArraySet<>())
                .add(emitterId);
        log.debug("Emitter {} subscribed to vehicle {}", emitterId, vehicleId);
    }

    public void unsubscribeFromVehicle(String emitterId, UUID vehicleId) {
        Set<String> subscribers = vehicleSubscribers.get(vehicleId);
        if (subscribers != null) {
            subscribers.remove(emitterId);
        }
    }

    public void broadcastLocationUpdate(LocationDto location) {
        Set<String> subscribers = vehicleSubscribers.get(location.getVehicleId());
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }

        for (String emitterId : subscribers) {
            SseEmitter emitter = emitters.get(emitterId);
            if (emitter != null) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("location-update")
                            .data(location));
                } catch (IOException e) {
                    log.warn("Failed to send SSE to emitter {}: {}", emitterId, e.getMessage());
                    removeEmitter(emitterId);
                }
            }
        }
    }

    public void broadcastToAll(LocationDto location) {
        for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
            try {
                entry.getValue().send(SseEmitter.event()
                        .name("location-update")
                        .data(location));
            } catch (IOException e) {
                log.warn("Failed to send SSE to emitter {}: {}", entry.getKey(), e.getMessage());
                removeEmitter(entry.getKey());
            }
        }
    }

    private void removeEmitter(String emitterId) {
        emitters.remove(emitterId);
        vehicleSubscribers.values().forEach(subscribers -> subscribers.remove(emitterId));
        log.info("Removed SSE emitter: {}", emitterId);
    }

    public int getActiveEmitterCount() {
        return emitters.size();
    }
}
