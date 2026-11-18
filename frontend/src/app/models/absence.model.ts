export interface AbsenceCreate {
  studentId: number;
  moduleId: number;
  date: string;
  sessionTime: string;
  justified: boolean;
  motif: string | null;
}

export interface Absence {
  id: number;
  studentId: number;
  moduleId: number;
  date: string;
  sessionTime: string;
  justified: boolean;
  motif: string | null;
}

export interface AbsenceJustification {
  motif: string;
}