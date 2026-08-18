package com.careerforge.service;

import com.careerforge.dto.request.RecruiterProfileRequest;
import com.careerforge.dto.response.RecruiterProfileResponse;
import com.careerforge.entity.RecruiterProfile;

public interface RecruiterService {

    RecruiterProfileResponse getProfileByUserId(Long userId);

    RecruiterProfileResponse createProfile(Long userId, RecruiterProfileRequest request);

    RecruiterProfileResponse updateProfile(Long userId, RecruiterProfileRequest request);

    RecruiterProfile getProfileEntityByUserId(Long userId);

    RecruiterProfile getOrCreateProfileEntity(Long userId);
}
