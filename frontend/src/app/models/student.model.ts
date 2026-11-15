export type Gender = 'MALE' | 'FEMALE';

export interface Student {
  id: number;
  studentNumber: string;
  cne?: string;
  birthDate?: string;
  birthPlace?: string;
  gender?: Gender;
  address?: string;
  city?: string;
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  enabled: boolean;
  createdAt?: string;
}

// POST /api/students : crée l'étudiant et son compte utilisateur
export interface StudentCreate {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
  studentNumber: string;
  cne?: string;
  birthDate?: string;
  birthPlace?: string;
  gender?: Gender;
  address?: string;
  city?: string;
}

// PUT /api/students/{id} : l'email et le mot de passe ne sont pas modifiables ici
export interface StudentUpdate {
  firstName: string;
  lastName: string;
  phone?: string;
  studentNumber: string;
  cne?: string;
  birthDate?: string;
  birthPlace?: string;
  gender?: Gender;
  address?: string;
  city?: string;
  enabled?: boolean;
}

// Format de la réponse paginée renvoyée par GET /api/students
export interface StudentPage {
  content: Student[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}
