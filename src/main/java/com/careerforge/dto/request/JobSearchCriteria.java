package com.careerforge.dto.request;

import com.careerforge.entity.enums.ExperienceLevel;
import com.careerforge.entity.enums.JobType;
import com.careerforge.entity.enums.WorkMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSearchCriteria {

    private String keyword;
    private String location;
    private Set<WorkMode> workModes;
    private Set<JobType> jobTypes;
    private Set<ExperienceLevel> experienceLevels;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private List<Long> skillIds;
    private Long companyId;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 10;

    @Builder.Default
    private String sortBy = "createdAt";

    @Builder.Default
    private String sortDirection = "desc";
}
