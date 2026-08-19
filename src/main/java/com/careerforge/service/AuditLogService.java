package com.careerforge.service;

import com.careerforge.dto.response.AuditLogDetailResponse;
import com.careerforge.dto.response.AuditLogSummaryResponse;
import com.careerforge.entity.enums.AuditEventType;
import com.careerforge.entity.enums.AuditStatus;
import com.careerforge.entity.enums.AuditTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Map;

public interface AuditLogService {

    void logSuccess(
            Long actorUserId,
            String actorEmail,
            String actorRole,
            AuditEventType eventType,
            AuditTargetType targetType,
            Long targetId,
            String targetIdentifier,
            String reason,
            Map<String, Object> sanitizedDetails
    );

    void logFailure(
            Long actorUserId,
            String actorEmail,
            String actorRole,
            AuditEventType eventType,
            AuditTargetType targetType,
            Long targetId,
            String targetIdentifier,
            String reason,
            Map<String, Object> sanitizedDetails
    );

    Page<AuditLogSummaryResponse> getAuditLogs(
            String search,
            AuditEventType eventType,
            AuditTargetType targetEntityType,
            AuditStatus status,
            Long actorUserId,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Pageable pageable
    );

    AuditLogDetailResponse getAuditLogById(Long id);
}
