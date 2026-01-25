import { Component, signal, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { 
  IonContent,
  IonHeader,
  IonToolbar,
  IonTitle, 
  IonButton,
  IonIcon,
  IonFab,
  IonFabButton,
  IonActionSheet
} from '@ionic/angular/standalone';

type TripPhase = 'GOING_TO_PICKUP' | 'AT_PICKUP' | 'IN_TRANSIT' | 'AT_DELIVERY' | 'COMPLETED';

interface TripState {
  orderId: number;
  phase: TripPhase;
  pickupAddress: string;
  deliveryAddress: string;
  customerName: string;
  customerPhone: string;
  distanceRemaining: number;
  etaMinutes: number;
  price: number;
}

@Component({
  selector: 'app-on-trip',
  standalone: true,
  imports: [
    CommonModule,
    IonContent,
    IonHeader,
    IonToolbar,
    IonTitle,
    IonButton,
    IonIcon,
    IonFab,
    IonFabButton,
    IonActionSheet
  ],
  template: `
    <ion-header class="ion-no-border">
      <ion-toolbar color="dark">
        <ion-title>
          <span class="text-body-sm text-slate-400">Pedido #{{ state().orderId }}</span>
        </ion-title>
        <ion-button slot="end" fill="clear" (click)="openOptions()">
          <ion-icon name="ellipsis-vertical"></ion-icon>
        </ion-button>
      </ion-toolbar>
    </ion-header>

    <ion-content class="on-trip-page">
      <div class="h-full flex flex-col bg-surface-900">
        <!-- Map Area (simulated) -->
        <div class="flex-1 relative bg-surface-800">
          <!-- Simulated Navigation View -->
          <div class="absolute inset-0 flex items-center justify-center">
            <div class="text-center">
              <div class="w-24 h-24 bg-primary-600/20 rounded-full flex items-center justify-center mx-auto">
                <svg class="w-12 h-12 text-primary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                        d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"/>
                </svg>
              </div>
              <p class="text-h2 text-white mt-4">{{ state().etaMinutes }} min</p>
              <p class="text-body text-slate-400">{{ state().distanceRemaining.toFixed(1) }} km restantes</p>
            </div>
          </div>

          <!-- Navigation Controls -->
          <ion-fab vertical="bottom" horizontal="end" slot="fixed">
            <ion-fab-button (click)="openNavigation()">
              <ion-icon name="navigate"></ion-icon>
            </ion-fab-button>
          </ion-fab>
        </div>

        <!-- Bottom Panel -->
        <div class="bg-surface-900 safe-area-inset-bottom">
          <!-- Phase Indicator -->
          <div class="flex items-center justify-center gap-2 py-3 border-b border-surface-700">
            @for (phase of phases; track phase.id; let i = $index) {
              <div 
                class="w-2 h-2 rounded-full transition-colors"
                [class]="isPhaseActive(phase.id) ? 'bg-primary-500' : 'bg-surface-600'">
              </div>
            }
          </div>

          <!-- Current Phase Info -->
          <div class="p-4">
            <div class="flex items-center justify-between mb-4">
              <div>
                <p class="text-caption text-slate-500 uppercase">{{ getPhaseLabel() }}</p>
                <p class="text-body-lg text-white font-medium">
                  {{ getDestinationAddress() }}
                </p>
              </div>

              @if (state().phase === 'GOING_TO_PICKUP' || state().phase === 'IN_TRANSIT') {
                <button class="btn-secondary py-2 px-4" (click)="callCustomer()">
                  <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                          d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"/>
                  </svg>
                  Ligar
                </button>
              }
            </div>

            <!-- Customer Info (at pickup/delivery) -->
            @if (state().phase === 'AT_PICKUP' || state().phase === 'AT_DELIVERY') {
              <div class="card mb-4">
                <div class="flex items-center gap-4">
                  <div class="w-12 h-12 bg-surface-700 rounded-full flex items-center justify-center">
                    <span class="text-body font-medium text-slate-300">
                      {{ state().customerName.charAt(0) }}
                    </span>
                  </div>
                  <div class="flex-1">
                    <p class="text-body text-white">{{ state().customerName }}</p>
                    <p class="text-caption text-slate-500">{{ state().customerPhone }}</p>
                  </div>
                  <button class="btn-secondary btn-icon" (click)="callCustomer()">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                            d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"/>
                    </svg>
                  </button>
                </div>
              </div>
            }

            <!-- Main Action Button -->
            <button 
              class="btn-primary w-full py-4 text-body font-semibold"
              (click)="advancePhase()">
              {{ getActionLabel() }}
            </button>

            <!-- Trip Value -->
            <div class="text-center mt-4">
              <span class="text-caption text-slate-500">Valor: </span>
              <span class="text-body font-semibold text-success">
                R$ {{ state().price.toFixed(2) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </ion-content>

    <ion-action-sheet
      [isOpen]="showOptions()"
      [buttons]="optionButtons"
      (didDismiss)="showOptions.set(false)">
    </ion-action-sheet>
  `,
  styles: [`
    .on-trip-page {
      --background: #0F172A;
    }

    ion-toolbar {
      --background: #0F172A;
      --color: #F1F5F9;
    }

    ion-fab-button {
      --background: #3B82F6;
      --color: white;
    }

    .safe-area-inset-bottom {
      padding-bottom: env(safe-area-inset-bottom, 0);
    }
  `]
})
export class OnTripPage implements OnInit, OnDestroy {
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  showOptions = signal(false);

  phases = [
    { id: 'GOING_TO_PICKUP', label: 'Indo para coleta' },
    { id: 'AT_PICKUP', label: 'No local de coleta' },
    { id: 'IN_TRANSIT', label: 'Em trânsito' },
    { id: 'AT_DELIVERY', label: 'No local de entrega' },
    { id: 'COMPLETED', label: 'Concluído' }
  ];

  optionButtons = [
    {
      text: 'Reportar Problema',
      icon: 'warning-outline',
      handler: () => console.log('Report problem')
    },
    {
      text: 'Cancelar Viagem',
      icon: 'close-circle-outline',
      role: 'destructive',
      handler: () => this.cancelTrip()
    },
    {
      text: 'Voltar',
      icon: 'arrow-back-outline',
      role: 'cancel'
    }
  ];

  state = signal<TripState>({
    orderId: 1234,
    phase: 'GOING_TO_PICKUP',
    pickupAddress: 'Av. Paulista, 1578 - Bela Vista',
    deliveryAddress: 'Rua Oscar Freire, 2100 - Jardins',
    customerName: 'João Silva',
    customerPhone: '+55 11 99999-0000',
    distanceRemaining: 1.2,
    etaMinutes: 5,
    price: 28.50
  });

  ngOnInit(): void {
    const orderId = this.route.snapshot.queryParams['orderId'];
    if (orderId) {
      this.state.update(s => ({ ...s, orderId: Number(orderId) }));
    }
  }

  ngOnDestroy(): void {}

  isPhaseActive(phaseId: string): boolean {
    const currentIndex = this.phases.findIndex(p => p.id === this.state().phase);
    const phaseIndex = this.phases.findIndex(p => p.id === phaseId);
    return phaseIndex <= currentIndex;
  }

  getPhaseLabel(): string {
    const phase = this.phases.find(p => p.id === this.state().phase);
    return phase?.label || '';
  }

  getDestinationAddress(): string {
    const phase = this.state().phase;
    if (phase === 'GOING_TO_PICKUP' || phase === 'AT_PICKUP') {
      return this.state().pickupAddress;
    }
    return this.state().deliveryAddress;
  }

  getActionLabel(): string {
    const labels: Record<TripPhase, string> = {
      GOING_TO_PICKUP: 'Cheguei no local',
      AT_PICKUP: 'Coletei o pedido',
      IN_TRANSIT: 'Cheguei no destino',
      AT_DELIVERY: 'Entreguei o pedido',
      COMPLETED: 'Voltar ao início'
    };
    return labels[this.state().phase];
  }

  advancePhase(): void {
    const phaseOrder: TripPhase[] = [
      'GOING_TO_PICKUP', 'AT_PICKUP', 'IN_TRANSIT', 'AT_DELIVERY', 'COMPLETED'
    ];
    
    const currentIndex = phaseOrder.indexOf(this.state().phase);
    
    if (currentIndex < phaseOrder.length - 1) {
      const nextPhase = phaseOrder[currentIndex + 1];
      this.state.update(s => ({
        ...s,
        phase: nextPhase,
        etaMinutes: nextPhase === 'IN_TRANSIT' ? 15 : s.etaMinutes,
        distanceRemaining: nextPhase === 'IN_TRANSIT' ? 4.5 : s.distanceRemaining
      }));
    } else {
      // Trip completed
      this.router.navigate(['/waiting']);
    }
  }

  callCustomer(): void {
    // Would open phone dialer
    console.log('Calling customer:', this.state().customerPhone);
  }

  openNavigation(): void {
    // Would open native maps app
    console.log('Opening navigation');
  }

  openOptions(): void {
    this.showOptions.set(true);
  }

  cancelTrip(): void {
    // Would cancel trip and return
    this.router.navigate(['/waiting']);
  }
}
