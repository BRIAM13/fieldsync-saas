export type UserRole = 'ADMIN' | 'DISPATCHER' | 'TECHNICIAN';

export interface Company {
  id: string;
  name: string;
}

export interface User {
  id: string;
  companyId: string;
  email: string;
  name: string;
  role: UserRole;
}

export interface AuthResponse {
  token: string;
  user: User;
  company: Company;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  companyName: string;
  name: string;
  email: string;
  password: string;
}
