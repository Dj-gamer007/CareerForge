package com.careerforge.service;

import com.careerforge.dto.response.NotificationCountResponse;
import com.careerforge.dto.response.NotificationResponse;
import com.careerforge.dto.response.PagedResponse;
import com.careerforge.entity.Notification;
import com.careerforge.entity.enums.NotificationType;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    PagedResponse<NotificationResponse> getUserNotifications(Long userId, Pageable pageable);

    NotificationCountResponse getUnreadCount(Long userId);

    NotificationResponse markAsRead(Long userId, Long notificationId);

    void markAllAsRead(Long userId);

    Notification sendNotification(Long userId, String title, String message, NotificationType type);

    Notification sendNotification(Long userId, Long actorUserId, String actorName, String title, String message, NotificationType type, String relatedEntityType, Long relatedEntityId);
}
