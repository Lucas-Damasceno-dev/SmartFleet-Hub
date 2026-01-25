import { Injectable, signal, computed } from '@angular/core';
import { OrderDto, OrderStatus } from '../models';
import { OrderWithDriver, StatusColor } from '../models/ui.models';

/**
 * Mock service for order data
 */
@Injectable({
  providedIn: 'root'
})
export class MockOrderService {
  private ordersSignal = signal<OrderDto[]>(this.generateMockOrders());

  // Public signals
  readonly orders = this.ordersSignal.asReadonly();

  readonly activeOrders = computed(() => 
    this.ordersSignal()
      .filter(o => !['DELIVERED', 'CANCELLED'].includes(o.status))
      .map(o => this.enrichOrder(o))
  );

  readonly completedOrders = computed(() =>
    this.ordersSignal()
      .filter(o => ['DELIVERED', 'CANCELLED'].includes(o.status))
      .map(o => this.enrichOrder(o))
  );

  readonly ordersCount = computed(() => ({
    total: this.ordersSignal().length,
    active: this.activeOrders().length,
    completed: this.completedOrders().length
  }));

  constructor() {}

  /**
   * Get order by ID
   */
  getOrder(id: number): OrderWithDriver | undefined {
    const order = this.ordersSignal().find(o => o.id === id);
    return order ? this.enrichOrder(order) : undefined;
  }

  /**
   * Update order status
   */
  updateOrderStatus(orderId: number, status: OrderStatus): void {
    this.ordersSignal.update(orders =>
      orders.map(o => o.id === orderId ? { 
        ...o, 
        status,
        updatedAt: new Date().toISOString()
      } : o)
    );
  }

  /**
   * Create new order
   */
  createOrder(order: Partial<OrderDto>): OrderDto {
    const newOrder: OrderDto = {
      id: Math.floor(Math.random() * 10000),
      customerId: order.customerId || 1,
      status: 'CREATED',
      pickupAddress: order.pickupAddress || this.generateMockAddress('pickup'),
      deliveryAddress: order.deliveryAddress || this.generateMockAddress('delivery'),
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      distanceKm: 5 + Math.random() * 15,
      durationMinutes: 15 + Math.floor(Math.random() * 45),
      ...order
    };

    this.ordersSignal.update(orders => [newOrder, ...orders]);
    return newOrder;
  }

  private generateMockOrders(): OrderDto[] {
    const statuses: OrderStatus[] = [
      'CREATED', 'SEARCHING', 'ASSIGNED', 'DRIVER_ARRIVED',
      'PICKED_UP', 'IN_TRANSIT', 'DELIVERED', 'CANCELLED'
    ];

    return Array.from({ length: 15 }, (_, index) => {
      const status = statuses[Math.floor(Math.random() * statuses.length)];
      const createdAt = new Date(Date.now() - Math.random() * 24 * 60 * 60 * 1000);
      const distance = 3 + Math.random() * 20;
      const duration = 10 + Math.floor(Math.random() * 60);
      const basePrice = 5 + distance * 1.5 + duration * 0.5;

      return {
        id: 1000 + index,
        customerId: Math.floor(Math.random() * 100) + 1,
        driverId: ['ASSIGNED', 'DRIVER_ARRIVED', 'PICKED_UP', 'IN_TRANSIT', 'DELIVERED'].includes(status)
          ? Math.floor(Math.random() * 10) + 1
          : undefined,
        vehicleId: ['ASSIGNED', 'DRIVER_ARRIVED', 'PICKED_UP', 'IN_TRANSIT', 'DELIVERED'].includes(status)
          ? 100 + Math.floor(Math.random() * 10)
          : undefined,
        status,
        pickupAddress: this.generateMockAddress('pickup'),
        deliveryAddress: this.generateMockAddress('delivery'),
        estimatedPrice: Math.round(basePrice * 100) / 100,
        finalPrice: status === 'DELIVERED' ? Math.round(basePrice * 100) / 100 : undefined,
        distanceKm: Math.round(distance * 10) / 10,
        durationMinutes: duration,
        createdAt: createdAt.toISOString(),
        updatedAt: new Date().toISOString(),
        assignedAt: ['ASSIGNED', 'DRIVER_ARRIVED', 'PICKED_UP', 'IN_TRANSIT', 'DELIVERED'].includes(status)
          ? new Date(createdAt.getTime() + 5 * 60 * 1000).toISOString()
          : undefined,
        pickedUpAt: ['PICKED_UP', 'IN_TRANSIT', 'DELIVERED'].includes(status)
          ? new Date(createdAt.getTime() + 15 * 60 * 1000).toISOString()
          : undefined,
        deliveredAt: status === 'DELIVERED'
          ? new Date(createdAt.getTime() + duration * 60 * 1000).toISOString()
          : undefined
      };
    });
  }

  private generateMockAddress(type: 'pickup' | 'delivery'): OrderDto['pickupAddress'] {
    const streets = [
      'Av. Paulista', 'Rua Augusta', 'Av. Brigadeiro Faria Lima',
      'Rua Oscar Freire', 'Av. Rebouças', 'Rua Consolação'
    ];
    const neighborhoods = ['Jardins', 'Pinheiros', 'Vila Madalena', 'Moema', 'Itaim Bibi'];

    const baseLat = -23.5629 + (type === 'pickup' ? 0 : 0.02);
    const baseLon = -46.6544 + (type === 'pickup' ? 0 : 0.02);

    return {
      street: streets[Math.floor(Math.random() * streets.length)],
      number: String(Math.floor(Math.random() * 2000) + 1),
      neighborhood: neighborhoods[Math.floor(Math.random() * neighborhoods.length)],
      city: 'São Paulo',
      state: 'SP',
      zipCode: `0${Math.floor(Math.random() * 10000).toString().padStart(4, '0')}-${Math.floor(Math.random() * 1000).toString().padStart(3, '0')}`,
      latitude: baseLat + (Math.random() - 0.5) * 0.02,
      longitude: baseLon + (Math.random() - 0.5) * 0.02
    };
  }

  private enrichOrder(order: OrderDto): OrderWithDriver {
    return {
      ...order,
      statusLabel: this.getStatusLabel(order.status),
      statusColor: this.getStatusColor(order.status),
      formattedPrice: order.estimatedPrice 
        ? `R$ ${order.estimatedPrice.toFixed(2)}`
        : 'Calculando...',
      formattedDistance: order.distanceKm
        ? `${order.distanceKm.toFixed(1)} km`
        : '-',
      formattedDuration: order.durationMinutes
        ? `${order.durationMinutes} min`
        : '-'
    };
  }

  private getStatusLabel(status: OrderStatus): string {
    const labels: Record<OrderStatus, string> = {
      CREATED: 'Novo',
      SEARCHING: 'Buscando motorista',
      ASSIGNED: 'Motorista atribuído',
      DRIVER_ARRIVED: 'Motorista chegou',
      PICKED_UP: 'Coletado',
      IN_TRANSIT: 'Em trânsito',
      DELIVERED: 'Entregue',
      CANCELLED: 'Cancelado'
    };
    return labels[status];
  }

  private getStatusColor(status: OrderStatus): StatusColor {
    const colors: Record<OrderStatus, StatusColor> = {
      CREATED: 'neutral',
      SEARCHING: 'warning',
      ASSIGNED: 'primary',
      DRIVER_ARRIVED: 'primary',
      PICKED_UP: 'primary',
      IN_TRANSIT: 'primary',
      DELIVERED: 'success',
      CANCELLED: 'danger'
    };
    return colors[status];
  }
}
