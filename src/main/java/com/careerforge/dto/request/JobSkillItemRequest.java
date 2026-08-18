package com.careerforge.dto.request;

import com.careerforge.entity.enums.SkillProficiency;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSkillItemRequest {

    @NotNull(message = "Skill ID is required")
    private Long skillId;

    @Builder.Default
    private boolean isRequired = true;

    @NotNull(message = "Minimum proficiency is required")
    @Builder.Default
    private SkillProficiency minimumProficiency = SkillProficiency.INTERMEDIATE;
}
