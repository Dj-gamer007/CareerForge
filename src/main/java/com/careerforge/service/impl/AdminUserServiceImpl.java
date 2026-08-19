package com.careerforge.service.impl;

import com.careerforge.dto.request.UserStatusUpdateRequest;
import com.careerforge.dto.response.AdminUserDetailResponse;
import com.careerforge.dto.response.AdminUserSummaryResponse;
import com.careerforge.entity.RecruiterProfile;
import com.careerforge.entity.Resume;
import com.careerforge.entity.StudentProfile;
import com.careerforge.entity.StudentSkill;
import com.careerforge.entity.User;
import com.careerforge.entity.enums.Role;
import com.careerforge.exception.BadRequestException;
import com.careerforge.exception.ResourceNotFoundException;
import com.careerforge.repository.*;
import com.careerforge.service.AdminUserService;
import com.careerforge.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final EducationRepository educationRepository;
    private final ProjectRepository projectRepository;
    private final CertificationRepository certificationRepository;
    private final ResumeRepository resumeRepository;
    private final StudentSkillRepository studentSkillRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserSummaryResponse> getUsers(String search, Role role, Boolean enabled, Pageable pageable) {
        Specification<User> spec = UserSpecification.buildAdminUserSpecification(search, role, enabled);
        Page<User> userPage = userRepository.findAll(spec, pageable);

        if (userPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> userIds = userPage.getContent().stream()
                .map(User::getId)
                .collect(Collectors.toList());

        // Batch load profiles to eliminate N+1 queries
        Map<Long, StudentProfile> studentProfileMap = studentProfileRepository.findAllByUser_IdIn(userIds).stream()
                .collect(Collectors.toMap(sp -> sp.getUser().getId(), sp -> sp, (a, b) -> a));

        Map<Long, RecruiterProfile> recruiterProfileMap = recruiterProfileRepository.findAllByUser_IdIn(userIds).stream()
                .collect(Collectors.toMap(rp -> rp.getUser().getId(), rp -> rp, (a, b) -> a));

        List<AdminUserSummaryResponse> responses = userPage.getContent().stream()
                .map(user -> mapToSummaryResponse(user, studentProfileMap.get(user.getId()), recruiterProfileMap.get(user.getId())))
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, userPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        AdminUserDetailResponse.StudentProfileSummaryDto studentDto = null;
        AdminUserDetailResponse.RecruiterProfileSummaryDto recruiterDto = null;

        if (user.getRole() == Role.ROLE_STUDENT) {
            Optional<StudentProfile> spOpt = studentProfileRepository.findByUser_Id(user.getId());
            if (spOpt.isPresent()) {
                StudentProfile sp = spOpt.get();
                List<StudentSkill> skills = studentSkillRepository.findAllByStudentProfileWithSkill(sp);
                List<String> skillNames = skills.stream()
                        .map(s -> s.getSkill().getName() + " (" + s.getProficiency().name() + ")")
                        .collect(Collectors.toList());

                Optional<Resume> activeResumeOpt = resumeRepository.findByStudentProfileAndIsActiveTrue(sp);

                studentDto = AdminUserDetailResponse.StudentProfileSummaryDto.builder()
                        .id(sp.getId())
                        .firstName(sp.getFirstName())
                        .lastName(sp.getLastName())
                        .phone(sp.getPhone())
                        .location(sp.getLocation())
                        .bio(sp.getBio())
                        .educationSummary(sp.getEducationSummary())
                        .githubUrl(sp.getGithubUrl())
                        .linkedinUrl(sp.getLinkedinUrl())
                        .portfolioUrl(sp.getPortfolioUrl())
                        .profileCompletionPercentage(sp.getProfileCompletionPercentage())
                        .totalSkills(skills.size())
                        .totalEducations((int) educationRepository.countByStudentProfile(sp))
                        .totalProjects((int) projectRepository.countByStudentProfile(sp))
                        .totalCertifications((int) certificationRepository.countByStudentProfile(sp))
                        .totalResumes((int) resumeRepository.countByStudentProfile(sp))
                        .activeResumeId(activeResumeOpt.map(Resume::getId).orElse(null))
                        .skills(skillNames)
                        .build();
            }
        } else if (user.getRole() == Role.ROLE_RECRUITER) {
            Optional<RecruiterProfile> rpOpt = recruiterProfileRepository.findByUser_Id(user.getId());
            if (rpOpt.isPresent()) {
                RecruiterProfile rp = rpOpt.get();
                recruiterDto = AdminUserDetailResponse.RecruiterProfileSummaryDto.builder()
                        .id(rp.getId())
                        .firstName(rp.getFirstName())
                        .lastName(rp.getLastName())
                        .designation(rp.getDesignation())
                        .department(rp.getDepartment())
                        .phone(rp.getPhone())
                        .isCompanyAdmin(rp.isCompanyAdmin())
                        .companyId(rp.getCompany() != null ? rp.getCompany().getId() : null)
                        .companyName(rp.getCompany() != null ? rp.getCompany().getName() : null)
                        .companySlug(rp.getCompany() != null ? rp.getCompany().getSlug() : null)
                        .companyVerificationStatus(rp.getCompany() != null && rp.getCompany().getVerificationStatus() != null
                                ? rp.getCompany().getVerificationStatus().name() : null)
                        .build();
            }
        }

        return AdminUserDetailResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .studentProfile(studentDto)
                .recruiterProfile(recruiterDto)
                .build();
    }

    @Override
    @Transactional
    public AdminUserSummaryResponse updateUserStatus(Long currentAdminId, Long targetUserId, UserStatusUpdateRequest request) {
        if (currentAdminId != null && currentAdminId.equals(targetUserId) && Boolean.FALSE.equals(request.getEnabled())) {
            throw new BadRequestException("Administrators cannot disable their own account");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));

        user.setEnabled(request.getEnabled());
        User savedUser = userRepository.save(user);

        log.info("Admin ID: {} updated user ID: {} enabled status to: {} (Reason: {})",
                currentAdminId, targetUserId, request.getEnabled(), request.getReason());

        StudentProfile sp = studentProfileRepository.findByUser_Id(savedUser.getId()).orElse(null);
        RecruiterProfile rp = recruiterProfileRepository.findByUser_Id(savedUser.getId()).orElse(null);

        return mapToSummaryResponse(savedUser, sp, rp);
    }

    private AdminUserSummaryResponse mapToSummaryResponse(User user, StudentProfile sp, RecruiterProfile rp) {
        String fullName = null;
        String profileType = "NONE";

        if (sp != null) {
            fullName = (sp.getFirstName() + " " + sp.getLastName()).trim();
            profileType = "STUDENT";
        } else if (rp != null) {
            fullName = (rp.getFirstName() + " " + rp.getLastName()).trim();
            profileType = "RECRUITER";
        }

        return AdminUserSummaryResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .fullName(fullName)
                .profileType(profileType)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
