package com.careerforge.dto.request;

import com.careerforge.config.FlexibleLocalDateTimeDeserializer;
import com.careerforge.entity.enums.ApplicationStatus;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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

    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime interviewScheduledAt;
}

