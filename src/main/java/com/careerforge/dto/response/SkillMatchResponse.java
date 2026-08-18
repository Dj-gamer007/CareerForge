package com.careerforge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillMatchResponse {

    private BigDecimal overallScore;
    private int matchedRequiredCount;
    private int totalRequiredCount;
    private int matchedOptionalCount;
    private int totalOptionalCount;
    private int totalJobSkillsCount;
    private int totalStudentSkillsCount;
    private boolean isEligible;
    private List<SkillMatchDetailDto> matchedSkills;
    private List<MissingSkillDto> missingRequiredSkills;
    private List<MissingSkillDto> missingOptionalSkills;
}
