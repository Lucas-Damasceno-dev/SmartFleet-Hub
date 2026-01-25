import { Injectable, signal, computed } from '@angular/core';
import { DriverDto, DriverStatus, OrderDto, OrderStatus } from '../models';
import { DriverMarker, OrderWithDriver } from '../models/ui.models';

/**
 * Central state management using Angular Signals.
 * Provides reactive state for drivers and orders with efficient updates.
 */
@Injectable({
  providedIn: 'root'
})
export class AppState {
  // ========================================
  // Driver State
  // ========================================
  
  private readonly _drivers = signal<Map<number, DriverDto>>(new Map());
  private readonly _selectedDriverId = signal<number | null>(null);

  /** All drivers as an array */
  readonly drivers = computed(() => Array.from(this._drivers().values()));

  /** Drivers converted to map markers */
  readonly driverMarkers = computed<DriverMarker[]>(() => 
    this.drivers()
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
      }))
  );

  /** Currently selected driver */
  readonly selectedDriver = computed(() => {
    const id = this._selectedDriverId();
    return id ? this._drivers().get(id) : null;
  });

  /** Driver counts by status */
  readonly driverStats = computed(() => {
    const drivers = this.drivers();
    return {
      total: drivers.length,
      available: drivers.filter(d => d.status === 'AVAILABLE').length,
      busy: drivers.filter(d => d.status === 'BUSY').length,
      onDelivery: drivers.filter(d => d.status === 'ON_DELIVERY').length,
      offline: drivers.filter(d => d.status === 'OFFLINE').length
    };
  });

  // ========================================
  // Order State
  // ========================================

  private readonly _orders = signal<Map<number, OrderDto>>(new Map());
  private readonly _selectedOrderId = signal<number | null>(null);

  /** All orders as an array */
  readonly orders = computed(() => Array.from(this._orders().values()));

  /** Active orders (not delivered or cancelled) */
  readonly activeOrders = computed(() => 
    this.orders()
      .filter(o => !['DELIVERED', 'CANCELLED'].includes(o.status))
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
  );

  /** Completed orders */
  readonly completedOrders = computed(() =>
    this.orders()
      .filter(o => ['DELIVERED', 'CANCELLED'].includes(o.status))
      .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
  );

  /** Currently selected order */
  readonly selectedOrder = computed(() => {
    const id = this._selectedOrderId();
    return id ? this._orders().get(id) : null;
  });

  /** Order counts */
  readonly orderStats = computed(() => {
    const orders = this.orders();
    return {
      total: orders.length,
      active: this.activeOrders().length,
      completed: this.completedOrders().length,
      searching: orders.filter(o => o.status === 'SEARCHING').length,
      inTransit: orders.filter(o => o.status === 'IN_TRANSIT').length
    };
  });

  // ========================================
  // UI State
  // ========================================

  private readonly _sidebarOpen = signal(true);
  private readonly _mapCenter = signal<[number, number]>([-23.5629, -46.6544]);
  private readonly _mapZoom = signal(13);

  readonly sidebarOpen = this._sidebarOpen.asReadonly();
  readonly mapCenter = this._mapCenter.asReadonly();
  readonly mapZoom = this._mapZoom.asReadonly();

  // ========================================
  // Driver Actions
  // ========================================

  /** Set all drivers (bulk update) */
  setDrivers(drivers: DriverDto[]): void {
    const map = new Map(drivers.map(d => [d.id, d]));
    this._drivers.set(map);
  }

  /** Update a single driver */
  updateDriver(driver: DriverDto): void {
    this._drivers.update(map => {
      const newMap = new Map(map);
      newMap.set(driver.id, driver);
      return newMap;
    });
  }

  /** Update driver location (optimized for frequent updates) */
  updateDriverLocation(driverId: number, lat: number, lon: number, speed?: number, heading?: number): void {
    this._drivers.update(map => {
      const driver = map.get(driverId);
      if (!driver || !driver.currentLocation) return map;

      const newMap = new Map(map);
      newMap.set(driverId, {
        ...driver,
        currentLocation: {
          ...driver.currentLocation,
          latitude: lat,
          longitude: lon,
          speedKmh: speed ?? driver.currentLocation.speedKmh,
          headingDegrees: heading ?? driver.currentLocation.headingDegrees,
          time: new Date().toISOString()
        }
      });
      return newMap;
    });
  }

  /** Update driver status */
  updateDriverStatus(driverId: number, status: DriverStatus): void {
    this._drivers.update(map => {
      const driver = map.get(driverId);
      if (!driver) return map;

      const newMap = new Map(map);
      newMap.set(driverId, { ...driver, status });
      return newMap;
    });
  }

  /** Select a driver */
  selectDriver(driverId: number | null): void {
    this._selectedDriverId.set(driverId);
  }

  // ========================================
  // Order Actions
  // ========================================

  /** Set all orders (bulk update) */
  setOrders(orders: OrderDto[]): void {
    const map = new Map(orders.map(o => [o.id, o]));
    this._orders.set(map);
  }

  /** Update a single order */
  updateOrder(order: OrderDto): void {
    this._orders.update(map => {
      const newMap = new Map(map);
      newMap.set(order.id, order);
      return newMap;
    });
  }

  /** Update order status */
  updateOrderStatus(orderId: number, status: OrderStatus): void {
    this._orders.update(map => {
      const order = map.get(orderId);
      if (!order) return map;

      const newMap = new Map(map);
      newMap.set(orderId, { 
        ...order, 
        status,
        updatedAt: new Date().toISOString()
      });
      return newMap;
    });
  }

  /** Select an order */
  selectOrder(orderId: number | null): void {
    this._selectedOrderId.set(orderId);
  }

  /** Add new order */
  addOrder(order: OrderDto): void {
    this._orders.update(map => {
      const newMap = new Map(map);
      newMap.set(order.id, order);
      return newMap;
    });
  }

  /** Remove order */
  removeOrder(orderId: number): void {
    this._orders.update(map => {
      const newMap = new Map(map);
      newMap.delete(orderId);
      return newMap;
    });
  }

  // ========================================
  // UI Actions
  // ========================================

  toggleSidebar(): void {
    this._sidebarOpen.update(v => !v);
  }

  setSidebarOpen(open: boolean): void {
    this._sidebarOpen.set(open);
  }

  setMapCenter(lat: number, lon: number): void {
    this._mapCenter.set([lat, lon]);
  }

  setMapZoom(zoom: number): void {
    this._mapZoom.set(zoom);
  }

  // ========================================
  // Reset
  // ========================================

  /** Reset all state */
  reset(): void {
    this._drivers.set(new Map());
    this._orders.set(new Map());
    this._selectedDriverId.set(null);
    this._selectedOrderId.set(null);
    this._sidebarOpen.set(true);
    this._mapCenter.set([-23.5629, -46.6544]);
    this._mapZoom.set(13);
  }
}
