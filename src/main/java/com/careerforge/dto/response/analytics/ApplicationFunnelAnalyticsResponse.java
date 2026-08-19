package com.careerforge.dto.response.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationFunnelAnalyticsResponse {

    private long totalApplications;
    private long appliedCount;
    private long underReviewCount;
    private long shortlistedCount;
    private long interviewScheduledCount;
    private long acceptedCount;
    private long rejectedCount;
    private long withdrawnCount;

    // Conversion percentages
    private double activeInPipelinePercentage;
    private double interviewRatePercentage;
    private double acceptanceRatePercentage;
    private double rejectionRatePercentage;
    private double withdrawalRatePercentage;
}
