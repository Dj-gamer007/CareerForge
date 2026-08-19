package com.careerforge.dto.response;

import com.careerforge.entity.enums.AuditEventType;
import com.careerforge.entity.enums.AuditStatus;
import com.careerforge.entity.enums.AuditTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDetailResponse {

    private Long id;
    private Long actorUserId;
    private String actorEmail;
    private String actorRole;
    private AuditEventType eventType;
    private AuditTargetType targetEntityType;
    private Long targetEntityId;
    private String targetIdentifier;
    private AuditStatus status;
    private String ipAddress;
    private String userAgent;
    private String reason;
    private String details;
    private LocalDateTime createdAt;
}
