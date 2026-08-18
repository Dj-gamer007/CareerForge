package com.careerforge.dto.response;

import com.careerforge.entity.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruiterApplicationDetailResponse {

    private Long id;
    private Long studentId;
    private Long jobId;
    private String jobTitle;
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;
    private String candidateLocation;
    private String candidateBio;
    private String candidateEducationSummary;
    private String candidateGithubUrl;
    private String candidateLinkedinUrl;
    private String candidatePortfolioUrl;
    private Integer profileCompletionPercentage;
    private BigDecimal matchScoreAtApplication;
    private ApplicationStatus status;
    private String coverLetter;
    private String recruiterNotes;
    private Long resumeId;
    private String resumeFileName;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime interviewScheduledAt;
    private LocalDateTime withdrawnAt;
    private LocalDateTime updatedAt;
    private SkillMatchResponse skillBreakdown;
}
