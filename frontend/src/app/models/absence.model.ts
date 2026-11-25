export interface AbsenceCreate {
  studentId: number;
  moduleId: number;
  date: string;
  sessionTime: string;
  justified: boolean;
  motif: string | null;
}

export type AbsenceStatus = 'UNJUSTIFIED' | 'PENDING_JUSTIFICATION' | 'JUSTIFIED';

export interface Absence {
  id: number;
  studentId: number;
  moduleId: number;
  date: string;
  sessionTime: string;
  justified: boolean;
  motif: string | null;
  status?: AbsenceStatus;
}

export interface AbsenceJustification {
  motif: string;
}