import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { 
  HeaderComponent, 
  SidebarComponent, 
  MapComponent, 
  OrderListComponent, 
  OrderDetailsComponent,
  CreateOrderModalComponent 
} from '../../components';
import { RealtimeService, LocationUpdate } from '../../core/realtime';
import { ApiService, Order, DriverLocation } from '../../core/api';
import { AuthService } from '../../core/auth';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [
    CommonModule,
    HeaderComponent,
    SidebarComponent,
    MapComponent,
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
          [collapsed]="sidebarCollapsed()"
          (menuSelect)="onMenuSelect($event)">
        </app-sidebar>

        <!-- Main Content -->
        <main class="flex-1 flex overflow-hidden">
          <!-- Map View -->
          <div class="flex-1 relative">
            <app-map 
              [drivers]="driverLocations()"
              [selectedOrder]="selectedOrder()"
              (driverClick)="onDriverClick($event)"
              (mapClick)="onMapClick($event)">
            </app-map>

            <!-- Connection Status -->
            @if (connectionState().reconnecting) {
              <div class="absolute top-4 left-1/2 -translate-x-1/2 bg-yellow-500/90 text-black px-4 py-2 rounded-lg shadow-lg">
                {{ connectionState().error }}
              </div>
            }
            @if (!connectionState().connected && !connectionState().reconnecting) {
              <div class="absolute top-4 left-1/2 -translate-x-1/2 bg-red-500/90 text-white px-4 py-2 rounded-lg shadow-lg">
                Disconnected from real-time feed
              </div>
            }
          </div>

          <!-- Order Panel -->
          <aside class="w-96 bg-slate-800 border-l border-slate-700 flex flex-col overflow-hidden">
            @if (!selectedOrder()) {
              <app-order-list 
                [orders]="orders()"
                [loading]="ordersLoading()"
                (orderSelect)="selectOrder($event)"
                (refresh)="loadOrders()">
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
          (orderCreated)="onOrderCreated($event)">
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
  private realtimeService = inject(RealtimeService);
  private apiService = inject(ApiService);
  private authService = inject(AuthService);
  
  private subscriptions: Subscription[] = [];

  // UI State
  sidebarCollapsed = signal(false);
  showCreateOrderModal = signal(false);
  activeMenu = signal('dashboard');

  // Data State
  orders = signal<Order[]>([]);
  ordersLoading = signal(false);
  selectedOrder = signal<Order | null>(null);
  driverLocations = signal<DriverLocation[]>([]);
  connectionState = signal({ connected: false, reconnecting: false, error: null as string | null });

  async ngOnInit(): Promise<void> {
    // Initialize auth
    await this.authService.init();
    
    // Load initial data
    this.loadOrders();
    this.loadDriverLocations();

    // Connect to real-time feed
    await this.realtimeService.connect();

    // Subscribe to real-time updates
    this.subscriptions.push(
      this.realtimeService.locationUpdates$.subscribe(update => {
        this.handleLocationUpdate(update);
      }),
      this.realtimeService.connectionState$.subscribe(state => {
        this.connectionState.set(state);
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.realtimeService.disconnect();
  }

  toggleSidebar(): void {
    this.sidebarCollapsed.update(v => !v);
  }

  onMenuSelect(menu: string): void {
    this.activeMenu.set(menu);
  }

  loadOrders(): void {
    this.ordersLoading.set(true);
    this.apiService.getOrders().subscribe({
      next: (response) => {
        this.orders.set(response.content);
        this.ordersLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load orders:', err);
        this.ordersLoading.set(false);
      }
    });
  }

  loadDriverLocations(): void {
    this.apiService.getActiveDrivers().subscribe({
      next: (drivers) => this.driverLocations.set(drivers),
      error: (err) => console.error('Failed to load driver locations:', err)
    });
  }

  selectOrder(order: Order): void {
    this.selectedOrder.set(order);
  }

  cancelOrder(orderId: string): void {
    this.apiService.cancelOrder(orderId).subscribe({
      next: () => {
        this.selectedOrder.set(null);
        this.loadOrders();
      },
      error: (err) => console.error('Failed to cancel order:', err)
    });
  }

  onOrderCreated(order: Order): void {
    this.orders.update(orders => [order, ...orders]);
    this.showCreateOrderModal.set(false);
  }

  onDriverClick(driverId: string): void {
    console.log('Driver clicked:', driverId);
  }

  onMapClick(coords: { lat: number; lng: number }): void {
    console.log('Map clicked:', coords);
  }

  private handleLocationUpdate(update: LocationUpdate): void {
    this.driverLocations.update(drivers => {
      const index = drivers.findIndex(d => d.driverId === update.driverId);
      if (index >= 0) {
        const updated = [...drivers];
        updated[index] = {
          ...updated[index],
          latitude: update.latitude,
          longitude: update.longitude,
          heading: update.heading,
          speedKmh: update.speedKmh,
          lastUpdate: update.timestamp
        };
        return updated;
      }
      return drivers;
    });
  }
}
