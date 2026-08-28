package com.careerforge.service;

import com.careerforge.dto.request.AdminJobModerationRequest;
import com.careerforge.dto.request.CompanyVerificationUpdateRequest;
import com.careerforge.dto.response.AdminCompanyDetailResponse;
import com.careerforge.dto.response.AdminCompanySummaryResponse;
import com.careerforge.dto.response.AdminJobDetailResponse;
import com.careerforge.dto.response.AdminJobSummaryResponse;
import com.careerforge.entity.*;
import com.careerforge.entity.enums.*;
import com.careerforge.exception.BadRequestException;
import com.careerforge.exception.ResourceNotFoundException;
import com.careerforge.repository.*;
import com.careerforge.service.impl.AdminModerationServiceImpl;
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminModerationServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private RecruiterProfileRepository recruiterProfileRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobSkillRepository jobSkillRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AdminModerationServiceImpl adminModerationService;

    private Company company;
    private User recruiterUser;
    private RecruiterProfile recruiterProfile;
    private Job job;
    private Skill javaSkill;
    private JobSkill jobSkill;

    @BeforeEach
    void setUp() {
        company = Company.builder()
                .id(10L)
                .name("Acme Corporation")
                .slug("acme-corporation")
                .industry("Technology")
                .location("Bengaluru")
                .companySize("100-500")
                .website("https://acme.example.com")
                .verificationStatus(CompanyVerificationStatus.PENDING)
                .build();

        recruiterUser = User.builder()
                .id(2L)
                .email("recruiter@acme.example.com")
                .role(Role.ROLE_RECRUITER)
                .enabled(true)
                .build();

        recruiterProfile = RecruiterProfile.builder()
                .id(20L)
                .user(recruiterUser)
                .company(company)
                .firstName("John")
                .lastName("Doe")
                .designation("Lead Recruiter")
                .department("HR")
                .phone("+919876543210")
                .isCompanyAdmin(true)
                .build();

        job = Job.builder()
                .id(100L)
                .company(company)
                .recruiter(recruiterProfile)
                .title("Backend Engineer")
                .slug("backend-engineer-abc")
                .description("Build scalable services")
                .location("Bengaluru")
                .workMode(WorkMode.HYBRID)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.MID_LEVEL)
                .salaryMin(new BigDecimal("1200000"))
                .salaryMax(new BigDecimal("1800000"))
                .currency("INR")
                .status(JobStatus.PUBLISHED)
                .build();

        javaSkill = Skill.builder()
                .id(1L)
                .name("Java")
                .category("Programming Languages")
                .build();

        jobSkill = JobSkill.builder()
                .id(1L)
                .job(job)
                .skill(javaSkill)
                .isRequired(true)
                .minimumProficiency(SkillProficiency.INTERMEDIATE)
                .build();
    }

    // ==========================================
    // Company Verification Tests
    // ==========================================

    @Test
    @DisplayName("Get companies - returns paginated summary with recruiter counts")
    void testGetCompanies_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Company> companyPage = new PageImpl<>(List.of(company), pageable, 1);

        when(companyRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(companyPage);
        when(recruiterProfileRepository.findAllByCompany_IdIn(List.of(10L))).thenReturn(List.of(recruiterProfile));
        when(jobRepository.countByCompany_Id(10L)).thenReturn(5L);
        when(jobRepository.countByCompany_IdAndStatus(10L, JobStatus.PUBLISHED)).thenReturn(3L);

        Page<AdminCompanySummaryResponse> result = adminModerationService.getCompanies(null, null, null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        AdminCompanySummaryResponse summary = result.getContent().get(0);
        assertThat(summary.getName()).isEqualTo("Acme Corporation");
        assertThat(summary.getVerificationStatus()).isEqualTo(CompanyVerificationStatus.PENDING);
        assertThat(summary.getTotalJobsCount()).isEqualTo(5L);
        assertThat(summary.getActiveJobsCount()).isEqualTo(3L);
        assertThat(summary.getRecruitersCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Get company by ID - returns detailed company with recruiter roster")
    void testGetCompanyById_Success() {
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(jobRepository.countByCompany_Id(10L)).thenReturn(5L);
        when(jobRepository.countByCompany_IdAndStatus(10L, JobStatus.PUBLISHED)).thenReturn(3L);
        when(recruiterProfileRepository.findAllByCompany_Id(10L)).thenReturn(List.of(recruiterProfile));

        AdminCompanyDetailResponse result = adminModerationService.getCompanyById(10L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("Acme Corporation");
        assertThat(result.getRecruiters()).hasSize(1);
        assertThat(result.getRecruiters().get(0).getEmail()).isEqualTo("recruiter@acme.example.com");
        assertThat(result.getRecruiters().get(0).isCompanyAdmin()).isTrue();
    }

    @Test
    @DisplayName("Get company by ID - nonexistent company throws ResourceNotFoundException")
    void testGetCompanyById_NotFound() {
        when(companyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminModerationService.getCompanyById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Company not found with id: '999'");
    }

    @Test
    @DisplayName("Update company verification - approve status dispatches notification to company recruiters")
    void testUpdateCompanyVerification_Approve() {
        CompanyVerificationUpdateRequest req = CompanyVerificationUpdateRequest.builder()
                .verificationStatus(CompanyVerificationStatus.VERIFIED)
                .reason("Documentation verified")
                .build();

        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(companyRepository.save(any(Company.class))).thenAnswer(i -> i.getArgument(0));
        when(recruiterProfileRepository.findAllByCompany_Id(10L)).thenReturn(List.of(recruiterProfile));

        AdminCompanySummaryResponse result = adminModerationService.updateCompanyVerification(1L, 10L, req);

        assertThat(result).isNotNull();
        assertThat(result.getVerificationStatus()).isEqualTo(CompanyVerificationStatus.VERIFIED);
        assertThat(company.getVerificationStatus()).isEqualTo(CompanyVerificationStatus.VERIFIED);
        verify(notificationService).sendNotification(
                eq(2L),
                eq(1L),
                eq("CareerForge Admin"),
                eq("Company Verified"),
                contains("has been verified by the CareerForge Admin team"),
                eq(NotificationType.COMPANY_VERIFIED),
                eq("COMPANY"),
                eq(10L)
        );
    }

    @Test
    @DisplayName("Update company verification - reject status dispatches notification with reason")
    void testUpdateCompanyVerification_Reject() {
        CompanyVerificationUpdateRequest req = CompanyVerificationUpdateRequest.builder()
                .verificationStatus(CompanyVerificationStatus.REJECTED)
                .reason("Invalid corporate domain")
                .build();

        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(companyRepository.save(any(Company.class))).thenAnswer(i -> i.getArgument(0));
        when(recruiterProfileRepository.findAllByCompany_Id(10L)).thenReturn(List.of(recruiterProfile));

        AdminCompanySummaryResponse result = adminModerationService.updateCompanyVerification(1L, 10L, req);

        assertThat(result).isNotNull();
        assertThat(result.getVerificationStatus()).isEqualTo(CompanyVerificationStatus.REJECTED);
        verify(notificationService).sendNotification(
                eq(2L),
                eq(1L),
                eq("CareerForge Admin"),
                eq("Company Verification Rejected"),
                contains("Invalid corporate domain"),
                eq(NotificationType.COMPANY_VERIFICATION_REJECTED),
                eq("COMPANY"),
                eq(10L)
        );
    }

    @Test
    @DisplayName("Update company verification - pending status dispatches notification")
    void testUpdateCompanyVerification_Pending() {
        company.setVerificationStatus(CompanyVerificationStatus.VERIFIED);
        CompanyVerificationUpdateRequest req = CompanyVerificationUpdateRequest.builder()
                .verificationStatus(CompanyVerificationStatus.PENDING)
                .reason("Under re-review")
                .build();

        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(companyRepository.save(any(Company.class))).thenAnswer(i -> i.getArgument(0));
        when(recruiterProfileRepository.findAllByCompany_Id(10L)).thenReturn(List.of(recruiterProfile));

        AdminCompanySummaryResponse result = adminModerationService.updateCompanyVerification(1L, 10L, req);

        assertThat(result).isNotNull();
        assertThat(result.getVerificationStatus()).isEqualTo(CompanyVerificationStatus.PENDING);
        verify(notificationService).sendNotification(
                eq(2L),
                eq(1L),
                eq("CareerForge Admin"),
                eq("Company Verification Pending"),
                contains("is currently pending admin verification"),
                eq(NotificationType.COMPANY_VERIFICATION_PENDING),
                eq("COMPANY"),
                eq(10L)
        );
    }

    // ==========================================
    // Job Moderation Tests
    // ==========================================

    @Test
    @DisplayName("Get jobs - returns paginated summary with skills and application counts")
    void testGetJobs_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Job> jobPage = new PageImpl<>(List.of(job), pageable, 1);

        when(jobRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(jobPage);
        when(jobSkillRepository.findAllByJob_IdInWithSkill(List.of(100L))).thenReturn(List.of(jobSkill));
        when(applicationRepository.countByJob_Id(100L)).thenReturn(12L);

        Page<AdminJobSummaryResponse> result = adminModerationService.getJobs(null, null, null, null, null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        AdminJobSummaryResponse summary = result.getContent().get(0);
        assertThat(summary.getTitle()).isEqualTo("Backend Engineer");
        assertThat(summary.getStatus()).isEqualTo(JobStatus.PUBLISHED);
        assertThat(summary.getApplicationsCount()).isEqualTo(12L);
        assertThat(summary.getSkills()).hasSize(1);
        assertThat(summary.getSkills().get(0).getSkillName()).isEqualTo("Java");
    }

    @Test
    @DisplayName("Get job by ID - returns detailed job response")
    void testGetJobById_Success() {
        when(jobRepository.findById(100L)).thenReturn(Optional.of(job));
        when(jobSkillRepository.findAllByJobWithSkill(job)).thenReturn(List.of(jobSkill));
        when(applicationRepository.countByJob_Id(100L)).thenReturn(8L);

        AdminJobDetailResponse result = adminModerationService.getJobById(100L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getTitle()).isEqualTo("Backend Engineer");
        assertThat(result.getRecruiterName()).isEqualTo("John Doe");
        assertThat(result.getRecruiterEmail()).isEqualTo("recruiter@acme.example.com");
        assertThat(result.getApplicationsCount()).isEqualTo(8L);
    }

    @Test
    @DisplayName("Moderate job - force close published job dispatches notification to recruiter")
    void testModerateJob_ForceClose() {
        AdminJobModerationRequest req = AdminJobModerationRequest.builder()
                .status(JobStatus.CLOSED)
                .reason("Salary out of compliance")
                .build();

        when(jobRepository.findById(100L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(i -> i.getArgument(0));
        when(jobSkillRepository.findAllByJobWithSkill(job)).thenReturn(List.of(jobSkill));
        when(applicationRepository.countByJob_Id(100L)).thenReturn(5L);

        AdminJobDetailResponse result = adminModerationService.moderateJob(1L, 100L, req);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(JobStatus.CLOSED);
        assertThat(job.getStatus()).isEqualTo(JobStatus.CLOSED);

        verify(notificationService).sendNotification(
                eq(2L),
                eq(1L),
                eq("CareerForge Admin"),
                eq("Job Posting Closed"),
                contains("Salary out of compliance"),
                eq(NotificationType.JOB_POSTING_CLOSED),
                eq("JOB"),
                eq(100L)
        );
    }

    @Test
    @DisplayName("Moderate job - force archive job dispatches notification")
    void testModerateJob_ForceArchive() {
        AdminJobModerationRequest req = AdminJobModerationRequest.builder()
                .status(JobStatus.ARCHIVED)
                .reason("Fraudulent listing")
                .build();

        when(jobRepository.findById(100L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(i -> i.getArgument(0));
        when(jobSkillRepository.findAllByJobWithSkill(job)).thenReturn(List.of(jobSkill));
        when(applicationRepository.countByJob_Id(100L)).thenReturn(0L);

        AdminJobDetailResponse result = adminModerationService.moderateJob(1L, 100L, req);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(JobStatus.ARCHIVED);
        assertThat(job.getStatus()).isEqualTo(JobStatus.ARCHIVED);

        verify(notificationService).sendNotification(
                eq(2L),
                eq(1L),
                eq("CareerForge Admin"),
                eq("Job Posting Archived"),
                contains("Fraudulent listing"),
                eq(NotificationType.JOB_POSTING_ARCHIVED),
                eq("JOB"),
                eq(100L)
        );
    }

    @Test
    @DisplayName("Moderate job - return closed job to draft dispatches notification")
    void testModerateJob_ReturnToDraft() {
        job.setStatus(JobStatus.CLOSED);
        AdminJobModerationRequest req = AdminJobModerationRequest.builder()
                .status(JobStatus.DRAFT)
                .reason("Please update job requirements and resubmit")
                .build();

        when(jobRepository.findById(100L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(i -> i.getArgument(0));
        when(jobSkillRepository.findAllByJobWithSkill(job)).thenReturn(List.of(jobSkill));
        when(applicationRepository.countByJob_Id(100L)).thenReturn(2L);

        AdminJobDetailResponse result = adminModerationService.moderateJob(1L, 100L, req);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(JobStatus.DRAFT);
        assertThat(job.getStatus()).isEqualTo(JobStatus.DRAFT);

        verify(notificationService).sendNotification(
                eq(2L),
                eq(1L),
                eq("CareerForge Admin"),
                eq("Job Posting Moved to Draft"),
                contains("Please update job requirements and resubmit"),
                eq(NotificationType.JOB_POSTING_DRAFTED),
                eq("JOB"),
                eq(100L)
        );
    }

    @Test
    @DisplayName("Moderate job - same target status throws BadRequestException")
    void testModerateJob_SameStatus() {
        AdminJobModerationRequest req = AdminJobModerationRequest.builder()
                .status(JobStatus.PUBLISHED)
                .reason("Already published")
                .build();

        when(jobRepository.findById(100L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> adminModerationService.moderateJob(1L, 100L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Job is already in status PUBLISHED");

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Moderate job - invalid moderation transition (DRAFT -> PUBLISHED directly) throws BadRequestException")
    void testModerateJob_InvalidTransition() {
        job.setStatus(JobStatus.DRAFT);
        AdminJobModerationRequest req = AdminJobModerationRequest.builder()
                .status(JobStatus.PUBLISHED)
                .reason("Cannot directly publish through moderation")
                .build();

        when(jobRepository.findById(100L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> adminModerationService.moderateJob(1L, 100L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid admin moderation transition from DRAFT to PUBLISHED");

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Moderate job - nonexistent job throws ResourceNotFoundException")
    void testModerateJob_NotFound() {
        AdminJobModerationRequest req = AdminJobModerationRequest.builder()
                .status(JobStatus.CLOSED)
                .reason("Not found")
                .build();

        when(jobRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminModerationService.moderateJob(1L, 999L, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Job not found with id: '999'");
    }
}
