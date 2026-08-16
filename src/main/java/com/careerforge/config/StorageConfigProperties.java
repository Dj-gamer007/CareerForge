package com.careerforge.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "careerforge.storage")
public class StorageConfigProperties {

    private String localDir = "./uploads/resumes";
    private long maxFileSizeBytes = 5 * 1024 * 1024; // 5 MB
    private List<String> allowedContentTypes = List.of("application/pdf");
}
