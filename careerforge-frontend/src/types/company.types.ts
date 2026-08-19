export type CompanyVerificationStatus = 'PENDING' | 'VERIFIED' | 'REJECTED';

export interface CompanySummaryResponse {
  id: number;
  name: string;
  slug: string;
  industry?: string;
  companySize?: string;
  location?: string;
  logoUrl?: string;
  verificationStatus: CompanyVerificationStatus;
}

export interface CompanyDetailResponse {
  id: number;
  name: string;
  slug: string;
  description?: string;
  website?: string;
  logoUrl?: string;
  industry?: string;
  companySize?: string;
  location?: string;
  verificationStatus: CompanyVerificationStatus;
  createdAt: string;
  updatedAt: string;
}

export interface RecruiterProfileResponse {
  id: number;
  userId: number;
  email: string;
  firstName?: string;
  lastName?: string;
  designation?: string;
  department?: string;
  phone?: string;
  isCompanyAdmin: boolean;
  company?: CompanyDetailResponse;
  createdAt: string;
  updatedAt: string;
}
