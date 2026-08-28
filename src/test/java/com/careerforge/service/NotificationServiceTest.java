package com.careerforge.service;

import com.careerforge.dto.response.NotificationCountResponse;
import com.careerforge.dto.response.NotificationResponse;
import com.careerforge.dto.response.PagedResponse;
import com.careerforge.entity.Notification;
import com.careerforge.entity.User;
import com.careerforge.entity.enums.NotificationType;
import com.careerforge.entity.enums.Role;
import com.careerforge.exception.ResourceNotFoundException;
import com.careerforge.repository.NotificationRepository;
import com.careerforge.repository.UserRepository;
import com.careerforge.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User testUser;
    private Notification notification1;
    private Notification notification2;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("student@careerforge.local")
                .role(Role.ROLE_STUDENT)
                .build();

        notification1 = Notification.builder()
                .id(100L)
                .user(testUser)
                .title("Application Shortlisted")
                .message("You have been shortlisted for Backend Engineer.")
                .type(NotificationType.APPLICATION_UPDATE)
                .isRead(false)
                .build();
        notification1.setCreatedAt(LocalDateTime.now().minusHours(1));

        notification2 = Notification.builder()
                .id(101L)
                .user(testUser)
                .title("Interview Invitation")
                .message("Interview scheduled for tomorrow at 10 AM.")
                .type(NotificationType.INTERVIEW_INVITE)
                .isRead(true)
                .build();
        notification2.setCreatedAt(LocalDateTime.now().minusHours(2));
    }

    @Test
    @DisplayName("Should get paginated notifications for user")
    void testGetUserNotifications() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(notification1, notification2), pageable, 2);

        when(notificationRepository.findAllByUser_IdOrderByCreatedAtDescIdDesc(1L, pageable)).thenReturn(page);

        PagedResponse<NotificationResponse> result = notificationService.getUserNotifications(1L, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Application Shortlisted");
        assertThat(result.getContent().get(0).isRead()).isFalse();
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return unread count for user")
    void testGetUnreadCount() {
        when(notificationRepository.countByUser_IdAndIsReadFalse(1L)).thenReturn(5L);

        NotificationCountResponse response = notificationService.getUnreadCount(1L);

        assertThat(response.getUnreadCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Should mark notification as read for user")
    void testMarkAsRead_Success() {
        when(notificationRepository.findByIdAndUser_Id(100L, 1L)).thenReturn(Optional.of(notification1));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification1);

        NotificationResponse response = notificationService.markAsRead(1L, 100L);

        assertThat(response.isRead()).isTrue();
        verify(notificationRepository).save(notification1);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when marking another user's notification as read")
    void testMarkAsRead_NotFound() {
        when(notificationRepository.findByIdAndUser_Id(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should send and persist a new notification")
    void testSendNotification() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(200L);
            return n;
        });

        Notification result = notificationService.sendNotification(1L, "Test Alert", "Alert Message", NotificationType.SYSTEM_ALERT);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(200L);
        assertThat(result.getTitle()).isEqualTo("Test Alert");
        assertThat(result.getType()).isEqualTo(NotificationType.SYSTEM_ALERT);
    }

    @Test
    @DisplayName("Should send and persist a new notification with actor and related entity")
    void testSendNotification_WithActorAndRelatedEntity() {
        User actorUser = User.builder().id(2L).email("recruiter@careerforge.local").role(Role.ROLE_RECRUITER).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(actorUser));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(201L);
            return n;
        });

        Notification result = notificationService.sendNotification(
                1L,
                2L,
                "John Smith",
                "Application Shortlisted",
                "Your application for 'Java Developer' at Delite Works has been shortlisted.",
                NotificationType.APPLICATION_SHORTLISTED,
                "APPLICATION",
                55L
        );

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(201L);
        assertThat(result.getActorName()).isEqualTo("John Smith");
        assertThat(result.getActorUser()).isEqualTo(actorUser);
        assertThat(result.getRelatedEntityType()).isEqualTo("APPLICATION");
        assertThat(result.getRelatedEntityId()).isEqualTo(55L);
        assertThat(result.getType()).isEqualTo(NotificationType.APPLICATION_SHORTLISTED);
    }
}
