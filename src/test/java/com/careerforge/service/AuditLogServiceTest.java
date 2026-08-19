package com.careerforge.service;

import com.careerforge.dto.response.AuditLogDetailResponse;
import com.careerforge.dto.response.AuditLogSummaryResponse;
import com.careerforge.entity.AuditLog;
import com.careerforge.entity.enums.AuditEventType;
import com.careerforge.entity.enums.AuditStatus;
import com.careerforge.entity.enums.AuditTargetType;
import com.careerforge.exception.ResourceNotFoundException;
import com.careerforge.repository.AuditLogRepository;
import com.careerforge.service.impl.AuditLogServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    private AuditLog sampleAuditLog;

    @BeforeEach
    void setUp() {
        sampleAuditLog = AuditLog.builder()
                .id(1L)
                .actorUserId(10L)
                .actorEmail("admin@careerforge.local")
                .actorRole("ROLE_ADMIN")
                .eventType(AuditEventType.USER_STATUS_UPDATED)
                .targetEntityType(AuditTargetType.USER)
                .targetEntityId(20L)
                .targetIdentifier("student@careerforge.local")
                .status(AuditStatus.SUCCESS)
                .reason("Account policy violation")
                .details("{\"previousEnabled\":true,\"newEnabled\":false}")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Log success - saves audit record with SUCCESS status")
    void testLogSuccess_SavesAuditRecord() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        auditLogService.logSuccess(
                10L,
                "admin@careerforge.local",
                "ROLE_ADMIN",
                AuditEventType.USER_STATUS_UPDATED,
                AuditTargetType.USER,
                20L,
                "student@careerforge.local",
                "Account policy violation",
                Map.of("previousEnabled", true, "newEnabled", false)
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(AuditStatus.SUCCESS);
        assertThat(saved.getActorUserId()).isEqualTo(10L);
        assertThat(saved.getActorEmail()).isEqualTo("admin@careerforge.local");
        assertThat(saved.getEventType()).isEqualTo(AuditEventType.USER_STATUS_UPDATED);
        assertThat(saved.getTargetEntityId()).isEqualTo(20L);
        assertThat(saved.getDetails()).contains("\"previousEnabled\":true");
    }

    @Test
    @DisplayName("Log failure - saves audit record with FAILURE status")
    void testLogFailure_SavesAuditRecord() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        auditLogService.logFailure(
                10L,
                "admin@careerforge.local",
                "ROLE_ADMIN",
                AuditEventType.USER_SELF_DISABLE_REJECTED,
                AuditTargetType.USER,
                10L,
                "admin@careerforge.local",
                "Cannot disable own account",
                Map.of("error", "Administrators cannot disable their own account")
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(AuditStatus.FAILURE);
        assertThat(saved.getEventType()).isEqualTo(AuditEventType.USER_SELF_DISABLE_REJECTED);
        assertThat(saved.getReason()).isEqualTo("Cannot disable own account");
    }

    @Test
    @DisplayName("Log success - database failure rethrows RuntimeException")
    void testLogSuccess_DbFailure_ThrowsException() {
        when(auditLogRepository.save(any(AuditLog.class))).thenThrow(new RuntimeException("DB Connection down"));

        assertThatThrownBy(() -> auditLogService.logSuccess(
                10L, "admin@test.com", "ROLE_ADMIN",
                AuditEventType.COMPANY_VERIFICATION_UPDATED, AuditTargetType.COMPANY,
                100L, "Acme", "Verified", Map.of()
        )).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to persist audit log");
    }

    @Test
    @DisplayName("Log failure - database failure caught and does not throw exception")
    void testLogFailure_DbFailure_DoesNotThrowException() {
        when(auditLogRepository.save(any(AuditLog.class))).thenThrow(new RuntimeException("DB Connection down"));

        // Must not throw exception
        auditLogService.logFailure(
                10L, "admin@test.com", "ROLE_ADMIN",
                AuditEventType.JOB_MODERATION_PERFORMED, AuditTargetType.JOB,
                100L, "Dev", "Invalid transition", Map.of()
        );

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Get audit logs - returns paginated summary responses")
    void testGetAuditLogs_ReturnsPaginatedSummary() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> page = new PageImpl<>(List.of(sampleAuditLog), pageable, 1);

        when(auditLogRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<AuditLogSummaryResponse> result = auditLogService.getAuditLogs(
                null, null, null, null, null, null, null, pageable
        );

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        AuditLogSummaryResponse summary = result.getContent().get(0);
        assertThat(summary.getId()).isEqualTo(1L);
        assertThat(summary.getActorEmail()).isEqualTo("admin@careerforge.local");
        assertThat(summary.getEventType()).isEqualTo(AuditEventType.USER_STATUS_UPDATED);
    }

    @Test
    @DisplayName("Get audit log by ID - returns detail response")
    void testGetAuditLogById_Found() {
        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(sampleAuditLog));

        AuditLogDetailResponse detail = auditLogService.getAuditLogById(1L);

        assertThat(detail).isNotNull();
        assertThat(detail.getId()).isEqualTo(1L);
        assertThat(detail.getActorEmail()).isEqualTo("admin@careerforge.local");
        assertThat(detail.getDetails()).contains("\"newEnabled\":false");
    }

    @Test
    @DisplayName("Get audit log by ID - nonexistent throws ResourceNotFoundException")
    void testGetAuditLogById_NotFound_ThrowsException() {
        when(auditLogRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditLogService.getAuditLogById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Audit log not found");
    }
}
