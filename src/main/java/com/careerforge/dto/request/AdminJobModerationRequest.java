package com.careerforge.dto.request;

import com.careerforge.entity.enums.JobStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminJobModerationRequest {

    @NotNull(message = "Target status is required")
    private JobStatus status;

    @NotBlank(message = "Moderation reason is required")
    private String reason;
}
