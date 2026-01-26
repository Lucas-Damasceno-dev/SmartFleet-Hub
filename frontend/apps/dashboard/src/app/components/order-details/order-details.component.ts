import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrderWithDriver } from '../../models';

@Component({
  selector: 'app-order-details',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (order) {
      <div class="card h-full flex flex-col">
        <!-- Header -->
        <div class="flex items-center justify-between pb-4 border-b border-surface-700">
          <div>
            <div class="flex items-center gap-3">
              <button class="btn-ghost btn-icon p-1" (click)="onClose()">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                </svg>
              </button>
              <h3 class="text-h4 text-slate-100">Pedido #{{ order.id }}</h3>
            </div>
          </div>
          <span [class]="'badge-' + order.statusColor + ' text-body-sm'">
            {{ order.statusLabel }}
          </span>
        </div>

        <!-- Content -->
        <div class="flex-1 overflow-y-auto scrollbar-dark py-4 space-y-6">
          
          <!-- Route Section -->
          <section>
            <h4 class="text-body-sm font-semibold text-slate-400 uppercase tracking-wider mb-3">Rota</h4>
            
            <div class="relative pl-6">
              <!-- Pickup -->
              <div class="relative pb-6">
                <div class="absolute left-0 top-1 w-3 h-3 rounded-full bg-success"></div>
                <div class="absolute left-1.5 top-4 w-0.5 h-full bg-surface-600"></div>
                <div>
                  <p class="text-caption text-slate-500">Coleta</p>
                  <p class="text-body-sm text-slate-200">
                    {{ order.pickupAddress.street }}, {{ order.pickupAddress.number }}
                  </p>
                  <p class="text-caption text-slate-500">
                    {{ order.pickupAddress.neighborhood }} - {{ order.pickupAddress.city }}
                  </p>
                </div>
              </div>

              <!-- Delivery -->
              <div class="relative">
                <div class="absolute left-0 top-1 w-3 h-3 rounded-full bg-danger"></div>
                <div>
                  <p class="text-caption text-slate-500">Entrega</p>
                  <p class="text-body-sm text-slate-200">
                    {{ order.deliveryAddress.street }}, {{ order.deliveryAddress.number }}
                  </p>
                  <p class="text-caption text-slate-500">
                    {{ order.deliveryAddress.neighborhood }} - {{ order.deliveryAddress.city }}
                  </p>
                </div>
              </div>
            </div>
          </section>

          <!-- Trip Info -->
          <section>
            <h4 class="text-body-sm font-semibold text-slate-400 uppercase tracking-wider mb-3">Informações</h4>
            
            <div class="grid grid-cols-3 gap-4">
              <div class="text-center p-3 bg-surface-800 rounded-lg">
                <p class="text-h4 text-primary-400">{{ order.formattedDistance }}</p>
                <p class="text-caption text-slate-500">Distância</p>
              </div>
              <div class="text-center p-3 bg-surface-800 rounded-lg">
                <p class="text-h4 text-primary-400">{{ order.formattedDuration }}</p>
                <p class="text-caption text-slate-500">Duração</p>
              </div>
              <div class="text-center p-3 bg-surface-800 rounded-lg">
                <p class="text-h4 text-success">{{ order.formattedPrice }}</p>
                <p class="text-caption text-slate-500">Valor</p>
              </div>
            </div>
          </section>

          <!-- Driver Section -->
          @if (order.driverId) {
            <section>
              <h4 class="text-body-sm font-semibold text-slate-400 uppercase tracking-wider mb-3">Motorista</h4>
              
              <div class="flex items-center gap-4 p-3 bg-surface-800 rounded-lg">
                <div class="w-12 h-12 bg-primary-600 rounded-full flex items-center justify-center">
                  <span class="text-body font-semibold text-white">M{{ order.driverId }}</span>
                </div>
                <div class="flex-1">
                  <p class="text-body-sm font-medium text-slate-200">Motorista #{{ order.driverId }}</p>
                  <p class="text-caption text-slate-500">Veículo #{{ order.vehicleId }}</p>
                </div>
                <button class="btn-secondary text-body-sm py-1.5 px-3">
                  Contatar
                </button>
              </div>
            </section>
          }

          <!-- Timeline -->
          <section>
            <h4 class="text-body-sm font-semibold text-slate-400 uppercase tracking-wider mb-3">Histórico</h4>
            
            <div class="space-y-3">
              <div class="flex items-start gap-3">
                <div class="w-2 h-2 mt-1.5 rounded-full bg-slate-500"></div>
                <div>
                  <p class="text-body-sm text-slate-300">Pedido criado</p>
                  <p class="text-caption text-slate-500">{{ formatDate(order.createdAt) }}</p>
                </div>
              </div>

              @if (order.assignedAt) {
                <div class="flex items-start gap-3">
                  <div class="w-2 h-2 mt-1.5 rounded-full bg-primary-500"></div>
                  <div>
                    <p class="text-body-sm text-slate-300">Motorista atribuído</p>
                    <p class="text-caption text-slate-500">{{ formatDate(order.assignedAt) }}</p>
                  </div>
                </div>
              }

              @if (order.pickedUpAt) {
                <div class="flex items-start gap-3">
                  <div class="w-2 h-2 mt-1.5 rounded-full bg-primary-500"></div>
                  <div>
                    <p class="text-body-sm text-slate-300">Pedido coletado</p>
                    <p class="text-caption text-slate-500">{{ formatDate(order.pickedUpAt) }}</p>
                  </div>
                </div>
              }

              @if (order.deliveredAt) {
                <div class="flex items-start gap-3">
                  <div class="w-2 h-2 mt-1.5 rounded-full bg-success"></div>
                  <div>
                    <p class="text-body-sm text-slate-300">Pedido entregue</p>
                    <p class="text-caption text-slate-500">{{ formatDate(order.deliveredAt) }}</p>
                  </div>
                </div>
              }
            </div>
          </section>
        </div>

        <!-- Actions -->
        <div class="pt-4 border-t border-surface-700 flex gap-3">
          @if (!['DELIVERED', 'CANCELLED'].includes(order.status)) {
            <button class="btn-danger flex-1" (click)="onCancel()">
              Cancelar
            </button>
            <button class="btn-primary flex-1" (click)="onAction()">
              {{ getActionLabel(order.status) }}
            </button>
          }
        </div>
      </div>
    } @else {
      <div class="card h-full flex items-center justify-center">
        <div class="text-center">
          <svg class="w-16 h-16 text-slate-700 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                  d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
          </svg>
          <p class="text-slate-500">Selecione um pedido para ver os detalhes</p>
        </div>
      </div>
    }
  `
})
export class OrderDetailsComponent {
  @Input() order?: OrderWithDriver;
  @Output() close = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<number>();
  @Output() action = new EventEmitter<{ orderId: number; action: string }>();

  onClose(): void {
    this.close.emit();
  }

  onCancel(): void {
    if (this.order) {
      this.cancel.emit(this.order.id);
    }
  }

  onAction(): void {
    if (this.order) {
      this.action.emit({ 
        orderId: this.order.id, 
        action: this.getNextAction(this.order.status) 
      });
    }
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getActionLabel(status: string): string {
    const labels: Record<string, string> = {
      CREATED: 'Buscar Motorista',
      SEARCHING: 'Aguardando...',
      ASSIGNED: 'Motorista a caminho',
      DRIVER_ARRIVED: 'Confirmar Coleta',
      PICKED_UP: 'Em Trânsito',
      IN_TRANSIT: 'Confirmar Entrega'
    };
    return labels[status] || 'Ação';
  }

  getNextAction(status: string): string {
    const actions: Record<string, string> = {
      CREATED: 'search',
      SEARCHING: 'waiting',
      ASSIGNED: 'track',
      DRIVER_ARRIVED: 'pickup',
      PICKED_UP: 'transit',
      IN_TRANSIT: 'deliver'
    };
    return actions[status] || 'unknown';
  }
}
