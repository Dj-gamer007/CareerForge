package com.careerforge.dto.response;

import com.careerforge.entity.enums.SkillProficiency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillMatchDetailDto {

    private Long skillId;
    private String skillName;
    private String category;
    private boolean isRequired;
    private SkillProficiency requiredProficiency;
    private SkillProficiency studentProficiency;
    private BigDecimal proficiencyMultiplier;
    private BigDecimal skillWeight;
    private BigDecimal effectiveScoreContribution;
}
