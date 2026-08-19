package com.careerforge.controller;

import com.careerforge.dto.response.ApiResponse;
import com.careerforge.dto.response.AuditLogDetailResponse;
import com.careerforge.dto.response.AuditLogSummaryResponse;
import com.careerforge.entity.enums.AuditEventType;
import com.careerforge.entity.enums.AuditStatus;
import com.careerforge.entity.enums.AuditTargetType;
import com.careerforge.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLogSummaryResponse>>> getAuditLogs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AuditEventType eventType,
            @RequestParam(required = false) AuditTargetType targetEntityType,
            @RequestParam(required = false) AuditStatus status,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Sort sort = sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AuditLogSummaryResponse> auditLogs = auditLogService.getAuditLogs(
                search,
                eventType,
                targetEntityType,
                status,
                actorUserId,
                dateFrom,
                dateTo,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", auditLogs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AuditLogDetailResponse>> getAuditLogById(@PathVariable Long id) {
        AuditLogDetailResponse detail = auditLogService.getAuditLogById(id);
        return ResponseEntity.ok(ApiResponse.success("Audit log details retrieved successfully", detail));
    }
}
