import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-unauthorized-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="min-h-screen bg-slate-900 flex items-center justify-center px-4">
      <div class="text-center">
        <div class="w-24 h-24 mx-auto bg-red-500/20 rounded-full flex items-center justify-center mb-6">
          <svg class="w-12 h-12 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                  d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
          </svg>
        </div>

        <h1 class="text-3xl font-bold text-white mb-4">Access Denied</h1>
        
        <p class="text-slate-400 max-w-md mx-auto mb-8">
          You don't have permission to access this page. Please contact your administrator 
          if you believe this is an error.
        </p>

        <a 
          routerLink="/dashboard"
          class="inline-flex items-center gap-2 px-6 py-3 bg-royal-blue hover:bg-royal-blue/90 text-white rounded-lg font-medium transition-colors">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"/>
          </svg>
          Back to Dashboard
        </a>
      </div>
    </div>
  `
})
export class UnauthorizedPageComponent {}
