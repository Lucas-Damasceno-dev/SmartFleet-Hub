import { Component, inject, signal, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ApiService, CreateOrderRequest, Order, PriceResult } from '../../core/api';

@Component({
  selector: 'app-create-order-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  template: `
    <div class="fixed inset-0 bg-black/70 flex items-center justify-center z-50" (click)="onBackdropClick($event)">
      <div class="bg-slate-800 rounded-lg shadow-xl w-full max-w-lg mx-4 overflow-hidden">
        <!-- Header -->
        <div class="bg-slate-900 px-6 py-4 flex items-center justify-between">
          <h2 class="text-xl font-semibold text-white">Create New Order</h2>
          <button 
            (click)="close.emit()" 
            class="text-slate-400 hover:text-white transition-colors">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <!-- Form -->
        <form [formGroup]="orderForm" (ngSubmit)="onSubmit()" class="p-6 space-y-4">
          <!-- Pickup Address -->
          <div>
            <label class="block text-sm font-medium text-slate-300 mb-1">Pickup Address</label>
            <input 
              type="text" 
              formControlName="pickupAddress"
              class="w-full px-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white placeholder-slate-400 focus:ring-2 focus:ring-royal-blue focus:border-transparent"
              placeholder="Enter pickup address...">
            @if (orderForm.get('pickupAddress')?.invalid && orderForm.get('pickupAddress')?.touched) {
              <p class="mt-1 text-sm text-red-400">Pickup address is required</p>
            }
          </div>

          <!-- Pickup Coordinates (simplified for demo) -->
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-slate-300 mb-1">Pickup Lat</label>
              <input 
                type="number" 
                formControlName="pickupLat"
                step="0.000001"
                class="w-full px-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white">
            </div>
            <div>
              <label class="block text-sm font-medium text-slate-300 mb-1">Pickup Lng</label>
              <input 
                type="number" 
                formControlName="pickupLng"
                step="0.000001"
                class="w-full px-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white">
            </div>
          </div>

          <!-- Delivery Address -->
          <div>
            <label class="block text-sm font-medium text-slate-300 mb-1">Delivery Address</label>
            <input 
              type="text" 
              formControlName="deliveryAddress"
              class="w-full px-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white placeholder-slate-400 focus:ring-2 focus:ring-royal-blue focus:border-transparent"
              placeholder="Enter delivery address...">
            @if (orderForm.get('deliveryAddress')?.invalid && orderForm.get('deliveryAddress')?.touched) {
              <p class="mt-1 text-sm text-red-400">Delivery address is required</p>
            }
          </div>

          <!-- Delivery Coordinates -->
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-slate-300 mb-1">Delivery Lat</label>
              <input 
                type="number" 
                formControlName="deliveryLat"
                step="0.000001"
                class="w-full px-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white">
            </div>
            <div>
              <label class="block text-sm font-medium text-slate-300 mb-1">Delivery Lng</label>
              <input 
                type="number" 
                formControlName="deliveryLng"
                step="0.000001"
                class="w-full px-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white">
            </div>
          </div>

          <!-- Notes -->
          <div>
            <label class="block text-sm font-medium text-slate-300 mb-1">Notes (optional)</label>
            <textarea 
              formControlName="notes"
              rows="2"
              class="w-full px-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white placeholder-slate-400 resize-none"
              placeholder="Any special instructions..."></textarea>
          </div>

          <!-- Price Estimate -->
          @if (priceEstimate()) {
            <div class="bg-slate-700/50 rounded-lg p-4">
              <p class="text-sm text-slate-400">Estimated Price</p>
              <p class="text-2xl font-bold text-green-400">
                {{ priceEstimate()!.currency }} {{ priceEstimate()!.totalPrice | number:'1.2-2' }}
              </p>
              <div class="text-xs text-slate-500 mt-1">
                Base: {{ priceEstimate()!.basePrice | number:'1.2-2' }} |
                Distance: {{ priceEstimate()!.distanceCharge | number:'1.2-2' }} |
                Time: {{ priceEstimate()!.timeCharge | number:'1.2-2' }}
              </div>
            </div>
          }

          <!-- Actions -->
          <div class="flex justify-end gap-3 pt-4">
            <button 
              type="button"
              (click)="calculatePrice()"
              [disabled]="loading()"
              class="px-4 py-2 bg-slate-600 hover:bg-slate-500 text-white rounded-lg transition-colors disabled:opacity-50">
              Calculate Price
            </button>
            <button 
              type="button"
              (click)="close.emit()"
              class="px-4 py-2 bg-slate-600 hover:bg-slate-500 text-white rounded-lg transition-colors">
              Cancel
            </button>
            <button 
              type="submit"
              [disabled]="orderForm.invalid || loading()"
              class="px-6 py-2 bg-royal-blue hover:bg-royal-blue/90 text-white rounded-lg font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
              @if (loading()) {
                <span class="flex items-center gap-2">
                  <svg class="animate-spin h-4 w-4" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"/>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
                  </svg>
                  Creating...
                </span>
              } @else {
                Create Order
              }
            </button>
          </div>
        </form>

        <!-- Error Message -->
        @if (error()) {
          <div class="mx-6 mb-6 p-4 bg-red-900/50 border border-red-500 rounded-lg text-red-300">
            {{ error() }}
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    :host {
      display: contents;
    }
  `]
})
export class CreateOrderModalComponent {
  private fb = inject(FormBuilder);
  private apiService = inject(ApiService);

  close = output<void>();
  orderCreated = output<Order>();

  loading = signal(false);
  error = signal<string | null>(null);
  priceEstimate = signal<PriceResult | null>(null);

  orderForm: FormGroup = this.fb.group({
    pickupAddress: ['', Validators.required],
    pickupLat: [-23.550520, Validators.required],
    pickupLng: [-46.633308, Validators.required],
    deliveryAddress: ['', Validators.required],
    deliveryLat: [-23.561414, Validators.required],
    deliveryLng: [-46.655882, Validators.required],
    notes: ['']
  });

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.close.emit();
    }
  }

  calculatePrice(): void {
    const pickupLat = this.orderForm.get('pickupLat')?.value;
    const pickupLng = this.orderForm.get('pickupLng')?.value;
    const deliveryLat = this.orderForm.get('deliveryLat')?.value;
    const deliveryLng = this.orderForm.get('deliveryLng')?.value;

    // Simple distance calculation (Haversine approximation)
    const R = 6371; // Earth's radius in km
    const dLat = (deliveryLat - pickupLat) * Math.PI / 180;
    const dLng = (deliveryLng - pickupLng) * Math.PI / 180;
    const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
              Math.cos(pickupLat * Math.PI / 180) * Math.cos(deliveryLat * Math.PI / 180) *
              Math.sin(dLng/2) * Math.sin(dLng/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    const distanceKm = R * c;
    const durationMinutes = distanceKm * 3; // Rough estimate: 20km/h average

    this.apiService.calculatePrice({ distanceKm, durationMinutes }).subscribe({
      next: (result) => this.priceEstimate.set(result),
      error: (err) => {
        console.error('Failed to calculate price:', err);
        // Mock response for demo
        this.priceEstimate.set({
          basePrice: 5.00,
          distanceCharge: distanceKm * 1.50,
          timeCharge: durationMinutes * 0.50,
          totalPrice: 5.00 + (distanceKm * 1.50) + (durationMinutes * 0.50),
          currency: 'BRL'
        });
      }
    });
  }

  onSubmit(): void {
    if (this.orderForm.invalid) return;

    this.loading.set(true);
    this.error.set(null);

    const formValue = this.orderForm.value;
    const request: CreateOrderRequest = {
      pickupAddress: formValue.pickupAddress,
      deliveryAddress: formValue.deliveryAddress,
      pickupCoordinates: {
        latitude: formValue.pickupLat,
        longitude: formValue.pickupLng
      },
      deliveryCoordinates: {
        latitude: formValue.deliveryLat,
        longitude: formValue.deliveryLng
      },
      notes: formValue.notes || undefined
    };

    this.apiService.createOrder(request).subscribe({
      next: (order) => {
        this.loading.set(false);
        this.orderCreated.emit(order);
        this.close.emit();
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Failed to create order. Please try again.');
        console.error('Failed to create order:', err);
      }
    });
  }
}
