export type GradeType =
  | 'CONTROLE_CONTINU'
  | 'EXAMEN_FINAL'
  | 'TP'
  | 'RATTRAPAGE';

export type GradeStatus = 'DRAFT' | 'PUBLISHED' | 'ABSENT';

export interface GradeCreate {
  studentId: number;
  moduleId: number;
  score: number;
  type: GradeType;
  coefficient: number;
  sessionDate: string;
}

export interface Grade {
  id: number;
  studentId: number;
  moduleId: number;
  score: number;
  type: GradeType;
  coefficient: number;
  sessionDate: string;
  status: GradeStatus;
}

export interface GradeAverage {
  studentId: number;
  moduleId: number;
  academicYearId: number;
  average: number | null;
}

export interface ModuleResult {
  code: string;
  name: string;
  score: number;
  status: 'VALIDE' | 'VALIDE_PAR_COMPENSATION' | 'NON_VALIDE' | string;
}

export interface ReleveNotes {
  studentName: string;
  studentNumber: string;
  semester: string;
  modules: ModuleResult[];
  semesterAverage: number;
  decision: string;
  mention: string | null;
}