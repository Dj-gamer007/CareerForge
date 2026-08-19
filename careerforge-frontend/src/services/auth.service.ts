import { apiClient } from '@/lib/axios';
import { ApiResponse } from '@/types/api.types';
import { JwtResponse, UserProfileResponse, Role } from '@/types/auth.types';

export interface LoginPayload {
  email: string;
  passwordHash: string; // backend accepts 'password' or 'passwordHash' as per AuthRequest
}

export interface RegisterPayload {
  email: string;
  password: string;
  role: Role;
}

export const authService = {
  async login(payload: { email: string; password: string }): Promise<JwtResponse> {
    const res = await apiClient.post<ApiResponse<JwtResponse>>('/auth/login', {
      email: payload.email,
      password: payload.password,
    });
    return res.data.data;
  },

  async register(payload: RegisterPayload): Promise<JwtResponse> {
    const res = await apiClient.post<ApiResponse<JwtResponse>>('/auth/register', payload);
    return res.data.data;
  },

  async getMe(): Promise<UserProfileResponse> {
    const res = await apiClient.get<ApiResponse<UserProfileResponse>>('/auth/me');
    return res.data.data;
  },

  async refresh(refreshToken: string): Promise<JwtResponse> {
    const res = await apiClient.post<ApiResponse<JwtResponse>>('/auth/refresh', { refreshToken });
    return res.data.data;
  },

  async logout(refreshToken: string): Promise<void> {
    await apiClient.post('/auth/logout', { refreshToken });
  },
};
