package com.careerforge.dto.response.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformTrendsAnalyticsResponse {

    private int windowDays;
    private List<DailyMetricDto> userRegistrations;
    private List<DailyMetricDto> jobPostings;
    private List<DailyMetricDto> applicationSubmissions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyMetricDto {
        private String date; // YYYY-MM-DD
        private long count;
    }
}
