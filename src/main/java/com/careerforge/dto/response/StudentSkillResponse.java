package com.careerforge.dto.response;

import com.careerforge.entity.enums.SkillProficiency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentSkillResponse {

    private Long id;
    private Long skillId;
    private String skillName;
    private String category;
    private SkillProficiency proficiency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
