export type ProficiencyLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | 'EXPERT';

export interface SkillItem {
  id: number;
  name: string;
  category: string;
}

export interface StudentSkillResponse {
  id: number;
  skillId: number;
  skillName: string;
  category: string;
  proficiency: ProficiencyLevel;
}

export interface EducationResponse {
  id: number;
  institution: string;
  degree: string;
  fieldOfStudy: string;
  startDate: string;
  endDate?: string;
  current: boolean;
  grade?: string;
  description?: string;
}

export interface ProjectResponse {
  id: number;
  title: string;
  description?: string;
  projectUrl?: string;
  githubUrl?: string;
  technologies?: string;
  startDate?: string;
  endDate?: string;
}

export interface CertificationResponse {
  id: number;
  name: string;
  issuingOrganization: string;
  issueDate?: string;
  expirationDate?: string;
  credentialId?: string;
  credentialUrl?: string;
}

export interface ResumeResponse {
  id: number;
  originalFileName: string;
  fileSize: number;
  contentType: string;
  version: number;
  isActive: boolean;
  uploadedAt: string;
}

export interface StudentProfileResponse {
  id: number;
  userId: number;
  email: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
  location?: string;
  bio?: string;
  educationSummary?: string;
  githubUrl?: string;
  linkedinUrl?: string;
  portfolioUrl?: string;
  profileCompletionPercentage: number;
  createdAt: string;
  updatedAt: string;
  skills: StudentSkillResponse[];
  education: EducationResponse[];
  projects: ProjectResponse[];
  certifications: CertificationResponse[];
  resumes: ResumeResponse[];
}
