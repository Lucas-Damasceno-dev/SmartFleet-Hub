import { Component, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { 
  SidebarComponent, 
  HeaderComponent, 
  FleetMapComponent,
  OrderListComponent,
  OrderDetailsComponent 
} from './components';
import { OrderWithDriver } from './models';

@Component({
  standalone: true,
  imports: [
    CommonModule, 
    RouterModule,
    SidebarComponent,
    HeaderComponent,
    FleetMapComponent,
    OrderListComponent,
    OrderDetailsComponent
  ],
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected title = 'SmartFleet Dashboard';
  
  sidebarOpen = signal(true);
  selectedOrder = signal<OrderWithDriver | undefined>(undefined);

  toggleSidebar(): void {
    this.sidebarOpen.update(v => !v);
  }

  onOrderSelected(order: OrderWithDriver): void {
    this.selectedOrder.set(order);
  }

  onCloseOrderDetails(): void {
    this.selectedOrder.set(undefined);
  }
}
