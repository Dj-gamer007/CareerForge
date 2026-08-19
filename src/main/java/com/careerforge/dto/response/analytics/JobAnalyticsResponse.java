package com.careerforge.dto.response.analytics;

import com.careerforge.entity.enums.ExperienceLevel;
import com.careerforge.entity.enums.JobStatus;
import com.careerforge.entity.enums.JobType;
import com.careerforge.entity.enums.WorkMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobAnalyticsResponse {

    private long totalJobs;
    private Map<JobStatus, Long> jobsByStatus;
    private Map<WorkMode, Long> jobsByWorkMode;
    private Map<JobType, Long> jobsByJobType;
    private Map<ExperienceLevel, Long> jobsByExperienceLevel;
}
