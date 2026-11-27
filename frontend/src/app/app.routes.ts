import { GradeEntry } from './pages/grades/grade-entry';
import { AbsenceManagement } from './pages/absences/absence-management';
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
  // PGDI2026-88 / 89 : annonces officielles et veille technologique (page publique)
  { path: 'actualites',
    loadComponent: () => import('./pages/actualites/actualites').then(m => m.Actualites) },
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

  // PGDI2026-70 / PGDI2026-71 : notes et absences
  { path: 'grades', component: GradeEntry, canActivate: [authGuard] },
  { path: 'absences', component: AbsenceManagement, canActivate: [authGuard] },

  // PGDI2026-75 (US36, US49) : emploi du temps et calendrier des examens
  // Chargement à la demande (lazy loading) : ces pages ne grossissent pas le bundle initial
  { path: 'emploi-du-temps/seances/new', canActivate: [authGuard],
    loadComponent: () => import('./pages/emploi-du-temps/seance-form/seance-form').then(m => m.SeanceForm) },
  { path: 'emploi-du-temps/seances/:id/edit', canActivate: [authGuard],
    loadComponent: () => import('./pages/emploi-du-temps/seance-form/seance-form').then(m => m.SeanceForm) },
  { path: 'emploi-du-temps', canActivate: [authGuard],
    loadComponent: () => import('./pages/emploi-du-temps/emploi-du-temps').then(m => m.EmploiDuTemps) },
  { path: 'examens/new', canActivate: [authGuard],
    loadComponent: () => import('./pages/examens/examen-form/examen-form').then(m => m.ExamenForm) },
  { path: 'examens/:id/edit', canActivate: [authGuard],
    loadComponent: () => import('./pages/examens/examen-form/examen-form').then(m => m.ExamenForm) },
  { path: 'examens', canActivate: [authGuard],
    loadComponent: () => import('./pages/examens/examen-list/examen-list').then(m => m.ExamenList) },
  // PGDI2026-80 (US66/US21) : stages et conventions PFE (chargement à la demande)
  { path: 'stages/validation', canActivate: [authGuard],
    loadComponent: () => import('./pages/stages/stages-validation/stages-validation').then(m => m.StagesValidation) },
  // PGDI2026-93 : recherche des offres de stage externes (Jooble / JSearch, PGDI2026-86)
  { path: 'stages/offres', canActivate: [authGuard],
    loadComponent: () => import('./pages/stages/offres-externes/offres-externes').then(m => m.OffresExternes) },
  { path: 'stages', canActivate: [authGuard],
    loadComponent: () => import('./pages/stages/mes-stages/mes-stages').then(m => m.MesStages) },
  { path: '**', redirectTo: '' }
];
