export interface Teacher {
  id: number;
  employeeNumber: string;
  academicTitle?: string;
  specialty?: string;
  office?: string;
  hireDate?: string;
  bio?: string;
  userId: number;
  userEmail: string;
  firstName: string;
  lastName: string;
  createdAt?: string;
}

export interface TeacherRequest {
  employeeNumber: string;
  academicTitle?: string;
  specialty?: string;
  office?: string;
  hireDate?: string;
  bio?: string;
  userId: number;
}
