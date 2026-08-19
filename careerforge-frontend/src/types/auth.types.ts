export type Role = 'ROLE_STUDENT' | 'ROLE_RECRUITER' | 'ROLE_ADMIN';

export interface UserSummary {
  id: number;
  email: string;
  role: Role;
}

export interface JwtResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserSummary;
}

export interface UserProfileResponse {
  id: number;
  email: string;
  role: Role;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}
