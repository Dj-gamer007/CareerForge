package com.careerforge.service.impl;

import com.careerforge.dto.response.AuditLogDetailResponse;
import com.careerforge.dto.response.AuditLogSummaryResponse;
import com.careerforge.entity.AuditLog;
import com.careerforge.entity.enums.AuditEventType;
import com.careerforge.entity.enums.AuditStatus;
import com.careerforge.entity.enums.AuditTargetType;
import com.careerforge.exception.ResourceNotFoundException;
import com.careerforge.repository.AuditLogRepository;
import com.careerforge.service.AuditLogService;
import com.careerforge.specification.AuditLogSpecification;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void logSuccess(
            Long actorUserId,
            String actorEmail,
            String actorRole,
            AuditEventType eventType,
            AuditTargetType targetType,
            Long targetId,
            String targetIdentifier,
            String reason,
            Map<String, Object> sanitizedDetails
    ) {
        try {
            AuditLog auditLog = buildAuditLog(
                    actorUserId,
                    actorEmail,
                    actorRole,
                    eventType,
                    targetType,
                    targetId,
                    targetIdentifier,
                    AuditStatus.SUCCESS,
                    reason,
                    sanitizedDetails
            );
            auditLogRepository.save(auditLog);
            log.info("Recorded SUCCESS audit log: event={}, targetType={}, targetId={}, actor={}",
                    eventType, targetType, targetId, actorEmail);
        } catch (Exception ex) {
            log.error("Failed to persist SUCCESS audit log for event {}: {}", eventType, ex.getMessage(), ex);
            throw new RuntimeException("Failed to persist audit log", ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(
            Long actorUserId,
            String actorEmail,
            String actorRole,
            AuditEventType eventType,
            AuditTargetType targetType,
            Long targetId,
            String targetIdentifier,
            String reason,
            Map<String, Object> sanitizedDetails
    ) {
        try {
            AuditLog auditLog = buildAuditLog(
                    actorUserId,
                    actorEmail,
                    actorRole,
                    eventType,
                    targetType,
                    targetId,
                    targetIdentifier,
                    AuditStatus.FAILURE,
                    reason,
                    sanitizedDetails
            );
            auditLogRepository.save(auditLog);
            log.warn("Recorded FAILURE audit log: event={}, targetType={}, targetId={}, actor={}, reason={}",
                    eventType, targetType, targetId, actorEmail, reason);
        } catch (Exception ex) {
            log.error("CRITICAL: Failed to persist FAILURE audit log for event {}: {}", eventType, ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogSummaryResponse> getAuditLogs(
            String search,
            AuditEventType eventType,
            AuditTargetType targetEntityType,
            AuditStatus status,
            Long actorUserId,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Pageable pageable
    ) {
        Specification<AuditLog> spec = AuditLogSpecification.buildSpecification(
                search,
                eventType,
                targetEntityType,
                status,
                actorUserId,
                dateFrom,
                dateTo
        );

        return auditLogRepository.findAll(spec, pageable).map(this::mapToSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogDetailResponse getAuditLogById(Long id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log not found with id: " + id));

        return mapToDetailResponse(auditLog);
    }

    private AuditLog buildAuditLog(
            Long actorUserId,
            String actorEmail,
            String actorRole,
            AuditEventType eventType,
            AuditTargetType targetType,
            Long targetId,
            String targetIdentifier,
            AuditStatus status,
            String reason,
            Map<String, Object> sanitizedDetails
    ) {
        HttpServletRequest request = getCurrentHttpRequest();
        String ipAddress = null;
        String userAgent = null;

        if (request != null) {
            ipAddress = getClientIpAddress(request);
            userAgent = request.getHeader("User-Agent");
            if (userAgent != null && userAgent.length() > 255) {
                userAgent = userAgent.substring(0, 255);
            }
        }

        String detailsJson = null;
        if (sanitizedDetails != null && !sanitizedDetails.isEmpty()) {
            try {
                detailsJson = objectMapper.writeValueAsString(sanitizedDetails);
            } catch (Exception e) {
                log.warn("Could not serialize audit details map to JSON: {}", e.getMessage());
            }
        }

        return AuditLog.builder()
                .actorUserId(actorUserId)
                .actorEmail(actorEmail != null ? actorEmail : "SYSTEM")
                .actorRole(actorRole != null ? actorRole : "SYSTEM")
                .eventType(eventType)
                .targetEntityType(targetType)
                .targetEntityId(targetId)
                .targetIdentifier(targetIdentifier)
                .status(status)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .reason(reason)
                .details(detailsJson)
                .build();
    }

    private HttpServletRequest getCurrentHttpRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private AuditLogSummaryResponse mapToSummaryResponse(AuditLog auditLog) {
        return AuditLogSummaryResponse.builder()
                .id(auditLog.getId())
                .actorUserId(auditLog.getActorUserId())
                .actorEmail(auditLog.getActorEmail())
                .actorRole(auditLog.getActorRole())
                .eventType(auditLog.getEventType())
                .targetEntityType(auditLog.getTargetEntityType())
                .targetEntityId(auditLog.getTargetEntityId())
                .targetIdentifier(auditLog.getTargetIdentifier())
                .status(auditLog.getStatus())
                .ipAddress(auditLog.getIpAddress())
                .reason(auditLog.getReason())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }

    private AuditLogDetailResponse mapToDetailResponse(AuditLog auditLog) {
        return AuditLogDetailResponse.builder()
                .id(auditLog.getId())
                .actorUserId(auditLog.getActorUserId())
                .actorEmail(auditLog.getActorEmail())
                .actorRole(auditLog.getActorRole())
                .eventType(auditLog.getEventType())
                .targetEntityType(auditLog.getTargetEntityType())
                .targetEntityId(auditLog.getTargetEntityId())
                .targetIdentifier(auditLog.getTargetIdentifier())
                .status(auditLog.getStatus())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .reason(auditLog.getReason())
                .details(auditLog.getDetails())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
