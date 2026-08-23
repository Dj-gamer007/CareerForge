import { ProficiencyLevel } from './student.types';

export type JobStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'ARCHIVED';
export type WorkMode = 'ONSITE' | 'REMOTE' | 'HYBRID';
export type JobType = 'FULL_TIME' | 'PART_TIME' | 'CONTRACT' | 'INTERNSHIP';
export type ExperienceLevel = 'ENTRY_LEVEL' | 'MID_LEVEL' | 'SENIOR_LEVEL' | 'EXECUTIVE';

export interface JobSkillResponse {
  id: number;
  skillId: number;
  skillName: string;
  category: string;
  isRequired: boolean;
  required?: boolean;
  minimumProficiency: ProficiencyLevel;
}

export interface JobSummaryResponse {
  id: number;
  title: string;
  slug: string;
  companyId: number;
  companyName: string;
  companySlug: string;
  companyLogoUrl?: string;
  location?: string;
  workMode: WorkMode;
  jobType: JobType;
  experienceLevel: ExperienceLevel;
  salaryMin?: number;
  salaryMax?: number;
  currency: string;
  status: JobStatus;
  deadline?: string;
  publishedAt?: string;
  createdAt: string;
  skills: JobSkillResponse[];
}

export interface JobDetailResponse extends JobSummaryResponse {
  description: string;
  recruiterId?: number;
  recruiterName?: string;
  updatedAt: string;
}

export interface MatchedSkillDto {
  skillId: number;
  skillName: string;
  category?: string;
  isRequired: boolean;
  requiredProficiency: ProficiencyLevel;
  studentProficiency: ProficiencyLevel;
  proficiencyMultiplier: number;
  skillWeight?: number;
  effectiveScoreContribution?: number;
}

export interface MissingSkillDto {
  skillId: number;
  skillName: string;
  category?: string;
  isRequired: boolean;
  requiredProficiency: ProficiencyLevel;
}

export interface SkillMatchResponse {
  overallScore: number;
  matchedRequiredCount: number;
  totalRequiredCount: number;
  matchedOptionalCount: number;
  totalOptionalCount: number;
  totalJobSkillsCount?: number;
  totalStudentSkillsCount?: number;
  isEligible: boolean;
  matchedSkills: MatchedSkillDto[];
  missingRequiredSkills: MissingSkillDto[];
  missingOptionalSkills: MissingSkillDto[];
}

export interface SavedJobResponse {
  id: number;
  jobId: number;
  jobTitle: string;
  jobSlug: string;
  companyId: number;
  companyName: string;
  companyLogoUrl?: string;
  location?: string;
  workMode: WorkMode;
  jobType: JobType;
  experienceLevel: ExperienceLevel;
  salaryMin?: number;
  salaryMax?: number;
  currency?: string;
  status: JobStatus;
  deadline?: string;
  savedAt: string;
}

