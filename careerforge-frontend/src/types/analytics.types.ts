import { JobStatus, WorkMode, JobType, ExperienceLevel } from './job.types';
import { CompanyVerificationStatus } from './company.types';
import { Role } from './auth.types';

export interface PlatformOverviewResponse {
  totalUsers: number;
  totalStudents: number;
  totalRecruiters: number;
  totalAdmins: number;
  activeEnabledUsers: number;
  disabledUsers: number;
  totalCompanies: number;
  verifiedCompanies: number;
  pendingCompanies: number;
  rejectedCompanies: number;
  totalJobs: number;
  publishedJobs: number;
  draftJobs: number;
  closedJobs: number;
  archivedJobs: number;
  totalApplications: number;
  activeApplications: number;
  acceptedApplications: number;
  rejectedApplications: number;
  withdrawnApplications: number;
}

export interface ApplicationFunnelResponse {
  totalApplications: number;
  appliedCount: number;
  underReviewCount: number;
  shortlistedCount: number;
  interviewScheduledCount: number;
  acceptedCount: number;
  rejectedCount: number;
  withdrawnCount: number;
  activeInPipelinePercentage: number;
  interviewRatePercentage: number;
  acceptanceRatePercentage: number;
  rejectionRatePercentage: number;
  withdrawalRatePercentage: number;
}

export interface JobAnalyticsResponse {
  totalJobs: number;
  jobsByStatus: Record<JobStatus, number>;
  jobsByWorkMode: Record<WorkMode, number>;
  jobsByJobType: Record<JobType, number>;
  jobsByExperienceLevel: Record<ExperienceLevel, number>;
}

export interface CompanyAnalyticsResponse {
  totalCompanies: number;
  companiesByVerificationStatus: Record<CompanyVerificationStatus, number>;
  companiesBySize: Record<string, number>;
  totalRecruiterProfiles: number;
  averageRecruitersPerCompany: number;
}

export interface UserAnalyticsResponse {
  totalUsers: number;
  usersByRole: Record<Role, number>;
  enabledUsers: number;
  disabledUsers: number;
  totalStudentProfiles: number;
  totalResumesUploaded: number;
}

export interface DailyMetricDto {
  date: string;
  count: number;
}

export interface PlatformTrendsResponse {
  windowDays: number;
  userRegistrations: DailyMetricDto[];
  jobPostings: DailyMetricDto[];
  applicationSubmissions: DailyMetricDto[];
}
