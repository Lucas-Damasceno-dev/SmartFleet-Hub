import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, Observable, from, of, throwError } from 'rxjs';
import { catchError, map, tap, switchMap } from 'rxjs/operators';
import Keycloak from 'keycloak-js';

export interface UserProfile {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
}

export interface AuthState {
  isAuthenticated: boolean;
  user: UserProfile | null;
  token: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private keycloak: Keycloak | null = null;

  private authState = new BehaviorSubject<AuthState>({
    isAuthenticated: false,
    user: null,
    token: null
  });

  authState$ = this.authState.asObservable();

  private readonly keycloakConfig = {
    url: 'http://localhost:8180',
    realm: 'smartfleet',
    clientId: 'web-dashboard'
  };

  async init(): Promise<boolean> {
    this.keycloak = new Keycloak(this.keycloakConfig);

    try {
      const authenticated = await this.keycloak.init({
        onLoad: 'check-sso',
        silentCheckSsoRedirectUri: window.location.origin + '/assets/silent-check-sso.html',
        checkLoginIframe: false
      });

      if (authenticated) {
        await this.loadUserProfile();
      }

      // Setup token refresh
      this.setupTokenRefresh();

      return authenticated;
    } catch (error) {
      console.error('Keycloak init failed:', error);
      return false;
    }
  }

  async login(): Promise<void> {
    if (!this.keycloak) {
      await this.init();
    }
    await this.keycloak?.login({
      redirectUri: window.location.origin + '/dashboard'
    });
  }

  async logout(): Promise<void> {
    this.authState.next({
      isAuthenticated: false,
      user: null,
      token: null
    });
    await this.keycloak?.logout({
      redirectUri: window.location.origin
    });
  }

  async getToken(): Promise<string | null> {
    if (!this.keycloak) {
      return null;
    }

    try {
      // Refresh token if it's about to expire (within 30 seconds)
      await this.keycloak.updateToken(30);
      return this.keycloak.token ?? null;
    } catch (error) {
      console.error('Token refresh failed:', error);
      await this.login();
      return null;
    }
  }

  isAuthenticated(): boolean {
    return this.keycloak?.authenticated ?? false;
  }

  hasRole(role: string): boolean {
    return this.keycloak?.hasRealmRole(role) ?? false;
  }

  hasAnyRole(roles: string[]): boolean {
    return roles.some(role => this.hasRole(role));
  }

  private async loadUserProfile(): Promise<void> {
    if (!this.keycloak) return;

    try {
      const profile = await this.keycloak.loadUserProfile();
      const tokenParsed = this.keycloak.tokenParsed;
      
      const user: UserProfile = {
        id: tokenParsed?.sub ?? '',
        username: profile.username ?? '',
        email: profile.email ?? '',
        firstName: profile.firstName ?? '',
        lastName: profile.lastName ?? '',
        roles: (tokenParsed?.realm_access?.roles ?? []) as string[]
      };

      this.authState.next({
        isAuthenticated: true,
        user,
        token: this.keycloak.token ?? null
      });
    } catch (error) {
      console.error('Failed to load user profile:', error);
    }
  }

  private setupTokenRefresh(): void {
    if (!this.keycloak) return;

    this.keycloak.onTokenExpired = () => {
      this.keycloak?.updateToken(30).catch(() => {
        console.warn('Token refresh failed, redirecting to login');
        this.login();
      });
    };

    this.keycloak.onAuthLogout = () => {
      this.authState.next({
        isAuthenticated: false,
        user: null,
        token: null
      });
    };
  }
}
