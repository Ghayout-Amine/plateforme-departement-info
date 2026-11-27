// PGDI2026-80 (US66/US21) : stages / PFE et conventions
export type InternshipType = 'INTERNSHIP' | 'PFE';

export type InternshipStatus =
  | 'PENDING_VALIDATION'
  | 'VALIDATED'
  | 'REJECTED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED';

export interface Internship {
  id: number;
  subject: string;
  internshipType: InternshipType;
  companyName: string;
  companyCity: string | null;
  companyCountry: string | null;
  startDate: string;
  endDate: string;
  status: InternshipStatus;
  companyTutorName: string | null;
  companyTutorEmail: string | null;
  companyTutorPhone: string | null;
  declaredAt: string;
  validationComment: string | null;
  validatedAt: string | null;
  studentId: number;
  studentName: string;
  studentNumber: string;
  supervisorId: number | null;
  supervisorName: string | null;
}

export interface InternshipRequest {
  subject: string;
  internshipType: InternshipType;
  companyName: string;
  companyCity?: string;
  companyCountry?: string;
  startDate: string;
  endDate: string;
  companyTutorName?: string;
  companyTutorEmail?: string;
  companyTutorPhone?: string;
}

export interface InternshipValidation {
  approved: boolean;
  comment?: string;
}

export const INTERNSHIP_TYPE_LABELS: Record<InternshipType, string> = {
  INTERNSHIP: 'Stage',
  PFE: 'PFE',
};

export const INTERNSHIP_STATUS_LABELS: Record<InternshipStatus, string> = {
  PENDING_VALIDATION: 'En attente de validation',
  VALIDATED: 'Convention validée',
  REJECTED: 'Convention refusée',
  IN_PROGRESS: 'En cours',
  COMPLETED: 'Terminé',
  CANCELLED: 'Annulé',
};
