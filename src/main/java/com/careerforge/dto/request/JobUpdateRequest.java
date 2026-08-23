package com.careerforge.dto.request;

import com.careerforge.entity.enums.ExperienceLevel;
import com.careerforge.entity.enums.JobType;
import com.careerforge.entity.enums.WorkMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class JobUpdateRequest {

    @NotBlank(message = "Job title is required")
    @Size(min = 3, max = 150, message = "Job title must be between 3 and 150 characters")
    private String title;

    @NotBlank(message = "Job description is required")
    @Size(min = 10, message = "Job description must be at least 10 characters")
    private String description;

    @Size(max = 150, message = "Location must not exceed 150 characters")
    private String location;

    @NotNull(message = "Work mode is required")
    private WorkMode workMode;

    @NotNull(message = "Job type is required")
    private JobType jobType;

    @NotNull(message = "Experience level is required")
    private ExperienceLevel experienceLevel;

    @DecimalMin(value = "0.0", inclusive = true, message = "Minimum salary cannot be negative")
    private BigDecimal salaryMin;

    @DecimalMin(value = "0.0", inclusive = true, message = "Maximum salary cannot be negative")
    private BigDecimal salaryMax;

    @Builder.Default
    @Size(max = 10, message = "Currency code must not exceed 10 characters")
    private String currency = "INR";

    private LocalDateTime deadline;

    @Valid
    private List<JobSkillItemRequest> skills;
}
