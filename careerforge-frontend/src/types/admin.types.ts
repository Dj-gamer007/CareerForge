import { Role } from './auth.types';
import { CompanyVerificationStatus } from './company.types';
import { JobStatus, WorkMode, JobType, ExperienceLevel, JobSkillResponse } from './job.types';
import { StudentProfileResponse } from './student.types';
import { RecruiterProfileResponse } from './company.types';

export interface AdminUserSummaryResponse {
  id: number;
  email: string;
  role: Role;
  enabled: boolean;
  fullName?: string;
  profileType?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AdminUserDetailResponse {
  id: number;
  email: string;
  role: Role;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
  studentProfile?: StudentProfileResponse;
  recruiterProfile?: RecruiterProfileResponse;
}

export interface AdminCompanySummaryResponse {
  id: number;
  name: string;
  slug: string;
  industry?: string;
  companySize?: string;
  location?: string;
  logoUrl?: string;
  verificationStatus: CompanyVerificationStatus;
  recruiterCount: number;
  jobCount: number;
  createdAt: string;
}

export interface AdminCompanyDetailResponse {
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
  recruiters: RecruiterProfileResponse[];
  jobCount: number;
}

export interface AdminJobSummaryResponse {
  id: number;
  title: string;
  slug: string;
  companyId: number;
  companyName: string;
  companySlug: string;
  companyVerificationStatus: string;
  recruiterName?: string;
  location?: string;
  workMode: WorkMode;
  jobType: JobType;
  experienceLevel: ExperienceLevel;
  status: JobStatus;
  applicationsCount: number;
  deadline?: string;
  publishedAt?: string;
  createdAt: string;
}

export interface AdminJobDetailResponse extends AdminJobSummaryResponse {
  description: string;
  recruiterId?: number;
  recruiterEmail?: string;
  salaryMin?: number;
  salaryMax?: number;
  currency: string;
  updatedAt: string;
  skills: JobSkillResponse[];
}
