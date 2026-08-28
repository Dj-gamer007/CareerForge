package com.careerforge.service.impl;

import com.careerforge.dto.request.AdminJobModerationRequest;
import com.careerforge.dto.request.CompanyVerificationUpdateRequest;
import com.careerforge.dto.response.*;
import com.careerforge.entity.*;
import com.careerforge.entity.enums.ApplicationStatus;
import com.careerforge.entity.enums.CompanyVerificationStatus;
import com.careerforge.entity.enums.JobStatus;
import com.careerforge.entity.enums.JobType;
import com.careerforge.entity.enums.NotificationType;
import com.careerforge.entity.enums.WorkMode;
import com.careerforge.exception.BadRequestException;
import com.careerforge.exception.ResourceNotFoundException;
import com.careerforge.repository.*;
import com.careerforge.service.AdminModerationService;
import com.careerforge.service.NotificationService;
import com.careerforge.specification.AdminJobSpecification;
import com.careerforge.specification.CompanySpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminModerationServiceImpl implements AdminModerationService {

    private final CompanyRepository companyRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;
    private final com.careerforge.service.AuditLogService auditLogService;

    // ==========================================
    // Company Verification & Inspection
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public Page<AdminCompanySummaryResponse> getCompanies(
            String search,
            CompanyVerificationStatus verificationStatus,
            String industry,
            Pageable pageable
    ) {
        Specification<Company> spec = CompanySpecification.buildAdminCompanySpecification(search, verificationStatus, industry);
        Page<Company> page = companyRepository.findAll(spec, pageable);

        if (page.isEmpty()) {
            return page.map(c -> null);
        }

        List<Long> companyIds = page.getContent().stream().map(Company::getId).collect(Collectors.toList());
        List<RecruiterProfile> recruiters = recruiterProfileRepository.findAllByCompany_IdIn(companyIds);
        Map<Long, List<RecruiterProfile>> recruitersByCompanyId = recruiters.stream()
                .filter(r -> r.getCompany() != null)
                .collect(Collectors.groupingBy(r -> r.getCompany().getId()));

        return page.map(company -> {
            long totalJobs = jobRepository.countByCompany_Id(company.getId());
            long activeJobs = jobRepository.countByCompany_IdAndStatus(company.getId(), JobStatus.PUBLISHED);
            int recruiterCount = recruitersByCompanyId.getOrDefault(company.getId(), Collections.emptyList()).size();

            return AdminCompanySummaryResponse.builder()
                    .id(company.getId())
                    .name(company.getName())
                    .slug(company.getSlug())
                    .industry(company.getIndustry())
                    .location(company.getLocation())
                    .companySize(company.getCompanySize())
                    .website(company.getWebsite())
                    .logoUrl(company.getLogoUrl())
                    .verificationStatus(company.getVerificationStatus())
                    .totalJobsCount(totalJobs)
                    .activeJobsCount(activeJobs)
                    .recruitersCount(recruiterCount)
                    .createdAt(company.getCreatedAt())
                    .updatedAt(company.getUpdatedAt())
                    .build();
        });
    }

    @Override
    @Transactional(readOnly = true)
    public AdminCompanyDetailResponse getCompanyById(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", id));

        long totalJobs = jobRepository.countByCompany_Id(company.getId());
        long activeJobs = jobRepository.countByCompany_IdAndStatus(company.getId(), JobStatus.PUBLISHED);

        List<RecruiterProfile> recruiters = recruiterProfileRepository.findAllByCompany_Id(company.getId());
        List<AdminCompanyDetailResponse.CompanyRecruiterSummaryDto> recruiterDtos = recruiters.stream()
                .map(r -> AdminCompanyDetailResponse.CompanyRecruiterSummaryDto.builder()
                        .recruiterId(r.getId())
                        .userId(r.getUser() != null ? r.getUser().getId() : null)
                        .email(r.getUser() != null ? r.getUser().getEmail() : null)
                        .firstName(r.getFirstName())
                        .lastName(r.getLastName())
                        .designation(r.getDesignation())
                        .department(r.getDepartment())
                        .phone(r.getPhone())
                        .isCompanyAdmin(r.isCompanyAdmin())
                        .build())
                .collect(Collectors.toList());

        return AdminCompanyDetailResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .slug(company.getSlug())
                .website(company.getWebsite())
                .logoUrl(company.getLogoUrl())
                .description(company.getDescription())
                .industry(company.getIndustry())
                .companySize(company.getCompanySize())
                .location(company.getLocation())
                .verificationStatus(company.getVerificationStatus())
                .totalJobsCount(totalJobs)
                .activeJobsCount(activeJobs)
                .recruiters(recruiterDtos)
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public AdminCompanySummaryResponse updateCompanyVerification(
            Long adminUserId,
            Long companyId,
            CompanyVerificationUpdateRequest request
    ) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", companyId));

        CompanyVerificationStatus oldStatus = company.getVerificationStatus();
        company.setVerificationStatus(request.getVerificationStatus());
        Company saved = companyRepository.save(company);

        // Notify recruiters of this company
        List<RecruiterProfile> recruiters = recruiterProfileRepository.findAllByCompany_Id(companyId);
        String title;
        String message;
        NotificationType notifType;

        if (request.getVerificationStatus() == CompanyVerificationStatus.VERIFIED) {
            title = "Company Verified";
            message = String.format("Your company '%s' has been verified by the CareerForge Admin team.", company.getName());
            notifType = NotificationType.COMPANY_VERIFIED;
        } else if (request.getVerificationStatus() == CompanyVerificationStatus.REJECTED) {
            title = "Company Verification Rejected";
            String reasonText = (request.getReason() != null && !request.getReason().trim().isEmpty())
                    ? " Reason: " + request.getReason().trim() + "."
                    : "";
            message = String.format("Your company '%s' verification was rejected by the CareerForge Admin team.%s", company.getName(), reasonText);
            notifType = NotificationType.COMPANY_VERIFICATION_REJECTED;
        } else {
            title = "Company Verification Pending";
            message = String.format("Your company '%s' is currently pending admin verification.", company.getName());
            notifType = NotificationType.COMPANY_VERIFICATION_PENDING;
        }

        for (RecruiterProfile recruiter : recruiters) {
            if (recruiter.getUser() != null) {
                notificationService.sendNotification(
                        recruiter.getUser().getId(),
                        adminUserId,
                        "CareerForge Admin",
                        title,
                        message,
                        notifType,
                        "COMPANY",
                        companyId
                );
            }
        }

        log.info("Admin ID: {} updated company ID: {} verification status from {} to {} (Reason: {})",
                adminUserId, companyId, oldStatus, request.getVerificationStatus(), request.getReason());

        String adminEmail = resolveAdminEmail(adminUserId);
        String adminRole = "ROLE_ADMIN";

        auditLogService.logSuccess(
                adminUserId,
                adminEmail,
                adminRole,
                com.careerforge.entity.enums.AuditEventType.COMPANY_VERIFICATION_UPDATED,
                com.careerforge.entity.enums.AuditTargetType.COMPANY,
                saved.getId(),
                saved.getName(),
                request.getReason(),
                Map.of(
                        "companyId", saved.getId(),
                        "companyName", saved.getName(),
                        "previousVerificationStatus", oldStatus.name(),
                        "newVerificationStatus", saved.getVerificationStatus().name()
                )
        );

        long totalJobs = jobRepository.countByCompany_Id(company.getId());
        long activeJobs = jobRepository.countByCompany_IdAndStatus(company.getId(), JobStatus.PUBLISHED);

        return AdminCompanySummaryResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .slug(saved.getSlug())
                .industry(saved.getIndustry())
                .location(saved.getLocation())
                .companySize(saved.getCompanySize())
                .website(saved.getWebsite())
                .logoUrl(saved.getLogoUrl())
                .verificationStatus(saved.getVerificationStatus())
                .totalJobsCount(totalJobs)
                .activeJobsCount(activeJobs)
                .recruitersCount(recruiters.size())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    // ==========================================
    // Job Moderation & Inspection
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public Page<AdminJobSummaryResponse> getJobs(
            String search,
            JobStatus status,
            Long companyId,
            WorkMode workMode,
            JobType jobType,
            Pageable pageable
    ) {
        Specification<Job> spec = AdminJobSpecification.buildAdminJobSpecification(search, status, companyId, workMode, jobType);
        Page<Job> page = jobRepository.findAll(spec, pageable);

        if (page.isEmpty()) {
            return page.map(j -> null);
        }

        List<Long> jobIds = page.getContent().stream().map(Job::getId).collect(Collectors.toList());
        List<JobSkill> allSkills = jobSkillRepository.findAllByJob_IdInWithSkill(jobIds);
        Map<Long, List<JobSkill>> skillsByJobId = allSkills.stream()
                .collect(Collectors.groupingBy(js -> js.getJob().getId()));

        return page.map(job -> {
            List<JobSkill> skills = skillsByJobId.getOrDefault(job.getId(), Collections.emptyList());
            List<JobSkillResponse> skillResponses = skills.stream()
                    .map(this::mapToSkillResponse)
                    .collect(Collectors.toList());
            long applicationCount = applicationRepository.countByJob_Id(job.getId());

            return AdminJobSummaryResponse.builder()
                    .id(job.getId())
                    .title(job.getTitle())
                    .slug(job.getSlug())
                    .companyId(job.getCompany().getId())
                    .companyName(job.getCompany().getName())
                    .companySlug(job.getCompany().getSlug())
                    .recruiterId(job.getRecruiter() != null ? job.getRecruiter().getId() : null)
                    .recruiterName(job.getRecruiter() != null ?
                            job.getRecruiter().getFirstName() + " " + job.getRecruiter().getLastName() : null)
                    .location(job.getLocation())
                    .workMode(job.getWorkMode())
                    .jobType(job.getJobType())
                    .experienceLevel(job.getExperienceLevel())
                    .salaryMin(job.getSalaryMin())
                    .salaryMax(job.getSalaryMax())
                    .currency(job.getCurrency())
                    .status(job.getStatus())
                    .applicationsCount(applicationCount)
                    .deadline(job.getDeadline())
                    .publishedAt(job.getPublishedAt())
                    .createdAt(job.getCreatedAt())
                    .skills(skillResponses)
                    .build();
        });
    }

    @Override
    @Transactional(readOnly = true)
    public AdminJobDetailResponse getJobById(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", id));

        List<JobSkill> skills = jobSkillRepository.findAllByJobWithSkill(job);
        List<JobSkillResponse> skillResponses = skills.stream()
                .map(this::mapToSkillResponse)
                .collect(Collectors.toList());
        long applicationCount = applicationRepository.countByJob_Id(job.getId());

        return AdminJobDetailResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .slug(job.getSlug())
                .description(job.getDescription())
                .companyId(job.getCompany().getId())
                .companyName(job.getCompany().getName())
                .companySlug(job.getCompany().getSlug())
                .companyVerificationStatus(job.getCompany().getVerificationStatus().name())
                .recruiterId(job.getRecruiter() != null ? job.getRecruiter().getId() : null)
                .recruiterName(job.getRecruiter() != null ?
                        job.getRecruiter().getFirstName() + " " + job.getRecruiter().getLastName() : null)
                .recruiterEmail(job.getRecruiter() != null && job.getRecruiter().getUser() != null ?
                        job.getRecruiter().getUser().getEmail() : null)
                .location(job.getLocation())
                .workMode(job.getWorkMode())
                .jobType(job.getJobType())
                .experienceLevel(job.getExperienceLevel())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .currency(job.getCurrency())
                .status(job.getStatus())
                .applicationsCount(applicationCount)
                .deadline(job.getDeadline())
                .publishedAt(job.getPublishedAt())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .skills(skillResponses)
                .build();
    }

    @Override
    @Transactional
    public AdminJobDetailResponse moderateJob(
            Long adminUserId,
            Long jobId,
            AdminJobModerationRequest request
    ) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));

        JobStatus current = job.getStatus();
        JobStatus target = request.getStatus();

        String adminEmail = resolveAdminEmail(adminUserId);
        String adminRole = "ROLE_ADMIN";

        if (current == target) {
            auditLogService.logFailure(
                    adminUserId,
                    adminEmail,
                    adminRole,
                    com.careerforge.entity.enums.AuditEventType.JOB_MODERATION_PERFORMED,
                    com.careerforge.entity.enums.AuditTargetType.JOB,
                    jobId,
                    job.getTitle(),
                    request.getReason(),
                    Map.of(
                            "jobId", jobId,
                            "currentStatus", current.name(),
                            "attemptedStatus", target.name(),
                            "error", "Job is already in status " + target
                    )
            );
            throw new BadRequestException("Job is already in status " + target);
        }

        try {
            validateModerationTransition(current, target);
        } catch (BadRequestException ex) {
            auditLogService.logFailure(
                    adminUserId,
                    adminEmail,
                    adminRole,
                    com.careerforge.entity.enums.AuditEventType.JOB_MODERATION_PERFORMED,
                    com.careerforge.entity.enums.AuditTargetType.JOB,
                    jobId,
                    job.getTitle(),
                    request.getReason(),
                    Map.of(
                            "jobId", jobId,
                            "currentStatus", current.name(),
                            "attemptedStatus", target.name(),
                            "error", ex.getMessage()
                    )
            );
            throw ex;
        }

        job.setStatus(target);
        Job saved = jobRepository.save(job);

        // Notify recruiter of this job
        if (job.getRecruiter() != null && job.getRecruiter().getUser() != null) {
            String title;
            String message;
            NotificationType notifType;
            String reasonText = (request.getReason() != null && !request.getReason().trim().isEmpty())
                    ? " Reason: " + request.getReason().trim() + "."
                    : "";

            if (target == JobStatus.CLOSED) {
                title = "Job Posting Closed";
                message = String.format("Your job posting '%s' has been closed by the CareerForge Admin team.%s", job.getTitle(), reasonText);
                notifType = NotificationType.JOB_POSTING_CLOSED;
            } else if (target == JobStatus.DRAFT) {
                title = "Job Posting Moved to Draft";
                message = String.format("Your job posting '%s' has been moved to draft by the CareerForge Admin team.%s", job.getTitle(), reasonText);
                notifType = NotificationType.JOB_POSTING_DRAFTED;
            } else if (target == JobStatus.ARCHIVED) {
                title = "Job Posting Archived";
                message = String.format("Your job posting '%s' has been archived by the CareerForge Admin team.%s", job.getTitle(), reasonText);
                notifType = NotificationType.JOB_POSTING_ARCHIVED;
            } else if (target == JobStatus.PUBLISHED) {
                title = "Job Posting Published";
                message = String.format("Your job posting '%s' has been published by the CareerForge Admin team.%s", job.getTitle(), reasonText);
                notifType = NotificationType.JOB_POSTING_PUBLISHED;
            } else {
                title = "Job Moderation Notice";
                message = String.format("Your job '%s' status has been changed to %s by an administrator.%s", job.getTitle(), target, reasonText);
                notifType = NotificationType.SYSTEM_ALERT;
            }

            notificationService.sendNotification(
                    job.getRecruiter().getUser().getId(),
                    adminUserId,
                    "CareerForge Admin",
                    title,
                    message,
                    notifType,
                    "JOB",
                    jobId
            );
        }

        // Notify active candidates who applied for this job if closed or archived
        if (target == JobStatus.CLOSED || target == JobStatus.ARCHIVED) {
            List<com.careerforge.entity.Application> candidateApps = applicationRepository.findAllByJob(saved);
            for (com.careerforge.entity.Application app : candidateApps) {
                if (app.getStatus() != ApplicationStatus.WITHDRAWN && app.getStudentProfile() != null && app.getStudentProfile().getUser() != null) {
                    notificationService.sendNotification(
                            app.getStudentProfile().getUser().getId(),
                            adminUserId,
                            "CareerForge Admin",
                            target == JobStatus.CLOSED ? "Job Closed" : "Job Archived",
                            target == JobStatus.CLOSED
                                    ? "The job '" + saved.getTitle() + "' at " + saved.getCompany().getName() + " is currently closed."
                                    : "The job '" + saved.getTitle() + "' at " + saved.getCompany().getName() + " has been archived.",
                            target == JobStatus.CLOSED ? NotificationType.JOB_POSTING_CLOSED : NotificationType.JOB_POSTING_ARCHIVED,
                            "JOB",
                            saved.getId()
                    );
                }
            }
        }

        log.info("Admin ID: {} moderated job ID: {} from {} to {} (Reason: {})",
                adminUserId, jobId, current, target, request.getReason());

        auditLogService.logSuccess(
                adminUserId,
                adminEmail,
                adminRole,
                com.careerforge.entity.enums.AuditEventType.JOB_MODERATION_PERFORMED,
                com.careerforge.entity.enums.AuditTargetType.JOB,
                saved.getId(),
                saved.getTitle(),
                request.getReason(),
                Map.of(
                        "jobId", saved.getId(),
                        "jobTitle", saved.getTitle(),
                        "companyId", saved.getCompany().getId(),
                        "companyName", saved.getCompany().getName(),
                        "previousStatus", current.name(),
                        "newStatus", saved.getStatus().name()
                )
        );

        List<JobSkill> skills = jobSkillRepository.findAllByJobWithSkill(saved);
        List<JobSkillResponse> skillResponses = skills.stream()
                .map(this::mapToSkillResponse)
                .collect(Collectors.toList());
        long applicationCount = applicationRepository.countByJob_Id(saved.getId());

        return AdminJobDetailResponse.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .slug(saved.getSlug())
                .description(saved.getDescription())
                .companyId(saved.getCompany().getId())
                .companyName(saved.getCompany().getName())
                .companySlug(saved.getCompany().getSlug())
                .companyVerificationStatus(saved.getCompany().getVerificationStatus().name())
                .recruiterId(saved.getRecruiter() != null ? saved.getRecruiter().getId() : null)
                .recruiterName(saved.getRecruiter() != null ?
                        saved.getRecruiter().getFirstName() + " " + saved.getRecruiter().getLastName() : null)
                .recruiterEmail(saved.getRecruiter() != null && saved.getRecruiter().getUser() != null ?
                        saved.getRecruiter().getUser().getEmail() : null)
                .location(saved.getLocation())
                .workMode(saved.getWorkMode())
                .jobType(saved.getJobType())
                .experienceLevel(saved.getExperienceLevel())
                .salaryMin(saved.getSalaryMin())
                .salaryMax(saved.getSalaryMax())
                .currency(saved.getCurrency())
                .status(saved.getStatus())
                .applicationsCount(applicationCount)
                .deadline(saved.getDeadline())
                .publishedAt(saved.getPublishedAt())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .skills(skillResponses)
                .build();
    }

    private void validateModerationTransition(JobStatus current, JobStatus target) {
        // Allowed admin transitions:
        // 1. PUBLISHED -> CLOSED (force close)
        // 2. PUBLISHED, DRAFT, CLOSED -> ARCHIVED (force archive)
        // 3. CLOSED, ARCHIVED -> DRAFT (return for corrections)
        boolean allowed = false;

        if (current == JobStatus.PUBLISHED && target == JobStatus.CLOSED) {
            allowed = true;
        } else if ((current == JobStatus.PUBLISHED || current == JobStatus.DRAFT || current == JobStatus.CLOSED)
                && target == JobStatus.ARCHIVED) {
            allowed = true;
        } else if ((current == JobStatus.CLOSED || current == JobStatus.ARCHIVED)
                && target == JobStatus.DRAFT) {
            allowed = true;
        }

        if (!allowed) {
            throw new BadRequestException("Invalid admin moderation transition from " + current + " to " + target);
        }
    }

    private JobSkillResponse mapToSkillResponse(JobSkill jobSkill) {
        return JobSkillResponse.builder()
                .id(jobSkill.getId())
                .skillId(jobSkill.getSkill().getId())
                .skillName(jobSkill.getSkill().getName())
                .category(jobSkill.getSkill().getCategory())
                .isRequired(jobSkill.isRequired())
                .minimumProficiency(jobSkill.getMinimumProficiency())
                .build();
    }

    private String resolveAdminEmail(Long adminUserId) {
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof com.careerforge.security.UserPrincipal principal) {
                return principal.getEmail();
            }
        } catch (Exception ignored) {
        }
        return adminUserId != null ? "admin-" + adminUserId + "@careerforge.local" : "ADMIN";
    }
}
