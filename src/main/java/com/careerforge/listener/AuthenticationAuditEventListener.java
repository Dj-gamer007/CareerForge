package com.careerforge.listener;

import com.careerforge.entity.User;
import com.careerforge.entity.enums.AuditEventType;
import com.careerforge.entity.enums.AuditTargetType;
import com.careerforge.entity.enums.Role;
import com.careerforge.repository.UserRepository;
import com.careerforge.security.UserPrincipal;
import com.careerforge.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationAuditEventListener {

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication auth = event.getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return;
        }

        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            auditLogService.logSuccess(
                    principal.getId(),
                    principal.getEmail(),
                    "ROLE_ADMIN",
                    AuditEventType.ADMIN_LOGIN_SUCCESS,
                    AuditTargetType.AUTH,
                    principal.getId(),
                    principal.getEmail(),
                    "Admin user authenticated successfully",
                    Map.of("email", principal.getEmail())
            );
        }
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        Authentication auth = event.getAuthentication();
        if (auth == null || !StringUtils.hasText(auth.getName())) {
            return;
        }

        String attemptedEmail = auth.getName().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(attemptedEmail);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getRole() == Role.ROLE_ADMIN) {
                String failureReason = event.getException() != null ?
                        event.getException().getMessage() : "Authentication failed";

                auditLogService.logFailure(
                        user.getId(),
                        user.getEmail(),
                        "ROLE_ADMIN",
                        AuditEventType.ADMIN_LOGIN_FAILURE,
                        AuditTargetType.AUTH,
                        user.getId(),
                        user.getEmail(),
                        "Admin login failure: " + failureReason,
                        Map.of(
                                "attemptedEmail", user.getEmail(),
                                "exceptionType", event.getException() != null ? event.getException().getClass().getSimpleName() : "Unknown"
                        )
                );
            }
        }
    }
}
