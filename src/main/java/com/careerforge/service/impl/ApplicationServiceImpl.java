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

        // Send notification to student
        notificationService.sendNotification(
                userId,
                "Application Submitted: " + job.getTitle(),
                "Your application for '" + job.getTitle() + "' at " + job.getCompany().getName() + " has been successfully submitted.",
                NotificationType.APPLICATION_UPDATE
        );

        return mapToStudentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<StudentApplicationResponse> getMyApplications(Long userId, ApplicationStatus status, Pageable pageable) {
        StudentProfile studentProfile = getStudentProfileByUserId(userId);
        Specification<Application> spec = ApplicationSpecification.buildStudentSpecification(studentProfile.getId(), status);
        Page<Application> page = applicationRepository.findAll(spec, pageable);

        List<StudentApplicationResponse> responses = page.getContent().stream()
                .map(this::mapToStudentResponse)
                .collect(Collectors.toList());

        return PagedResponse.of(page, responses);
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

        application.setStatus(ApplicationStatus.WITHDRAWN);
        application.setWithdrawnAt(LocalDateTime.now());

        Application saved = applicationRepository.save(application);
        log.info("Application ID: {} withdrawn by student ID: {}", applicationId, studentProfile.getId());

        notificationService.sendNotification(
                userId,
                "Application Withdrawn",
                "You have withdrawn your application for '" + application.getJob().getTitle() + "'.",
                NotificationType.APPLICATION_UPDATE
        );

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

        // Notify candidate
        Long candidateUserId = application.getStudentProfile().getUser().getId();
        if (targetStatus == ApplicationStatus.INTERVIEW_SCHEDULED) {
            String formattedInterviewTime = application.getInterviewScheduledAt() != null
                    ? application.getInterviewScheduledAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' h:mm a", Locale.ENGLISH))
                    : "TBD";
            notificationService.sendNotification(
                    candidateUserId,
                    "Interview Invitation: " + application.getJob().getTitle(),
                    "Congratulations! An interview has been scheduled for '" + application.getJob().getTitle() +
                            "' at " + application.getJob().getCompany().getName() + " on " + formattedInterviewTime + ".",
                    NotificationType.INTERVIEW_INVITE
            );
        } else {
            notificationService.sendNotification(
                    candidateUserId,
                    "Application Update: " + application.getJob().getTitle(),
                    "Your application status for '" + application.getJob().getTitle() + "' at " +
                            application.getJob().getCompany().getName() + " is now " + targetStatus + ".",
                    NotificationType.APPLICATION_UPDATE
            );
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
}
