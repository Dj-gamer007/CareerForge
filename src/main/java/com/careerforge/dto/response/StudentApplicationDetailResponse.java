package com.careerforge.dto.response;

import com.careerforge.entity.enums.ApplicationStatus;
import com.careerforge.entity.enums.ExperienceLevel;
import com.careerforge.entity.enums.JobType;
import com.careerforge.entity.enums.WorkMode;
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
public class StudentApplicationDetailResponse {

    private Long id;
    private Long jobId;
    private String jobTitle;
    private String jobSlug;
    private String jobDescription;
    private Long companyId;
    private String companyName;
    private String companyLogoUrl;
    private String location;
    private WorkMode workMode;
    private JobType jobType;
    private ExperienceLevel experienceLevel;
    private ApplicationStatus status;
    private String coverLetter;
    private BigDecimal matchScoreAtApplication;
    private Long resumeId;
    private String resumeFileName;
    private LocalDateTime appliedAt;
    private LocalDateTime interviewScheduledAt;
    private LocalDateTime withdrawnAt;
    private LocalDateTime updatedAt;
    private SkillMatchResponse currentMatchAnalysis;
}
