package com.careerforge.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSubmitRequest {

    @NotNull(message = "Job ID is required")
    private Long jobId;

    private Long resumeId;

    @Size(max = 3000, message = "Cover letter must not exceed 3000 characters")
    private String coverLetter;
}
