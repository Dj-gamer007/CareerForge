import { apiClient } from '@/lib/axios';
import { ApiResponse, SpringPage } from '@/types/api.types';
import {
  JobSummaryResponse,
  JobDetailResponse,
  SkillMatchResponse,
  JobStatus,
  WorkMode,
  JobType,
  ExperienceLevel,
} from '@/types/job.types';

export interface SearchJobParams {
  keyword?: string;
  location?: string;
  workMode?: WorkMode;
  jobType?: JobType;
  experienceLevel?: ExperienceLevel;
  minSalary?: number;
  maxSalary?: number;
  companyId?: number;
  skills?: string[];
  page?: number;
  size?: number;
  sort?: string;
}

export interface CreateJobPayload {
  title: string;
  description: string;
  location?: string;
  workMode: WorkMode;
  jobType: JobType;
  experienceLevel: ExperienceLevel;
  salaryMin?: number;
  salaryMax?: number;
  currency?: string;
  deadline?: string;
  skills: Array<{
    skillId: number;
    required: boolean;
    minimumProficiency: string;
  }>;
}

export const jobService = {
  // Public Jobs
  async searchJobs(params?: SearchJobParams): Promise<SpringPage<JobSummaryResponse>> {
    const res = await apiClient.get<ApiResponse<SpringPage<JobSummaryResponse>>>('/jobs', {
      params,
    });
    return res.data.data;
  },

  async getJobById(id: number): Promise<JobDetailResponse> {
    const res = await apiClient.get<ApiResponse<JobDetailResponse>>(`/jobs/${id}`);
    return res.data.data;
  },

  async getJobBySlug(slug: string): Promise<JobDetailResponse> {
    const res = await apiClient.get<ApiResponse<JobDetailResponse>>(`/jobs/slug/${slug}`);
    return res.data.data;
  },

  // Recruiter Jobs
  async getRecruiterJobs(params?: { status?: JobStatus; page?: number; size?: number }): Promise<SpringPage<JobSummaryResponse>> {
    const res = await apiClient.get<ApiResponse<SpringPage<JobSummaryResponse>>>('/recruiters/jobs', {
      params,
    });
    return res.data.data;
  },

  async getRecruiterJobById(id: number): Promise<JobDetailResponse> {
    const res = await apiClient.get<ApiResponse<JobDetailResponse>>(`/recruiters/jobs/${id}`);
    return res.data.data;
  },

  async createJob(payload: CreateJobPayload): Promise<JobDetailResponse> {
    const res = await apiClient.post<ApiResponse<JobDetailResponse>>('/recruiters/jobs', payload);
    return res.data.data;
  },

  async updateJob(id: number, payload: Partial<CreateJobPayload>): Promise<JobDetailResponse> {
    const res = await apiClient.put<ApiResponse<JobDetailResponse>>(`/recruiters/jobs/${id}`, payload);
    return res.data.data;
  },

  async publishJob(id: number): Promise<JobDetailResponse> {
    const res = await apiClient.patch<ApiResponse<JobDetailResponse>>(`/recruiters/jobs/${id}/publish`);
    return res.data.data;
  },

  async unpublishJob(id: number): Promise<JobDetailResponse> {
    const res = await apiClient.patch<ApiResponse<JobDetailResponse>>(`/recruiters/jobs/${id}/unpublish`);
    return res.data.data;
  },

  async closeJob(id: number): Promise<JobDetailResponse> {
    const res = await apiClient.patch<ApiResponse<JobDetailResponse>>(`/recruiters/jobs/${id}/close`);
    return res.data.data;
  },

  async reopenJob(id: number): Promise<JobDetailResponse> {
    const res = await apiClient.patch<ApiResponse<JobDetailResponse>>(`/recruiters/jobs/${id}/reopen`);
    return res.data.data;
  },

  async archiveJob(id: number): Promise<JobDetailResponse> {
    const res = await apiClient.patch<ApiResponse<JobDetailResponse>>(`/recruiters/jobs/${id}/archive`);
    return res.data.data;
  },

  async deleteJob(id: number): Promise<void> {
    await apiClient.delete(`/recruiters/jobs/${id}`);
  },

  // Skill Matching Preview
  async getMatchPreview(jobId: number): Promise<SkillMatchResponse> {
    const res = await apiClient.get<ApiResponse<SkillMatchResponse>>(`/students/jobs/${jobId}/match-preview`);
    return res.data.data;
  },
};
