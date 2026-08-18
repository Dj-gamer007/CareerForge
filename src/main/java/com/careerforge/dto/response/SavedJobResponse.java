package com.careerforge.dto.response;

import com.careerforge.entity.enums.ExperienceLevel;
import com.careerforge.entity.enums.JobStatus;
import com.careerforge.entity.enums.JobType;
import com.careerforge.entity.enums.WorkMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedJobResponse {

    private Long id;
    private Long jobId;
    private String jobTitle;
    private String jobSlug;
    private Long companyId;
    private String companyName;
    private String companyLogoUrl;
    private String location;
    private WorkMode workMode;
    private JobType jobType;
    private ExperienceLevel experienceLevel;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String currency;
    private JobStatus status;
    private LocalDateTime deadline;
    private LocalDateTime savedAt;
    private List<JobSkillResponse> skills;
}
