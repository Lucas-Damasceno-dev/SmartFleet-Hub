import { Component, signal, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { 
  IonContent, 
  IonButton,
  IonIcon,
  IonProgressBar
} from '@ionic/angular/standalone';

interface OrderOffer {
  id: number;
  pickupAddress: string;
  deliveryAddress: string;
  distance: number;
  duration: number;
  price: number;
  pickupDistance: number;
}

@Component({
  selector: 'app-order-offer',
  standalone: true,
  imports: [
    CommonModule,
    IonContent,
    IonButton,
    IonIcon,
    IonProgressBar
  ],
  template: `
    <ion-content class="order-offer-page">
      <div class="min-h-full flex flex-col bg-surface-900">
        <!-- Timer Progress -->
        <div class="w-full bg-surface-800">
          <div 
            class="h-1 bg-primary-500 transition-all duration-1000"
            [style.width.%]="timeRemaining() / 30 * 100">
          </div>
        </div>

        <!-- Order Details -->
        <div class="flex-1 flex flex-col p-4">
          <!-- Header -->
          <div class="text-center py-4">
            <div class="w-16 h-16 bg-primary-600 rounded-full mx-auto flex items-center justify-center animate-bounce-subtle">
              <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                      d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
              </svg>
            </div>
            <h2 class="text-h2 text-white mt-4">Novo Pedido!</h2>
            <p class="text-body text-slate-400 mt-1">
              {{ timeRemaining() }}s para aceitar
            </p>
          </div>

          <!-- Price Card -->
          <div class="card text-center my-4">
            <p class="text-display text-success">
              R$ {{ order().price.toFixed(2) }}
            </p>
            <p class="text-caption text-slate-500">Valor estimado</p>
          </div>

          <!-- Route Info -->
          <div class="card flex-1">
            <!-- Pickup -->
            <div class="flex items-start gap-4 pb-4 border-b border-surface-700">
              <div class="flex-shrink-0">
                <div class="w-10 h-10 bg-success/20 rounded-full flex items-center justify-center">
                  <span class="text-success text-lg">●</span>
                </div>
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-center justify-between">
                  <p class="text-caption text-slate-500">COLETA</p>
                  <span class="badge-primary">{{ order().pickupDistance.toFixed(1) }} km</span>
                </div>
                <p class="text-body text-slate-200 mt-1 truncate">
                  {{ order().pickupAddress }}
                </p>
              </div>
            </div>

            <!-- Route line -->
            <div class="pl-5 py-2">
              <div class="w-0.5 h-8 bg-surface-600 ml-4"></div>
            </div>

            <!-- Delivery -->
            <div class="flex items-start gap-4 pt-4 border-t border-surface-700">
              <div class="flex-shrink-0">
                <div class="w-10 h-10 bg-danger/20 rounded-full flex items-center justify-center">
                  <span class="text-danger text-lg">●</span>
                </div>
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-caption text-slate-500">ENTREGA</p>
                <p class="text-body text-slate-200 mt-1 truncate">
                  {{ order().deliveryAddress }}
                </p>
              </div>
            </div>

            <!-- Trip Stats -->
            <div class="grid grid-cols-2 gap-4 mt-6 pt-4 border-t border-surface-700">
              <div class="text-center">
                <p class="text-h4 text-primary-400">{{ order().distance.toFixed(1) }} km</p>
                <p class="text-caption text-slate-500">Distância total</p>
              </div>
              <div class="text-center">
                <p class="text-h4 text-primary-400">{{ order().duration }} min</p>
                <p class="text-caption text-slate-500">Tempo estimado</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="p-4 pb-8 safe-area-inset-bottom">
          <div class="grid grid-cols-2 gap-4">
            <button 
              class="btn-danger py-4 text-body font-semibold"
              (click)="rejectOrder()">
              Recusar
            </button>
            <button 
              class="btn-success py-4 text-body font-semibold"
              (click)="acceptOrder()">
              Aceitar
            </button>
          </div>
        </div>
      </div>
    </ion-content>
  `,
  styles: [`
    .order-offer-page {
      --background: #0F172A;
    }

    .safe-area-inset-bottom {
      padding-bottom: calc(env(safe-area-inset-bottom, 16px) + 16px);
    }
  `]
})
export class OrderOfferPage implements OnInit, OnDestroy {
  private router = inject(Router);
  private timerId?: ReturnType<typeof setInterval>;

  timeRemaining = signal(30);

  order = signal<OrderOffer>({
    id: 1234,
    pickupAddress: 'Av. Paulista, 1578 - Bela Vista',
    deliveryAddress: 'Rua Oscar Freire, 2100 - Jardins',
    distance: 4.5,
    duration: 18,
    price: 28.50,
    pickupDistance: 1.2
  });

  ngOnInit(): void {
    this.startTimer();
  }

  ngOnDestroy(): void {
    this.stopTimer();
  }

  acceptOrder(): void {
    this.stopTimer();
    // Would send acceptance to backend
    this.router.navigate(['/on-trip'], { 
      queryParams: { orderId: this.order().id } 
    });
  }

  rejectOrder(): void {
    this.stopTimer();
    // Would send rejection to backend
    this.router.navigate(['/waiting']);
  }

  private startTimer(): void {
    this.timerId = setInterval(() => {
      this.timeRemaining.update(t => {
        if (t <= 1) {
          this.rejectOrder(); // Auto-reject on timeout
          return 0;
        }
        return t - 1;
      });
    }, 1000);
  }

  private stopTimer(): void {
    if (this.timerId) {
      clearInterval(this.timerId);
      this.timerId = undefined;
    }
  }
}
