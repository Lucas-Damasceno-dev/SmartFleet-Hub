import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/auth';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="min-h-screen bg-slate-900 flex items-center justify-center px-4">
      <div class="max-w-md w-full">
        <!-- Logo -->
        <div class="text-center mb-8">
          <div class="w-16 h-16 mx-auto bg-royal-blue rounded-xl flex items-center justify-center mb-4">
            <svg class="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                    d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"/>
            </svg>
          </div>
          <h1 class="text-3xl font-bold text-white">SmartFleet Hub</h1>
          <p class="text-slate-400 mt-2">Fleet Management Dashboard</p>
        </div>

        <!-- Login Card -->
        <div class="bg-slate-800 rounded-xl shadow-xl p-8">
          <h2 class="text-xl font-semibold text-white mb-6">Welcome Back</h2>
          
          <p class="text-slate-400 mb-8">
            Sign in with your company credentials to access the fleet management dashboard.
          </p>

          <button 
            (click)="login()"
            class="w-full py-3 px-4 bg-royal-blue hover:bg-royal-blue/90 text-white font-medium rounded-lg transition-colors flex items-center justify-center gap-3">
            <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12.5 8c-2.65 0-5.05.99-6.9 2.6L2 7.5V16h8.5l-3.45-3.45C8.3 11.42 10.24 10.5 12.5 10.5c3.41 0 6.28 2.22 7.28 5.3l2.08-1.04C20.32 11.24 16.77 8 12.5 8z"/>
            </svg>
            Sign in with Keycloak
          </button>

          <div class="mt-6 text-center text-sm text-slate-500">
            <p>Having trouble signing in?</p>
            <a href="mailto:support@smartfleet.io" class="text-royal-blue hover:underline">
              Contact Support
            </a>
          </div>
        </div>

        <!-- Footer -->
        <p class="text-center text-slate-600 text-sm mt-8">
          © 2024 SmartFleet Hub. All rights reserved.
        </p>
      </div>
    </div>
  `
})
export class LoginPageComponent {
  private authService = inject(AuthService);

  async login(): Promise<void> {
    await this.authService.login();
  }
}
