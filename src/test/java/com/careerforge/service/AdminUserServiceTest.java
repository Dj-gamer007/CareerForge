package com.careerforge.service;

import com.careerforge.dto.request.UserStatusUpdateRequest;
import com.careerforge.dto.response.AdminUserDetailResponse;
import com.careerforge.dto.response.AdminUserSummaryResponse;
import com.careerforge.entity.*;
import com.careerforge.entity.enums.CompanyVerificationStatus;
import com.careerforge.entity.enums.Role;
import com.careerforge.entity.enums.SkillProficiency;
import com.careerforge.exception.BadRequestException;
import com.careerforge.exception.ResourceNotFoundException;
import com.careerforge.repository.*;
import com.careerforge.service.impl.AdminUserServiceImpl;
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
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private RecruiterProfileRepository recruiterProfileRepository;

    @Mock
    private EducationRepository educationRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private CertificationRepository certificationRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private StudentSkillRepository studentSkillRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private User adminUser;
    private User studentUser;
    private User recruiterUser;
    private StudentProfile studentProfile;
    private RecruiterProfile recruiterProfile;
    private Company company;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .email("admin@careerforge.local")
                .role(Role.ROLE_ADMIN)
                .enabled(true)
                .build();

        studentUser = User.builder()
                .id(2L)
                .email("student@careerforge.local")
                .role(Role.ROLE_STUDENT)
                .enabled(true)
                .build();

        recruiterUser = User.builder()
                .id(3L)
                .email("recruiter@careerforge.local")
                .role(Role.ROLE_RECRUITER)
                .enabled(true)
                .build();

        studentProfile = StudentProfile.builder()
                .id(10L)
                .user(studentUser)
                .firstName("Alice")
                .lastName("Smith")
                .location("Bengaluru")
                .phone("+919876543210")
                .bio("CS Student")
                .profileCompletionPercentage(80)
                .build();

        company = Company.builder()
                .id(100L)
                .name("Acme Corp")
                .slug("acme-corp")
                .verificationStatus(CompanyVerificationStatus.VERIFIED)
                .build();

        recruiterProfile = RecruiterProfile.builder()
                .id(20L)
                .user(recruiterUser)
                .company(company)
                .firstName("Bob")
                .lastName("Recruiter")
                .designation("Tech Talent Partner")
                .isCompanyAdmin(true)
                .build();
    }

    @Test
    @DisplayName("Get users - returns paginated summary with profile names")
    void testGetUsers_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(studentUser, recruiterUser), pageable, 2);

        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);
        when(studentProfileRepository.findAllByUser_IdIn(List.of(2L, 3L))).thenReturn(List.of(studentProfile));
        when(recruiterProfileRepository.findAllByUser_IdIn(List.of(2L, 3L))).thenReturn(List.of(recruiterProfile));

        Page<AdminUserSummaryResponse> result = adminUserService.getUsers(null, null, null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);

        AdminUserSummaryResponse studentSummary = result.getContent().get(0);
        assertThat(studentSummary.getEmail()).isEqualTo("student@careerforge.local");
        assertThat(studentSummary.getFullName()).isEqualTo("Alice Smith");
        assertThat(studentSummary.getProfileType()).isEqualTo("STUDENT");

        AdminUserSummaryResponse recruiterSummary = result.getContent().get(1);
        assertThat(recruiterSummary.getEmail()).isEqualTo("recruiter@careerforge.local");
        assertThat(recruiterSummary.getFullName()).isEqualTo("Bob Recruiter");
        assertThat(recruiterSummary.getProfileType()).isEqualTo("RECRUITER");
    }

    @Test
    @DisplayName("Get users - empty page returns empty summary page")
    void testGetUsers_Empty() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty(pageable));

        Page<AdminUserSummaryResponse> result = adminUserService.getUsers("nonexistent", null, null, pageable);

        assertThat(result).isEmpty();
        verifyNoInteractions(studentProfileRepository);
        verifyNoInteractions(recruiterProfileRepository);
    }

    @Test
    @DisplayName("Get user by ID - student user returns full student profile details")
    void testGetUserById_Student() {
        Skill javaSkill = Skill.builder().id(1L).name("Java").build();
        StudentSkill studentSkill = StudentSkill.builder()
                .id(1L)
                .studentProfile(studentProfile)
                .skill(javaSkill)
                .proficiency(SkillProficiency.ADVANCED)
                .build();

        Resume activeResume = Resume.builder()
                .id(50L)
                .studentProfile(studentProfile)
                .isActive(true)
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(studentUser));
        when(studentProfileRepository.findByUser_Id(2L)).thenReturn(Optional.of(studentProfile));
        when(studentSkillRepository.findAllByStudentProfileWithSkill(studentProfile)).thenReturn(List.of(studentSkill));
        when(educationRepository.countByStudentProfile(studentProfile)).thenReturn(1L);
        when(projectRepository.countByStudentProfile(studentProfile)).thenReturn(2L);
        when(certificationRepository.countByStudentProfile(studentProfile)).thenReturn(1L);
        when(resumeRepository.countByStudentProfile(studentProfile)).thenReturn(1L);
        when(resumeRepository.findByStudentProfileAndIsActiveTrue(studentProfile)).thenReturn(Optional.of(activeResume));

        AdminUserDetailResponse result = adminUserService.getUserById(2L);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("student@careerforge.local");
        assertThat(result.getRole()).isEqualTo(Role.ROLE_STUDENT);
        assertThat(result.getStudentProfile()).isNotNull();
        assertThat(result.getStudentProfile().getFirstName()).isEqualTo("Alice");
        assertThat(result.getStudentProfile().getTotalSkills()).isEqualTo(1);
        assertThat(result.getStudentProfile().getTotalEducations()).isEqualTo(1);
        assertThat(result.getStudentProfile().getTotalProjects()).isEqualTo(2);
        assertThat(result.getStudentProfile().getActiveResumeId()).isEqualTo(50L);
        assertThat(result.getRecruiterProfile()).isNull();
    }

    @Test
    @DisplayName("Get user by ID - recruiter user returns recruiter profile & company details")
    void testGetUserById_Recruiter() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(recruiterUser));
        when(recruiterProfileRepository.findByUser_Id(3L)).thenReturn(Optional.of(recruiterProfile));

        AdminUserDetailResponse result = adminUserService.getUserById(3L);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("recruiter@careerforge.local");
        assertThat(result.getRole()).isEqualTo(Role.ROLE_RECRUITER);
        assertThat(result.getRecruiterProfile()).isNotNull();
        assertThat(result.getRecruiterProfile().getFirstName()).isEqualTo("Bob");
        assertThat(result.getRecruiterProfile().getCompanyName()).isEqualTo("Acme Corp");
        assertThat(result.getRecruiterProfile().getCompanyVerificationStatus()).isEqualTo("VERIFIED");
        assertThat(result.getStudentProfile()).isNull();
    }

    @Test
    @DisplayName("Get user by ID - nonexistent user throws ResourceNotFoundException")
    void testGetUserById_NotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: '999'");
    }

    @Test
    @DisplayName("Update user status - Admin disables student user successfully")
    void testUpdateUserStatus_DisableStudent() {
        UserStatusUpdateRequest req = UserStatusUpdateRequest.builder()
                .enabled(false)
                .reason("Suspicious activity")
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(studentUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(studentProfileRepository.findByUser_Id(2L)).thenReturn(Optional.of(studentProfile));
        when(recruiterProfileRepository.findByUser_Id(2L)).thenReturn(Optional.empty());

        AdminUserSummaryResponse result = adminUserService.updateUserStatus(1L, 2L, req);

        assertThat(result).isNotNull();
        assertThat(result.isEnabled()).isFalse();
        assertThat(studentUser.isEnabled()).isFalse();
        verify(userRepository).save(studentUser);
        verify(notificationService).sendNotification(
                eq(2L),
                eq(1L),
                eq("CareerForge Admin"),
                eq("Account Disabled"),
                contains("student account has been disabled"),
                eq(com.careerforge.entity.enums.NotificationType.ACCOUNT_DISABLED),
                eq("USER"),
                eq(2L)
        );
    }

    @Test
    @DisplayName("Update user status - Admin enables disabled user successfully without sending account disabled notification")
    void testUpdateUserStatus_EnableUser() {
        studentUser.setEnabled(false);
        UserStatusUpdateRequest req = UserStatusUpdateRequest.builder()
                .enabled(true)
                .reason("Verification complete")
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(studentUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(studentProfileRepository.findByUser_Id(2L)).thenReturn(Optional.of(studentProfile));
        when(recruiterProfileRepository.findByUser_Id(2L)).thenReturn(Optional.empty());

        AdminUserSummaryResponse result = adminUserService.updateUserStatus(1L, 2L, req);

        assertThat(result).isNotNull();
        assertThat(result.isEnabled()).isTrue();
        assertThat(studentUser.isEnabled()).isTrue();
        verify(userRepository).save(studentUser);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Update user status - Admin cannot disable their own account (throws BadRequestException)")
    void testUpdateUserStatus_SelfDisableRejected() {
        UserStatusUpdateRequest req = UserStatusUpdateRequest.builder()
                .enabled(false)
                .reason("Attempting to disable self")
                .build();

        assertThatThrownBy(() -> adminUserService.updateUserStatus(1L, 1L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Administrators cannot disable their own account");

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Update user status - Nonexistent user throws ResourceNotFoundException")
    void testUpdateUserStatus_NotFound() {
        UserStatusUpdateRequest req = UserStatusUpdateRequest.builder()
                .enabled(false)
                .reason("Testing not found")
                .build();

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.updateUserStatus(1L, 999L, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: '999'");
    }
}
