package io.smartfleet.geo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

/**
 * DTO for route calculation result.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteInfo {

    private BigDecimal distanceMeters;
    private Duration duration;
    private String polyline;
    private List<Waypoint> waypoints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Waypoint {
        private BigDecimal latitude;
        private BigDecimal longitude;
    }
}
