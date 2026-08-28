package com.careerforge.service.impl;

import com.careerforge.config.StorageConfigProperties;
import com.careerforge.dto.response.ResumeResponse;
import com.careerforge.entity.Resume;
import com.careerforge.entity.StudentProfile;
import com.careerforge.exception.BadRequestException;
import com.careerforge.exception.FileStorageException;
import com.careerforge.exception.ResourceNotFoundException;
import com.careerforge.entity.Application;
import com.careerforge.repository.ApplicationRepository;
import com.careerforge.repository.ResumeRepository;
import com.careerforge.service.ResumeService;
import com.careerforge.service.StorageService;
import com.careerforge.service.StudentProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;
    private final StudentProfileService studentProfileService;
    private final ApplicationRepository applicationRepository;
    private final StorageService storageService;
    private final StorageConfigProperties storageConfigProperties;

    @Override
    @Transactional
    public ResumeResponse uploadResume(Long userId, MultipartFile file) {
        // 1. Validation
        validateFile(file);

        StudentProfile profile = studentProfileService.getOrCreateProfileEntity(userId);

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            originalFilename = StringUtils.cleanPath(originalFilename);
        } else {
            originalFilename = "resume.pdf";
        }

        // 2. Store file on local filesystem via StorageService
        String storedFileName = storageService.store(file);
        Path storedFilePath = storageService.getFilePath(storedFileName);

        // 3. Compute version for this student
        List<Resume> existingResumes = resumeRepository.findAllByStudentProfileOrderByUploadedAtDesc(profile);
        int nextVersion = existingResumes.stream()
                .mapToInt(Resume::getVersion)
                .max()
                .orElse(0) + 1;

        // If this is the student's first resume, make it active by default
        boolean shouldBeActive = existingResumes.isEmpty();
        if (shouldBeActive) {
            resumeRepository.deactivateAllByStudentProfile(profile);
        }

        // 4. Save metadata in MySQL
        Resume resume = Resume.builder()
                .studentProfile(profile)
                .originalFileName(originalFilename)
                .storedFileName(storedFileName)
                .storagePath(storedFilePath.toString())
                .contentType("application/pdf")
                .fileSize(file.getSize())
                .version(nextVersion)
                .isActive(shouldBeActive)
                .build();

        Resume saved = resumeRepository.save(resume);
        studentProfileService.updateProfileCompletion(profile);

        log.info("Uploaded resume v{} ({}) for user ID: {}", nextVersion, storedFileName, userId);
        return mapToResumeResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponse> getStudentResumes(Long userId) {
        StudentProfile profile = studentProfileService.getProfileEntityByUserId(userId);
        return resumeRepository.findAllByStudentProfileOrderByUploadedAtDesc(profile)
                .stream()
                .map(this::mapToResumeResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeDownloadResult downloadResume(Long userId, Long resumeId) {
        StudentProfile profile = studentProfileService.getProfileEntityByUserId(userId);

        Resume resume = resumeRepository.findByIdAndStudentProfile(resumeId, profile)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId));

        Resource resource = storageService.loadAsResource(resume.getStoredFileName());
        return new ResumeDownloadResult(resource, resume.getOriginalFileName(), resume.getContentType());
    }

    @Override
    @Transactional
    public ResumeResponse setActiveResume(Long userId, Long resumeId) {
        StudentProfile profile = studentProfileService.getProfileEntityByUserId(userId);

        Resume resume = resumeRepository.findByIdAndStudentProfile(resumeId, profile)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId));

        // Deactivate other resumes
        resumeRepository.deactivateAllByStudentProfile(profile);

        resume.setActive(true);
        Resume updated = resumeRepository.save(resume);

        studentProfileService.updateProfileCompletion(profile);
        log.info("Set resume ID: {} as active for user ID: {}", resumeId, userId);
        return mapToResumeResponse(updated);
    }

    @Override
    @Transactional
    public void deleteResume(Long userId, Long resumeId) {
        StudentProfile profile = studentProfileService.getOrCreateProfileEntity(userId);

        Resume resume = resumeRepository.findByIdAndStudentProfile(resumeId, profile)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId));

        boolean wasActive = resume.isActive();

        // 1. Resolve any student applications referencing this resume to preserve application lifecycle history
        List<Application> linkedApplications = applicationRepository.findAllByResume(resume);
        if (!linkedApplications.isEmpty()) {
            List<Resume> remaining = resumeRepository.findAllByStudentProfileOrderByUploadedAtDesc(profile)
                    .stream()
                    .filter(r -> !r.getId().equals(resumeId))
                    .toList();
            Resume fallbackResume = !remaining.isEmpty() ? remaining.get(0) : null;
            for (Application app : linkedApplications) {
                app.setResume(fallbackResume);
            }
            applicationRepository.saveAll(linkedApplications);
            applicationRepository.flush();
        }

        // 2. Delete resume metadata
        resumeRepository.delete(resume);
        resumeRepository.flush();

        // 3. If the deleted resume was active, activate the latest available remaining resume if any exists
        if (wasActive) {
            List<Resume> remaining = resumeRepository.findAllByStudentProfileOrderByUploadedAtDesc(profile);
            if (!remaining.isEmpty()) {
                Resume latest = remaining.get(0);
                latest.setActive(true);
                resumeRepository.save(latest);
                resumeRepository.flush();
            }
        }

        // 4. Delete physical file from storage
        storageService.delete(resume.getStoredFileName());

        // 5. Recalculate and persist updated profile completion
        studentProfileService.updateProfileCompletion(profile);
        log.info("Deleted resume ID: {} for user ID: {}", resumeId, userId);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty");
        }

        if (file.getSize() > storageConfigProperties.getMaxFileSizeBytes()) {
            throw new BadRequestException("File size exceeds maximum permitted limit of " +
                    (storageConfigProperties.getMaxFileSizeBytes() / (1024 * 1024)) + " MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new BadRequestException("Only PDF files (.pdf) are supported");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
            throw new BadRequestException("Invalid MIME content type. Only application/pdf is allowed");
        }
    }

    private ResumeResponse mapToResumeResponse(Resume resume) {
        return ResumeResponse.builder()
                .id(resume.getId())
                .originalFileName(resume.getOriginalFileName())
                .contentType(resume.getContentType())
                .fileSize(resume.getFileSize())
                .version(resume.getVersion())
                .isActive(resume.isActive())
                .uploadedAt(resume.getUploadedAt())
                .build();
    }
}
