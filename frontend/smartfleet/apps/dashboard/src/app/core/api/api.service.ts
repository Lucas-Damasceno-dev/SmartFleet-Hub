import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ApiResponse<T> {
  data: T;
  message?: string;
  timestamp: string;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private http = inject(HttpClient);
  private baseUrl = 'http://localhost:8080/api';

  // Orders
  getOrders(page = 0, size = 20): Observable<PagedResponse<Order>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PagedResponse<Order>>(`${this.baseUrl}/orders`, { params });
  }

  getOrder(orderId: string): Observable<Order> {
    return this.http.get<Order>(`${this.baseUrl}/orders/${orderId}`);
  }

  createOrder(order: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>(`${this.baseUrl}/orders`, order);
  }

  cancelOrder(orderId: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/orders/${orderId}/cancel`, {});
  }

  // Drivers
  getDrivers(): Observable<Driver[]> {
    return this.http.get<Driver[]>(`${this.baseUrl}/tracking/drivers`);
  }

  getDriverLocation(driverId: string): Observable<DriverLocation> {
    return this.http.get<DriverLocation>(`${this.baseUrl}/tracking/drivers/${driverId}/location`);
  }

  getActiveDrivers(): Observable<DriverLocation[]> {
    return this.http.get<DriverLocation[]>(`${this.baseUrl}/tracking/drivers/active`);
  }

  // Vehicles
  getVehicles(): Observable<Vehicle[]> {
    return this.http.get<Vehicle[]>(`${this.baseUrl}/tracking/vehicles`);
  }

  getVehicleLocation(vehicleId: string): Observable<VehicleLocation> {
    return this.http.get<VehicleLocation>(`${this.baseUrl}/tracking/vehicles/${vehicleId}/location`);
  }

  // Geo
  calculateRoute(origin: Coordinates, destination: Coordinates): Observable<RouteResult> {
    return this.http.post<RouteResult>(`${this.baseUrl}/geo/route`, { origin, destination });
  }

  getEta(vehicleId: string, destination: Coordinates): Observable<EtaResult> {
    return this.http.post<EtaResult>(`${this.baseUrl}/geo/eta/${vehicleId}`, destination);
  }

  // Payments
  calculatePrice(request: PriceCalculationRequest): Observable<PriceResult> {
    return this.http.post<PriceResult>(`${this.baseUrl}/payments/calculate`, request);
  }
}

// Types
export interface Order {
  id: string;
  status: OrderStatus;
  pickupAddress: string;
  deliveryAddress: string;
  pickupCoordinates: Coordinates;
  deliveryCoordinates: Coordinates;
  driverId?: string;
  vehicleId?: string;
  estimatedPrice: number;
  finalPrice?: number;
  createdAt: string;
  assignedAt?: string;
  pickedUpAt?: string;
  deliveredAt?: string;
}

export type OrderStatus = 
  | 'CREATED' 
  | 'SEARCHING' 
  | 'ASSIGNED' 
  | 'PICKED_UP' 
  | 'IN_TRANSIT' 
  | 'DELIVERED' 
  | 'CANCELLED';

export interface CreateOrderRequest {
  pickupAddress: string;
  deliveryAddress: string;
  pickupCoordinates: Coordinates;
  deliveryCoordinates: Coordinates;
  notes?: string;
}

export interface Driver {
  id: string;
  name: string;
  phone: string;
  status: DriverStatus;
  vehicleId?: string;
}

export type DriverStatus = 'AVAILABLE' | 'BUSY' | 'OFFLINE';

export interface DriverLocation {
  driverId: string;
  driverName: string;
  vehicleId: string;
  latitude: number;
  longitude: number;
  heading: number;
  speedKmh: number;
  status: DriverStatus;
  lastUpdate: string;
}

export interface Vehicle {
  id: string;
  plate: string;
  model: string;
  type: string;
  status: string;
}

export interface VehicleLocation {
  vehicleId: string;
  plate: string;
  latitude: number;
  longitude: number;
  heading: number;
  speedKmh: number;
  lastUpdate: string;
}

export interface Coordinates {
  latitude: number;
  longitude: number;
}

export interface RouteResult {
  distanceKm: number;
  durationMinutes: number;
  polyline: string;
  waypoints: Coordinates[];
}

export interface EtaResult {
  etaMinutes: number;
  distanceKm: number;
  arrivalTime: string;
}

export interface PriceCalculationRequest {
  distanceKm: number;
  durationMinutes: number;
  vehicleType?: string;
}

export interface PriceResult {
  basePrice: number;
  distanceCharge: number;
  timeCharge: number;
  totalPrice: number;
  currency: string;
}
