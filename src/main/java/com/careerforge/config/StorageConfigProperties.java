package com.careerforge.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "careerforge.storage")
public class StorageConfigProperties {

    private String localDir = "./uploads/resumes";
}
