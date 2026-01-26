import { computed, inject } from '@angular/core';
import { signalStore, withState, withMethods, withComputed, patchState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe, tap, switchMap } from 'rxjs';
import { tapResponse } from '@ngrx/operators';

export interface VehicleLocation {
  vehicleId: number;
  driverId?: number;
  latitude: number;
  longitude: number;
  speedKmh: number;
  headingDegrees: number;
  status: 'AVAILABLE' | 'BUSY' | 'ON_DELIVERY' | 'OFFLINE';
  lastUpdate: string;
}

export interface TrackingState {
  vehicles: VehicleLocation[];
  loading: boolean;
  error: string | null;
  selectedVehicleId: number | null;
}

const initialState: TrackingState = {
  vehicles: [],
  loading: false,
  error: null,
  selectedVehicleId: null
};

export const TrackingStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed((store) => ({
    availableVehicles: computed(() => 
      store.vehicles().filter(v => v.status === 'AVAILABLE')
    ),
    busyVehicles: computed(() => 
      store.vehicles().filter(v => v.status === 'BUSY' || v.status === 'ON_DELIVERY')
    ),
    selectedVehicle: computed(() => {
      const id = store.selectedVehicleId();
      return id ? store.vehicles().find(v => v.vehicleId === id) : null;
    }),
    vehicleCount: computed(() => ({
      total: store.vehicles().length,
      available: store.vehicles().filter(v => v.status === 'AVAILABLE').length,
      busy: store.vehicles().filter(v => v.status === 'BUSY').length,
      onDelivery: store.vehicles().filter(v => v.status === 'ON_DELIVERY').length,
      offline: store.vehicles().filter(v => v.status === 'OFFLINE').length
    }))
  })),
  withMethods((store) => ({
    setVehicles(vehicles: VehicleLocation[]): void {
      patchState(store, { vehicles, loading: false, error: null });
    },
    
    updateVehicleLocation(vehicleId: number, lat: number, lon: number, speed?: number, heading?: number): void {
      patchState(store, (state) => ({
        vehicles: state.vehicles.map(v => 
          v.vehicleId === vehicleId 
            ? { 
                ...v, 
                latitude: lat, 
                longitude: lon,
                speedKmh: speed ?? v.speedKmh,
                headingDegrees: heading ?? v.headingDegrees,
                lastUpdate: new Date().toISOString()
              }
            : v
        )
      }));
    },

    updateVehicleStatus(vehicleId: number, status: VehicleLocation['status']): void {
      patchState(store, (state) => ({
        vehicles: state.vehicles.map(v => 
          v.vehicleId === vehicleId ? { ...v, status } : v
        )
      }));
    },

    selectVehicle(vehicleId: number | null): void {
      patchState(store, { selectedVehicleId: vehicleId });
    },

    setLoading(loading: boolean): void {
      patchState(store, { loading });
    },

    setError(error: string | null): void {
      patchState(store, { error, loading: false });
    },

    addVehicle(vehicle: VehicleLocation): void {
      patchState(store, (state) => ({
        vehicles: [...state.vehicles, vehicle]
      }));
    },

    removeVehicle(vehicleId: number): void {
      patchState(store, (state) => ({
        vehicles: state.vehicles.filter(v => v.vehicleId !== vehicleId)
      }));
    },

    reset(): void {
      patchState(store, initialState);
    }
  }))
);
