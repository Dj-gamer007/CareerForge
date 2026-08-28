package com.careerforge.service;

import com.careerforge.dto.response.StudentApplicationResponse;
import com.careerforge.entity.*;
import com.careerforge.entity.enums.*;
import com.careerforge.repository.*;
import com.careerforge.service.impl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationLifecycleAndPreservationTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private ApplicationStatusHistoryRepository applicationStatusHistoryRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private JobSkillRepository jobSkillRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private StudentProfileService studentProfileService;
    @Mock
    private StorageService storageService;
    @Mock
    private RecruiterService recruiterService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @InjectMocks
    private ResumeServiceImpl resumeService;

    @InjectMocks
    private JobServiceImpl jobService;

    private User studentUser;
    private User recruiterUser;
    private StudentProfile studentProfile;
    private RecruiterProfile recruiterProfile;
    private Company company;
    private Job job;
    private Resume resume;
    private Application application;

    @BeforeEach
    void setUp() {
        studentUser = User.builder()
                .id(1L)
                .email("student@test.com")
                .role(Role.ROLE_STUDENT)
                .enabled(true)
                .build();

        recruiterUser = User.builder()
                .id(2L)
                .email("recruiter@test.com")
                .role(Role.ROLE_RECRUITER)
                .enabled(true)
                .build();

        company = Company.builder()
                .id(10L)
                .name("Acme Corp")
                .verificationStatus(CompanyVerificationStatus.VERIFIED)
                .build();

        recruiterProfile = RecruiterProfile.builder()
                .id(20L)
                .user(recruiterUser)
                .firstName("Jane")
                .lastName("Recruiter")
                .company(company)
                .build();

        studentProfile = StudentProfile.builder()
                .id(30L)
                .user(studentUser)
                .firstName("John")
                .lastName("Student")
                .build();

        job = Job.builder()
                .id(100L)
                .title("Software Engineer")
                .slug("software-engineer")
                .status(JobStatus.PUBLISHED)
                .recruiter(recruiterProfile)
                .company(company)
                .build();

        resume = Resume.builder()
                .id(200L)
                .studentProfile(studentProfile)
                .storedFileName("stored-resume.pdf")
                .originalFileName("john_resume.pdf")
                .isActive(true)
                .build();

        application = Application.builder()
                .id(500L)
                .job(job)
                .studentProfile(studentProfile)
                .resume(resume)
                .status(ApplicationStatus.APPLIED)
                .matchScoreAtApplication(BigDecimal.valueOf(85.0))
                .build();
    }

    @Test
    @DisplayName("Candidate self-withdrawal notifies the recruiter owning the job and updates status")
    void testWithdrawApplication_NotifiesRecruiter() {
        when(studentProfileService.getOrCreateProfileEntity(1L)).thenReturn(studentProfile);
        when(applicationRepository.findByIdAndStudentProfile_Id(500L, 30L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));

        StudentApplicationResponse response = applicationService.withdrawApplication(1L, 500L);

        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);

        // Verify status history recorded
        verify(applicationStatusHistoryRepository).save(argThat(h ->
                h.getToStatus() == ApplicationStatus.WITHDRAWN && "STUDENT".equals(h.getChangedBy())
        ));

        // Verify recruiter notification was sent
        verify(notificationService).sendNotification(
                eq(2L), // recruiter user ID
                eq(1L), // student user ID
                eq("John Student"),
                eq("Candidate Application Withdrawn"),
                contains("withdrawn their application"),
                eq(NotificationType.APPLICATION_UPDATE),
                eq("APPLICATION"),
                eq(500L)
        );
    }

    @Test
    @DisplayName("Resume deletion sets application.resume to null without deleting the application")
    void testDeleteResume_PreservesApplication() {
        when(studentProfileService.getOrCreateProfileEntity(1L)).thenReturn(studentProfile);
        when(resumeRepository.findByIdAndStudentProfile(200L, studentProfile)).thenReturn(Optional.of(resume));
        when(applicationRepository.findAllByResume(resume)).thenReturn(List.of(application));
        when(resumeRepository.findAllByStudentProfileOrderByUploadedAtDesc(studentProfile)).thenReturn(List.of(resume));

        resumeService.deleteResume(1L, 200L);

        // Verify application was NOT deleted
        verify(applicationRepository, never()).deleteAll(anyList());
        verify(applicationRepository, never()).delete(any(Application.class));

        // Verify resume reference was detached safely
        assertThat(application.getResume()).isNull();
        verify(applicationRepository).saveAll(List.of(application));
        verify(resumeRepository).delete(resume);
        verify(storageService).delete("stored-resume.pdf");
    }

    @Test
    @DisplayName("Closing a job preserves candidate application status and notifies candidate")
    void testCloseJob_PreservesApplicationAndNotifiesCandidate() {
        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);
        when(jobRepository.findById(100L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(i -> i.getArgument(0));
        when(jobSkillRepository.findAllByJobWithSkill(any(Job.class))).thenReturn(Collections.emptyList());
        when(applicationRepository.findAllByJob(job)).thenReturn(List.of(application));

        application.setStatus(ApplicationStatus.ACCEPTED);

        jobService.closeJob(2L, 100L);

        assertThat(job.getStatus()).isEqualTo(JobStatus.CLOSED);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED); // Application status unchanged

        // Verify candidate notification
        verify(notificationService).sendNotification(
                eq(1L), // candidate user ID
                eq(2L), // recruiter user ID
                eq("Jane Recruiter"),
                eq("Job Closed"),
                contains("is currently closed"),
                eq(NotificationType.JOB_POSTING_CLOSED),
                eq("JOB"),
                eq(100L)
        );
    }

    @Test
    @DisplayName("Archiving a job preserves candidate application status and notifies candidate")
    void testArchiveJob_PreservesApplicationAndNotifiesCandidate() {
        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);
        when(jobRepository.findById(100L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(i -> i.getArgument(0));
        when(jobSkillRepository.findAllByJobWithSkill(any(Job.class))).thenReturn(Collections.emptyList());
        when(applicationRepository.findAllByJob(job)).thenReturn(List.of(application));

        application.setStatus(ApplicationStatus.SHORTLISTED);

        jobService.archiveJob(2L, 100L);

        assertThat(job.getStatus()).isEqualTo(JobStatus.ARCHIVED);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SHORTLISTED); // Application status unchanged

        // Verify candidate notification
        verify(notificationService).sendNotification(
                eq(1L), // candidate user ID
                eq(2L), // recruiter user ID
                eq("Jane Recruiter"),
                eq("Job Archived"),
                contains("has been archived"),
                eq(NotificationType.JOB_POSTING_ARCHIVED),
                eq("JOB"),
                eq(100L)
        );
    }
}
