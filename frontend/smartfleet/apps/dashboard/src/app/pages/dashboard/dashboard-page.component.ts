import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { 
  HeaderComponent, 
  SidebarComponent, 
  FleetMapComponent, 
  OrderListComponent, 
  OrderDetailsComponent,
  CreateOrderModalComponent 
} from '../../components';
import { MockDriverService, MockOrderService } from '../../services';
import { OrderWithDriver, SidebarItem } from '../../models';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [
    CommonModule,
    HeaderComponent,
    SidebarComponent,
    FleetMapComponent,
    OrderListComponent,
    OrderDetailsComponent,
    CreateOrderModalComponent
  ],
  template: `
    <div class="h-screen flex flex-col bg-slate-900">
      <!-- Header -->
      <app-header 
        (toggleSidebar)="toggleSidebar()"
        (createOrder)="showCreateOrderModal.set(true)">
      </app-header>

      <div class="flex-1 flex overflow-hidden">
        <!-- Sidebar -->
        <app-sidebar 
          [activeOrdersCount]="orderService.ordersCount().active"
          (itemSelected)="onItemSelected($event)">
        </app-sidebar>

        <!-- Main Content -->
        <main class="flex-1 flex overflow-hidden">
          <!-- Map View -->
          <div class="flex-1 relative">
            <app-fleet-map></app-fleet-map>
          </div>

          <!-- Order Panel -->
          <aside class="w-96 bg-slate-800 border-l border-slate-700 flex flex-col overflow-hidden">
            @if (!selectedOrder()) {
              <app-order-list 
                [selectedOrderId]="selectedOrder()?.id"
                (orderSelected)="selectOrder($event)"
                (createOrder)="showCreateOrderModal.set(true)">
              </app-order-list>
            } @else {
              <app-order-details
                [order]="selectedOrder()!"
                (close)="selectedOrder.set(null)"
                (cancel)="cancelOrder($event)">
              </app-order-details>
            }
          </aside>
        </main>
      </div>

      <!-- Create Order Modal -->
      @if (showCreateOrderModal()) {
        <app-create-order-modal
          (close)="showCreateOrderModal.set(false)"
          (orderCreated)="onOrderCreated()">
        </app-create-order-modal>
      }
    </div>
  `,
  styles: [`
    :host {
      display: contents;
    }
  `]
})
export class DashboardPageComponent implements OnInit, OnDestroy {
  private driverService = inject(MockDriverService);
  readonly orderService = inject(MockOrderService);

  // UI State
  sidebarCollapsed = signal(false);
  showCreateOrderModal = signal(false);
  activeMenu = signal('dashboard');
  selectedOrder = signal<OrderWithDriver | null>(null);

  ngOnInit(): void {
    // Services auto-start simulation in constructor
  }

  ngOnDestroy(): void {
    this.driverService.stopSimulation();
  }

  toggleSidebar(): void {
    this.sidebarCollapsed.update(v => !v);
  }

  onItemSelected(item: SidebarItem): void {
    this.activeMenu.set(item.id);
  }

  selectOrder(order: OrderWithDriver): void {
    this.selectedOrder.set(order);
  }

  cancelOrder(orderId: number): void {
    this.orderService.updateOrderStatus(orderId, 'CANCELLED');
    this.selectedOrder.set(null);
  }

  onOrderCreated(): void {
    this.showCreateOrderModal.set(false);
  }
}
