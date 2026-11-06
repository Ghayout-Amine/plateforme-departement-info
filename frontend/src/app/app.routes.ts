import { Routes } from '@angular/router';
import { Home } from './pages/home/home';
import { Login } from './pages/login/login';
import { Dashboard } from './pages/dashboard/dashboard';
import { TeacherList } from './pages/teachers/teacher-list/teacher-list';
import { TeacherForm } from './pages/teachers/teacher-form/teacher-form';
import { TeacherProfile } from './pages/teachers/teacher-profile/teacher-profile';
import { TeacherEdit } from './pages/teachers/teacher-edit/teacher-edit';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'login', component: Login },

  {
    path: 'dashboard',
    component: Dashboard,
    canActivate: [authGuard]
  },
  {
    path: 'teachers/new',
    component: TeacherForm,
    canActivate: [authGuard]
  },
  {
    path: 'teachers/:id/edit',
    component: TeacherEdit,
    canActivate: [authGuard]
  },
  {
    path: 'teachers/:id',
    component: TeacherProfile,
    canActivate: [authGuard]
  },
  {
    path: 'teachers',
    component: TeacherList,
    canActivate: [authGuard]
  },
  { path: '**', redirectTo: '' }
];
