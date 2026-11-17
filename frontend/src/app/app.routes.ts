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
import { SalleList } from './pages/salles/salle-list/salle-list';
import { SalleForm } from './pages/salles/salle-form/salle-form';
import { GroupeList } from './pages/groupes/groupe-list/groupe-list';
import { GroupeForm } from './pages/groupes/groupe-form/groupe-form';
import { Contact } from './pages/contact/contact';
import { UserList } from './pages/admin/user-list/user-list';
import { UserProfile } from './pages/admin/user-profile/user-profile';
import { EnrollmentList } from './pages/enrollments/enrollment-list/enrollment-list';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'filieres', component: Filieres },
  { path: 'contact', component: Contact },
  { path: 'login', component: Login },

  { path: 'dashboard', component: Dashboard, canActivate: [authGuard] },

  // ─── Enseignants ──────────────────────────────────────────────────────────
  { path: 'teachers/new', component: TeacherForm, canActivate: [authGuard] },
  { path: 'teachers/:id/edit', component: TeacherEdit, canActivate: [authGuard] },
  { path: 'teachers/:id', component: TeacherProfile, canActivate: [authGuard] },
  { path: 'teachers', component: TeacherList, canActivate: [authGuard] },

  // ─── Étudiants ────────────────────────────────────────────────────────────
  { path: 'students/new', component: StudentForm, canActivate: [authGuard] },
  { path: 'students/:id/edit', component: StudentForm, canActivate: [authGuard] },
  { path: 'students', component: StudentList, canActivate: [authGuard] },

  // ─── Salles & Groupes ─────────────────────────────────────────────────────
  { path: 'salles/new', component: SalleForm, canActivate: [authGuard] },
  { path: 'salles/:id/edit', component: SalleForm, canActivate: [authGuard] },
  { path: 'salles', component: SalleList, canActivate: [authGuard] },
  { path: 'groupes/new', component: GroupeForm, canActivate: [authGuard] },
  { path: 'groupes/:id/edit', component: GroupeForm, canActivate: [authGuard] },
  { path: 'groupes', component: GroupeList, canActivate: [authGuard] },

  // PGDI2026-60 (US26) : inscriptions pédagogiques
  { path: 'inscriptions', component: EnrollmentList, canActivate: [authGuard] },

  // ─── Admin : Gestion des utilisateurs [PGDI2026-50] ──────────────────────
  { path: 'admin/users', component: UserList, canActivate: [authGuard] },
  { path: 'profile', component: UserProfile, canActivate: [authGuard] },

  { path: '**', redirectTo: '' }
];
