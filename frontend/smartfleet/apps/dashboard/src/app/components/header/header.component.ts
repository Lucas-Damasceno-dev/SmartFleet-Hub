import { Component, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule],
  template: `
    <header class="h-16 bg-surface-900 border-b border-surface-700 flex items-center justify-between px-6 z-header">
      <!-- Left Section -->
      <div class="flex items-center gap-4">
        <button 
          class="btn-ghost btn-icon lg:hidden"
          (click)="toggleSidebar.emit()">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"/>
          </svg>
        </button>

        <!-- Search -->
        <div class="relative hidden sm:block">
          <svg class="w-5 h-5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" 
               fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                  d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
          </svg>
          <input 
            type="text" 
            placeholder="Buscar pedidos, motoristas..."
            class="input pl-10 w-64 lg:w-80 py-2 text-body-sm">
        </div>
      </div>

      <!-- Right Section -->
      <div class="flex items-center gap-4">
        <!-- Create Order Button -->
        <button 
          (click)="createOrder.emit()"
          class="flex items-center gap-2 px-4 py-2 bg-royal-blue hover:bg-royal-blue/90 text-white rounded-lg font-medium transition-colors">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          <span class="hidden sm:inline">Novo Pedido</span>
        </button>

        <!-- Notifications -->
        <button class="btn-ghost btn-icon relative">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                  d="M15 17h5l-5-5-5 5h5zm0 0v3a2 2 0 01-4 0v-3m-6-2a9 9 0 0118 0v2a2 2 0 01-2 2H5a2 2 0 01-2-2v-2z"/>
          </svg>
          <span class="absolute top-1 right-1 w-2 h-2 bg-danger rounded-full"></span>
        </button>

        <!-- Live Status -->
        <div class="hidden md:flex items-center gap-2 px-3 py-1.5 bg-success/10 rounded-full">
          <span class="w-2 h-2 bg-success rounded-full animate-pulse"></span>
          <span class="text-caption text-success font-medium">Sistema Online</span>
        </div>

        <!-- Current Time -->
        <div class="hidden lg:block text-right">
          <p class="text-body-sm text-slate-200 font-medium">{{ currentTime }}</p>
          <p class="text-caption text-slate-500">{{ currentDate }}</p>
        </div>
      </div>
    </header>
  `
})
export class HeaderComponent {
  @Output() toggleSidebar = new EventEmitter<void>();
  @Output() createOrder = new EventEmitter<void>();

  currentTime = '';
  currentDate = '';

  constructor() {
    this.updateTime();
    setInterval(() => this.updateTime(), 1000);
  }

  private updateTime(): void {
    const now = new Date();
    this.currentTime = now.toLocaleTimeString('pt-BR', { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
    this.currentDate = now.toLocaleDateString('pt-BR', { 
      weekday: 'short', 
      day: '2-digit', 
      month: 'short' 
    });
  }
}
