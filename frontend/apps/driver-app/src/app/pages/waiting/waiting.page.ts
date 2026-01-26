import { Component, signal, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { 
  IonContent, 
  IonHeader,
  IonToolbar,
  IonTitle,
  IonButton,
  IonIcon,
  IonBadge,
  IonFab,
  IonFabButton
} from '@ionic/angular/standalone';

interface DriverState {
  status: 'AVAILABLE' | 'BUSY' | 'OFFLINE';
  latitude: number;
  longitude: number;
  earnings: number;
  completedTrips: number;
}

@Component({
  selector: 'app-waiting',
  standalone: true,
  imports: [
    CommonModule,
    IonContent,
    IonHeader,
    IonToolbar,
    IonTitle,
    IonButton,
    IonIcon,
    IonBadge,
    IonFab,
    IonFabButton
  ],
  template: `
    <ion-header class="ion-no-border">
      <ion-toolbar color="dark">
        <ion-title>
          <div class="flex items-center gap-2">
            <span [class]="getStatusDotClass()"></span>
            <span>{{ getStatusLabel() }}</span>
          </div>
        </ion-title>
        <ion-button slot="end" fill="clear" (click)="openProfile()">
          <div class="w-8 h-8 bg-primary-600 rounded-full flex items-center justify-center">
            <span class="text-white font-medium">M</span>
          </div>
        </ion-button>
      </ion-toolbar>
    </ion-header>

    <ion-content class="waiting-page">
      <!-- Map Container -->
      <div class="h-full relative">
        <!-- Simulated Map Background -->
        <div class="absolute inset-0 bg-surface-800">
          <div class="w-full h-full bg-gradient-to-b from-surface-800 to-surface-900 flex items-center justify-center">
            <div class="text-center">
              <div class="w-16 h-16 border-4 border-primary-500 border-t-transparent rounded-full animate-spin mx-auto"></div>
              <p class="text-slate-400 mt-4">Carregando mapa...</p>
            </div>
          </div>
        </div>

        <!-- Current Location Marker -->
        <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-10">
          <div class="w-20 h-20 bg-primary-500/20 rounded-full flex items-center justify-center animate-pulse">
            <div class="w-10 h-10 bg-primary-600 rounded-full flex items-center justify-center shadow-glow-primary">
              <svg class="w-6 h-6 text-white" fill="currentColor" viewBox="0 0 24 24">
                <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
              </svg>
            </div>
          </div>
        </div>

        <!-- Status Card -->
        <div class="absolute bottom-0 left-0 right-0 p-4 safe-area-inset-bottom">
          <div class="card animate-slide-in">
            <!-- Stats Row -->
            <div class="grid grid-cols-3 gap-4 pb-4 border-b border-surface-700">
              <div class="text-center">
                <p class="text-h4 text-success">R$ {{ state().earnings.toFixed(2) }}</p>
                <p class="text-caption text-slate-500">Hoje</p>
              </div>
              <div class="text-center">
                <p class="text-h4 text-primary-400">{{ state().completedTrips }}</p>
                <p class="text-caption text-slate-500">Viagens</p>
              </div>
              <div class="text-center">
                <p class="text-h4 text-slate-200">4.8</p>
                <p class="text-caption text-slate-500">Avaliação</p>
              </div>
            </div>

            <!-- Status Message -->
            <div class="py-4 text-center">
              @if (state().status === 'AVAILABLE') {
                <p class="text-body text-slate-300">
                  Você está online e visível para pedidos
                </p>
                <p class="text-caption text-slate-500 mt-1">
                  Aguardando novos pedidos na sua região
                </p>
              } @else {
                <p class="text-body text-slate-300">
                  Você está offline
                </p>
                <p class="text-caption text-slate-500 mt-1">
                  Ative para receber pedidos
                </p>
              }
            </div>

            <!-- Toggle Button -->
            <button 
              [class]="state().status === 'AVAILABLE' ? 'btn-danger w-full' : 'btn-success w-full'"
              (click)="toggleStatus()">
              @if (state().status === 'AVAILABLE') {
                Ficar Offline
              } @else {
                Ficar Online
              }
            </button>
          </div>
        </div>

        <!-- Center on me FAB -->
        <ion-fab vertical="bottom" horizontal="start" slot="fixed" class="mb-48">
          <ion-fab-button size="small" (click)="centerOnMe()">
            <ion-icon name="locate-outline"></ion-icon>
          </ion-fab-button>
        </ion-fab>
      </div>
    </ion-content>
  `,
  styles: [`
    .waiting-page {
      --background: #0F172A;
    }

    ion-toolbar {
      --background: #0F172A;
      --color: #F1F5F9;
    }

    ion-fab-button {
      --background: #1E293B;
      --color: #F1F5F9;
    }

    .safe-area-inset-bottom {
      padding-bottom: env(safe-area-inset-bottom, 16px);
    }
  `]
})
export class WaitingPage implements OnInit, OnDestroy {
  private router = inject(Router);
  private intervalId?: ReturnType<typeof setInterval>;

  state = signal<DriverState>({
    status: 'AVAILABLE',
    latitude: -23.5629,
    longitude: -46.6544,
    earnings: 127.50,
    completedTrips: 5
  });

  ngOnInit(): void {
    // Simulate incoming order after some time
    this.simulateOrderOffer();
  }

  ngOnDestroy(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  toggleStatus(): void {
    this.state.update(s => ({
      ...s,
      status: s.status === 'AVAILABLE' ? 'OFFLINE' : 'AVAILABLE'
    }));
  }

  getStatusDotClass(): string {
    return this.state().status === 'AVAILABLE' 
      ? 'status-available' 
      : 'status-offline';
  }

  getStatusLabel(): string {
    return this.state().status === 'AVAILABLE' ? 'Online' : 'Offline';
  }

  centerOnMe(): void {
    // Would center map on current location
    console.log('Centering on current location');
  }

  openProfile(): void {
    // Navigate to profile
    console.log('Opening profile');
  }

  private simulateOrderOffer(): void {
    // Simulate order coming in after 5 seconds (for demo)
    this.intervalId = setTimeout(() => {
      if (this.state().status === 'AVAILABLE') {
        this.router.navigate(['/order-offer']);
      }
    }, 8000);
  }
}
