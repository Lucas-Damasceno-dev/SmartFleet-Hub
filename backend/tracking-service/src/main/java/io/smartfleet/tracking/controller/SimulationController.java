package io.smartfleet.tracking.controller;

import io.smartfleet.common.dto.ApiResponse;
import io.smartfleet.tracking.simulation.GhostDriverSimulator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for simulation monitoring (dev profile only).
 */
@RestController
@RequestMapping("/api/simulation")
@Profile("dev")
@RequiredArgsConstructor
public class SimulationController {

    private final GhostDriverSimulator ghostDriverSimulator;

    @GetMapping("/drivers")
    public ApiResponse<List<GhostDriverSimulator.GhostDriver>> getGhostDrivers() {
        return ApiResponse.of(ghostDriverSimulator.getGhostDrivers());
    }

    @GetMapping("/status")
    public ApiResponse<SimulationStatus> getSimulationStatus() {
        List<GhostDriverSimulator.GhostDriver> drivers = ghostDriverSimulator.getGhostDrivers();
        
        long available = drivers.stream()
                .filter(d -> "AVAILABLE".equals(d.getStatus()))
                .count();
        
        return ApiResponse.of(SimulationStatus.builder()
                .totalDrivers(drivers.size())
                .availableDrivers((int) available)
                .busyDrivers(drivers.size() - (int) available)
                .running(true)
                .build());
    }

    @lombok.Data
    @lombok.Builder
    public static class SimulationStatus {
        private int totalDrivers;
        private int availableDrivers;
        private int busyDrivers;
        private boolean running;
    }
}
