package com.careerforge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanySummaryResponse {

    private Long id;
    private String name;
    private String slug;
    private String logoUrl;
    private String industry;
    private String location;
    private String companySize;
    private long activeJobsCount;
}
