import { 
  Component, 
  OnInit, 
  OnDestroy, 
  AfterViewInit,
  ElementRef, 
  ViewChild, 
  Input,
  effect,
  inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import * as L from 'leaflet';
import { DriverMarker, MapConfig } from '../../models';
import { MockDriverService } from '../../services';

// Fix for default marker icons in Leaflet with Angular
const iconRetinaUrl = 'assets/marker-icon-2x.png';
const iconUrl = 'assets/marker-icon.png';
const shadowUrl = 'assets/marker-shadow.png';

@Component({
  selector: 'app-fleet-map',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="map-wrapper relative w-full h-full">
      <!-- Map Container -->
      <div #mapContainer class="map-container w-full h-full"></div>
      
      <!-- Map Controls Overlay -->
      <div class="absolute top-4 right-4 z-10 flex flex-col gap-2">
        <!-- Driver Count Badge -->
        <div class="glass rounded-lg px-3 py-2">
          <div class="flex items-center gap-2">
            <span class="status-available"></span>
            <span class="text-body-sm text-slate-200">
              {{ driverService.availableDriversCount() }} disponíveis
            </span>
          </div>
          <div class="flex items-center gap-2 mt-1">
            <span class="status-on-delivery"></span>
            <span class="text-body-sm text-slate-200">
              {{ driverService.busyDriversCount() }} em entrega
            </span>
          </div>
        </div>

        <!-- Center Map Button -->
        <button 
          class="btn-secondary btn-icon"
          (click)="centerMap()"
          title="Centralizar mapa">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                  d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/>
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                  d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/>
          </svg>
        </button>
      </div>

      <!-- Loading Overlay -->
      @if (isLoading) {
        <div class="absolute inset-0 bg-surface-900/80 flex items-center justify-center z-20">
          <div class="text-center">
            <div class="animate-spin w-8 h-8 border-2 border-primary-500 border-t-transparent rounded-full mx-auto"></div>
            <p class="text-slate-400 mt-2">Carregando mapa...</p>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    :host {
      display: block;
      width: 100%;
      height: 100%;
    }

    .map-wrapper {
      min-height: 400px;
    }
  `]
})
export class FleetMapComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('mapContainer', { static: true }) mapContainer!: ElementRef;

  @Input() config: MapConfig = {
    center: [-23.5629, -46.6544], // São Paulo
    zoom: 13,
    minZoom: 10,
    maxZoom: 18
  };

  readonly driverService = inject(MockDriverService);

  private map!: L.Map;
  private markersLayer!: L.LayerGroup;
  private markerCache = new Map<number, L.Marker>();

  isLoading = true;

  constructor() {
    // React to driver location changes
    effect(() => {
      const markers = this.driverService.driverMarkers();
      if (this.map) {
        this.updateMarkers(markers);
      }
    });
  }

  ngOnInit(): void {
    this.initLeafletIcons();
  }

  ngAfterViewInit(): void {
    this.initMap();
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }

  centerMap(): void {
    this.map?.setView(this.config.center, this.config.zoom);
  }

  private initLeafletIcons(): void {
    // Custom icon definition for Leaflet
    L.Icon.Default.mergeOptions({
      iconRetinaUrl,
      iconUrl,
      shadowUrl,
    });
  }

  private initMap(): void {
    // Create map instance
    this.map = L.map(this.mapContainer.nativeElement, {
      center: this.config.center,
      zoom: this.config.zoom,
      minZoom: this.config.minZoom,
      maxZoom: this.config.maxZoom,
      zoomControl: true,
      attributionControl: false
    });

    // Dark theme map tiles (CartoDB Dark Matter)
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      maxZoom: 19,
      subdomains: 'abcd'
    }).addTo(this.map);

    // Create markers layer group
    this.markersLayer = L.layerGroup().addTo(this.map);

    // Map ready
    this.map.whenReady(() => {
      this.isLoading = false;
      // Initial markers
      this.updateMarkers(this.driverService.driverMarkers());
    });

    // Handle resize
    setTimeout(() => this.map.invalidateSize(), 100);
  }

  private updateMarkers(drivers: DriverMarker[]): void {
    if (!this.markersLayer) return;

    const currentIds = new Set(drivers.map(d => d.id));

    // Remove markers for drivers no longer in list
    this.markerCache.forEach((marker, id) => {
      if (!currentIds.has(id)) {
        this.markersLayer.removeLayer(marker);
        this.markerCache.delete(id);
      }
    });

    // Update or create markers
    drivers.forEach(driver => {
      const existingMarker = this.markerCache.get(driver.id);

      if (existingMarker) {
        // Smooth update position
        existingMarker.setLatLng([driver.latitude, driver.longitude]);
        this.updateMarkerPopup(existingMarker, driver);
      } else {
        // Create new marker
        const marker = this.createDriverMarker(driver);
        this.markersLayer.addLayer(marker);
        this.markerCache.set(driver.id, marker);
      }
    });
  }

  private createDriverMarker(driver: DriverMarker): L.Marker {
    const icon = this.createDriverIcon(driver.status);
    
    const marker = L.marker([driver.latitude, driver.longitude], { icon })
      .bindPopup(this.createPopupContent(driver), {
        className: 'driver-popup'
      });

    return marker;
  }

  private createDriverIcon(status: DriverMarker['status']): L.DivIcon {
    const colorClass = this.getStatusColorClass(status);
    const initials = status === 'ON_DELIVERY' ? '🚗' : '👤';

    return L.divIcon({
      className: 'custom-driver-marker',
      html: `
        <div class="driver-marker ${colorClass}">
          <span>${initials}</span>
        </div>
      `,
      iconSize: [40, 40],
      iconAnchor: [20, 20],
      popupAnchor: [0, -25]
    });
  }

  private getStatusColorClass(status: DriverMarker['status']): string {
    switch (status) {
      case 'AVAILABLE': return 'driver-marker-available';
      case 'ON_DELIVERY': return 'driver-marker-on-delivery';
      case 'BUSY': return 'driver-marker-busy';
      default: return 'driver-marker-busy';
    }
  }

  private createPopupContent(driver: DriverMarker): string {
    return `
      <div class="p-2">
        <h4 class="font-semibold text-slate-100">${driver.name}</h4>
        <p class="text-caption text-slate-400 mt-1">
          Velocidade: ${driver.speed?.toFixed(0) || 0} km/h
        </p>
        <p class="text-caption text-slate-400">
          Atualizado: ${this.formatTime(driver.lastUpdate)}
        </p>
      </div>
    `;
  }

  private updateMarkerPopup(marker: L.Marker, driver: DriverMarker): void {
    marker.setPopupContent(this.createPopupContent(driver));
  }

  private formatTime(date: Date): string {
    return date.toLocaleTimeString('pt-BR', { 
      hour: '2-digit', 
      minute: '2-digit',
      second: '2-digit'
    });
  }
}
