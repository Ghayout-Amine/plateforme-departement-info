export interface UserCreate {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
  role: 'ENSEIGNANT';
}

export interface UserResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  role: string;
  enabled: boolean;
  createdAt?: string;
}
