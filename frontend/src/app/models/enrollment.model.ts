// PGDI2026-60 (US26) : inscription pédagogique — même forme que EnrollmentResponseDTO

export type EnrollmentStatus = 'ACTIVE' | 'VALIDATED' | 'REPEATING' | 'SUSPENDED' | 'WITHDRAWN';

export const ENROLLMENT_STATUS_LABELS: Record<EnrollmentStatus, string> = {
  ACTIVE: 'Inscrit',
  VALIDATED: 'Année validée',
  REPEATING: 'Redoublant',
  SUSPENDED: 'Suspendu',
  WITHDRAWN: 'Abandon'
};

export interface Enrollment {
  id: number;
  status: EnrollmentStatus;
  studentId: number;
  studentNumber: string;
  studentFirstName: string;
  studentLastName: string;
  filiereId: number;
  filiereCode: string;
  filiereNom: string;
  academicYearId: number;
  academicYearLabel: string;
  groupeId?: number | null;
  groupeName?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

// POST /api/enrollments et PUT /api/enrollments/{id}
export interface EnrollmentRequest {
  studentId: number;
  filiereId: number;
  academicYearId: number;
  groupeId?: number | null;
  status?: EnrollmentStatus;
}

export interface AcademicYear {
  id: number;
  label: string;
  startDate: string;
  endDate: string;
  current: boolean;
}

export interface AcademicYearRequest {
  label: string;
  startDate: string;
  endDate: string;
  current: boolean;
}
