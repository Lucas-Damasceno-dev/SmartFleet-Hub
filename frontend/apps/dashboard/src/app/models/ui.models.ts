import { DriverDto, LocationDto, OrderDto } from './dto.models';

// UI-specific models with computed properties

export interface DriverMarker {
  id: number;
  name: string;
  status: DriverDto['status'];
  latitude: number;
  longitude: number;
  heading?: number;
  speed?: number;
  vehiclePlate?: string;
  lastUpdate: Date;
}

export interface OrderWithDriver extends OrderDto {
  driver?: DriverDto;
  statusLabel: string;
  statusColor: StatusColor;
  formattedPrice: string;
  formattedDistance: string;
  formattedDuration: string;
}

export type StatusColor = 'primary' | 'success' | 'warning' | 'danger' | 'neutral';

export interface MapBounds {
  north: number;
  south: number;
  east: number;
  west: number;
}

export interface MapConfig {
  center: [number, number];
  zoom: number;
  minZoom: number;
  maxZoom: number;
}

export interface SidebarItem {
  id: string;
  label: string;
  icon: string;
  route: string;
  badge?: number;
}

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'info' | 'warning';
  title: string;
  message: string;
  duration?: number;
}
