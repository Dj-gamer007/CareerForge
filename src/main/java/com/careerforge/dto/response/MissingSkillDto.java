package com.careerforge.dto.response;

import com.careerforge.entity.enums.SkillProficiency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissingSkillDto {

    private Long skillId;
    private String skillName;
    private String category;
    private boolean isRequired;
    private SkillProficiency requiredProficiency;
}
