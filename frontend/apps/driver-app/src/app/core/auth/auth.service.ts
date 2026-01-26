import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, from, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { Preferences } from '@capacitor/preferences';

export interface UserCredentials {
  username: string;
  password: string;
}

export interface TokenResponse {
  access_token: string;
  refresh_token: string;
  expires_in: number;
  token_type: string;
}

export interface DriverProfile {
  id: string;
  username: string;
  name: string;
  email: string;
  phone: string;
  vehicleId: string;
  status: 'AVAILABLE' | 'BUSY' | 'OFFLINE';
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly TOKEN_KEY = 'auth_token';
  private readonly REFRESH_TOKEN_KEY = 'refresh_token';
  private readonly USER_KEY = 'user_profile';

  private isAuthenticated = new BehaviorSubject<boolean>(false);
  private user = new BehaviorSubject<DriverProfile | null>(null);

  isAuthenticated$ = this.isAuthenticated.asObservable();
  user$ = this.user.asObservable();

  private readonly keycloakUrl = 'http://localhost:8180';
  private readonly realm = 'smartfleet';
  private readonly clientId = 'mobile-driver-app';
  private readonly apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {
    this.loadStoredAuth();
  }

  private async loadStoredAuth(): Promise<void> {
    const { value: token } = await Preferences.get({ key: this.TOKEN_KEY });
    const { value: userJson } = await Preferences.get({ key: this.USER_KEY });

    if (token && userJson) {
      try {
        const user = JSON.parse(userJson) as DriverProfile;
        this.user.next(user);
        this.isAuthenticated.next(true);
      } catch (e) {
        await this.logout();
      }
    }
  }

  login(credentials: UserCredentials): Observable<boolean> {
    const tokenUrl = `${this.keycloakUrl}/realms/${this.realm}/protocol/openid-connect/token`;
    
    const body = new URLSearchParams();
    body.set('grant_type', 'password');
    body.set('client_id', this.clientId);
    body.set('username', credentials.username);
    body.set('password', credentials.password);

    return this.http.post<TokenResponse>(tokenUrl, body.toString(), {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    }).pipe(
      tap(async (response) => {
        await Preferences.set({ key: this.TOKEN_KEY, value: response.access_token });
        await Preferences.set({ key: this.REFRESH_TOKEN_KEY, value: response.refresh_token });
        this.isAuthenticated.next(true);
      }),
      map(() => true),
      catchError(error => {
        console.error('Login failed:', error);
        return of(false);
      })
    );
  }

  loadUserProfile(): Observable<DriverProfile> {
    return this.http.get<DriverProfile>(`${this.apiUrl}/auth/profile`).pipe(
      tap(async (profile) => {
        this.user.next(profile);
        await Preferences.set({ key: this.USER_KEY, value: JSON.stringify(profile) });
      })
    );
  }

  async getToken(): Promise<string | null> {
    const { value } = await Preferences.get({ key: this.TOKEN_KEY });
    return value;
  }

  async refreshToken(): Promise<boolean> {
    const { value: refreshToken } = await Preferences.get({ key: this.REFRESH_TOKEN_KEY });
    
    if (!refreshToken) {
      return false;
    }

    const tokenUrl = `${this.keycloakUrl}/realms/${this.realm}/protocol/openid-connect/token`;
    
    const body = new URLSearchParams();
    body.set('grant_type', 'refresh_token');
    body.set('client_id', this.clientId);
    body.set('refresh_token', refreshToken);

    try {
      const response = await this.http.post<TokenResponse>(tokenUrl, body.toString(), {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
      }).toPromise();

      if (response) {
        await Preferences.set({ key: this.TOKEN_KEY, value: response.access_token });
        await Preferences.set({ key: this.REFRESH_TOKEN_KEY, value: response.refresh_token });
        return true;
      }
      return false;
    } catch (error) {
      console.error('Token refresh failed:', error);
      await this.logout();
      return false;
    }
  }

  async logout(): Promise<void> {
    await Preferences.remove({ key: this.TOKEN_KEY });
    await Preferences.remove({ key: this.REFRESH_TOKEN_KEY });
    await Preferences.remove({ key: this.USER_KEY });
    this.user.next(null);
    this.isAuthenticated.next(false);
  }

  updateDriverStatus(status: 'AVAILABLE' | 'BUSY' | 'OFFLINE'): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/auth/status`, { status }).pipe(
      tap(() => {
        const currentUser = this.user.value;
        if (currentUser) {
          this.user.next({ ...currentUser, status });
        }
      })
    );
  }
}
