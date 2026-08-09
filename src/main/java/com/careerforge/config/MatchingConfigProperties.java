package com.careerforge.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "careerforge.matching")
public class MatchingConfigProperties {

    private double requiredSkillWeight = 2.0;
    private double optionalSkillWeight = 1.0;
}
