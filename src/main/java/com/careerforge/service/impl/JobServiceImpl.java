package com.careerforge.service.impl;

import com.careerforge.dto.request.*;
import com.careerforge.dto.response.*;
import com.careerforge.entity.*;
import com.careerforge.entity.enums.JobStatus;
import com.careerforge.entity.enums.WorkMode;
import com.careerforge.exception.BadRequestException;
import com.careerforge.exception.ResourceNotFoundException;
import com.careerforge.repository.JobRepository;
import com.careerforge.repository.JobSkillRepository;
import com.careerforge.repository.SkillRepository;
import com.careerforge.service.JobService;
import com.careerforge.service.RecruiterService;
import com.careerforge.specification.JobSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final SkillRepository skillRepository;
    private final RecruiterService recruiterService;
    private final com.careerforge.service.AuditLogService auditLogService;

    // ==========================================
    // Recruiter Operations
    // ==========================================

    @Override
    @Transactional
    public JobDetailResponse createJob(Long userId, JobCreateRequest request) {
        RecruiterProfile recruiter = getValidatedRecruiterWithCompany(userId);
        validateSalaryRange(request.getSalaryMin(), request.getSalaryMax());

        String slug = generateUniqueJobSlug(request.getTitle());

        String location = StringUtils.hasText(request.getLocation()) ? request.getLocation().trim() : (request.getWorkMode() == WorkMode.REMOTE ? "Remote" : "Not Specified");

        Job job = Job.builder()
                .company(recruiter.getCompany())
                .recruiter(recruiter)
                .title(request.getTitle().trim())
                .slug(slug)
                .description(request.getDescription().trim())
                .location(location)
                .workMode(request.getWorkMode())
                .jobType(request.getJobType())
                .experienceLevel(request.getExperienceLevel())
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .currency(StringUtils.hasText(request.getCurrency()) ? request.getCurrency().trim().toUpperCase() : "INR")
                .status(JobStatus.DRAFT)
                .deadline(request.getDeadline())
                .build();

        Job savedJob = jobRepository.save(job);

        // Attach skills if provided
        List<JobSkill> attachedSkills = attachSkillsToJob(savedJob, request.getSkills());

        log.info("Created job '{}' (id: {}) as DRAFT for company ID: {} by user ID: {}",
                savedJob.getTitle(), savedJob.getId(), recruiter.getCompany().getId(), userId);

        return mapToDetailResponse(savedJob, attachedSkills);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<JobSummaryResponse> getCompanyJobs(Long userId, JobStatus status, Pageable pageable) {
        RecruiterProfile recruiter = getValidatedRecruiterWithCompany(userId);
        Long companyId = recruiter.getCompany().getId();

        Page<Job> page = (status != null) ?
                jobRepository.findAllByCompany_IdAndStatus(companyId, status, pageable) :
                jobRepository.findAllByCompany_Id(companyId, pageable);

        return mapToPagedSummaryResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public JobDetailResponse getJobDetailForRecruiter(Long userId, Long jobId) {
        RecruiterProfile recruiter = getValidatedRecruiterWithCompany(userId);
        Job job = getJobWithOwnershipCheck(jobId, recruiter);
        List<JobSkill> skills = jobSkillRepository.findAllByJobWithSkill(job);
        return mapToDetailResponse(job, skills);
    }

    @Override
    @Transactional
    public JobDetailResponse updateJob(Long userId, Long jobId, JobUpdateRequest request) {
        RecruiterProfile recruiter = getValidatedRecruiterWithCompany(userId);
        Job job = getJobWithOwnershipCheck(jobId, recruiter);
        validateSalaryRange(request.getSalaryMin(), request.getSalaryMax());

        job.setTitle(request.getTitle().trim());
        job.setDescription(request.getDescription().trim());
        String location = StringUtils.hasText(request.getLocation()) ? request.getLocation().trim() : (request.getWorkMode() == WorkMode.REMOTE ? "Remote" : "Not Specified");
        job.setLocation(location);
        job.setWorkMode(request.getWorkMode());
        job.setJobType(request.getJobType());
        job.setExperienceLevel(request.getExperienceLevel());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        if (StringUtils.hasText(request.getCurrency())) {
            job.setCurrency(request.getCurrency().trim().toUpperCase());
        }
        job.setDeadline(request.getDeadline());

        Job updatedJob = jobRepository.save(job);

        // Replace skills
        jobSkillRepository.deleteAllByJob(updatedJob);
        List<JobSkill> attachedSkills = attachSkillsToJob(updatedJob, request.getSkills());

        log.info("Updated job ID: {} by user ID: {}", jobId, userId);
        return mapToDetailResponse(updatedJob, attachedSkills);
    }

    @Override
    @Transactional
    public JobDetailResponse publishJob(Long userId, Long jobId) {
        RecruiterProfile recruiter = getValidatedRecruiterWithCompany(userId);
        Job job = getJobWithOwnershipCheck(jobId, recruiter);

        if (job.getStatus() == JobStatus.PUBLISHED) {
            throw new BadRequestException("Job is already published");
        }

        if (job.getCompany().getVerificationStatus() != com.careerforge.entity.enums.CompanyVerificationStatus.VERIFIED) {
            auditLogService.logFailure(
                    userId,
                    recruiter.getUser() != null ? recruiter.getUser().getEmail() : "RECRUITER",
                    "ROLE_RECRUITER",
                    com.careerforge.entity.enums.AuditEventType.JOB_PUBLISH_GUARD_BLOCKED,
                    com.careerforge.entity.enums.AuditTargetType.JOB,
                    jobId,
                    job.getTitle(),
                    "Cannot publish jobs for an unverified company",
                    Map.of(
                            "jobId", jobId,
                            "companyId", job.getCompany().getId(),
                            "companyName", job.getCompany().getName(),
                            "companyVerificationStatus", job.getCompany().getVerificationStatus().name()
                    )
            );
            throw new BadRequestException("Cannot publish jobs for an unverified company. Current verification status: " + job.getCompany().getVerificationStatus());
        }

        // Validate publishing guards
        if (job.getDeadline() != null && job.getDeadline().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cannot publish job with a deadline in the past. Please update the deadline.");
        }

        long requiredSkillsCount = jobSkillRepository.countByJobAndIsRequiredTrue(job);
        if (requiredSkillsCount == 0) {
            throw new BadRequestException("Job must have at least one required skill before publishing.");
        }

        job.setStatus(JobStatus.PUBLISHED);
        if (job.getPublishedAt() == null) {
            job.setPublishedAt(LocalDateTime.now());
        }

        Job saved = jobRepository.save(job);
        List<JobSkill> skills = jobSkillRepository.findAllByJobWithSkill(saved);
        log.info("Published job ID: {} for company ID: {}", jobId, recruiter.getCompany().getId());
        return mapToDetailResponse(saved, skills);
    }

    @Override
    @Transactional
    public JobDetailResponse unpublishJob(Long userId, Long jobId) {
        RecruiterProfile recruiter = getValidatedRecruiterWithCompany(userId);
        Job job = getJobWithOwnershipCheck(jobId, recruiter);

        if (job.getStatus() != JobStatus.PUBLISHED) {
            throw new BadRequestException("Only published jobs can be unpublished to draft");
        }

        job.setStatus(JobStatus.DRAFT);
        Job saved = jobRepository.save(job);
        List<JobSkill> skills = jobSkillRepository.findAllByJobWithSkill(saved);
        log.info("Unpublished job ID: {} back to DRAFT", jobId);
        return mapToDetailResponse(saved, skills);
    }

    @Override
    @Transactional
    public JobDetailResponse closeJob(Long userId, Long jobId) {
        RecruiterProfile recruiter = getValidatedRecruiterWithCompany(userId);
        Job job = getJobWithOwnershipCheck(jobId, recruiter);

        if (job.getStatus() != JobStatus.PUBLISHED) {
            throw new BadRequestException("Only published jobs can be closed");
        }

        job.setStatus(JobStatus.CLOSED);
        Job saved = jobRepository.save(job);
        List<JobSkill> skills = jobSkillRepository.findAllByJobWithSkill(saved);
        log.info("Closed job ID: {}", jobId);
        return mapToDetailResponse(saved, skills);
    }

    @Override
    @Transactional
    public JobDetailResponse reopenJob(Long userId, Long jobId) {
        RecruiterProfile recruiter = getValidatedRecruiterWithCompany(userId);
        Job job = getJobWithOwnershipCheck(jobId, recruiter);

        if (job.getStatus() != JobStatus.CLOSED) {
            throw new BadRequestException("Only closed jobs can be reopened");
        }

        if (job.getCompany().getVerificationStatus() != com.careerforge.entity.enums.CompanyVerificationStatus.VERIFIED) {
            auditLogService.logFailure(
                    userId,
                    recruiter.getUser() != null ? recruiter.getUser().getEmail() : "RECRUITER",
                    "ROLE_RECRUITER",
                    com.careerforge.entity.enums.AuditEventType.JOB_PUBLISH_GUARD_BLOCKED,
                    com.careerforge.entity.enums.AuditTargetType.JOB,
                    jobId,
                    job.getTitle(),
                    "Cannot reopen jobs for an unverified company",
                    Map.of(
                            "jobId", jobId,
                            "companyId", job.getCompany().getId(),
                            "companyName", job.getCompany().getName(),
                            "companyVerificationStatus", job.getCompany().getVerificationStatus().name()
                    )
            );
            throw new BadRequestException("Cannot reopen jobs for an unverified company. Current verification status: " + job.getCompany().getVerificationStatus());
        }

        if (job.getDeadline() != null && job.getDeadline().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cannot reopen job with an expired deadline. Please update the deadline.");
        }

        job.setStatus(JobStatus.PUBLISHED);
        Job saved = jobRepository.save(job);
        List<JobSkill> skills = jobSkillRepository.findAllByJobWithSkill(saved);
        log.info("Reopened job ID: {}", jobId);
        return mapToDetailResponse(saved, skills);
    }

    @Override
    @Transactional
    public JobDetailResponse archiveJob(Long userId, Long jobId) {
        RecruiterProfile recruiter = getValidatedRecruiterWithCompany(userId);
        Job job = getJobWithOwnershipCheck(jobId, recruiter);

        if (job.getStatus() == JobStatus.ARCHIVED) {
            throw new BadRequestException("Job is already archived");
        }

        job.setStatus(JobStatus.ARCHIVED);
        Job saved = jobRepository.save(job);
        List<JobSkill> skills = jobSkillRepository.findAllByJobWithSkill(saved);
        log.info("Archived job ID: {}", jobId);
        return mapToDetailResponse(saved, skills);
    }

    @Override
    @Transactional
    public void deleteJob(Long userId, Long jobId) {
        RecruiterProfile recruiter = getValidatedRecruiterWithCompany(userId);
        Job job = getJobWithOwnershipCheck(jobId, recruiter);

        if (job.getStatus() == JobStatus.PUBLISHED || job.getStatus() == JobStatus.CLOSED) {
            throw new BadRequestException("Cannot delete a " + job.getStatus() + " job. Please unpublish or archive it first.");
        }

        jobSkillRepository.deleteAllByJob(job);
        jobRepository.delete(job);
        log.info("Deleted job ID: {}", jobId);
    }

    // ==========================================
    // Public / Candidate Discovery
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<JobSummaryResponse> searchJobs(JobSearchCriteria criteria) {
        Specification<Job> spec = JobSpecification.buildPublicSpecification(criteria);

        Sort sort = criteria.getSortDirection().equalsIgnoreCase("asc") ?
                Sort.by(criteria.getSortBy()).ascending() :
                Sort.by(criteria.getSortBy()).descending();

        Pageable pageable = PageRequest.of(criteria.getPage(), criteria.getSize(), sort);
        Page<Job> page = jobRepository.findAll(spec, pageable);

        return mapToPagedSummaryResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public JobDetailResponse getPublicJobDetail(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));

        if (job.getStatus() != JobStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Job", "id", jobId);
        }

        List<JobSkill> skills = jobSkillRepository.findAllByJobWithSkill(job);
        return mapToDetailResponse(job, skills);
    }

    @Override
    @Transactional(readOnly = true)
    public JobDetailResponse getPublicJobDetailBySlug(String slug) {
        Job job = jobRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "slug", slug));

        if (job.getStatus() != JobStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Job", "slug", slug);
        }

        List<JobSkill> skills = jobSkillRepository.findAllByJobWithSkill(job);
        return mapToDetailResponse(job, skills);
    }

    // ==========================================
    // Private Helpers & Validation
    // ==========================================

    private RecruiterProfile getValidatedRecruiterWithCompany(Long userId) {
        RecruiterProfile recruiter = recruiterService.getOrCreateProfileEntity(userId);
        if (recruiter.getCompany() == null) {
            throw new BadRequestException("Recruiter must be associated with a registered company before managing jobs");
        }
        return recruiter;
    }

    private Job getJobWithOwnershipCheck(Long jobId, RecruiterProfile recruiter) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));

        if (!job.getCompany().getId().equals(recruiter.getCompany().getId())) {
            throw new ResourceNotFoundException("Job", "id", jobId);
        }

        return job;
    }

    private void validateSalaryRange(java.math.BigDecimal min, java.math.BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BadRequestException("Minimum salary cannot exceed maximum salary");
        }
    }

    private List<JobSkill> attachSkillsToJob(Job job, List<JobSkillItemRequest> skillRequests) {
        if (skillRequests == null || skillRequests.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> processedSkillIds = new HashSet<>();
        List<JobSkill> skillsToSave = new ArrayList<>();

        for (JobSkillItemRequest req : skillRequests) {
            if (processedSkillIds.contains(req.getSkillId())) {
                continue; // Deduplicate
            }
            processedSkillIds.add(req.getSkillId());

            Skill skill = skillRepository.findById(req.getSkillId())
                    .orElseThrow(() -> new ResourceNotFoundException("Skill", "id", req.getSkillId()));

            JobSkill jobSkill = JobSkill.builder()
                    .job(job)
                    .skill(skill)
                    .isRequired(req.isRequired())
                    .minimumProficiency(req.getMinimumProficiency())
                    .build();

            skillsToSave.add(jobSkill);
        }

        return jobSkillRepository.saveAll(skillsToSave);
    }

    private String generateUniqueJobSlug(String title) {
        String nowhitespace = WHITESPACE.matcher(title).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("").toLowerCase(Locale.ENGLISH);

        String randomSuffix = UUID.randomUUID().toString().substring(0, 6);
        String finalSlug = slug + "-" + randomSuffix;

        if (!jobRepository.existsBySlug(finalSlug)) {
            return finalSlug;
        }

        return slug + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private PagedResponse<JobSummaryResponse> mapToPagedSummaryResponse(Page<Job> page) {
        if (page.isEmpty()) {
            return PagedResponse.of(page, Collections.emptyList());
        }

        List<Long> jobIds = page.getContent().stream().map(Job::getId).collect(Collectors.toList());
        List<JobSkill> allSkills = jobSkillRepository.findAllByJob_IdInWithSkill(jobIds);

        Map<Long, List<JobSkill>> skillsByJobId = allSkills.stream()
                .collect(Collectors.groupingBy(js -> js.getJob().getId()));

        List<JobSummaryResponse> summaries = page.getContent().stream()
                .map(job -> mapToSummaryResponse(job, skillsByJobId.getOrDefault(job.getId(), Collections.emptyList())))
                .collect(Collectors.toList());

        return PagedResponse.of(page, summaries);
    }

    private JobSummaryResponse mapToSummaryResponse(Job job, List<JobSkill> skills) {
        List<JobSkillResponse> skillResponses = skills.stream()
                .map(this::mapToSkillResponse)
                .collect(Collectors.toList());

        return JobSummaryResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .slug(job.getSlug())
                .companyId(job.getCompany().getId())
                .companyName(job.getCompany().getName())
                .companyLogoUrl(job.getCompany().getLogoUrl())
                .location(job.getLocation())
                .workMode(job.getWorkMode())
                .jobType(job.getJobType())
                .experienceLevel(job.getExperienceLevel())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .currency(job.getCurrency())
                .status(job.getStatus())
                .deadline(job.getDeadline())
                .publishedAt(job.getPublishedAt())
                .createdAt(job.getCreatedAt())
                .skills(skillResponses)
                .build();
    }

    private JobDetailResponse mapToDetailResponse(Job job, List<JobSkill> skills) {
        List<JobSkillResponse> skillResponses = skills.stream()
                .map(this::mapToSkillResponse)
                .collect(Collectors.toList());

        return JobDetailResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .slug(job.getSlug())
                .description(job.getDescription())
                .companyId(job.getCompany().getId())
                .companyName(job.getCompany().getName())
                .companySlug(job.getCompany().getSlug())
                .companyLogoUrl(job.getCompany().getLogoUrl())
                .companyWebsite(job.getCompany().getWebsite())
                .companyIndustry(job.getCompany().getIndustry())
                .companyLocation(job.getCompany().getLocation())
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
                .deadline(job.getDeadline())
                .publishedAt(job.getPublishedAt())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .skills(skillResponses)
                .build();
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
}
