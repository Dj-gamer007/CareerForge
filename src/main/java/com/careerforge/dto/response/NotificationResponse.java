package com.careerforge.dto.response;

import com.careerforge.entity.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private String title;
    private String message;
    private NotificationType type;

    @JsonProperty("isRead")
    private boolean isRead;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
