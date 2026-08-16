package com.careerforge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeResponse {

    private Long id;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private Integer version;
    private boolean isActive;
    private Instant uploadedAt;
}
