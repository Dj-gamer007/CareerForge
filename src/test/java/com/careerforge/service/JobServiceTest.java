package com.careerforge.service;

import com.careerforge.dto.request.JobCreateRequest;
import com.careerforge.dto.request.JobSkillItemRequest;
import com.careerforge.dto.response.JobDetailResponse;
import com.careerforge.entity.*;
import com.careerforge.entity.enums.*;
import com.careerforge.exception.BadRequestException;
import com.careerforge.exception.ResourceNotFoundException;
import com.careerforge.repository.JobRepository;
import com.careerforge.repository.JobSkillRepository;
import com.careerforge.repository.SkillRepository;
import com.careerforge.repository.UserRepository;
import com.careerforge.service.impl.JobServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;
    @Mock
    private JobSkillRepository jobSkillRepository;
    @Mock
    private SkillRepository skillRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private com.careerforge.repository.ApplicationRepository applicationRepository;
    @Mock
    private com.careerforge.repository.SavedJobRepository savedJobRepository;
    @Mock
    private RecruiterService recruiterService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private JobServiceImpl jobService;

    private User recruiterUser;
    private Company testCompany;
    private RecruiterProfile recruiterProfile;
    private Job testJob;
    private Skill javaSkill;

    @BeforeEach
    void setUp() {
        recruiterUser = User.builder().id(2L).email("recruiter@careerforge.local").role(Role.ROLE_RECRUITER).build();

        testCompany = Company.builder()
                .id(10L)
                .name("Acme Corp")
                .slug("acme-corp")
                .industry("Tech")
                .verificationStatus(CompanyVerificationStatus.VERIFIED)
                .build();

        recruiterProfile = RecruiterProfile.builder()
                .id(20L)
                .user(recruiterUser)
                .company(testCompany)
                .firstName("John")
                .lastName("Recruiter")
                .designation("Talent Lead")
                .isCompanyAdmin(true)
                .build();

        testJob = Job.builder()
                .id(100L)
                .company(testCompany)
                .recruiter(recruiterProfile)
                .title("Software Engineer")
                .slug("software-engineer-123456")
                .description("Build high scale services using Java and Spring Boot.")
                .location("Bengaluru, India")
                .workMode(WorkMode.HYBRID)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.MID_LEVEL)
                .salaryMin(new BigDecimal("1200000"))
                .salaryMax(new BigDecimal("1800000"))
                .currency("INR")
                .status(JobStatus.DRAFT)
                .deadline(LocalDateTime.now().plusDays(30))
                .build();

        javaSkill = Skill.builder()
                .id(1L)
                .name("Java")
                .category("Backend")
                .build();
    }

    @Test
    @DisplayName("Should create job as DRAFT with skills successfully")
    void testCreateJob_Success() {
        JobCreateRequest request = JobCreateRequest.builder()
                .title("Software Engineer")
                .description("Build high scale services using Java and Spring Boot.")
                .location("Bengaluru, India")
                .workMode(WorkMode.HYBRID)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.MID_LEVEL)
                .salaryMin(new BigDecimal("1200000"))
                .salaryMax(new BigDecimal("1800000"))
                .currency("INR")
                .deadline(LocalDateTime.now().plusDays(30))
                .skills(List.of(
                        JobSkillItemRequest.builder()
                                .skillId(1L)
                                .isRequired(true)
                                .minimumProficiency(SkillProficiency.ADVANCED)
                                .build()
                ))
                .build();

        User admin = User.builder().id(1L).email("admin@careerforge.local").role(Role.ROLE_ADMIN).enabled(true).build();
        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);
        when(jobRepository.existsBySlug(anyString())).thenReturn(false);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job j = invocation.getArgument(0);
            j.setId(100L);
            return j;
        });
        when(skillRepository.findById(1L)).thenReturn(Optional.of(javaSkill));
        when(jobSkillRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<JobSkill> skills = invocation.getArgument(0);
            return skills;
        });
        when(userRepository.findAllByRoleAndEnabledTrue(Role.ROLE_ADMIN)).thenReturn(List.of(admin));

        JobDetailResponse response = jobService.createJob(2L, request);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Software Engineer");
        assertThat(response.getStatus()).isEqualTo(JobStatus.DRAFT);
        assertThat(response.getCurrency()).isEqualTo("INR");
        verify(jobRepository).save(any(Job.class));
        verify(notificationService).sendNotification(
                eq(1L),
                eq("New Job Pending Moderation"),
                contains("Software Engineer"),
                eq(NotificationType.SYSTEM_ALERT)
        );
    }

    @Test
    @DisplayName("Should reject job creation when salaryMin > salaryMax")
    void testCreateJob_InvalidSalary() {
        JobCreateRequest request = JobCreateRequest.builder()
                .title("Software Engineer")
                .description("Sample description")
                .location("Bengaluru")
                .workMode(WorkMode.REMOTE)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.MID_LEVEL)
                .salaryMin(new BigDecimal("2000000"))
                .salaryMax(new BigDecimal("1000000"))
                .build();

        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);

        assertThatThrownBy(() -> jobService.createJob(2L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Minimum salary cannot exceed maximum salary");
    }

    @Test
    @DisplayName("Should publish draft job when requirements and future deadline are met and notify students")
    void testPublishJob_Success() {
        User student = User.builder().id(10L).email("student@careerforge.local").role(Role.ROLE_STUDENT).enabled(true).build();
        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);
        when(jobRepository.findById(100L)).thenReturn(Optional.of(testJob));
        when(jobSkillRepository.countByJobAndIsRequiredTrue(testJob)).thenReturn(1L);
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);
        when(jobSkillRepository.findAllByJobWithSkill(testJob)).thenReturn(Collections.emptyList());
        when(userRepository.findAllByRoleAndEnabledTrue(Role.ROLE_STUDENT)).thenReturn(List.of(student));

        JobDetailResponse response = jobService.publishJob(2L, 100L);

        assertThat(response).isNotNull();
        assertThat(testJob.getStatus()).isEqualTo(JobStatus.PUBLISHED);
        assertThat(testJob.getPublishedAt()).isNotNull();
        verify(notificationService).sendNotification(
                eq(10L),
                eq("New Job Posted"),
                contains("Software Engineer"),
                eq(NotificationType.JOB_RECOMMENDATION)
        );
    }

    @Test
    @DisplayName("Should reject publishing job with past deadline")
    void testPublishJob_PastDeadline() {
        testJob.setDeadline(LocalDateTime.now().minusDays(1));

        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);
        when(jobRepository.findById(100L)).thenReturn(Optional.of(testJob));

        assertThatThrownBy(() -> jobService.publishJob(2L, 100L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("deadline in the past");
    }

    @Test
    @DisplayName("Should reject publishing job without required skills")
    void testPublishJob_NoRequiredSkills() {
        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);
        when(jobRepository.findById(100L)).thenReturn(Optional.of(testJob));
        when(jobSkillRepository.countByJobAndIsRequiredTrue(testJob)).thenReturn(0L);

        assertThatThrownBy(() -> jobService.publishJob(2L, 100L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least one required skill");
    }

    @Test
    @DisplayName("Should unpublish PUBLISHED job back to DRAFT")
    void testUnpublishJob_Success() {
        testJob.setStatus(JobStatus.PUBLISHED);

        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);
        when(jobRepository.findById(100L)).thenReturn(Optional.of(testJob));
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);
        when(jobSkillRepository.findAllByJobWithSkill(testJob)).thenReturn(Collections.emptyList());

        JobDetailResponse response = jobService.unpublishJob(2L, 100L);

        assertThat(response).isNotNull();
        assertThat(testJob.getStatus()).isEqualTo(JobStatus.DRAFT);
    }

    @Test
    @DisplayName("Should close PUBLISHED job")
    void testCloseJob_Success() {
        testJob.setStatus(JobStatus.PUBLISHED);

        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);
        when(jobRepository.findById(100L)).thenReturn(Optional.of(testJob));
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);
        when(jobSkillRepository.findAllByJobWithSkill(testJob)).thenReturn(Collections.emptyList());

        JobDetailResponse response = jobService.closeJob(2L, 100L);

        assertThat(response).isNotNull();
        assertThat(testJob.getStatus()).isEqualTo(JobStatus.CLOSED);
    }

    @Test
    @DisplayName("Should reopen CLOSED job")
    void testReopenJob_Success() {
        testJob.setStatus(JobStatus.CLOSED);
        testJob.setDeadline(LocalDateTime.now().plusDays(10));

        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);
        when(jobRepository.findById(100L)).thenReturn(Optional.of(testJob));
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);
        when(jobSkillRepository.findAllByJobWithSkill(testJob)).thenReturn(Collections.emptyList());

        JobDetailResponse response = jobService.reopenJob(2L, 100L);

        assertThat(response).isNotNull();
        assertThat(testJob.getStatus()).isEqualTo(JobStatus.PUBLISHED);
    }

    @Test
    @DisplayName("Should archive DRAFT or CLOSED job")
    void testArchiveJob_Success() {
        testJob.setStatus(JobStatus.CLOSED);

        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);
        when(jobRepository.findById(100L)).thenReturn(Optional.of(testJob));
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);
        when(jobSkillRepository.findAllByJobWithSkill(testJob)).thenReturn(Collections.emptyList());

        JobDetailResponse response = jobService.archiveJob(2L, 100L);

        assertThat(response).isNotNull();
        assertThat(testJob.getStatus()).isEqualTo(JobStatus.ARCHIVED);
    }

    @Test
    @DisplayName("Should reject deleting a PUBLISHED job")
    void testDeleteJob_RejectPublished() {
        testJob.setStatus(JobStatus.PUBLISHED);

        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);
        when(jobRepository.findById(100L)).thenReturn(Optional.of(testJob));

        assertThatThrownBy(() -> jobService.deleteJob(2L, 100L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot delete a PUBLISHED job");
    }

    @Test
    @DisplayName("Should successfully delete DRAFT/ARCHIVED job and notify active applicants and admins")
    void testDeleteJob_WithApplicants_DispatchesNotificationsAndAudit() {
        testJob.setStatus(JobStatus.DRAFT);

        User studentUser1 = User.builder().id(50L).email("student1@careerforge.local").role(Role.ROLE_STUDENT).build();
        User studentUser2 = User.builder().id(51L).email("student2@careerforge.local").role(Role.ROLE_STUDENT).build();
        StudentProfile studentProfile1 = StudentProfile.builder().id(500L).user(studentUser1).firstName("Alice").lastName("Smith").build();
        StudentProfile studentProfile2 = StudentProfile.builder().id(501L).user(studentUser2).firstName("Bob").lastName("Jones").build();

        Application app1 = Application.builder().id(1001L).job(testJob).studentProfile(studentProfile1).status(ApplicationStatus.APPLIED).build();
        Application app2 = Application.builder().id(1002L).job(testJob).studentProfile(studentProfile2).status(ApplicationStatus.SHORTLISTED).build();

        User adminUser = User.builder().id(99L).email("admin@careerforge.local").role(Role.ROLE_ADMIN).build();

        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);
        when(jobRepository.findById(100L)).thenReturn(Optional.of(testJob));
        when(applicationRepository.findAllByJob(testJob)).thenReturn(List.of(app1, app2));
        when(userRepository.findAllByRoleAndEnabledTrue(Role.ROLE_ADMIN)).thenReturn(List.of(adminUser));

        jobService.deleteJob(2L, 100L);

        // Verify notifications sent to applicants
        verify(notificationService, times(1)).sendNotification(
                eq(50L), eq(2L), anyString(), eq("Job Position Deleted"),
                contains("has been deleted due to business requirements"),
                eq(NotificationType.JOB_POSTING_CLOSED), eq("JOB"), eq(100L)
        );
        verify(notificationService, times(1)).sendNotification(
                eq(51L), eq(2L), anyString(), eq("Job Position Deleted"),
                contains("has been deleted due to business requirements"),
                eq(NotificationType.JOB_POSTING_CLOSED), eq("JOB"), eq(100L)
        );

        // Verify admin notification
        verify(notificationService, times(1)).sendNotification(
                eq(99L), eq(2L), anyString(), eq("Job Deleted"),
                contains("All affected applicants have been notified"),
                eq(NotificationType.SYSTEM_ALERT), eq("JOB"), eq(100L)
        );

        // Verify entity deletions
        verify(savedJobRepository, times(1)).deleteAllByJob(testJob);
        verify(jobSkillRepository, times(1)).deleteAllByJob(testJob);
        verify(applicationRepository, times(1)).deleteAll(List.of(app1, app2));
        verify(jobRepository, times(1)).delete(testJob);

        // Verify audit log
        verify(auditLogService, times(1)).logSuccess(
                eq(2L), anyString(), eq("ROLE_RECRUITER"), eq(AuditEventType.JOB_DELETED),
                eq(AuditTargetType.JOB), eq(100L), eq("Software Engineer"), anyString(), anyMap()
        );
    }

    @Test
    @DisplayName("Should successfully delete job with zero applicants and record audit log")
    void testDeleteJob_ZeroApplicants() {
        testJob.setStatus(JobStatus.ARCHIVED);
        User adminUser = User.builder().id(99L).email("admin@careerforge.local").role(Role.ROLE_ADMIN).build();

        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);
        when(jobRepository.findById(100L)).thenReturn(Optional.of(testJob));
        when(applicationRepository.findAllByJob(testJob)).thenReturn(Collections.emptyList());
        when(userRepository.findAllByRoleAndEnabledTrue(Role.ROLE_ADMIN)).thenReturn(List.of(adminUser));

        jobService.deleteJob(2L, 100L);

        verify(notificationService, never()).sendNotification(
                anyLong(), anyLong(), anyString(), eq("Job Position Deleted"), anyString(), any(), anyString(), anyLong()
        );
        verify(notificationService, times(1)).sendNotification(
                eq(99L), eq(2L), anyString(), eq("Job Deleted"), anyString(), eq(NotificationType.SYSTEM_ALERT), eq("JOB"), eq(100L)
        );
        verify(savedJobRepository, times(1)).deleteAllByJob(testJob);
        verify(jobSkillRepository, times(1)).deleteAllByJob(testJob);
        verify(jobRepository, times(1)).delete(testJob);
        verify(auditLogService, times(1)).logSuccess(
                eq(2L), anyString(), eq("ROLE_RECRUITER"), eq(AuditEventType.JOB_DELETED),
                eq(AuditTargetType.JOB), eq(100L), eq("Software Engineer"), anyString(), anyMap()
        );
    }

    @Test
    @DisplayName("Should enforce cross-company job access isolation")
    void testCrossCompanyIsolation_RejectAccess() {
        Company otherCompany = Company.builder().id(999L).name("Other Corp").build();
        testJob.setCompany(otherCompany);

        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);
        when(jobRepository.findById(100L)).thenReturn(Optional.of(testJob));

        assertThatThrownBy(() -> jobService.getJobDetailForRecruiter(2L, 100L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should preserve required boolean flag for both required and optional skills in JobDetailResponse")
    void testJobSkillsRequiredOptionalPreservation() {
        testJob.setStatus(JobStatus.PUBLISHED);

        Skill javaSkill = Skill.builder().id(1L).name("Java").category("Backend").build();
        Skill mysqlSkill = Skill.builder().id(2L).name("MySQL").category("Database").build();

        JobSkill reqSkill = JobSkill.builder().id(10L).job(testJob).skill(javaSkill).isRequired(true).minimumProficiency(SkillProficiency.INTERMEDIATE).build();
        JobSkill optSkill = JobSkill.builder().id(11L).job(testJob).skill(mysqlSkill).isRequired(false).minimumProficiency(SkillProficiency.INTERMEDIATE).build();

        when(jobRepository.findById(100L)).thenReturn(Optional.of(testJob));
        when(jobSkillRepository.findAllByJobWithSkill(testJob)).thenReturn(List.of(reqSkill, optSkill));

        JobDetailResponse response = jobService.getPublicJobDetail(100L);

        assertThat(response).isNotNull();
        assertThat(response.getSkills()).hasSize(2);
        assertThat(response.getSkills().get(0).getSkillName()).isEqualTo("Java");
        assertThat(response.getSkills().get(0).isRequired()).isTrue();
        assertThat(response.getSkills().get(1).getSkillName()).isEqualTo("MySQL");
        assertThat(response.getSkills().get(1).isRequired()).isFalse();
    }
}
