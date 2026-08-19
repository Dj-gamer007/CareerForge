package com.careerforge.dto.response;

import com.careerforge.entity.enums.CompanyVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCompanyDetailResponse {

    private Long id;
    private String name;
    private String slug;
    private String website;
    private String logoUrl;
    private String description;
    private String industry;
    private String companySize;
    private String location;
    private CompanyVerificationStatus verificationStatus;
    private long totalJobsCount;
    private long activeJobsCount;
    private List<CompanyRecruiterSummaryDto> recruiters;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanyRecruiterSummaryDto {
        private Long recruiterId;
        private Long userId;
        private String email;
        private String firstName;
        private String lastName;
        private String designation;
        private String department;
        private String phone;
        private boolean isCompanyAdmin;
    }
}
