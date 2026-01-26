// Shared DTO interfaces synced from backend common-libs

export interface LocationDto {
  time: string;
  vehicleId: number;
  tripId?: number;
  latitude: number;
  longitude: number;
  altitude?: number;
  speedKmh: number;
  headingDegrees?: number;
  accuracy?: number;
  ignitionOn: boolean;
}

export interface DriverDto {
  id: number;
  name: string;
  email: string;
  phone: string;
  licenseNumber: string;
  status: DriverStatus;
  rating?: number;
  vehicleId?: number;
  currentLocation?: LocationDto;
}

export type DriverStatus = 'AVAILABLE' | 'BUSY' | 'OFFLINE' | 'ON_DELIVERY';

export interface VehicleDto {
  id: number;
  plate: string;
  model: string;
  year: number;
  status: VehicleStatus;
  driverId?: number;
}

export type VehicleStatus = 'ACTIVE' | 'MAINTENANCE' | 'INACTIVE';

export interface OrderDto {
  id: number;
  customerId: number;
  driverId?: number;
  vehicleId?: number;
  status: OrderStatus;
  pickupAddress: AddressDto;
  deliveryAddress: AddressDto;
  estimatedPrice?: number;
  finalPrice?: number;
  distanceKm?: number;
  durationMinutes?: number;
  createdAt: string;
  updatedAt: string;
  assignedAt?: string;
  pickedUpAt?: string;
  deliveredAt?: string;
}

export type OrderStatus = 
  | 'CREATED' 
  | 'SEARCHING' 
  | 'ASSIGNED' 
  | 'DRIVER_ARRIVED' 
  | 'PICKED_UP' 
  | 'IN_TRANSIT' 
  | 'DELIVERED' 
  | 'CANCELLED';

export interface AddressDto {
  street: string;
  number: string;
  complement?: string;
  neighborhood: string;
  city: string;
  state: string;
  zipCode: string;
  latitude: number;
  longitude: number;
}

export interface OrderEventDto {
  eventId: string;
  orderId: number;
  eventType: OrderEventType;
  payload: Record<string, unknown>;
  occurredAt: string;
}

export type OrderEventType = 
  | 'ORDER_CREATED'
  | 'ORDER_ASSIGNED'
  | 'DRIVER_NEARBY'
  | 'DRIVER_ARRIVED'
  | 'ORDER_PICKED_UP'
  | 'ORDER_DELIVERED'
  | 'ORDER_CANCELLED';

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ApiError {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance: string;
  timestamp: string;
  errors?: Record<string, string[]>;
}
