package com.careerforge.service.impl;

import com.careerforge.dto.request.RecruiterProfileRequest;
import com.careerforge.dto.response.RecruiterProfileResponse;
import com.careerforge.entity.RecruiterProfile;
import com.careerforge.entity.User;
import com.careerforge.exception.BadRequestException;
import com.careerforge.exception.ResourceNotFoundException;
import com.careerforge.repository.RecruiterProfileRepository;
import com.careerforge.repository.UserRepository;
import com.careerforge.service.RecruiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecruiterServiceImpl implements RecruiterService {

    private final RecruiterProfileRepository recruiterProfileRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public RecruiterProfileResponse getProfileByUserId(Long userId) {
        RecruiterProfile profile = getOrCreateProfileEntity(userId);
        return mapToResponse(profile);
    }

    @Override
    @Transactional
    public RecruiterProfileResponse createProfile(Long userId, RecruiterProfileRequest request) {
        if (recruiterProfileRepository.existsByUser_Id(userId)) {
            throw new BadRequestException("Recruiter profile already exists for user ID: " + userId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        RecruiterProfile profile = RecruiterProfile.builder()
                .user(user)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .phone(request.getPhone())
                .designation(request.getDesignation().trim())
                .department(request.getDepartment())
                .isCompanyAdmin(false)
                .build();

        RecruiterProfile saved = recruiterProfileRepository.save(profile);
        log.info("Created recruiter profile for user ID: {}", userId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public RecruiterProfileResponse updateProfile(Long userId, RecruiterProfileRequest request) {
        RecruiterProfile profile = recruiterProfileRepository.findByUser_Id(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                    return RecruiterProfile.builder().user(user).build();
                });

        profile.setFirstName(request.getFirstName().trim());
        profile.setLastName(request.getLastName().trim());
        profile.setPhone(request.getPhone());
        profile.setDesignation(request.getDesignation().trim());
        profile.setDepartment(request.getDepartment());

        RecruiterProfile saved = recruiterProfileRepository.save(profile);
        log.info("Updated recruiter profile for user ID: {}", userId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RecruiterProfile getProfileEntityByUserId(Long userId) {
        return recruiterProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("RecruiterProfile", "userId", userId));
    }

    @Override
    @Transactional
    public RecruiterProfile getOrCreateProfileEntity(Long userId) {
        return recruiterProfileRepository.findByUser_Id(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                    RecruiterProfile newProfile = RecruiterProfile.builder()
                            .user(user)
                            .firstName("Recruiter")
                            .lastName("User")
                            .designation("Talent Acquisition")
                            .build();
                    return recruiterProfileRepository.save(newProfile);
                });
    }

    private RecruiterProfileResponse mapToResponse(RecruiterProfile profile) {
        return RecruiterProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser() != null ? profile.getUser().getId() : null)
                .email(profile.getUser() != null ? profile.getUser().getEmail() : null)
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .phone(profile.getPhone())
                .designation(profile.getDesignation())
                .department(profile.getDepartment())
                .isCompanyAdmin(profile.isCompanyAdmin())
                .companyId(profile.getCompany() != null ? profile.getCompany().getId() : null)
                .companyName(profile.getCompany() != null ? profile.getCompany().getName() : null)
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
