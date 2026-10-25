import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
  {
    path: 'filieres',
    loadComponent: () =>
      import('./filieres-modules/filieres-modules-page').then((m) => m.FilieresModulesPage),
    title: 'Filières et modules',
  },
  { path: '', redirectTo: 'filieres', pathMatch: 'full' },
];
