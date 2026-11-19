// PGDI2026-75 (US36) : séances de l'emploi du temps — même forme que PlanningResponseDTO côté Spring Boot
export type SessionType = 'CM' | 'TD' | 'TP';
export type PlanningStatus = 'PLANNED' | 'CANCELLED';

export const SESSION_TYPES: SessionType[] = ['CM', 'TD', 'TP'];

export const SESSION_TYPE_LABELS: Record<SessionType, string> = {
  CM: 'Cours magistral',
  TD: 'Travaux dirigés',
  TP: 'Travaux pratiques'
};

export interface Planning {
  id: number;
  sessionType: SessionType;
  status: PlanningStatus;
  sessionDate: string;   // AAAA-MM-JJ
  dayOfWeek: string;     // MONDAY ... SUNDAY
  startTime: string;     // HH:mm:ss
  endTime: string;
  notes?: string;
  teacherId: number;
  teacherName: string;
  moduleId: number;
  moduleCode: string;
  moduleNom: string;
  groupeId: number;
  groupeName: string;
  filiereCode: string;
  salleId: number;
  salleCode: string;
  salleName: string;
}

export interface PlanningRequest {
  teacherId: number;
  moduleId: number;
  groupeId: number;
  salleId: number;
  sessionType: SessionType;
  sessionDate: string;
  startTime: string;     // HH:mm
  endTime: string;
  notes?: string;
  weeks?: number;        // POST /api/plannings/serie
}

// Filtre de l'emploi du temps : une ressource (groupe, enseignant ou salle) ou "moi"
export type PlanningView = 'me' | 'groupe' | 'teacher' | 'salle';
