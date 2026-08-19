package com.careerforge.dto.response.analytics;

import com.careerforge.entity.enums.CompanyVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyAnalyticsResponse {

    private long totalCompanies;
    private Map<CompanyVerificationStatus, Long> companiesByVerificationStatus;
    private Map<String, Long> companiesBySize;
    private long totalRecruiterProfiles;
    private double averageRecruitersPerCompany;
}
