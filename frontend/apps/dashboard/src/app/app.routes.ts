import { Route } from '@angular/router';
import { authGuard } from './core/auth';

export const appRoutes: Route[] = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./pages/dashboard').then(m => m.DashboardPageComponent),
    canActivate: [authGuard]
  },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login-page.component').then(m => m.LoginPageComponent)
  },
  {
    path: 'unauthorized',
    loadComponent: () => import('./pages/unauthorized/unauthorized-page.component').then(m => m.UnauthorizedPageComponent)
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
