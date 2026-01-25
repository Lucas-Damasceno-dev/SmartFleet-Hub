import { Component, inject, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MockOrderService } from '../../services';
import { OrderWithDriver } from '../../models';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="card h-full flex flex-col">
      <!-- Header -->
      <div class="flex items-center justify-between pb-4 border-b border-surface-700">
        <div>
          <h3 class="text-h4 text-slate-100">Pedidos Ativos</h3>
          <p class="text-caption text-slate-500">
            {{ orderService.ordersCount().active }} pedidos em andamento
          </p>
        </div>
        <button class="btn-primary" (click)="onCreateOrder()">
          <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          Novo Pedido
        </button>
      </div>

      <!-- Filter Tabs -->
      <div class="flex gap-2 py-3 border-b border-surface-700">
        @for (tab of tabs; track tab.id) {
          <button 
            class="px-3 py-1.5 rounded-button text-body-sm transition-colors"
            [class]="activeTab === tab.id ? 'bg-primary-600 text-white' : 'text-slate-400 hover:text-slate-200 hover:bg-surface-700'"
            (click)="activeTab = tab.id">
            {{ tab.label }}
          </button>
        }
      </div>

      <!-- Order List -->
      <div class="flex-1 overflow-y-auto scrollbar-dark py-2 space-y-2">
        @for (order of filteredOrders(); track order.id) {
          <div 
            class="card-interactive p-3"
            [class.ring-1]="selectedOrderId === order.id"
            [class.ring-primary-500]="selectedOrderId === order.id"
            (click)="onOrderSelect(order)">
            
            <div class="flex items-start justify-between">
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2">
                  <span class="text-body-sm font-medium text-slate-200">#{{ order.id }}</span>
                  <span [class]="'badge-' + order.statusColor">
                    {{ order.statusLabel }}
                  </span>
                </div>
                
                <!-- Addresses -->
                <div class="mt-2 space-y-1">
                  <div class="flex items-start gap-2">
                    <span class="text-success mt-0.5">●</span>
                    <span class="text-caption text-slate-400 truncate">
                      {{ order.pickupAddress.street }}, {{ order.pickupAddress.number }}
                    </span>
                  </div>
                  <div class="flex items-start gap-2">
                    <span class="text-danger mt-0.5">●</span>
                    <span class="text-caption text-slate-400 truncate">
                      {{ order.deliveryAddress.street }}, {{ order.deliveryAddress.number }}
                    </span>
                  </div>
                </div>
              </div>

              <!-- Right Info -->
              <div class="text-right ml-3">
                <p class="text-body-sm font-semibold text-slate-100">
                  {{ order.formattedPrice }}
                </p>
                <p class="text-caption text-slate-500">
                  {{ order.formattedDistance }} • {{ order.formattedDuration }}
                </p>
              </div>
            </div>

            <!-- Driver Info (if assigned) -->
            @if (order.driverId) {
              <div class="mt-3 pt-3 border-t border-surface-700 flex items-center gap-2">
                <div class="w-6 h-6 bg-primary-600 rounded-full flex items-center justify-center">
                  <span class="text-caption font-medium text-white">M</span>
                </div>
                <span class="text-caption text-slate-400">
                  Motorista #{{ order.driverId }}
                </span>
              </div>
            }
          </div>
        } @empty {
          <div class="flex flex-col items-center justify-center py-12 text-center">
            <svg class="w-12 h-12 text-slate-600 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                    d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
            </svg>
            <p class="text-slate-500">Nenhum pedido encontrado</p>
          </div>
        }
      </div>
    </div>
  `
})
export class OrderListComponent {
  @Input() selectedOrderId?: number;
  @Output() orderSelected = new EventEmitter<OrderWithDriver>();
  @Output() createOrder = new EventEmitter<void>();

  readonly orderService = inject(MockOrderService);

  activeTab = 'active';

  tabs = [
    { id: 'active', label: 'Ativos' },
    { id: 'completed', label: 'Concluídos' },
    { id: 'all', label: 'Todos' }
  ];

  filteredOrders(): OrderWithDriver[] {
    switch (this.activeTab) {
      case 'active':
        return this.orderService.activeOrders();
      case 'completed':
        return this.orderService.completedOrders();
      default:
        return [...this.orderService.activeOrders(), ...this.orderService.completedOrders()];
    }
  }

  onOrderSelect(order: OrderWithDriver): void {
    this.orderSelected.emit(order);
  }

  onCreateOrder(): void {
    this.createOrder.emit();
  }
}
