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
public class CompanyResponse {

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
