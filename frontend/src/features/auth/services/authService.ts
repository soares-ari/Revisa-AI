import { api } from '@/lib/axios';
import type { AuthResponse, LoginRequest, RegisterRequest } from '../types';

export const authService = {
  login: (data: LoginRequest) =>
    api.post<AuthResponse>('/auth/login', data).then((r) => r.data),
  register: (data: RegisterRequest) =>
    api.post('/auth/register', data),
  refresh: () =>
    api.post<AuthResponse>('/auth/refresh').then((r) => r.data),
};
