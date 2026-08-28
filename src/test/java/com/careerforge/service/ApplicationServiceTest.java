package com.careerforge.service;

import com.careerforge.dto.request.ApplicationNotesRequest;
import com.careerforge.dto.request.ApplicationStatusUpdateRequest;
import com.careerforge.dto.request.ApplicationSubmitRequest;
import com.careerforge.dto.response.SkillMatchResponse;
import com.careerforge.dto.response.StudentApplicationResponse;
import com.careerforge.entity.*;
import com.careerforge.entity.enums.*;
import com.careerforge.exception.BadRequestException;
import com.careerforge.exception.ResourceNotFoundException;
import com.careerforge.repository.*;
import com.careerforge.service.impl.ApplicationServiceImpl;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private StudentProfileService studentProfileService;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private RecruiterProfileRepository recruiterProfileRepository;
    @Mock
    private RecruiterService recruiterService;
    @Mock
    private SkillMatchingService skillMatchingService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private StorageService storageService;
    @Mock
    private ApplicationStatusHistoryRepository applicationStatusHistoryRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    private User studentUser;
    private StudentProfile studentProfile;
    private User recruiterUser;
    private Company company;
    private RecruiterProfile recruiterProfile;
    private Job publishedJob;
    private Resume activeResume;
    private Application application;

    @BeforeEach
    void setUp() {
        studentUser = User.builder().id(1L).email("student@careerforge.local").role(Role.ROLE_STUDENT).build();
        studentProfile = StudentProfile.builder()
                .id(10L)
                .user(studentUser)
                .firstName("Alice")
                .lastName("Wonder")
                .profileCompletionPercentage(80)
                .build();

        recruiterUser = User.builder().id(2L).email("recruiter@careerforge.local").role(Role.ROLE_RECRUITER).build();
        company = Company.builder().id(50L).name("Acme Corp").build();
        recruiterProfile = RecruiterProfile.builder()
                .id(20L)
                .user(recruiterUser)
                .company(company)
                .firstName("Bob")
                .lastName("Recruiter")
                .build();

        publishedJob = Job.builder()
                .id(100L)
                .title("Software Engineer")
                .slug("software-engineer-100")
                .company(company)
                .recruiter(recruiterProfile)
                .status(JobStatus.PUBLISHED)
                .deadline(LocalDateTime.now().plusDays(30))
                .build();

        activeResume = Resume.builder()
                .id(200L)
                .studentProfile(studentProfile)
                .originalFileName("Alice_Resume.pdf")
                .storedFileName("uuid-alice.pdf")
                .contentType("application/pdf")
                .isActive(true)
                .build();

        application = Application.builder()
                .id(500L)
                .studentProfile(studentProfile)
                .job(publishedJob)
                .resume(activeResume)
                .status(ApplicationStatus.APPLIED)
                .matchScoreAtApplication(BigDecimal.valueOf(85.50))
                .build();
    }

    @Test
    @DisplayName("Should submit application successfully with score snapshot and notification")
    void testSubmitApplication_Success() {
        ApplicationSubmitRequest request = ApplicationSubmitRequest.builder()
                .jobId(100L)
                .coverLetter("Excited about this role.")
                .build();

        when(studentProfileService.getOrCreateProfileEntity(1L)).thenReturn(studentProfile);
        when(jobRepository.findById(100L)).thenReturn(Optional.of(publishedJob));
        when(applicationRepository.findByStudentProfile_IdAndJob_Id(10L, 100L)).thenReturn(Optional.empty());
        when(resumeRepository.findByStudentProfileAndIsActiveTrue(studentProfile)).thenReturn(Optional.of(activeResume));

        SkillMatchResponse matchResponse = SkillMatchResponse.builder()
                .overallScore(BigDecimal.valueOf(85.50))
                .build();
        when(skillMatchingService.calculateMatchForStudentAndJob(10L, 100L)).thenReturn(matchResponse);

        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> {
            Application a = i.getArgument(0);
            a.setId(500L);
            return a;
        });

        StudentApplicationResponse response = applicationService.submitApplication(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(500L);
        assertThat(response.getMatchScoreAtApplication()).isEqualByComparingTo("85.50");
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        verify(notificationService).sendNotification(eq(1L), anyString(), anyString(), eq(NotificationType.APPLICATION_UPDATE));
    }

    @Test
    @DisplayName("Should permit re-applying when previously withdrawn")
    void testSubmitApplication_ReapplyAfterWithdrawal_Success() {
        Application withdrawnApp = Application.builder()
                .id(500L)
                .studentProfile(studentProfile)
                .job(publishedJob)
                .resume(activeResume)
                .status(ApplicationStatus.WITHDRAWN)
                .withdrawnAt(LocalDateTime.now().minusDays(2))
                .matchScoreAtApplication(BigDecimal.valueOf(50.0))
                .build();

        ApplicationSubmitRequest request = ApplicationSubmitRequest.builder()
                .jobId(100L)
                .coverLetter("Re-applying with fresh skills.")
                .build();

        when(studentProfileService.getOrCreateProfileEntity(1L)).thenReturn(studentProfile);
        when(jobRepository.findById(100L)).thenReturn(Optional.of(publishedJob));
        when(applicationRepository.findByStudentProfile_IdAndJob_Id(10L, 100L)).thenReturn(Optional.of(withdrawnApp));
        when(resumeRepository.findByStudentProfileAndIsActiveTrue(studentProfile)).thenReturn(Optional.of(activeResume));

        SkillMatchResponse matchResponse = SkillMatchResponse.builder()
                .overallScore(BigDecimal.valueOf(92.00))
                .build();
        when(skillMatchingService.calculateMatchForStudentAndJob(10L, 100L)).thenReturn(matchResponse);

        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));

        StudentApplicationResponse response = applicationService.submitApplication(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(500L);
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(response.getMatchScoreAtApplication()).isEqualByComparingTo("92.00");
    }

    @Test
    @DisplayName("Should reject submission when profile completion < 30%")
    void testSubmitApplication_LowProfileCompletion() {
        studentProfile.setProfileCompletionPercentage(20);
        when(studentProfileService.getOrCreateProfileEntity(1L)).thenReturn(studentProfile);

        ApplicationSubmitRequest request = ApplicationSubmitRequest.builder().jobId(100L).build();

        assertThatThrownBy(() -> applicationService.submitApplication(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Profile completion must be at least 30%");
    }

    @Test
    @DisplayName("Should reject submission when job is unpublished (DRAFT)")
    void testSubmitApplication_UnpublishedJob() {
        publishedJob.setStatus(JobStatus.DRAFT);
        when(studentProfileService.getOrCreateProfileEntity(1L)).thenReturn(studentProfile);
        when(jobRepository.findById(100L)).thenReturn(Optional.of(publishedJob));

        ApplicationSubmitRequest request = ApplicationSubmitRequest.builder().jobId(100L).build();

        assertThatThrownBy(() -> applicationService.submitApplication(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot apply to an unpublished job");
    }

    @Test
    @DisplayName("Should reject submission when job deadline has passed")
    void testSubmitApplication_ExpiredDeadline() {
        publishedJob.setDeadline(LocalDateTime.now().minusDays(1));
        when(studentProfileService.getOrCreateProfileEntity(1L)).thenReturn(studentProfile);
        when(jobRepository.findById(100L)).thenReturn(Optional.of(publishedJob));

        ApplicationSubmitRequest request = ApplicationSubmitRequest.builder().jobId(100L).build();

        assertThatThrownBy(() -> applicationService.submitApplication(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("deadline for this job has passed");
    }

    @Test
    @DisplayName("Should reject duplicate active application submission")
    void testSubmitApplication_Duplicate() {
        Application activeApp = Application.builder()
                .id(500L)
                .studentProfile(studentProfile)
                .job(publishedJob)
                .status(ApplicationStatus.APPLIED)
                .build();

        when(studentProfileService.getOrCreateProfileEntity(1L)).thenReturn(studentProfile);
        when(jobRepository.findById(100L)).thenReturn(Optional.of(publishedJob));
        when(applicationRepository.findByStudentProfile_IdAndJob_Id(10L, 100L)).thenReturn(Optional.of(activeApp));

        ApplicationSubmitRequest request = ApplicationSubmitRequest.builder().jobId(100L).build();

        assertThatThrownBy(() -> applicationService.submitApplication(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already submitted an active application");
    }

    @Test
    @DisplayName("Should reject submission when student has no active resume and none specified")
    void testSubmitApplication_NoActiveResume() {
        when(studentProfileService.getOrCreateProfileEntity(1L)).thenReturn(studentProfile);
        when(jobRepository.findById(100L)).thenReturn(Optional.of(publishedJob));
        when(applicationRepository.findByStudentProfile_IdAndJob_Id(10L, 100L)).thenReturn(Optional.empty());
        when(resumeRepository.findByStudentProfileAndIsActiveTrue(studentProfile)).thenReturn(Optional.empty());

        ApplicationSubmitRequest request = ApplicationSubmitRequest.builder().jobId(100L).build();

        assertThatThrownBy(() -> applicationService.submitApplication(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No active resume found");
    }

    @Test
    @DisplayName("Should allow candidate to withdraw application from APPLIED state")
    void testWithdrawApplication_FromApplied() {
        when(studentProfileService.getOrCreateProfileEntity(1L)).thenReturn(studentProfile);
        when(applicationRepository.findByIdAndStudentProfile_Id(500L, 10L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);

        StudentApplicationResponse response = applicationService.withdrawApplication(1L, 500L);

        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
        assertThat(application.getWithdrawnAt()).isNotNull();
        verify(notificationService).sendNotification(eq(1L), anyString(), anyString(), eq(NotificationType.APPLICATION_UPDATE));
    }

    @Test
    @DisplayName("Should reject withdrawing application from terminal REJECTED state")
    void testWithdrawApplication_TerminalState() {
        application.setStatus(ApplicationStatus.REJECTED);
        when(studentProfileService.getOrCreateProfileEntity(1L)).thenReturn(studentProfile);
        when(applicationRepository.findByIdAndStudentProfile_Id(500L, 10L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.withdrawApplication(1L, 500L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be withdrawn");
    }

    @Test
    @DisplayName("Recruiter transitions APPLIED -> UNDER_REVIEW and sets reviewedAt")
    void testRecruiterTransition_ToUnderReview() {
        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);
        when(applicationRepository.findByIdAndJob_Company_Id(500L, 50L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);
        when(skillMatchingService.calculateMatchForStudentAndJob(10L, 100L)).thenReturn(SkillMatchResponse.builder().build());

        ApplicationStatusUpdateRequest request = ApplicationStatusUpdateRequest.builder()
                .status(ApplicationStatus.UNDER_REVIEW)
                .build();

        applicationService.updateApplicationStatus(2L, 500L, request);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
        assertThat(application.getReviewedAt()).isNotNull();
    }

    @Test
    @DisplayName("Recruiter transitions SHORTLISTED -> INTERVIEW_SCHEDULED with future timestamp and notification")
    void testRecruiterTransition_ToInterviewScheduled() {
        application.setStatus(ApplicationStatus.SHORTLISTED);
        LocalDateTime interviewTime = LocalDateTime.now().plusDays(5);

        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);
        when(applicationRepository.findByIdAndJob_Company_Id(500L, 50L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);
        when(skillMatchingService.calculateMatchForStudentAndJob(10L, 100L)).thenReturn(SkillMatchResponse.builder().build());

        ApplicationStatusUpdateRequest request = ApplicationStatusUpdateRequest.builder()
                .status(ApplicationStatus.INTERVIEW_SCHEDULED)
                .interviewScheduledAt(interviewTime)
                .build();

        applicationService.updateApplicationStatus(2L, 500L, request);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.INTERVIEW_SCHEDULED);
        assertThat(application.getInterviewScheduledAt()).isEqualTo(interviewTime);
        verify(notificationService).sendNotification(
                eq(1L),
                eq(2L),
                eq("Bob Recruiter"),
                eq("Interview Scheduled"),
                contains("An interview has been scheduled"),
                eq(NotificationType.INTERVIEW_INVITE),
                eq("APPLICATION"),
                eq(500L)
        );
    }

    @Test
    @DisplayName("Recruiter transitions to INTERVIEW_SCHEDULED formats notification message in Asia/Kolkata timezone")
    void testRecruiterTransition_ToInterviewScheduled_FormatsNotificationInAsiaKolkata() {
        application.setStatus(ApplicationStatus.SHORTLISTED);
        // 2026-08-29 04:58:00 UTC = 2026-08-29 10:28:00 IST (+5:30)
        LocalDateTime utcInterviewInstant = LocalDateTime.of(2026, 8, 29, 4, 58, 0);

        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);
        when(applicationRepository.findByIdAndJob_Company_Id(500L, 50L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);
        when(skillMatchingService.calculateMatchForStudentAndJob(10L, 100L)).thenReturn(SkillMatchResponse.builder().build());

        ApplicationStatusUpdateRequest request = ApplicationStatusUpdateRequest.builder()
                .status(ApplicationStatus.INTERVIEW_SCHEDULED)
                .interviewScheduledAt(utcInterviewInstant)
                .build();

        applicationService.updateApplicationStatus(2L, 500L, request);

        org.mockito.ArgumentCaptor<String> messageCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(notificationService).sendNotification(
                eq(1L),
                eq(2L),
                eq("Bob Recruiter"),
                eq("Interview Scheduled"),
                messageCaptor.capture(),
                eq(NotificationType.INTERVIEW_INVITE),
                eq("APPLICATION"),
                eq(500L)
        );

        assertThat(messageCaptor.getValue()).contains("Aug 29, 2026 at 10:28 AM");
    }

    @Test
    @DisplayName("Recruiter reschedules interview sends INTERVIEW_RESCHEDULED notification")
    void testRecruiterTransition_RescheduleInterview() {
        application.setStatus(ApplicationStatus.INTERVIEW_SCHEDULED);
        application.setInterviewScheduledAt(LocalDateTime.now().plusDays(2));
        LocalDateTime newInterviewTime = LocalDateTime.now().plusDays(4);

        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);
        when(applicationRepository.findByIdAndJob_Company_Id(500L, 50L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);
        when(skillMatchingService.calculateMatchForStudentAndJob(10L, 100L)).thenReturn(SkillMatchResponse.builder().build());

        ApplicationStatusUpdateRequest request = ApplicationStatusUpdateRequest.builder()
                .status(ApplicationStatus.INTERVIEW_SCHEDULED)
                .interviewScheduledAt(newInterviewTime)
                .build();

        applicationService.updateApplicationStatus(2L, 500L, request);

        verify(notificationService).sendNotification(
                eq(1L),
                eq(2L),
                eq("Bob Recruiter"),
                eq("Interview Rescheduled"),
                contains("has been rescheduled to"),
                eq(NotificationType.INTERVIEW_RESCHEDULED),
                eq("APPLICATION"),
                eq(500L)
        );
    }

    @Test
    @DisplayName("Recruiter transitions UNDER_REVIEW -> SHORTLISTED sends APPLICATION_SHORTLISTED notification")
    void testRecruiterTransition_ToShortlisted() {
        application.setStatus(ApplicationStatus.UNDER_REVIEW);

        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);
        when(applicationRepository.findByIdAndJob_Company_Id(500L, 50L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);
        when(skillMatchingService.calculateMatchForStudentAndJob(10L, 100L)).thenReturn(SkillMatchResponse.builder().build());

        ApplicationStatusUpdateRequest request = ApplicationStatusUpdateRequest.builder()
                .status(ApplicationStatus.SHORTLISTED)
                .build();

        applicationService.updateApplicationStatus(2L, 500L, request);

        verify(notificationService).sendNotification(
                eq(1L),
                eq(2L),
                eq("Bob Recruiter"),
                eq("Application Shortlisted"),
                contains("has been shortlisted by the hiring team"),
                eq(NotificationType.APPLICATION_SHORTLISTED),
                eq("APPLICATION"),
                eq(500L)
        );
    }

    @Test
    @DisplayName("Recruiter transitions INTERVIEW_SCHEDULED -> ACCEPTED sends APPLICATION_ACCEPTED notification")
    void testRecruiterTransition_ToAccepted() {
        application.setStatus(ApplicationStatus.INTERVIEW_SCHEDULED);

        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);
        when(applicationRepository.findByIdAndJob_Company_Id(500L, 50L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);
        when(skillMatchingService.calculateMatchForStudentAndJob(10L, 100L)).thenReturn(SkillMatchResponse.builder().build());

        ApplicationStatusUpdateRequest request = ApplicationStatusUpdateRequest.builder()
                .status(ApplicationStatus.ACCEPTED)
                .build();

        applicationService.updateApplicationStatus(2L, 500L, request);

        verify(notificationService).sendNotification(
                eq(1L),
                eq(2L),
                eq("Bob Recruiter"),
                eq("Application Accepted"),
                contains("has been accepted"),
                eq(NotificationType.APPLICATION_ACCEPTED),
                eq("APPLICATION"),
                eq(500L)
        );
    }

    @Test
    @DisplayName("Recruiter transitions to REJECTED sends APPLICATION_REJECTED notification")
    void testRecruiterTransition_ToRejected() {
        application.setStatus(ApplicationStatus.UNDER_REVIEW);

        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);
        when(applicationRepository.findByIdAndJob_Company_Id(500L, 50L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);
        when(skillMatchingService.calculateMatchForStudentAndJob(10L, 100L)).thenReturn(SkillMatchResponse.builder().build());

        ApplicationStatusUpdateRequest request = ApplicationStatusUpdateRequest.builder()
                .status(ApplicationStatus.REJECTED)
                .build();

        applicationService.updateApplicationStatus(2L, 500L, request);

        verify(notificationService).sendNotification(
                eq(1L),
                eq(2L),
                eq("Bob Recruiter"),
                eq("Application Rejected"),
                contains("was not selected by the hiring team"),
                eq(NotificationType.APPLICATION_REJECTED),
                eq("APPLICATION"),
                eq(500L)
        );
    }

    @Test
    @DisplayName("Recruiter scheduling interview with past timestamp throws BadRequestException")
    void testRecruiterTransition_InterviewPastTimestamp() {
        application.setStatus(ApplicationStatus.SHORTLISTED);
        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);
        when(applicationRepository.findByIdAndJob_Company_Id(500L, 50L)).thenReturn(Optional.of(application));

        ApplicationStatusUpdateRequest request = ApplicationStatusUpdateRequest.builder()
                .status(ApplicationStatus.INTERVIEW_SCHEDULED)
                .interviewScheduledAt(LocalDateTime.now().minusHours(1))
                .build();

        assertThatThrownBy(() -> applicationService.updateApplicationStatus(2L, 500L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be in the future");
    }

    @Test
    @DisplayName("Recruiter invalid state transition (APPLIED -> ACCEPTED) throws BadRequestException")
    void testRecruiterInvalidTransition_ThrowsBadRequest() {
        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);
        when(applicationRepository.findByIdAndJob_Company_Id(500L, 50L)).thenReturn(Optional.of(application));

        ApplicationStatusUpdateRequest request = ApplicationStatusUpdateRequest.builder()
                .status(ApplicationStatus.ACCEPTED)
                .build();

        assertThatThrownBy(() -> applicationService.updateApplicationStatus(2L, 500L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    @DisplayName("Cross-company recruiter access to application throws ResourceNotFoundException (404)")
    void testCrossCompanyAccess_ThrowsNotFound() {
        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);
        when(applicationRepository.findByIdAndJob_Company_Id(500L, 50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getApplicationDetailForRecruiter(2L, 500L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Cross-student access to application throws ResourceNotFoundException (404)")
    void testCrossStudentAccess_ThrowsNotFound() {
        when(studentProfileService.getOrCreateProfileEntity(1L)).thenReturn(studentProfile);
        when(applicationRepository.findByIdAndStudentProfile_Id(500L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getMyApplicationDetail(1L, 500L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
