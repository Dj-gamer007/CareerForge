package com.careerforge.service.impl;

import com.careerforge.dto.request.*;
import com.careerforge.dto.response.*;
import com.careerforge.entity.*;
import com.careerforge.exception.BadRequestException;
import com.careerforge.exception.ResourceNotFoundException;
import com.careerforge.repository.*;
import com.careerforge.service.StudentProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentProfileServiceImpl implements StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final EducationRepository educationRepository;
    private final ProjectRepository projectRepository;
    private final CertificationRepository certificationRepository;
    private final ResumeRepository resumeRepository;

    // ==========================================
    // Profile Management
    // ==========================================

    @Override
    @Transactional
    public StudentProfileResponse getProfileByUserId(Long userId) {
        StudentProfile profile = getOrCreateProfileEntity(userId);
        return mapToProfileResponse(profile);
    }

    @Override
    @Transactional
    public StudentProfileResponse createProfile(Long userId, StudentProfileRequest request) {
        if (studentProfileRepository.existsByUser_Id(userId)) {
            throw new BadRequestException("Student profile already exists for user ID: " + userId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        StudentProfile profile = StudentProfile.builder()
                .user(user)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .phone(request.getPhone())
                .location(request.getLocation())
                .bio(request.getBio())
                .educationSummary(request.getEducationSummary())
                .githubUrl(request.getGithubUrl())
                .linkedinUrl(request.getLinkedinUrl())
                .portfolioUrl(request.getPortfolioUrl())
                .build();

        int completion = calculateCompletionPercentage(profile);
        profile.setProfileCompletionPercentage(completion);

        StudentProfile saved = studentProfileRepository.save(profile);
        log.info("Created student profile for user ID: {}", userId);
        return mapToProfileResponse(saved);
    }

    @Override
    @Transactional
    public StudentProfileResponse updateProfile(Long userId, StudentProfileRequest request) {
        StudentProfile profile = studentProfileRepository.findByUser_Id(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                    return StudentProfile.builder().user(user).build();
                });

        profile.setFirstName(request.getFirstName().trim());
        profile.setLastName(request.getLastName().trim());
        profile.setPhone(request.getPhone());
        profile.setLocation(request.getLocation());
        profile.setBio(request.getBio());
        profile.setEducationSummary(request.getEducationSummary());
        profile.setGithubUrl(request.getGithubUrl());
        profile.setLinkedinUrl(request.getLinkedinUrl());
        profile.setPortfolioUrl(request.getPortfolioUrl());

        int completion = calculateCompletionPercentage(profile);
        profile.setProfileCompletionPercentage(completion);

        StudentProfile saved = studentProfileRepository.save(profile);
        log.info("Updated student profile for user ID: {}", userId);
        return mapToProfileResponse(saved);
    }

    @Override
    @Transactional
    public StudentProfile getOrCreateProfileEntity(Long userId) {
        return studentProfileRepository.findByUser_Id(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                    StudentProfile newProfile = StudentProfile.builder()
                            .user(user)
                            .firstName("Student")
                            .lastName("User")
                            .build();
                    newProfile.setProfileCompletionPercentage(calculateCompletionPercentage(newProfile));
                    return studentProfileRepository.save(newProfile);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public StudentProfile getProfileEntityByUserId(Long userId) {
        return studentProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentProfile", "userId", userId));
    }

    @Override
    @Transactional
    public void updateProfileCompletion(StudentProfile profile) {
        int completion = calculateCompletionPercentage(profile);
        profile.setProfileCompletionPercentage(completion);
        studentProfileRepository.save(profile);
    }

    // ==========================================
    // Skills Management
    // ==========================================

    @Override
    @Transactional
    public List<StudentSkillResponse> getSkills(Long userId) {
        StudentProfile profile = getOrCreateProfileEntity(userId);
        return studentSkillRepository.findAllByStudentProfileWithSkill(profile)
                .stream()
                .map(this::mapToSkillResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StudentSkillResponse addSkill(Long userId, StudentSkillRequest request) {
        StudentProfile profile = getOrCreateProfileEntity(userId);

        Skill skill = skillRepository.findById(request.getSkillId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill", "id", request.getSkillId()));

        if (studentSkillRepository.existsByStudentProfileAndSkill_Id(profile, skill.getId())) {
            throw new BadRequestException("Skill '" + skill.getName() + "' is already added to your profile");
        }

        StudentSkill studentSkill = StudentSkill.builder()
                .studentProfile(profile)
                .skill(skill)
                .proficiency(request.getProficiency())
                .build();

        StudentSkill saved = studentSkillRepository.save(studentSkill);
        updateProfileCompletion(profile);
        log.info("Added skill '{}' with proficiency '{}' for user ID: {}", skill.getName(), request.getProficiency(), userId);
        return mapToSkillResponse(saved);
    }

    @Override
    @Transactional
    public StudentSkillResponse updateSkillProficiency(Long userId, Long skillId, StudentSkillRequest request) {
        StudentProfile profile = getProfileEntityByUserId(userId);

        StudentSkill studentSkill = studentSkillRepository.findByStudentProfileAndSkill_Id(profile, skillId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentSkill", "skillId", skillId));

        studentSkill.setProficiency(request.getProficiency());
        StudentSkill saved = studentSkillRepository.save(studentSkill);
        log.info("Updated proficiency for skill ID: {} for user ID: {}", skillId, userId);
        return mapToSkillResponse(saved);
    }

    @Override
    @Transactional
    public void removeSkill(Long userId, Long skillId) {
        StudentProfile profile = getProfileEntityByUserId(userId);

        StudentSkill studentSkill = studentSkillRepository.findByStudentProfileAndSkill_Id(profile, skillId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentSkill", "skillId", skillId));

        studentSkillRepository.delete(studentSkill);
        updateProfileCompletion(profile);
        log.info("Removed skill ID: {} for user ID: {}", skillId, userId);
    }

    // ==========================================
    // Education Management
    // ==========================================

    @Override
    @Transactional
    public List<EducationResponse> getEducationList(Long userId) {
        StudentProfile profile = getOrCreateProfileEntity(userId);
        return educationRepository.findAllByStudentProfileOrderByStartDateDesc(profile)
                .stream()
                .map(this::mapToEducationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EducationResponse addEducation(Long userId, EducationRequest request) {
        StudentProfile profile = getOrCreateProfileEntity(userId);

        Education education = Education.builder()
                .studentProfile(profile)
                .institution(request.getInstitution())
                .degree(request.getDegree())
                .fieldOfStudy(request.getFieldOfStudy())
                .startDate(request.getStartDate())
                .endDate(request.isCurrentlyStudying() ? null : request.getEndDate())
                .currentlyStudying(request.isCurrentlyStudying())
                .gradeOrGpa(request.getGradeOrGpa())
                .build();

        Education saved = educationRepository.save(education);
        updateProfileCompletion(profile);
        log.info("Added education entry for user ID: {}", userId);
        return mapToEducationResponse(saved);
    }

    @Override
    @Transactional
    public EducationResponse updateEducation(Long userId, Long educationId, EducationRequest request) {
        StudentProfile profile = getProfileEntityByUserId(userId);

        Education education = educationRepository.findByIdAndStudentProfile(educationId, profile)
                .orElseThrow(() -> new ResourceNotFoundException("Education", "id", educationId));

        education.setInstitution(request.getInstitution());
        education.setDegree(request.getDegree());
        education.setFieldOfStudy(request.getFieldOfStudy());
        education.setStartDate(request.getStartDate());
        education.setEndDate(request.isCurrentlyStudying() ? null : request.getEndDate());
        education.setCurrentlyStudying(request.isCurrentlyStudying());
        education.setGradeOrGpa(request.getGradeOrGpa());

        Education saved = educationRepository.save(education);
        return mapToEducationResponse(saved);
    }

    @Override
    @Transactional
    public void deleteEducation(Long userId, Long educationId) {
        StudentProfile profile = getProfileEntityByUserId(userId);

        Education education = educationRepository.findByIdAndStudentProfile(educationId, profile)
                .orElseThrow(() -> new ResourceNotFoundException("Education", "id", educationId));

        educationRepository.delete(education);
        updateProfileCompletion(profile);
        log.info("Deleted education ID: {} for user ID: {}", educationId, userId);
    }

    // ==========================================
    // Projects Management
    // ==========================================

    @Override
    @Transactional
    public List<ProjectResponse> getProjectList(Long userId) {
        StudentProfile profile = getOrCreateProfileEntity(userId);
        return projectRepository.findAllByStudentProfileOrderByStartDateDesc(profile)
                .stream()
                .map(this::mapToProjectResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProjectResponse addProject(Long userId, ProjectRequest request) {
        StudentProfile profile = getOrCreateProfileEntity(userId);

        Project project = Project.builder()
                .studentProfile(profile)
                .title(request.getTitle())
                .description(request.getDescription())
                .technologies(request.getTechnologies())
                .projectUrl(request.getProjectUrl())
                .githubUrl(request.getGithubUrl())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        Project saved = projectRepository.save(project);
        updateProfileCompletion(profile);
        log.info("Added project '{}' for user ID: {}", request.getTitle(), userId);
        return mapToProjectResponse(saved);
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(Long userId, Long projectId, ProjectRequest request) {
        StudentProfile profile = getProfileEntityByUserId(userId);

        Project project = projectRepository.findByIdAndStudentProfile(projectId, profile)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        project.setTitle(request.getTitle());
        project.setDescription(request.getDescription());
        project.setTechnologies(request.getTechnologies());
        project.setProjectUrl(request.getProjectUrl());
        project.setGithubUrl(request.getGithubUrl());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());

        Project saved = projectRepository.save(project);
        return mapToProjectResponse(saved);
    }

    @Override
    @Transactional
    public void deleteProject(Long userId, Long projectId) {
        StudentProfile profile = getProfileEntityByUserId(userId);

        Project project = projectRepository.findByIdAndStudentProfile(projectId, profile)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        projectRepository.delete(project);
        updateProfileCompletion(profile);
        log.info("Deleted project ID: {} for user ID: {}", projectId, userId);
    }

    // ==========================================
    // Certifications Management
    // ==========================================

    @Override
    @Transactional
    public List<CertificationResponse> getCertificationList(Long userId) {
        StudentProfile profile = getOrCreateProfileEntity(userId);
        return certificationRepository.findAllByStudentProfileOrderByIssueDateDesc(profile)
                .stream()
                .map(this::mapToCertificationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CertificationResponse addCertification(Long userId, CertificationRequest request) {
        StudentProfile profile = getOrCreateProfileEntity(userId);

        Certification certification = Certification.builder()
                .studentProfile(profile)
                .name(request.getName())
                .issuingOrganization(request.getIssuingOrganization())
                .issueDate(request.getIssueDate())
                .expiryDate(request.getExpiryDate())
                .credentialId(request.getCredentialId())
                .credentialUrl(request.getCredentialUrl())
                .build();

        Certification saved = certificationRepository.save(certification);
        updateProfileCompletion(profile);
        log.info("Added certification '{}' for user ID: {}", request.getName(), userId);
        return mapToCertificationResponse(saved);
    }

    @Override
    @Transactional
    public CertificationResponse updateCertification(Long userId, Long certificationId, CertificationRequest request) {
        StudentProfile profile = getProfileEntityByUserId(userId);

        Certification certification = certificationRepository.findByIdAndStudentProfile(certificationId, profile)
                .orElseThrow(() -> new ResourceNotFoundException("Certification", "id", certificationId));

        certification.setName(request.getName());
        certification.setIssuingOrganization(request.getIssuingOrganization());
        certification.setIssueDate(request.getIssueDate());
        certification.setExpiryDate(request.getExpiryDate());
        certification.setCredentialId(request.getCredentialId());
        certification.setCredentialUrl(request.getCredentialUrl());

        Certification saved = certificationRepository.save(certification);
        return mapToCertificationResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCertification(Long userId, Long certificationId) {
        StudentProfile profile = getProfileEntityByUserId(userId);

        Certification certification = certificationRepository.findByIdAndStudentProfile(certificationId, profile)
                .orElseThrow(() -> new ResourceNotFoundException("Certification", "id", certificationId));

        certificationRepository.delete(certification);
        updateProfileCompletion(profile);
        log.info("Deleted certification ID: {} for user ID: {}", certificationId, userId);
    }

    // ==========================================
    // Profile Completion Logic
    // ==========================================

    private int calculateCompletionPercentage(StudentProfile profile) {
        int percentage = 0;

        // 1. Basic Info (15%)
        if (StringUtils.hasText(profile.getFirstName()) && StringUtils.hasText(profile.getLastName())) {
            percentage += 10;
        }
        if (StringUtils.hasText(profile.getPhone()) || StringUtils.hasText(profile.getLocation())) {
            percentage += 5;
        }

        // 2. Summary / Bio (15%)
        if (StringUtils.hasText(profile.getBio())) {
            percentage += 10;
        }
        if (StringUtils.hasText(profile.getEducationSummary())) {
            percentage += 5;
        }

        // 3. Social / Professional Links (10%)
        if (StringUtils.hasText(profile.getGithubUrl()) || StringUtils.hasText(profile.getLinkedinUrl()) || StringUtils.hasText(profile.getPortfolioUrl())) {
            percentage += 10;
        }

        // Check related entities if profile is persisted
        if (profile.getId() != null) {
            // 4. Skills (20%)
            long skillCount = studentSkillRepository.countByStudentProfile(profile);
            if (skillCount >= 3) {
                percentage += 20;
            } else if (skillCount >= 1) {
                percentage += 10;
            }

            // 5. Education (15%)
            long eduCount = educationRepository.countByStudentProfile(profile);
            if (eduCount > 0) {
                percentage += 15;
            }

            // 6. Projects (15%)
            long projectCount = projectRepository.countByStudentProfile(profile);
            if (projectCount > 0) {
                percentage += 15;
            }

            // 7. Certifications (5%)
            long certCount = certificationRepository.countByStudentProfile(profile);
            if (certCount > 0) {
                percentage += 5;
            }

            // 8. Resume (5%)
            long resumeCount = resumeRepository.countByStudentProfileAndIsActiveTrue(profile);
            if (resumeCount > 0) {
                percentage += 5;
            }
        }

        return Math.min(100, percentage);
    }

    // ==========================================
    // Response Mappings
    // ==========================================

    private StudentProfileResponse mapToProfileResponse(StudentProfile profile) {
        return StudentProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser() != null ? profile.getUser().getId() : null)
                .email(profile.getUser() != null ? profile.getUser().getEmail() : null)
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .phone(profile.getPhone())
                .location(profile.getLocation())
                .bio(profile.getBio())
                .educationSummary(profile.getEducationSummary())
                .githubUrl(profile.getGithubUrl())
                .linkedinUrl(profile.getLinkedinUrl())
                .portfolioUrl(profile.getPortfolioUrl())
                .profileCompletionPercentage(profile.getProfileCompletionPercentage())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private StudentSkillResponse mapToSkillResponse(StudentSkill studentSkill) {
        return StudentSkillResponse.builder()
                .id(studentSkill.getId())
                .skillId(studentSkill.getSkill().getId())
                .skillName(studentSkill.getSkill().getName())
                .category(studentSkill.getSkill().getCategory())
                .proficiency(studentSkill.getProficiency())
                .createdAt(studentSkill.getCreatedAt())
                .updatedAt(studentSkill.getUpdatedAt())
                .build();
    }

    private EducationResponse mapToEducationResponse(Education education) {
        return EducationResponse.builder()
                .id(education.getId())
                .institution(education.getInstitution())
                .degree(education.getDegree())
                .fieldOfStudy(education.getFieldOfStudy())
                .startDate(education.getStartDate())
                .endDate(education.getEndDate())
                .currentlyStudying(education.isCurrentlyStudying())
                .gradeOrGpa(education.getGradeOrGpa())
                .createdAt(education.getCreatedAt())
                .updatedAt(education.getUpdatedAt())
                .build();
    }

    private ProjectResponse mapToProjectResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .title(project.getTitle())
                .description(project.getDescription())
                .technologies(project.getTechnologies())
                .projectUrl(project.getProjectUrl())
                .githubUrl(project.getGithubUrl())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    private CertificationResponse mapToCertificationResponse(Certification certification) {
        return CertificationResponse.builder()
                .id(certification.getId())
                .name(certification.getName())
                .issuingOrganization(certification.getIssuingOrganization())
                .issueDate(certification.getIssueDate())
                .expiryDate(certification.getExpiryDate())
                .credentialId(certification.getCredentialId())
                .credentialUrl(certification.getCredentialUrl())
                .createdAt(certification.getCreatedAt())
                .updatedAt(certification.getUpdatedAt())
                .build();
    }
}
