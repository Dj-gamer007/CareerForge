package com.careerforge.dto.response;

import com.careerforge.entity.enums.SkillProficiency;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSkillResponse {

    private Long id;
    private Long skillId;
    private String skillName;
    private String category;

    @JsonProperty("required")
    private boolean isRequired;

    private SkillProficiency minimumProficiency;
}
