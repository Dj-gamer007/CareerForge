package com.careerforge.config;

import com.careerforge.entity.Application;
import com.careerforge.entity.Notification;
import com.careerforge.entity.Skill;
import com.careerforge.entity.User;
import com.careerforge.entity.enums.NotificationType;
import com.careerforge.entity.enums.Role;
import com.careerforge.repository.ApplicationRepository;
import com.careerforge.repository.NotificationRepository;
import com.careerforge.repository.SkillRepository;
import com.careerforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationRepository applicationRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Override
    @Transactional
    public void run(String... args) {
        seedSkills();

        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            bootstrapProdAdmin();
        } else {
            seedDevAccounts();
        }

        migrateLegacyInterviewNotifications();
    }

    private void seedSkills() {
        if (skillRepository.count() > 0) {
            log.info("Skills database already populated (count: {}). Skipping skill seeding.", skillRepository.count());
            return;
        }

        log.info("Seeding initial foundational skills...");
        List<Skill> initialSkills = List.of(
                Skill.builder().name("Java").category("Backend").build(),
                Skill.builder().name("Spring Boot").category("Backend").build(),
                Skill.builder().name("MySQL").category("Database").build(),
                Skill.builder().name("Git").category("DevOps").build(),
                Skill.builder().name("Docker").category("DevOps").build(),
                Skill.builder().name("REST API").category("Backend").build(),
                Skill.builder().name("React").category("Frontend").build(),
                Skill.builder().name("TypeScript").category("Frontend").build(),
                Skill.builder().name("Python").category("Backend").build(),
                Skill.builder().name("Microservices").category("Backend").build()
        );

        skillRepository.saveAll(initialSkills);
        log.info("Successfully seeded {} skills.", initialSkills.size());
    }

    private void seedDevAccounts() {
        if (userRepository.count() > 0) {
            log.info("Users database already populated (count: {}). Skipping dev account seeding.", userRepository.count());
            return;
        }

        log.info("------------------------------------------------------------------");
        log.info("SEEDING DEVELOPMENT-ONLY ACCOUNTS (DO NOT USE IN PRODUCTION)");
        log.info("Default Password for all dev accounts: DevPass123!");
        log.info("------------------------------------------------------------------");

        String defaultDevPassword = passwordEncoder.encode("DevPass123!");

        User admin = User.builder()
                .email("admin@careerforge.local")
                .passwordHash(defaultDevPassword)
                .role(Role.ROLE_ADMIN)
                .enabled(true)
                .build();

        User recruiter = User.builder()
                .email("recruiter@careerforge.local")
                .passwordHash(defaultDevPassword)
                .role(Role.ROLE_RECRUITER)
                .enabled(true)
                .build();

        User student = User.builder()
                .email("student@careerforge.local")
                .passwordHash(defaultDevPassword)
                .role(Role.ROLE_STUDENT)
                .enabled(true)
                .build();

        userRepository.saveAll(List.of(admin, recruiter, student));
        log.info("Successfully seeded 3 development-only accounts (ADMIN, RECRUITER, STUDENT).");
    }

    private void bootstrapProdAdmin() {
        if (userRepository.count() > 0) {
            log.info("Production database already initialized (user count: {}). Skipping admin bootstrapping.", userRepository.count());
            return;
        }

        String adminEmail = environment.getProperty("ADMIN_INIT_EMAIL");
        String adminPassword = environment.getProperty("ADMIN_INIT_PASSWORD");

        if (adminEmail != null && !adminEmail.isBlank() && adminPassword != null && !adminPassword.isBlank()) {
            User admin = User.builder()
                    .email(adminEmail.trim())
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .role(Role.ROLE_ADMIN)
                    .enabled(true)
                    .build();

            userRepository.save(admin);
            log.info("Successfully bootstrapped initial production administrator account for: {}", adminEmail.trim());
        } else {
            log.warn("Production mode active with empty user database, but ADMIN_INIT_EMAIL / ADMIN_INIT_PASSWORD were not set.");
        }
    }

    private void migrateLegacyInterviewNotifications() {
        List<Notification> interviewNotifs = notificationRepository.findByType(NotificationType.INTERVIEW_INVITE);
        if (interviewNotifs.isEmpty()) {
            return;
        }

        DateTimeFormatter legacyUtcFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' h:mm a", Locale.ENGLISH);
        DateTimeFormatter istFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' h:mm a", Locale.ENGLISH);

        for (Notification notif : interviewNotifs) {
            String msg = notif.getMessage();
            if (msg == null) continue;

            List<Application> apps = applicationRepository.findAllByStudentProfile_User_Id(notif.getUser().getId());
            for (Application app : apps) {
                if (app.getInterviewScheduledAt() != null) {
                    String rawUtcFormatted = app.getInterviewScheduledAt().format(legacyUtcFormatter);
                    String istFormatted = app.getInterviewScheduledAt()
                            .atZone(ZoneOffset.UTC)
                            .withZoneSameInstant(ZoneId.of("Asia/Kolkata"))
                            .format(istFormatter);

                    if (!rawUtcFormatted.equals(istFormatted) && msg.contains(rawUtcFormatted)) {
                        String updatedMsg = msg.replace(rawUtcFormatted, istFormatted);
                        notif.setMessage(updatedMsg);
                        notificationRepository.save(notif);
                        log.info("Migrated legacy interview notification ID {} for user ID {}: replaced '{}' with '{}'",
                                notif.getId(), notif.getUser().getId(), rawUtcFormatted, istFormatted);
                    }
                }
            }
        }
    }
}

