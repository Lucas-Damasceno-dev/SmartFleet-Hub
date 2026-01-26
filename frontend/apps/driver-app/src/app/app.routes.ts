import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.page').then(m => m.LoginPage)
  },
  {
    path: 'waiting',
    loadComponent: () => import('./pages/waiting/waiting.page').then(m => m.WaitingPage)
  },
  {
    path: 'order-offer',
    loadComponent: () => import('./pages/order-offer/order-offer.page').then(m => m.OrderOfferPage)
  },
  {
    path: 'on-trip',
    loadComponent: () => import('./pages/on-trip/on-trip.page').then(m => m.OnTripPage)
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];
