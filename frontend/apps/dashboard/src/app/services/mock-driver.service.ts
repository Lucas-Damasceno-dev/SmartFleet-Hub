import { Injectable, signal, computed } from '@angular/core';
import { DriverDto, DriverStatus, LocationDto } from '../models';
import { DriverMarker } from '../models/ui.models';

/**
 * Mock service for driver data - simulates real-time driver locations
 */
@Injectable({
  providedIn: 'root'
})
export class MockDriverService {
  // São Paulo coordinates as center
  private readonly CENTER_LAT = -23.5629;
  private readonly CENTER_LON = -46.6544;

  // Signal-based state
  private driversSignal = signal<DriverDto[]>(this.generateMockDrivers());
  private driverMarkersSignal = signal<DriverMarker[]>([]);

  // Public computed signals
  readonly drivers = this.driversSignal.asReadonly();
  readonly driverMarkers = computed(() => this.mapToMarkers(this.driversSignal()));

  readonly availableDriversCount = computed(() => 
    this.driversSignal().filter(d => d.status === 'AVAILABLE').length
  );

  readonly busyDriversCount = computed(() => 
    this.driversSignal().filter(d => d.status === 'BUSY' || d.status === 'ON_DELIVERY').length
  );

  private intervalId?: ReturnType<typeof setInterval>;

  constructor() {
    this.startSimulation();
  }

  /**
   * Start real-time simulation of driver movements (Brownian motion)
   */
  startSimulation(): void {
    if (this.intervalId) return;

    this.intervalId = setInterval(() => {
      this.driversSignal.update(drivers => 
        drivers.map(driver => this.moveDriver(driver))
      );
    }, 3000); // Update every 3 seconds
  }

  /**
   * Stop the simulation
   */
  stopSimulation(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = undefined;
    }
  }

  /**
   * Get driver by ID
   */
  getDriver(id: number): DriverDto | undefined {
    return this.driversSignal().find(d => d.id === id);
  }

  /**
   * Update driver status
   */
  updateDriverStatus(driverId: number, status: DriverStatus): void {
    this.driversSignal.update(drivers =>
      drivers.map(d => d.id === driverId ? { ...d, status } : d)
    );
  }

  private generateMockDrivers(): DriverDto[] {
    const statuses: DriverStatus[] = ['AVAILABLE', 'BUSY', 'ON_DELIVERY', 'OFFLINE'];
    const names = [
      'Carlos Silva', 'Maria Santos', 'João Oliveira', 'Ana Souza',
      'Pedro Costa', 'Juliana Lima', 'Ricardo Ferreira', 'Fernanda Alves',
      'Bruno Martins', 'Camila Rodrigues'
    ];

    return names.map((name, index) => ({
      id: index + 1,
      name,
      email: `${name.toLowerCase().replace(' ', '.')}@smartfleet.com`,
      phone: `+55 11 9${String(Math.floor(Math.random() * 100000000)).padStart(8, '0')}`,
      licenseNumber: `SP${String(Math.floor(Math.random() * 1000000)).padStart(7, '0')}`,
      status: statuses[Math.floor(Math.random() * statuses.length)],
      rating: 3.5 + Math.random() * 1.5,
      vehicleId: 100 + index,
      currentLocation: this.generateRandomLocation(index)
    }));
  }

  private generateRandomLocation(seed: number): LocationDto {
    // Distribute drivers around the center with some randomness
    const angle = (seed / 10) * 2 * Math.PI;
    const radius = 0.01 + Math.random() * 0.02; // ~1-3km radius

    return {
      time: new Date().toISOString(),
      vehicleId: 100 + seed,
      latitude: this.CENTER_LAT + radius * Math.cos(angle),
      longitude: this.CENTER_LON + radius * Math.sin(angle),
      speedKmh: Math.floor(Math.random() * 60),
      headingDegrees: Math.floor(Math.random() * 360),
      ignitionOn: true
    };
  }

  private moveDriver(driver: DriverDto): DriverDto {
    if (!driver.currentLocation || driver.status === 'OFFLINE') {
      return driver;
    }

    // Brownian motion: random movement ~500m
    const deltaLat = (Math.random() - 0.5) * 0.009; // ~500m
    const deltaLon = (Math.random() - 0.5) * 0.009;

    return {
      ...driver,
      currentLocation: {
        ...driver.currentLocation,
        time: new Date().toISOString(),
        latitude: driver.currentLocation.latitude + deltaLat,
        longitude: driver.currentLocation.longitude + deltaLon,
        speedKmh: driver.status === 'ON_DELIVERY' 
          ? 30 + Math.floor(Math.random() * 40)
          : Math.floor(Math.random() * 20),
        headingDegrees: Math.floor(Math.random() * 360)
      }
    };
  }

  private mapToMarkers(drivers: DriverDto[]): DriverMarker[] {
    return drivers
      .filter(d => d.currentLocation && d.status !== 'OFFLINE')
      .map(d => ({
        id: d.id,
        name: d.name,
        status: d.status,
        latitude: d.currentLocation!.latitude,
        longitude: d.currentLocation!.longitude,
        heading: d.currentLocation!.headingDegrees,
        speed: d.currentLocation!.speedKmh,
        lastUpdate: new Date(d.currentLocation!.time)
      }));
  }
}
