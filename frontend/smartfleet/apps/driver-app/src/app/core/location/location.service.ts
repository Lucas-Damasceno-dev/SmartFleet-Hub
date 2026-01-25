import { Injectable, inject, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Subject, interval, Subscription } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Geolocation, Position } from '@capacitor/geolocation';
import { Capacitor } from '@capacitor/core';

export interface GpsLocation {
  latitude: number;
  longitude: number;
  heading: number;
  speed: number;
  accuracy: number;
  timestamp: number;
}

@Injectable({
  providedIn: 'root'
})
export class LocationService implements OnDestroy {
  private http = inject(HttpClient);
  private destroy$ = new Subject<void>();
  
  private currentLocation = new BehaviorSubject<GpsLocation | null>(null);
  private isTracking = new BehaviorSubject<boolean>(false);
  
  currentLocation$ = this.currentLocation.asObservable();
  isTracking$ = this.isTracking.asObservable();

  private watchId: string | null = null;
  private uploadSubscription: Subscription | null = null;
  
  private readonly apiUrl = 'http://localhost:8080/api';
  private readonly uploadIntervalMs = 10000; // 10 seconds

  async startTracking(): Promise<boolean> {
    try {
      // Request permissions
      const permissions = await Geolocation.requestPermissions();
      if (permissions.location !== 'granted') {
        console.error('Location permission denied');
        return false;
      }

      // Start watching position
      if (Capacitor.isNativePlatform()) {
        this.watchId = await Geolocation.watchPosition(
          {
            enableHighAccuracy: true,
            timeout: 10000,
            maximumAge: 0
          },
          (position, err) => {
            if (err) {
              console.error('Geolocation error:', err);
              return;
            }
            if (position) {
              this.handlePositionUpdate(position);
            }
          }
        );
      } else {
        // Web fallback - use interval-based polling
        this.startWebTracking();
      }

      // Start uploading locations to server
      this.startLocationUpload();
      
      this.isTracking.next(true);
      return true;
    } catch (error) {
      console.error('Failed to start tracking:', error);
      return false;
    }
  }

  async stopTracking(): Promise<void> {
    if (this.watchId) {
      await Geolocation.clearWatch({ id: this.watchId });
      this.watchId = null;
    }

    if (this.uploadSubscription) {
      this.uploadSubscription.unsubscribe();
      this.uploadSubscription = null;
    }

    this.isTracking.next(false);
  }

  async getCurrentPosition(): Promise<GpsLocation | null> {
    try {
      const position = await Geolocation.getCurrentPosition({
        enableHighAccuracy: true,
        timeout: 10000
      });
      
      const location = this.positionToGpsLocation(position);
      this.currentLocation.next(location);
      return location;
    } catch (error) {
      console.error('Failed to get current position:', error);
      return null;
    }
  }

  private handlePositionUpdate(position: Position): void {
    const location = this.positionToGpsLocation(position);
    this.currentLocation.next(location);
  }

  private positionToGpsLocation(position: Position): GpsLocation {
    return {
      latitude: position.coords.latitude,
      longitude: position.coords.longitude,
      heading: position.coords.heading ?? 0,
      speed: (position.coords.speed ?? 0) * 3.6, // Convert m/s to km/h
      accuracy: position.coords.accuracy,
      timestamp: position.timestamp
    };
  }

  private startWebTracking(): void {
    interval(5000).pipe(
      takeUntil(this.destroy$)
    ).subscribe(async () => {
      await this.getCurrentPosition();
    });
  }

  private startLocationUpload(): void {
    this.uploadSubscription = interval(this.uploadIntervalMs).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      const location = this.currentLocation.value;
      if (location) {
        this.uploadLocation(location);
      }
    });
  }

  private uploadLocation(location: GpsLocation): void {
    const payload = {
      latitude: location.latitude,
      longitude: location.longitude,
      heading: location.heading,
      speedKmh: location.speed,
      timestamp: new Date(location.timestamp).toISOString()
    };

    this.http.post(`${this.apiUrl}/tracking/location`, payload).subscribe({
      next: () => console.debug('Location uploaded successfully'),
      error: (err) => console.error('Failed to upload location:', err)
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.stopTracking();
  }
}
