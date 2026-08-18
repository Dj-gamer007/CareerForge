package com.careerforge.service.impl;

import com.careerforge.dto.response.JobSkillResponse;
import com.careerforge.dto.response.PagedResponse;
import com.careerforge.dto.response.SavedJobResponse;
import com.careerforge.entity.Job;
import com.careerforge.entity.JobSkill;
import com.careerforge.entity.SavedJob;
import com.careerforge.entity.StudentProfile;
import com.careerforge.entity.enums.JobStatus;
import com.careerforge.exception.BadRequestException;
import com.careerforge.exception.ResourceNotFoundException;
import com.careerforge.repository.JobRepository;
import com.careerforge.repository.JobSkillRepository;
import com.careerforge.repository.SavedJobRepository;
import com.careerforge.repository.StudentProfileRepository;
import com.careerforge.service.SavedJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavedJobServiceImpl implements SavedJobService {

    private final SavedJobRepository savedJobRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;

    @Override
    @Transactional
    public SavedJobResponse saveJob(Long userId, Long jobId) {
        StudentProfile profile = getStudentProfileByUserId(userId);

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));

        if (job.getStatus() != JobStatus.PUBLISHED) {
            throw new BadRequestException("Only published jobs can be bookmarked");
        }

        if (savedJobRepository.existsByStudentProfile_IdAndJob_Id(profile.getId(), jobId)) {
            throw new BadRequestException("Job is already saved in your bookmarks");
        }

        SavedJob savedJob = SavedJob.builder()
                .studentProfile(profile)
                .job(job)
                .build();

        SavedJob saved = savedJobRepository.save(savedJob);
        log.info("Student profile ID: {} saved job ID: {}", profile.getId(), jobId);

        List<JobSkill> skills = jobSkillRepository.findAllByJobWithSkill(job);
        return mapToResponse(saved, skills);
    }

    @Override
    @Transactional
    public void removeSavedJob(Long userId, Long jobId) {
        StudentProfile profile = getStudentProfileByUserId(userId);

        SavedJob savedJob = savedJobRepository.findByStudentProfile_IdAndJob_Id(profile.getId(), jobId)
                .orElseThrow(() -> new ResourceNotFoundException("SavedJob", "jobId", jobId));

        savedJobRepository.delete(savedJob);
        log.info("Student profile ID: {} removed saved job ID: {}", profile.getId(), jobId);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<SavedJobResponse> getSavedJobs(Long userId, Pageable pageable) {
        StudentProfile profile = getStudentProfileByUserId(userId);
        Page<SavedJob> page = savedJobRepository.findAllByStudentProfile_Id(profile.getId(), pageable);

        if (page.isEmpty()) {
            return PagedResponse.of(page, Collections.emptyList());
        }

        List<Long> jobIds = page.getContent().stream()
                .map(sj -> sj.getJob().getId())
                .collect(Collectors.toList());

        List<JobSkill> allSkills = jobSkillRepository.findAllByJob_IdInWithSkill(jobIds);
        Map<Long, List<JobSkill>> skillsByJobId = allSkills.stream()
                .collect(Collectors.groupingBy(js -> js.getJob().getId()));

        List<SavedJobResponse> responses = page.getContent().stream()
                .map(sj -> mapToResponse(sj, skillsByJobId.getOrDefault(sj.getJob().getId(), Collections.emptyList())))
                .collect(Collectors.toList());

        return PagedResponse.of(page, responses);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isJobSaved(Long userId, Long jobId) {
        StudentProfile profile = getStudentProfileByUserId(userId);
        return savedJobRepository.existsByStudentProfile_IdAndJob_Id(profile.getId(), jobId);
    }

    private StudentProfile getStudentProfileByUserId(Long userId) {
        return studentProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentProfile", "userId", userId));
    }

    private SavedJobResponse mapToResponse(SavedJob savedJob, List<JobSkill> skills) {
        Job job = savedJob.getJob();
        List<JobSkillResponse> skillResponses = skills.stream()
                .map(js -> JobSkillResponse.builder()
                        .id(js.getId())
                        .skillId(js.getSkill().getId())
                        .skillName(js.getSkill().getName())
                        .category(js.getSkill().getCategory())
                        .isRequired(js.isRequired())
                        .minimumProficiency(js.getMinimumProficiency())
                        .build())
                .collect(Collectors.toList());

        return SavedJobResponse.builder()
                .id(savedJob.getId())
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
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .currency(job.getCurrency())
                .status(job.getStatus())
                .deadline(job.getDeadline())
                .savedAt(savedJob.getCreatedAt())
                .skills(skillResponses)
                .build();
    }
}
