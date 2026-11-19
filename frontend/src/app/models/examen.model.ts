// PGDI2026-75 (US49) : sessions d'examens — même forme que ExamenResponseDTO côté Spring Boot
export type ExamType = 'CC' | 'TP' | 'FINAL' | 'RATTRAPAGE';

export const EXAM_TYPES: ExamType[] = ['CC', 'TP', 'FINAL', 'RATTRAPAGE'];

export const EXAM_TYPE_LABELS: Record<ExamType, string> = {
  CC: 'Contrôle continu',
  TP: 'Évaluation de TP',
  FINAL: 'Examen final',
  RATTRAPAGE: 'Rattrapage'
};

export interface Examen {
  id: number;
  title: string;
  examType: ExamType;
  examDate: string;
  startTime: string;
  endTime: string;
  notes?: string;
  moduleId: number;
  moduleCode: string;
  moduleNom: string;
  groupeId: number;
  groupeName: string;
  groupeCapacity?: number;
  filiereCode: string;
  academicYearId: number;
  academicYearLabel: string;
  salleIds: number[];
  salleCodes: string[];
  totalCapacity: number;
}

export interface ExamenRequest {
  title: string;
  examType: ExamType;
  moduleId: number;
  groupeId: number;
  academicYearId?: number;
  salleIds: number[];
  examDate: string;
  startTime: string;
  endTime: string;
  notes?: string;
}
