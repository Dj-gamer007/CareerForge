package com.careerforge.controller;

import com.careerforge.entity.Notification;
import com.careerforge.entity.User;
import com.careerforge.entity.enums.NotificationType;
import com.careerforge.entity.enums.Role;
import com.careerforge.repository.NotificationRepository;
import com.careerforge.repository.UserRepository;
import com.careerforge.security.JwtTokenProvider;
import com.careerforge.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User user1;
    private User user2;
    private String token1;
    private String token2;
    private Notification notif1;

    @BeforeEach
    void setUp() {
        user1 = userRepository.findByEmail("notif_user1@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("notif_user1@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_STUDENT)
                        .enabled(true)
                        .build()));

        user2 = userRepository.findByEmail("notif_user2@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("notif_user2@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_STUDENT)
                        .enabled(true)
                        .build()));

        token1 = "Bearer " + jwtTokenProvider.generateAccessToken(UserPrincipal.create(user1));
        token2 = "Bearer " + jwtTokenProvider.generateAccessToken(UserPrincipal.create(user2));

        notif1 = notificationRepository.save(Notification.builder()
                .user(user1)
                .title("Welcome to CareerForge")
                .message("Your profile setup is in progress.")
                .type(NotificationType.SYSTEM_ALERT)
                .isRead(false)
                .build());

        notificationRepository.save(Notification.builder()
                .user(user1)
                .title("Application Update")
                .message("You have a new update.")
                .type(NotificationType.APPLICATION_UPDATE)
                .isRead(false)
                .build());
    }

    @Test
    @DisplayName("User can list their notifications and retrieve unread count")
    void testListNotificationsAndUnreadCount() throws Exception {
        // 1. Get unread count
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(2));

        // 2. List notifications
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].isRead").value(false));
    }

    @Test
    @DisplayName("User can mark a single notification and all notifications as read")
    void testMarkAsReadAndMarkAllAsRead() throws Exception {
        // 1. Mark single notification as read
        mockMvc.perform(patch("/api/v1/notifications/" + notif1.getId() + "/read")
                        .header("Authorization", token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isRead").value(true));

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        // 2. Mark all as read
        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .header("Authorization", token1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    @Test
    @DisplayName("User cannot mark another user's notification as read (returns 404)")
    void testCannotAccessAnotherUsersNotification() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/" + notif1.getId() + "/read")
                        .header("Authorization", token2))
                .andExpect(status().isNotFound());
    }
}
