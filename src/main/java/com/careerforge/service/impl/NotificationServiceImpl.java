package com.careerforge.service.impl;

import com.careerforge.dto.response.NotificationCountResponse;
import com.careerforge.dto.response.NotificationResponse;
import com.careerforge.dto.response.PagedResponse;
import com.careerforge.entity.Notification;
import com.careerforge.entity.User;
import com.careerforge.entity.enums.NotificationType;
import com.careerforge.exception.ResourceNotFoundException;
import com.careerforge.repository.NotificationRepository;
import com.careerforge.repository.UserRepository;
import com.careerforge.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getUserNotifications(Long userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, pageable);
        List<NotificationResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return PagedResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationCountResponse getUnreadCount(Long userId) {
        long count = notificationRepository.countByUser_IdAndIsReadFalse(userId);
        return NotificationCountResponse.builder().unreadCount(count).build();
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUser_Id(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        log.info("Marked notification ID: {} as read for user ID: {}", notificationId, userId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        int updated = notificationRepository.markAllAsReadByUserId(userId);
        log.info("Marked {} notifications as read for user ID: {}", updated, userId);
    }

    @Override
    @Transactional
    public Notification sendNotification(Long userId, String title, String message, NotificationType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Notification notification = Notification.builder()
                .user(user)
                .title(title.trim())
                .message(message.trim())
                .type(type)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Dispatched notification (id: {}, type: {}) to user ID: {}", saved.getId(), type, userId);
        return saved;
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }
}
