package com.careerforge.dto.response;

import com.careerforge.entity.enums.ApplicationStatus;
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
public class RecruiterApplicationSummaryResponse {

    private Long id;
    private Long studentId;
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;
    private String location;
    private Integer profileCompletionPercentage;
    private BigDecimal matchScoreAtApplication;
    private ApplicationStatus status;
    private Long resumeId;
    private String resumeFileName;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime interviewScheduledAt;
    private LocalDateTime withdrawnAt;
}
