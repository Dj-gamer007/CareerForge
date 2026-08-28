import { apiClient } from '@/lib/axios';
import { ApiResponse, SpringPage } from '@/types/api.types';
import {
  RecruiterProfileResponse,
  CompanyDetailResponse,
  CompanySummaryResponse,
} from '@/types/company.types';

export const recruiterService = {
  // Recruiter Profile
  async getProfile(): Promise<RecruiterProfileResponse> {
    const res = await apiClient.get<ApiResponse<RecruiterProfileResponse>>('/recruiters/profile');
    return res.data.data;
  },

  async createProfile(payload: {
    firstName: string;
    lastName: string;
    designation: string;
    department?: string;
    phone?: string;
  }): Promise<RecruiterProfileResponse> {
    const res = await apiClient.post<ApiResponse<RecruiterProfileResponse>>('/recruiters/profile', payload);
    return res.data.data;
  },

  async updateProfile(payload: {
    firstName?: string;
    lastName?: string;
    designation?: string;
    department?: string;
    phone?: string;
  }): Promise<RecruiterProfileResponse> {
    const res = await apiClient.put<ApiResponse<RecruiterProfileResponse>>('/recruiters/profile', payload);
    return res.data.data;
  },

  // Company
  async registerCompany(payload: {
    name: string;
    website?: string;
    logoUrl?: string;
    description?: string;
    industry?: string;
    companySize?: string;
    location?: string;
  }): Promise<CompanyDetailResponse> {
    const res = await apiClient.post<ApiResponse<CompanyDetailResponse>>('/companies', payload);
    return res.data.data;
  },

  async getMyCompany(): Promise<CompanyDetailResponse | null> {
    const res = await apiClient.get<ApiResponse<CompanyDetailResponse | null>>('/companies/my-company');
    return res.data.data ?? null;
  },

  async updateMyCompany(payload: {
    name?: string;
    website?: string;
    logoUrl?: string;
    description?: string;
    industry?: string;
    companySize?: string;
    location?: string;
  }): Promise<CompanyDetailResponse> {
    const res = await apiClient.put<ApiResponse<CompanyDetailResponse>>('/companies/my-company', payload);
    return res.data.data;
  },

  // Public Companies
  async getCompanies(params?: { search?: string; name?: string; page?: number; size?: number }): Promise<SpringPage<CompanySummaryResponse>> {
    const searchParam = params?.search || params?.name;
    const queryParams = {
      ...params,
      search: searchParam || undefined,
      name: searchParam || undefined,
    };
    const res = await apiClient.get<ApiResponse<SpringPage<CompanySummaryResponse>>>('/companies', { params: queryParams });
    return res.data.data;
  },

  async getCompanyById(id: number): Promise<CompanyDetailResponse> {
    const res = await apiClient.get<ApiResponse<CompanyDetailResponse>>(`/companies/${id}`);
    return res.data.data;
  },

  async getCompanyBySlug(slug: string): Promise<CompanyDetailResponse> {
    const res = await apiClient.get<ApiResponse<CompanyDetailResponse>>(`/companies/slug/${slug}`);
    return res.data.data;
  },
};
