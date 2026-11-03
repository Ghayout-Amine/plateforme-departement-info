import { UserRole } from './auth.model';

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  role: UserRole;
  enabled: boolean;
  createdAt?: string;
}

export interface UserCreatePayload {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
  role?: UserRole;
}

export interface UserUpdatePayload {
  firstName: string;
  lastName: string;
  phone?: string;
  role?: UserRole;
  enabled?: boolean;
}
