import { Routes } from '@angular/router';
import { Home } from './pages/home/home';
import { Login } from './pages/login/login';
import { Dashboard } from './pages/dashboard/dashboard';
import { TeacherList } from './pages/teachers/teacher-list/teacher-list';
import { TeacherForm } from './pages/teachers/teacher-form/teacher-form';
import { TeacherProfile } from './pages/teachers/teacher-profile/teacher-profile';
import { TeacherEdit } from './pages/teachers/teacher-edit/teacher-edit';
import { StudentList } from './pages/students/student-list/student-list';
import { StudentForm } from './pages/students/student-form/student-form';
import { Filieres } from './pages/filieres/filieres';
import { Contact } from './pages/contact/contact';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'filieres', component: Filieres },
  { path: 'contact', component: Contact },
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
  {
    path: 'students/new',
    component: StudentForm,
    canActivate: [authGuard]
  },
  {
    path: 'students/:id/edit',
    component: StudentForm,
    canActivate: [authGuard]
  },
  {
    path: 'students',
    component: StudentList,
    canActivate: [authGuard]
  },
  { path: '**', redirectTo: '' }
];
