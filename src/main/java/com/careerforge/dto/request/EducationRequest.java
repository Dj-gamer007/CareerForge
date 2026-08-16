package com.careerforge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EducationRequest {

    @NotBlank(message = "Institution name is required")
    @Size(max = 150, message = "Institution name must not exceed 150 characters")
    private String institution;

    @Size(max = 100, message = "Degree must not exceed 100 characters")
    private String degree;

    @Size(max = 100, message = "Field of study must not exceed 100 characters")
    private String fieldOfStudy;

    private LocalDate startDate;

    private LocalDate endDate;

    @Builder.Default
    private boolean currentlyStudying = false;

    @Size(max = 50, message = "Grade/GPA must not exceed 50 characters")
    private String gradeOrGpa;
}
