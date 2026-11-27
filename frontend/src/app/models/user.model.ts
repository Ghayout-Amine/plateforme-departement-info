export type UserRole = 'ADMIN' | 'CHEF_DEPARTEMENT' | 'ENSEIGNANT' | 'ETUDIANT';

export interface UserResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  role: UserRole;
  enabled: boolean;
  avatarUrl?: string;
  createdAt?: string;
  lastLoginAt?: string;
}

export interface UserCreate {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
  role: UserRole;
}

export interface UserUpdate {
  firstName: string;
  lastName: string;
  phone?: string;
  role?: UserRole;
  enabled?: boolean;
}

export interface UpdateProfile {
  firstName: string;
  lastName: string;
  phone?: string;
  avatarUrl?: string;
}

export interface ChangePassword {
  oldPassword: string;
  newPassword: string;
}

export interface UserPage {
  users: UserResponse[];
  currentPage: number;
  totalItems: number;
  totalPages: number;
}
