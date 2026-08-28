package com.careerforge.service.impl;

import com.careerforge.dto.request.ApplicationNotesRequest;
import com.careerforge.dto.request.ApplicationStatusUpdateRequest;
import com.careerforge.dto.request.ApplicationSubmitRequest;
import com.careerforge.dto.response.*;
import com.careerforge.entity.*;
import com.careerforge.entity.enums.ApplicationStatus;
import com.careerforge.entity.enums.JobStatus;
import com.careerforge.entity.enums.NotificationType;
import com.careerforge.exception.BadRequestException;
import com.careerforge.exception.ResourceNotFoundException;
import com.careerforge.repository.*;
import com.careerforge.service.ApplicationService;
import com.careerforge.service.NotificationService;
import com.careerforge.service.RecruiterService;
import com.careerforge.service.ResumeService;
import com.careerforge.service.SkillMatchingService;
import com.careerforge.service.StorageService;
import com.careerforge.service.StudentProfileService;
import com.careerforge.specification.ApplicationSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentProfileService studentProfileService;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final RecruiterService recruiterService;
    private final SkillMatchingService skillMatchingService;
    private final NotificationService notificationService;
    private final StorageService storageService;
    private final ApplicationStatusHistoryRepository applicationStatusHistoryRepository;
    private final UserRepository userRepository;

    // ==========================================
    // Student Operations
    // ==========================================

    @Override
    @Transactional
    public StudentApplicationResponse submitApplication(Long userId, ApplicationSubmitRequest request) {
        StudentProfile studentProfile = getStudentProfileByUserId(userId);

        if (studentProfile.getProfileCompletionPercentage() == null || studentProfile.getProfileCompletionPercentage() < 30) {
            throw new BadRequestException("Profile completion must be at least 30% to submit a job application. Current: "
                    + (studentProfile.getProfileCompletionPercentage() == null ? 0 : studentProfile.getProfileCompletionPercentage()) + "%");
        }

        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", request.getJobId()));

        if (job.getStatus() != JobStatus.PUBLISHED) {
            throw new BadRequestException("Cannot apply to an unpublished job. Current status: " + job.getStatus());
        }

        if (job.getDeadline() != null && job.getDeadline().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("The application deadline for this job has passed (" + job.getDeadline() + ")");
        }

        // Check existing application status for re-application eligibility
        Optional<Application> existingAppOpt = applicationRepository.findByStudentProfile_IdAndJob_Id(studentProfile.getId(), job.getId());
        if (existingAppOpt.isPresent() && existingAppOpt.get().getStatus() != ApplicationStatus.WITHDRAWN) {
            throw new BadRequestException("You have already submitted an active application for this job");
        }

        // Resolve resume
        Resume resume;
        if (request.getResumeId() != null) {
            resume = resumeRepository.findByIdAndStudentProfile(request.getResumeId(), studentProfile)
                    .orElseThrow(() -> new BadRequestException("Specified resume ID: " + request.getResumeId() + " does not belong to your profile"));
        } else {
            resume = resumeRepository.findByStudentProfileAndIsActiveTrue(studentProfile)
                    .orElseThrow(() -> new BadRequestException("No active resume found. Please specify a resume ID or upload an active resume before applying."));
        }

        // Snapshot match score at the exact moment of application
        SkillMatchResponse matchAnalysis = skillMatchingService.calculateMatchForStudentAndJob(studentProfile.getId(), job.getId());
        BigDecimal matchScoreSnapshot = matchAnalysis.getOverallScore();

        Application application;
        if (existingAppOpt.isPresent()) {
            // Reactivate withdrawn application with fresh snapshot
            application = existingAppOpt.get();
            application.setResume(resume);
            application.setStatus(ApplicationStatus.APPLIED);
            application.setCoverLetter(request.getCoverLetter() != null ? request.getCoverLetter().trim() : null);
            application.setMatchScoreAtApplication(matchScoreSnapshot);
            application.setWithdrawnAt(null);
            application.setReviewedAt(null);
            application.setInterviewScheduledAt(null);
            application.setRecruiterNotes(null);
        } else {
            application = Application.builder()
                    .studentProfile(studentProfile)
                    .job(job)
                    .resume(resume)
                    .status(ApplicationStatus.APPLIED)
                    .coverLetter(request.getCoverLetter() != null ? request.getCoverLetter().trim() : null)
                    .matchScoreAtApplication(matchScoreSnapshot)
                    .build();
        }

        Application saved = applicationRepository.save(application);
        log.info("Application submitted successfully (id: {}) by student ID: {} for job ID: {}",
                saved.getId(), studentProfile.getId(), job.getId());

        // Append initial status history
        ApplicationStatusHistory initialHistory = ApplicationStatusHistory.builder()
                .application(saved)
                .fromStatus(null)
                .toStatus(ApplicationStatus.APPLIED)
                .changedAt(saved.getCreatedAt() != null ? saved.getCreatedAt() : LocalDateTime.now())
                .changedBy("STUDENT")
                .notes("Application submitted by candidate")
                .build();
        applicationStatusHistoryRepository.save(initialHistory);

        // Send notification to student
        notificationService.sendNotification(
                userId,
                "Application Submitted: " + job.getTitle(),
                "Your application for '" + job.getTitle() + "' at " + job.getCompany().getName() + " has been successfully submitted.",
                NotificationType.APPLICATION_UPDATE
        );

        // Send notification to recruiter who owns the job posting
        if (job.getRecruiter() != null && job.getRecruiter().getUser() != null) {
            String candidateName = (studentProfile.getFirstName() + " " + studentProfile.getLastName()).trim();
            notificationService.sendNotification(
                    job.getRecruiter().getUser().getId(),
                    "New application received",
                    candidateName + " applied for " + job.getTitle() + ".",
                    NotificationType.APPLICATION_UPDATE
            );
        }

        return mapToStudentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<StudentApplicationResponse> getMyApplications(Long userId, ApplicationStatus status, String tab, Pageable pageable) {
        StudentProfile studentProfile = getStudentProfileByUserId(userId);
        Specification<Application> spec = ApplicationSpecification.buildStudentSpecification(studentProfile.getId(), status, tab);
        Page<Application> page = applicationRepository.findAll(spec, pageable);

        List<StudentApplicationResponse> responses = page.getContent().stream()
                .map(this::mapToStudentResponse)
                .collect(Collectors.toList());

        return PagedResponse.of(page, responses);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationTabCountsResponse getStudentApplicationCounts(Long userId) {
        StudentProfile studentProfile = getStudentProfileByUserId(userId);
        Long profileId = studentProfile.getId();

        Specification<Application> allSpec = ApplicationSpecification.buildStudentSpecification(profileId, null, "ALL");
        Specification<Application> appliedSpec = ApplicationSpecification.buildStudentSpecification(profileId, null, "APPLIED");
        Specification<Application> shortlistedSpec = ApplicationSpecification.buildStudentSpecification(profileId, null, "SHORTLISTED");
        Specification<Application> interviewSpec = ApplicationSpecification.buildStudentSpecification(profileId, null, "INTERVIEW");

        long allCount = applicationRepository.count(allSpec);
        long appliedCount = applicationRepository.count(appliedSpec);
        long shortlistedCount = applicationRepository.count(shortlistedSpec);
        long interviewCount = applicationRepository.count(interviewSpec);

        return ApplicationTabCountsResponse.builder()
                .all(allCount)
                .applied(appliedCount)
                .shortlisted(shortlistedCount)
                .interview(interviewCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StudentApplicationDetailResponse getMyApplicationDetail(Long userId, Long applicationId) {
        StudentProfile studentProfile = getStudentProfileByUserId(userId);
        Application application = applicationRepository.findByIdAndStudentProfile_Id(applicationId, studentProfile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        SkillMatchResponse liveMatch = skillMatchingService.calculateMatchForStudentAndJob(
                studentProfile.getId(), application.getJob().getId());

        return mapToStudentDetailResponse(application, liveMatch);
    }

    @Override
    @Transactional
    public StudentApplicationResponse withdrawApplication(Long userId, Long applicationId) {
        StudentProfile studentProfile = getStudentProfileByUserId(userId);
        Application application = applicationRepository.findByIdAndStudentProfile_Id(applicationId, studentProfile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        if (application.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new BadRequestException("Application is already withdrawn");
        }

        if (application.getStatus() != ApplicationStatus.APPLIED
                && application.getStatus() != ApplicationStatus.UNDER_REVIEW
                && application.getStatus() != ApplicationStatus.SHORTLISTED
                && application.getStatus() != ApplicationStatus.INTERVIEW_SCHEDULED) {
            throw new BadRequestException("Applications in status '" + application.getStatus() + "' cannot be withdrawn");
        }

        ApplicationStatus previousStatus = application.getStatus();
        application.setStatus(ApplicationStatus.WITHDRAWN);
        application.setWithdrawnAt(LocalDateTime.now());

        Application saved = applicationRepository.save(application);
        log.info("Application ID: {} withdrawn by student ID: {}", applicationId, studentProfile.getId());

        // Append withdrawal status history
        ApplicationStatusHistory withdrawalHistory = ApplicationStatusHistory.builder()
                .application(saved)
                .fromStatus(previousStatus)
                .toStatus(ApplicationStatus.WITHDRAWN)
                .changedAt(saved.getWithdrawnAt())
                .changedBy("STUDENT")
                .notes("Application withdrawn by candidate")
                .build();
        applicationStatusHistoryRepository.save(withdrawalHistory);

        notificationService.sendNotification(
                userId,
                "Application Withdrawn",
                "You have withdrawn your application for '" + application.getJob().getTitle() + "'.",
                NotificationType.APPLICATION_UPDATE
        );

        // Notify recruiter owning the job
        if (saved.getJob() != null && saved.getJob().getRecruiter() != null && saved.getJob().getRecruiter().getUser() != null) {
            String candidateName = ((studentProfile.getFirstName() != null ? studentProfile.getFirstName() : "") + " "
                    + (studentProfile.getLastName() != null ? studentProfile.getLastName() : "")).trim();
            if (candidateName.isEmpty()) {
                candidateName = "Candidate";
            }
            notificationService.sendNotification(
                    saved.getJob().getRecruiter().getUser().getId(),
                    userId,
                    candidateName,
                    "Candidate Application Withdrawn",
                    "Candidate " + candidateName + " has withdrawn their application for '" + saved.getJob().getTitle() + "'.",
                    NotificationType.APPLICATION_UPDATE,
                    "APPLICATION",
                    saved.getId()
            );
        }

        return mapToStudentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SkillMatchResponse previewMatchForJob(Long userId, Long jobId) {
        StudentProfile studentProfile = getStudentProfileByUserId(userId);
        if (!jobRepository.existsById(jobId)) {
            throw new ResourceNotFoundException("Job", "id", jobId);
        }
        return skillMatchingService.calculateMatchForStudentAndJob(studentProfile.getId(), jobId);
    }

    // ==========================================
    // Recruiter Operations
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<RecruiterApplicationSummaryResponse> getJobApplications(
            Long userId,
            Long jobId,
            ApplicationStatus status,
            BigDecimal minScore,
            BigDecimal maxScore,
            String search,
            Pageable pageable) {

        RecruiterProfile recruiter = getValidatedRecruiterWithCompany(userId);
        Long companyId = recruiter.getCompany().getId();

        // Verify the job belongs to recruiter's company
        jobRepository.findByIdAndCompany_Id(jobId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));

        Specification<Application> spec = ApplicationSpecification.buildRecruiterSpecification(
                jobId, companyId, status, minScore, maxScore, search);

        Page<Application> page = applicationRepository.findAll(spec, pageable);
        List<RecruiterApplicationSummaryResponse> responses = page.getContent().stream()
                .map(this::mapToRecruiterSummaryResponse)
                .collect(Collectors.toList());

        return PagedResponse.of(page, responses);
    }

    @Override
    @Transactional(readOnly = true)
    public RecruiterApplicationDetailResponse getApplicationDetailForRecruiter(Long userId, Long applicationId) {
        RecruiterProfile recruiter = getValidatedRecruiterWithCompany(userId);
        Application application = applicationRepository.findByIdAndJob_Company_Id(applicationId, recruiter.getCompany().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        SkillMatchResponse liveMatch = skillMatchingService.calculateMatchForStudentAndJob(
                application.getStudentProfile().getId(), application.getJob().getId());

        return mapToRecruiterDetailResponse(application, liveMatch);
    }

    @Override
    @Transactional
    public RecruiterApplicationDetailResponse updateApplicationStatus(
            Long userId,
            Long applicationId,
            ApplicationStatusUpdateRequest request) {

        RecruiterProfile recruiter = getValidatedRecruiterWithCompany(userId);
        Application application = applicationRepository.findByIdAndJob_Company_Id(applicationId, recruiter.getCompany().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        ApplicationStatus currentStatus = application.getStatus();
        ApplicationStatus targetStatus = request.getStatus();

        validateStateTransition(currentStatus, targetStatus);

        // Transition-specific logic
        if (targetStatus == ApplicationStatus.UNDER_REVIEW) {
            if (application.getReviewedAt() == null) {
                application.setReviewedAt(LocalDateTime.now());
            }
        } else if (targetStatus == ApplicationStatus.SHORTLISTED) {
            if (application.getShortlistedAt() == null) {
                application.setShortlistedAt(LocalDateTime.now());
            }
        } else if (targetStatus == ApplicationStatus.INTERVIEW_SCHEDULED) {
            if (request.getInterviewScheduledAt() == null) {
                throw new BadRequestException("interviewScheduledAt is required when scheduling an interview");
            }
            if (request.getInterviewScheduledAt().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("Interview date and time must be in the future");
            }
            application.setInterviewScheduledAt(request.getInterviewScheduledAt());
        }

        if (request.getRecruiterNotes() != null) {
            application.setRecruiterNotes(request.getRecruiterNotes().trim());
        }

        application.setStatus(targetStatus);
        Application saved = applicationRepository.save(application);
        log.info("Application ID: {} status transitioned from {} to {} by recruiter user ID: {}",
                applicationId, currentStatus, targetStatus, userId);

        // Append transition status history
        ApplicationStatusHistory transitionHistory = ApplicationStatusHistory.builder()
                .application(saved)
                .fromStatus(currentStatus)
                .toStatus(targetStatus)
                .changedAt(LocalDateTime.now())
                .changedBy("RECRUITER")
                .notes(request.getRecruiterNotes() != null ? request.getRecruiterNotes().trim() : null)
                .build();
        applicationStatusHistoryRepository.save(transitionHistory);

        // Notify candidate
        Long candidateUserId = application.getStudentProfile().getUser().getId();
        String recruiterName = (recruiter.getFirstName() + " " + recruiter.getLastName()).trim();
        String candidateName = (application.getStudentProfile().getFirstName() + " " + application.getStudentProfile().getLastName()).trim();
        String jobTitle = application.getJob().getTitle();
        String companyName = application.getJob().getCompany().getName();

        if (targetStatus == ApplicationStatus.INTERVIEW_SCHEDULED) {
            String formattedInterviewTime = "TBD";
            if (application.getInterviewScheduledAt() != null) {
                // Application timestamps are stored in UTC; convert to canonical display zone (Asia/Kolkata)
                ZonedDateTime istZoned = application.getInterviewScheduledAt()
                        .atZone(ZoneOffset.UTC)
                        .withZoneSameInstant(ZoneId.of("Asia/Kolkata"));
                formattedInterviewTime = istZoned.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' h:mm a", Locale.ENGLISH));
            }

            if (currentStatus == ApplicationStatus.INTERVIEW_SCHEDULED) {
                // Interview Rescheduled
                notificationService.sendNotification(
                        candidateUserId,
                        userId,
                        recruiterName,
                        "Interview Rescheduled",
                        "Your interview for '" + jobTitle + "' at " + companyName + " has been rescheduled to " + formattedInterviewTime + ".",
                        NotificationType.INTERVIEW_RESCHEDULED,
                        "APPLICATION",
                        saved.getId()
                );
            } else {
                // First interview scheduling
                notificationService.sendNotification(
                        candidateUserId,
                        userId,
                        recruiterName,
                        "Interview Scheduled",
                        "Congratulations! An interview has been scheduled for '" + jobTitle + "' at " + companyName + " on " + formattedInterviewTime + ".",
                        NotificationType.INTERVIEW_INVITE,
                        "APPLICATION",
                        saved.getId()
                );
            }
        } else if (targetStatus == ApplicationStatus.SHORTLISTED) {
            notificationService.sendNotification(
                    candidateUserId,
                    userId,
                    recruiterName,
                    "Application Shortlisted",
                    "Your application for '" + jobTitle + "' at " + companyName + " has been shortlisted by the hiring team.",
                    NotificationType.APPLICATION_SHORTLISTED,
                    "APPLICATION",
                    saved.getId()
            );
        } else if (targetStatus == ApplicationStatus.ACCEPTED) {
            notificationService.sendNotification(
                    candidateUserId,
                    userId,
                    recruiterName,
                    "Application Accepted",
                    "Congratulations! Your application for '" + jobTitle + "' at " + companyName + " has been accepted.",
                    NotificationType.APPLICATION_ACCEPTED,
                    "APPLICATION",
                    saved.getId()
            );
        } else if (targetStatus == ApplicationStatus.REJECTED) {
            notificationService.sendNotification(
                    candidateUserId,
                    userId,
                    recruiterName,
                    "Application Rejected",
                    "Your application for '" + jobTitle + "' at " + companyName + " was not selected by the hiring team.",
                    NotificationType.APPLICATION_REJECTED,
                    "APPLICATION",
                    saved.getId()
            );
        } else {
            notificationService.sendNotification(
                    candidateUserId,
                    userId,
                    recruiterName,
                    "Application Updated",
                    "Recruiter updated your application for '" + jobTitle + "' at " + companyName + ".",
                    NotificationType.APPLICATION_UPDATED,
                    "APPLICATION",
                    saved.getId()
            );
        }

        // Notify active admins of recruiter application lifecycle change
        List<User> activeAdmins = userRepository.findAllByRoleAndEnabledTrue(com.careerforge.entity.enums.Role.ROLE_ADMIN);
        String adminTitle;
        String adminMessage;
        if (targetStatus == ApplicationStatus.REJECTED) {
            adminTitle = "Application Rejected by Recruiter";
            adminMessage = String.format("Recruiter '%s' rejected the application of '%s' for '%s' at '%s'.",
                    recruiterName, candidateName, jobTitle, companyName);
        } else if (targetStatus == ApplicationStatus.ACCEPTED) {
            adminTitle = "Application Accepted by Recruiter";
            adminMessage = String.format("Recruiter '%s' accepted the application of '%s' for '%s' at '%s'.",
                    recruiterName, candidateName, jobTitle, companyName);
        } else if (targetStatus == ApplicationStatus.SHORTLISTED) {
            adminTitle = "Application Shortlisted by Recruiter";
            adminMessage = String.format("Recruiter '%s' shortlisted the application of '%s' for '%s' at '%s'.",
                    recruiterName, candidateName, jobTitle, companyName);
        } else if (targetStatus == ApplicationStatus.INTERVIEW_SCHEDULED) {
            adminTitle = "Interview Scheduled by Recruiter";
            adminMessage = String.format("Recruiter '%s' scheduled an interview for '%s' for '%s' at '%s'.",
                    recruiterName, candidateName, jobTitle, companyName);
        } else {
            adminTitle = "Application Updated by Recruiter";
            adminMessage = String.format("Recruiter '%s' updated the application of '%s' for '%s' at '%s'.",
                    recruiterName, candidateName, jobTitle, companyName);
        }

        for (User admin : activeAdmins) {
            if (!admin.getId().equals(userId)) {
                notificationService.sendNotification(
                        admin.getId(),
                        userId,
                        recruiterName,
                        adminTitle,
                        adminMessage,
                        NotificationType.APPLICATION_UPDATED,
                        "APPLICATION",
                        saved.getId()
                );
            }
        }

        SkillMatchResponse liveMatch = skillMatchingService.calculateMatchForStudentAndJob(
                saved.getStudentProfile().getId(), saved.getJob().getId());

        return mapToRecruiterDetailResponse(saved, liveMatch);
    }

    @Override
    @Transactional
    public RecruiterApplicationDetailResponse updateApplicationNotes(Long userId, Long applicationId, ApplicationNotesRequest request) {
        RecruiterProfile recruiter = getValidatedRecruiterWithCompany(userId);
        Application application = applicationRepository.findByIdAndJob_Company_Id(applicationId, recruiter.getCompany().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        application.setRecruiterNotes(request.getRecruiterNotes() != null ? request.getRecruiterNotes().trim() : "");
        Application saved = applicationRepository.save(application);
        log.info("Updated recruiter notes for application ID: {} by user ID: {}", applicationId, userId);

        String recruiterName = (recruiter.getFirstName() + " " + recruiter.getLastName()).trim();
        String candidateName = (saved.getStudentProfile().getFirstName() + " " + saved.getStudentProfile().getLastName()).trim();
        String jobTitle = saved.getJob().getTitle();
        String companyName = saved.getJob().getCompany().getName();

        // Notify candidate of update
        Long candidateUserId = saved.getStudentProfile().getUser().getId();
        notificationService.sendNotification(
                candidateUserId,
                userId,
                recruiterName,
                "Application Updated",
                "Recruiter updated your application for '" + jobTitle + "' at " + companyName + ".",
                NotificationType.APPLICATION_UPDATED,
                "APPLICATION",
                saved.getId()
        );

        // Notify active admins
        List<User> activeAdmins = userRepository.findAllByRoleAndEnabledTrue(com.careerforge.entity.enums.Role.ROLE_ADMIN);
        for (User admin : activeAdmins) {
            if (!admin.getId().equals(userId)) {
                notificationService.sendNotification(
                        admin.getId(),
                        userId,
                        recruiterName,
                        "Application Updated by Recruiter",
                        String.format("Recruiter '%s' updated the application of '%s' for '%s' at '%s'.",
                                recruiterName, candidateName, jobTitle, companyName),
                        NotificationType.APPLICATION_UPDATED,
                        "APPLICATION",
                        saved.getId()
                );
            }
        }

        SkillMatchResponse liveMatch = skillMatchingService.calculateMatchForStudentAndJob(
                saved.getStudentProfile().getId(), saved.getJob().getId());

        return mapToRecruiterDetailResponse(saved, liveMatch);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeService.ResumeDownloadResult downloadApplicantResume(Long userId, Long applicationId) {
        RecruiterProfile recruiter = getValidatedRecruiterWithCompany(userId);
        Application application = applicationRepository.findByIdAndJob_Company_Id(applicationId, recruiter.getCompany().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        Resume resume = application.getResume();
        if (resume == null) {
            throw new ResourceNotFoundException("Resume", "applicationId", applicationId);
        }

        Resource resource = storageService.loadAsResource(resume.getStoredFileName());
        return new ResumeService.ResumeDownloadResult(resource, resume.getOriginalFileName(), resume.getContentType());
    }

    // ==========================================
    // Private Helpers & Validation
    // ==========================================

    private void validateStateTransition(ApplicationStatus currentStatus, ApplicationStatus targetStatus) {
        if (currentStatus == targetStatus && currentStatus == ApplicationStatus.INTERVIEW_SCHEDULED) {
            return; // Rescheduling interview is allowed
        }

        if (currentStatus == targetStatus) {
            throw new BadRequestException("Application is already in status: " + targetStatus);
        }

        if (currentStatus == ApplicationStatus.ACCEPTED ||
                currentStatus == ApplicationStatus.REJECTED ||
                currentStatus == ApplicationStatus.WITHDRAWN) {
            throw new BadRequestException("Cannot change status of a terminal application (" + currentStatus + ")");
        }

        boolean valid = switch (currentStatus) {
            case APPLIED -> (targetStatus == ApplicationStatus.UNDER_REVIEW || targetStatus == ApplicationStatus.REJECTED || targetStatus == ApplicationStatus.WITHDRAWN);
            case UNDER_REVIEW -> (targetStatus == ApplicationStatus.SHORTLISTED || targetStatus == ApplicationStatus.REJECTED || targetStatus == ApplicationStatus.WITHDRAWN);
            case SHORTLISTED -> (targetStatus == ApplicationStatus.INTERVIEW_SCHEDULED || targetStatus == ApplicationStatus.REJECTED || targetStatus == ApplicationStatus.WITHDRAWN);
            case INTERVIEW_SCHEDULED -> (targetStatus == ApplicationStatus.ACCEPTED || targetStatus == ApplicationStatus.REJECTED || targetStatus == ApplicationStatus.WITHDRAWN);
            default -> false;
        };

        if (!valid) {
            throw new BadRequestException("Invalid status transition from '" + currentStatus + "' to '" + targetStatus + "'");
        }
    }

    private StudentProfile getStudentProfileByUserId(Long userId) {
        return studentProfileService.getOrCreateProfileEntity(userId);
    }

    private RecruiterProfile getValidatedRecruiterWithCompany(Long userId) {
        RecruiterProfile recruiter = recruiterService.getOrCreateProfileEntity(userId);

        if (recruiter.getCompany() == null) {
            throw new BadRequestException("Recruiter must be associated with a registered company");
        }
        return recruiter;
    }

    private StudentApplicationResponse mapToStudentResponse(Application app) {
        Job job = app.getJob();
        return StudentApplicationResponse.builder()
                .id(app.getId())
                .jobId(job.getId())
                .jobTitle(job.getTitle())
                .jobSlug(job.getSlug())
                .jobStatus(job.getStatus())
                .companyId(job.getCompany().getId())
                .companyName(job.getCompany().getName())
                .companyLogoUrl(job.getCompany().getLogoUrl())
                .location(job.getLocation())
                .workMode(job.getWorkMode())
                .jobType(job.getJobType())
                .experienceLevel(job.getExperienceLevel())
                .status(app.getStatus())
                .matchScoreAtApplication(app.getMatchScoreAtApplication())
                .resumeId(app.getResume() != null ? app.getResume().getId() : null)
                .resumeFileName(app.getResume() != null ? app.getResume().getOriginalFileName() : null)
                .appliedAt(app.getCreatedAt())
                .shortlistedAt(app.getShortlistedAt())
                .interviewScheduledAt(app.getInterviewScheduledAt())
                .withdrawnAt(app.getWithdrawnAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }

    private StudentApplicationDetailResponse mapToStudentDetailResponse(Application app, SkillMatchResponse liveMatch) {
        Job job = app.getJob();
        return StudentApplicationDetailResponse.builder()
                .id(app.getId())
                .jobId(job.getId())
                .jobTitle(job.getTitle())
                .jobSlug(job.getSlug())
                .jobDescription(job.getDescription())
                .jobStatus(job.getStatus())
                .companyId(job.getCompany().getId())
                .companyName(job.getCompany().getName())
                .companyLogoUrl(job.getCompany().getLogoUrl())
                .location(job.getLocation())
                .workMode(job.getWorkMode())
                .jobType(job.getJobType())
                .experienceLevel(job.getExperienceLevel())
                .status(app.getStatus())
                .coverLetter(app.getCoverLetter())
                .matchScoreAtApplication(app.getMatchScoreAtApplication())
                .resumeId(app.getResume() != null ? app.getResume().getId() : null)
                .resumeFileName(app.getResume() != null ? app.getResume().getOriginalFileName() : null)
                .appliedAt(app.getCreatedAt())
                .interviewScheduledAt(app.getInterviewScheduledAt())
                .withdrawnAt(app.getWithdrawnAt())
                .updatedAt(app.getUpdatedAt())
                .currentMatchAnalysis(liveMatch)
                .build();
    }

    private RecruiterApplicationSummaryResponse mapToRecruiterSummaryResponse(Application app) {
        StudentProfile sp = app.getStudentProfile();
        return RecruiterApplicationSummaryResponse.builder()
                .id(app.getId())
                .studentId(sp.getId())
                .candidateName(sp.getFirstName() + " " + sp.getLastName())
                .candidateEmail(sp.getUser() != null ? sp.getUser().getEmail() : null)
                .candidatePhone(sp.getPhone())
                .location(sp.getLocation())
                .profileCompletionPercentage(sp.getProfileCompletionPercentage())
                .matchScoreAtApplication(app.getMatchScoreAtApplication())
                .status(app.getStatus())
                .resumeId(app.getResume() != null ? app.getResume().getId() : null)
                .resumeFileName(app.getResume() != null ? app.getResume().getOriginalFileName() : null)
                .appliedAt(app.getCreatedAt())
                .reviewedAt(app.getReviewedAt())
                .interviewScheduledAt(app.getInterviewScheduledAt())
                .withdrawnAt(app.getWithdrawnAt())
                .build();
    }

    private RecruiterApplicationDetailResponse mapToRecruiterDetailResponse(Application app, SkillMatchResponse liveMatch) {
        StudentProfile sp = app.getStudentProfile();
        return RecruiterApplicationDetailResponse.builder()
                .id(app.getId())
                .studentId(sp.getId())
                .jobId(app.getJob().getId())
                .jobTitle(app.getJob().getTitle())
                .candidateName(sp.getFirstName() + " " + sp.getLastName())
                .candidateEmail(sp.getUser() != null ? sp.getUser().getEmail() : null)
                .candidatePhone(sp.getPhone())
                .candidateLocation(sp.getLocation())
                .candidateBio(sp.getBio())
                .candidateEducationSummary(sp.getEducationSummary())
                .candidateGithubUrl(sp.getGithubUrl())
                .candidateLinkedinUrl(sp.getLinkedinUrl())
                .candidatePortfolioUrl(sp.getPortfolioUrl())
                .profileCompletionPercentage(sp.getProfileCompletionPercentage())
                .matchScoreAtApplication(app.getMatchScoreAtApplication())
                .status(app.getStatus())
                .coverLetter(app.getCoverLetter())
                .recruiterNotes(app.getRecruiterNotes())
                .resumeId(app.getResume() != null ? app.getResume().getId() : null)
                .resumeFileName(app.getResume() != null ? app.getResume().getOriginalFileName() : null)
                .appliedAt(app.getCreatedAt())
                .reviewedAt(app.getReviewedAt())
                .interviewScheduledAt(app.getInterviewScheduledAt())
                .withdrawnAt(app.getWithdrawnAt())
                .updatedAt(app.getUpdatedAt())
                .skillBreakdown(liveMatch)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationStatusHistoryResponse> getApplicationHistoryForStudent(Long userId, Long applicationId) {
        StudentProfile studentProfile = getStudentProfileByUserId(userId);
        Application application = applicationRepository.findByIdAndStudentProfile_Id(applicationId, studentProfile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        List<ApplicationStatusHistory> historyList = applicationStatusHistoryRepository
                .findByApplication_IdOrderByChangedAtAscIdAsc(application.getId());

        if (historyList.isEmpty()) {
            return reconstructLegacyHistory(application);
        }

        return historyList.stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationStatusHistoryResponse> getApplicationHistoryForRecruiter(Long userId, Long applicationId) {
        RecruiterProfile recruiter = getValidatedRecruiterWithCompany(userId);
        Application application = applicationRepository.findByIdAndJob_Company_Id(applicationId, recruiter.getCompany().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        List<ApplicationStatusHistory> historyList = applicationStatusHistoryRepository
                .findByApplication_IdOrderByChangedAtAscIdAsc(application.getId());

        if (historyList.isEmpty()) {
            return reconstructLegacyHistory(application);
        }

        return historyList.stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());
    }

    private List<ApplicationStatusHistoryResponse> reconstructLegacyHistory(Application app) {
        List<ApplicationStatusHistoryResponse> history = new java.util.ArrayList<>();
        LocalDateTime submissionTime = app.getCreatedAt() != null ? app.getCreatedAt() : LocalDateTime.now();

        // 1. Initial submission (always present for any application)
        history.add(ApplicationStatusHistoryResponse.builder()
                .applicationId(app.getId())
                .fromStatus(null)
                .toStatus(ApplicationStatus.APPLIED)
                .changedAt(submissionTime)
                .changedBy("STUDENT")
                .notes("Application submitted by candidate")
                .build());

        // 2. Under Review milestone (if reviewedAt exists or current status has progressed past APPLIED)
        if (app.getReviewedAt() != null || (app.getStatus() != ApplicationStatus.APPLIED && app.getStatus() != ApplicationStatus.WITHDRAWN)) {
            LocalDateTime reviewTime = app.getReviewedAt() != null ? app.getReviewedAt() : submissionTime.plusMinutes(1);
            history.add(ApplicationStatusHistoryResponse.builder()
                    .applicationId(app.getId())
                    .fromStatus(ApplicationStatus.APPLIED)
                    .toStatus(ApplicationStatus.UNDER_REVIEW)
                    .changedAt(reviewTime)
                    .changedBy("RECRUITER")
                    .build());
        }

        // 3. Shortlisted milestone (if shortlistedAt exists or status is SHORTLISTED, INTERVIEW_SCHEDULED, ACCEPTED)
        if (app.getShortlistedAt() != null || app.getStatus() == ApplicationStatus.SHORTLISTED
                || app.getStatus() == ApplicationStatus.INTERVIEW_SCHEDULED || app.getStatus() == ApplicationStatus.ACCEPTED) {
            LocalDateTime shortlistTime = app.getShortlistedAt() != null ? app.getShortlistedAt()
                    : (app.getReviewedAt() != null ? app.getReviewedAt().plusMinutes(1) : submissionTime.plusMinutes(2));
            history.add(ApplicationStatusHistoryResponse.builder()
                    .applicationId(app.getId())
                    .fromStatus(ApplicationStatus.UNDER_REVIEW)
                    .toStatus(ApplicationStatus.SHORTLISTED)
                    .changedAt(shortlistTime)
                    .changedBy("RECRUITER")
                    .build());
        }

        // 4. Interview Scheduled milestone (if interviewScheduledAt exists or status is INTERVIEW_SCHEDULED, ACCEPTED)
        if (app.getInterviewScheduledAt() != null || app.getStatus() == ApplicationStatus.INTERVIEW_SCHEDULED
                || app.getStatus() == ApplicationStatus.ACCEPTED) {
            LocalDateTime interviewTime = app.getInterviewScheduledAt() != null ? app.getInterviewScheduledAt()
                    : (app.getShortlistedAt() != null ? app.getShortlistedAt().plusMinutes(1) : submissionTime.plusMinutes(3));
            history.add(ApplicationStatusHistoryResponse.builder()
                    .applicationId(app.getId())
                    .fromStatus(ApplicationStatus.SHORTLISTED)
                    .toStatus(ApplicationStatus.INTERVIEW_SCHEDULED)
                    .changedAt(interviewTime)
                    .changedBy("RECRUITER")
                    .notes("Interview scheduled")
                    .build());
        }

        // 5. Terminal status events
        if (app.getStatus() == ApplicationStatus.ACCEPTED) {
            ApplicationStatus from = app.getInterviewScheduledAt() != null ? ApplicationStatus.INTERVIEW_SCHEDULED
                    : (app.getShortlistedAt() != null ? ApplicationStatus.SHORTLISTED
                    : (app.getReviewedAt() != null ? ApplicationStatus.UNDER_REVIEW : ApplicationStatus.APPLIED));
            LocalDateTime acceptTime = app.getUpdatedAt() != null ? app.getUpdatedAt() : submissionTime.plusMinutes(4);
            history.add(ApplicationStatusHistoryResponse.builder()
                    .applicationId(app.getId())
                    .fromStatus(from)
                    .toStatus(ApplicationStatus.ACCEPTED)
                    .changedAt(acceptTime)
                    .changedBy("RECRUITER")
                    .notes("Application accepted")
                    .build());
        } else if (app.getStatus() == ApplicationStatus.REJECTED) {
            ApplicationStatus from = app.getInterviewScheduledAt() != null ? ApplicationStatus.INTERVIEW_SCHEDULED
                    : (app.getShortlistedAt() != null ? ApplicationStatus.SHORTLISTED
                    : (app.getReviewedAt() != null ? ApplicationStatus.UNDER_REVIEW : ApplicationStatus.APPLIED));
            LocalDateTime rejectTime = app.getUpdatedAt() != null ? app.getUpdatedAt() : submissionTime.plusMinutes(4);
            history.add(ApplicationStatusHistoryResponse.builder()
                    .applicationId(app.getId())
                    .fromStatus(from)
                    .toStatus(ApplicationStatus.REJECTED)
                    .changedAt(rejectTime)
                    .changedBy("RECRUITER")
                    .notes("Application not selected")
                    .build());
        } else if (app.getStatus() == ApplicationStatus.WITHDRAWN) {
            ApplicationStatus from = app.getInterviewScheduledAt() != null ? ApplicationStatus.INTERVIEW_SCHEDULED
                    : (app.getShortlistedAt() != null ? ApplicationStatus.SHORTLISTED
                    : (app.getReviewedAt() != null ? ApplicationStatus.UNDER_REVIEW : ApplicationStatus.APPLIED));
            LocalDateTime withdrawTime = app.getWithdrawnAt() != null ? app.getWithdrawnAt()
                    : (app.getUpdatedAt() != null ? app.getUpdatedAt() : submissionTime.plusMinutes(4));
            history.add(ApplicationStatusHistoryResponse.builder()
                    .applicationId(app.getId())
                    .fromStatus(from)
                    .toStatus(ApplicationStatus.WITHDRAWN)
                    .changedAt(withdrawTime)
                    .changedBy("STUDENT")
                    .notes("Application withdrawn by candidate")
                    .build());
        }

        return history;
    }

    private ApplicationStatusHistoryResponse mapToHistoryResponse(ApplicationStatusHistory history) {
        return ApplicationStatusHistoryResponse.builder()
                .id(history.getId())
                .applicationId(history.getApplication().getId())
                .fromStatus(history.getFromStatus())
                .toStatus(history.getToStatus())
                .changedAt(history.getChangedAt())
                .changedBy(history.getChangedBy())
                .reason(history.getReason())
                .notes(history.getNotes())
                .build();
    }
}
