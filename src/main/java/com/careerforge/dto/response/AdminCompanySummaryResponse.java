package com.careerforge.dto.response;

import com.careerforge.entity.enums.CompanyVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCompanySummaryResponse {

    private Long id;
    private String name;
    private String slug;
    private String industry;
    private String location;
    private String companySize;
    private String website;
    private String logoUrl;
    private CompanyVerificationStatus verificationStatus;
    private long totalJobsCount;
    private long activeJobsCount;
    private int recruitersCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
