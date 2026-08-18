package com.careerforge.service;

import com.careerforge.dto.request.CompanyCreateRequest;
import com.careerforge.dto.request.CompanyUpdateRequest;
import com.careerforge.dto.response.CompanyResponse;
import com.careerforge.dto.response.CompanySummaryResponse;
import com.careerforge.dto.response.PagedResponse;
import com.careerforge.entity.Company;
import org.springframework.data.domain.Pageable;

public interface CompanyService {

    CompanyResponse createCompany(Long userId, CompanyCreateRequest request);

    CompanyResponse getMyCompany(Long userId);

    CompanyResponse updateMyCompany(Long userId, CompanyUpdateRequest request);

    CompanyResponse getCompanyById(Long id);

    CompanyResponse getCompanyBySlug(String slug);

    PagedResponse<CompanySummaryResponse> getVerifiedCompanies(String search, Pageable pageable);

    Company getCompanyEntityById(Long id);
}
