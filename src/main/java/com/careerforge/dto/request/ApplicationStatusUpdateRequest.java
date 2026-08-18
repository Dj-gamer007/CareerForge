package com.careerforge.dto.request;

import com.careerforge.entity.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private ApplicationStatus status;

    @Size(max = 3000, message = "Recruiter notes must not exceed 3000 characters")
    private String recruiterNotes;

    private LocalDateTime interviewScheduledAt;
}
