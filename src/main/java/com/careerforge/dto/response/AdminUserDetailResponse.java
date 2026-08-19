package com.careerforge.dto.response;

import com.careerforge.entity.enums.Role;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class AdminUserDetailResponse {

    private Long id;
    private String email;
    private Role role;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private StudentProfileSummaryDto studentProfile;
    private RecruiterProfileSummaryDto recruiterProfile;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentProfileSummaryDto {
        private Long id;
        private String firstName;
        private String lastName;
        private String phone;
        private String location;
        private String bio;
        private String educationSummary;
        private String githubUrl;
        private String linkedinUrl;
        private String portfolioUrl;
        private Integer profileCompletionPercentage;
        private int totalSkills;
        private int totalEducations;
        private int totalProjects;
        private int totalCertifications;
        private int totalResumes;
        private Long activeResumeId;
        private List<String> skills;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecruiterProfileSummaryDto {
        private Long id;
        private String firstName;
        private String lastName;
        private String designation;
        private String department;
        private String phone;

        @JsonProperty("isCompanyAdmin")
        private boolean isCompanyAdmin;

        private Long companyId;
        private String companyName;
        private String companySlug;
        private String companyVerificationStatus;
    }
}
