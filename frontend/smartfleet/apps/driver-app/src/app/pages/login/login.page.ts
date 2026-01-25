import { Component, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { 
  IonContent, 
  IonInput, 
  IonButton, 
  IonSpinner,
  IonIcon,
  IonText
} from '@ionic/angular/standalone';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    IonContent,
    IonInput,
    IonButton,
    IonSpinner,
    IonIcon,
    IonText
  ],
  template: `
    <ion-content class="login-page">
      <div class="min-h-full flex flex-col justify-center px-6 py-12 bg-surface-900">
        <!-- Logo -->
        <div class="text-center mb-10">
          <div class="w-20 h-20 bg-primary-600 rounded-2xl mx-auto flex items-center justify-center mb-4">
            <svg class="w-12 h-12 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                    d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"/>
            </svg>
          </div>
          <h1 class="text-h1 text-white">SmartFleet</h1>
          <p class="text-body text-slate-400 mt-2">App do Motorista</p>
        </div>

        <!-- Login Form -->
        <div class="w-full max-w-sm mx-auto">
          <form (ngSubmit)="onLogin()" class="space-y-6">
            <!-- Email -->
            <div>
              <label class="label">E-mail</label>
              <ion-input
                type="email"
                [(ngModel)]="email"
                name="email"
                placeholder="seu@email.com"
                class="input"
                [disabled]="isLoading()">
              </ion-input>
            </div>

            <!-- Password -->
            <div>
              <label class="label">Senha</label>
              <ion-input
                type="password"
                [(ngModel)]="password"
                name="password"
                placeholder="••••••••"
                class="input"
                [disabled]="isLoading()">
              </ion-input>
            </div>

            <!-- Error Message -->
            @if (error()) {
              <div class="p-3 bg-danger/10 border border-danger/30 rounded-lg">
                <ion-text color="danger" class="text-body-sm">
                  {{ error() }}
                </ion-text>
              </div>
            }

            <!-- Submit Button -->
            <ion-button
              expand="block"
              type="submit"
              [disabled]="isLoading()"
              class="btn-primary h-12">
              @if (isLoading()) {
                <ion-spinner name="crescent" class="mr-2"></ion-spinner>
                Entrando...
              } @else {
                Entrar
              }
            </ion-button>
          </form>

          <!-- Forgot Password -->
          <div class="text-center mt-6">
            <a href="#" class="text-body-sm text-primary-400 hover:text-primary-300">
              Esqueci minha senha
            </a>
          </div>

          <!-- Version -->
          <p class="text-center text-caption text-slate-600 mt-12">
            Versão 1.0.0
          </p>
        </div>
      </div>
    </ion-content>
  `,
  styles: [`
    .login-page {
      --background: #0F172A;
    }

    ion-input {
      --background: #1E293B;
      --color: #F1F5F9;
      --placeholder-color: #64748B;
      --padding-start: 16px;
      --padding-end: 16px;
      --padding-top: 12px;
      --padding-bottom: 12px;
      border-radius: 8px;
      border: 1px solid #334155;
    }

    ion-input:focus-within {
      border-color: #3B82F6;
    }

    ion-button {
      --background: #3B82F6;
      --background-hover: #2563EB;
      --border-radius: 8px;
    }
  `]
})
export class LoginPage implements OnInit {
  email = '';
  password = '';
  
  isLoading = signal(false);
  error = signal<string | null>(null);

  constructor(private router: Router) {}

  ngOnInit(): void {}

  async onLogin(): Promise<void> {
    if (!this.email || !this.password) {
      this.error.set('Por favor, preencha todos os campos');
      return;
    }

    this.isLoading.set(true);
    this.error.set(null);

    try {
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1500));

      // Mock validation
      if (this.email === 'driver@test.com' && this.password === '123456') {
        this.router.navigate(['/waiting']);
      } else {
        this.error.set('E-mail ou senha incorretos');
      }
    } catch (e) {
      this.error.set('Erro de conexão. Tente novamente.');
    } finally {
      this.isLoading.set(false);
    }
  }
}
