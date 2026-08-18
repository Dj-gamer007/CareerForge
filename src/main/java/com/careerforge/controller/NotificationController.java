package com.careerforge.controller;

import com.careerforge.dto.response.ApiResponse;
import com.careerforge.dto.response.NotificationCountResponse;
import com.careerforge.dto.response.NotificationResponse;
import com.careerforge.dto.response.PagedResponse;
import com.careerforge.security.UserPrincipal;
import com.careerforge.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getUserNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Sort sort = sortDirection.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PagedResponse<NotificationResponse> response = notificationService.getUserNotifications(userPrincipal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", response));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<NotificationCountResponse>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        NotificationCountResponse response = notificationService.getUnreadCount(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Unread notification count retrieved", response));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        NotificationResponse response = notificationService.markAsRead(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", response));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        notificationService.markAllAsRead(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }
}
