export type UserRole = 'ADMIN' | 'CHEF_DEPARTEMENT' | 'ENSEIGNANT' | 'ETUDIANT';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  role: UserRole;
  email: string;
  firstName: string;
  lastName: string;
}
