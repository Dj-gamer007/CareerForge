import { ResumeResponse } from './student.types';
import { JobSummaryResponse } from './job.types';

export type ApplicationStatus =
  | 'APPLIED'
  | 'UNDER_REVIEW'
  | 'SHORTLISTED'
  | 'INTERVIEW_SCHEDULED'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'WITHDRAWN';

export interface StudentApplicationResponse {
  id: number;
  jobId: number;
  jobTitle: string;
  jobSlug: string;
  companyId: number;
  companyName: string;
  companySlug: string;
  status: ApplicationStatus;
  matchScoreAtApplication: number;
  appliedAt: string;
  interviewScheduledAt?: string;
  reviewedAt?: string;
  withdrawnAt?: string;
}

export interface StudentApplicationDetailResponse extends StudentApplicationResponse {
  coverLetter?: string;
  resume: ResumeResponse;
  job: JobSummaryResponse;
}

export interface RecruiterApplicationResponse {
  id: number;
  jobId: number;
  jobTitle: string;
  studentProfileId: number;
  candidateName: string;
  candidateEmail: string;
  status: ApplicationStatus;
  matchScoreAtApplication: number;
  appliedAt: string;
  interviewScheduledAt?: string;
  reviewedAt?: string;
  withdrawnAt?: string;
  hasResume: boolean;
}

export interface RecruiterApplicationDetailResponse extends RecruiterApplicationResponse {
  coverLetter?: string;
  recruiterNotes?: string;
  candidatePhone?: string;
  candidateLocation?: string;
  resumeId?: number;
  resumeFileName?: string;
}
