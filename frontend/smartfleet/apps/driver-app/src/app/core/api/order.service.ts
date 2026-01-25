import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface OrderOffer {
  id: string;
  pickupAddress: string;
  deliveryAddress: string;
  pickupCoordinates: Coordinates;
  deliveryCoordinates: Coordinates;
  estimatedDistance: number;
  estimatedDuration: number;
  estimatedPrice: number;
  expiresAt: string;
}

export interface ActiveOrder {
  id: string;
  status: OrderStatus;
  pickupAddress: string;
  deliveryAddress: string;
  pickupCoordinates: Coordinates;
  deliveryCoordinates: Coordinates;
  customerName?: string;
  customerPhone?: string;
  notes?: string;
  estimatedPrice: number;
  pickedUpAt?: string;
}

export type OrderStatus = 
  | 'ASSIGNED' 
  | 'EN_ROUTE_PICKUP' 
  | 'AT_PICKUP' 
  | 'PICKED_UP' 
  | 'IN_TRANSIT' 
  | 'AT_DESTINATION' 
  | 'DELIVERED';

export interface Coordinates {
  latitude: number;
  longitude: number;
}

export interface OrderAction {
  orderId: string;
  action: 'ACCEPT' | 'REJECT' | 'START_PICKUP' | 'ARRIVED_PICKUP' | 'PICKUP_COMPLETE' | 'ARRIVED_DESTINATION' | 'DELIVER';
}

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api';

  getPendingOffers(): Observable<OrderOffer[]> {
    return this.http.get<OrderOffer[]>(`${this.apiUrl}/orders/offers`);
  }

  getActiveOrder(): Observable<ActiveOrder | null> {
    return this.http.get<ActiveOrder | null>(`${this.apiUrl}/orders/active`);
  }

  acceptOrder(orderId: string): Observable<ActiveOrder> {
    return this.http.post<ActiveOrder>(`${this.apiUrl}/orders/${orderId}/accept`, {});
  }

  rejectOrder(orderId: string, reason?: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/orders/${orderId}/reject`, { reason });
  }

  startPickup(orderId: string): Observable<ActiveOrder> {
    return this.http.post<ActiveOrder>(`${this.apiUrl}/orders/${orderId}/start-pickup`, {});
  }

  arrivedAtPickup(orderId: string): Observable<ActiveOrder> {
    return this.http.post<ActiveOrder>(`${this.apiUrl}/orders/${orderId}/arrived-pickup`, {});
  }

  confirmPickup(orderId: string): Observable<ActiveOrder> {
    return this.http.post<ActiveOrder>(`${this.apiUrl}/orders/${orderId}/pickup-complete`, {});
  }

  arrivedAtDestination(orderId: string): Observable<ActiveOrder> {
    return this.http.post<ActiveOrder>(`${this.apiUrl}/orders/${orderId}/arrived-destination`, {});
  }

  completeDelivery(orderId: string, signature?: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/orders/${orderId}/deliver`, { signature });
  }

  getOrderHistory(page = 0, size = 20): Observable<OrderHistory> {
    return this.http.get<OrderHistory>(`${this.apiUrl}/orders/history`, {
      params: { page: page.toString(), size: size.toString() }
    });
  }
}

export interface OrderHistory {
  orders: CompletedOrder[];
  totalCount: number;
  page: number;
  size: number;
}

export interface CompletedOrder {
  id: string;
  pickupAddress: string;
  deliveryAddress: string;
  completedAt: string;
  finalPrice: number;
  rating?: number;
}
