import { Injectable, inject, NgZone, OnDestroy } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { AuthService } from '../auth/auth.service';

export interface LocationUpdate {
  vehicleId: string;
  driverId: string;
  latitude: number;
  longitude: number;
  heading: number;
  speedKmh: number;
  timestamp: string;
}

export interface ConnectionState {
  connected: boolean;
  reconnecting: boolean;
  error: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class RealtimeService implements OnDestroy {
  private ngZone = inject(NgZone);
  private authService = inject(AuthService);
  
  private eventSource: EventSource | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 1000;

  private locationUpdates = new Subject<LocationUpdate>();
  private connectionState = new BehaviorSubject<ConnectionState>({
    connected: false,
    reconnecting: false,
    error: null
  });

  locationUpdates$ = this.locationUpdates.asObservable();
  connectionState$ = this.connectionState.asObservable();

  private apiUrl = 'http://localhost:8080/api';

  async connect(): Promise<void> {
    if (this.eventSource) {
      this.disconnect();
    }

    const token = await this.authService.getToken();
    if (!token) {
      this.connectionState.next({
        connected: false,
        reconnecting: false,
        error: 'Not authenticated'
      });
      return;
    }

    this.connectionState.next({
      connected: false,
      reconnecting: this.reconnectAttempts > 0,
      error: null
    });

    // Note: EventSource doesn't support custom headers natively
    // Use URL parameter for token or implement custom SSE client
    const url = `${this.apiUrl}/stream/locations?token=${encodeURIComponent(token)}`;
    
    this.ngZone.runOutsideAngular(() => {
      this.eventSource = new EventSource(url);

      this.eventSource.onopen = () => {
        this.ngZone.run(() => {
          console.log('SSE connection established');
          this.reconnectAttempts = 0;
          this.connectionState.next({
            connected: true,
            reconnecting: false,
            error: null
          });
        });
      };

      this.eventSource.addEventListener('location-update', (event: MessageEvent) => {
        this.ngZone.run(() => {
          try {
            const data = JSON.parse(event.data) as LocationUpdate;
            this.locationUpdates.next(data);
          } catch (e) {
            console.error('Failed to parse location update:', e);
          }
        });
      });

      this.eventSource.addEventListener('connected', (event: MessageEvent) => {
        console.log('SSE connected event:', event.data);
      });

      this.eventSource.onerror = (error) => {
        this.ngZone.run(() => {
          console.error('SSE error:', error);
          this.handleError();
        });
      };
    });
  }

  disconnect(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
    this.connectionState.next({
      connected: false,
      reconnecting: false,
      error: null
    });
  }

  private handleError(): void {
    this.disconnect();

    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);
      
      this.connectionState.next({
        connected: false,
        reconnecting: true,
        error: `Connection lost. Reconnecting in ${delay / 1000}s...`
      });

      setTimeout(() => this.connect(), delay);
    } else {
      this.connectionState.next({
        connected: false,
        reconnecting: false,
        error: 'Max reconnection attempts reached. Please refresh the page.'
      });
    }
  }

  isConnected(): boolean {
    return this.eventSource?.readyState === EventSource.OPEN;
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
