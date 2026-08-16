package com.careerforge.service;

import com.careerforge.dto.request.*;
import com.careerforge.dto.response.*;
import com.careerforge.entity.StudentProfile;

import java.util.List;

public interface StudentProfileService {

    // Profile
    StudentProfileResponse getProfileByUserId(Long userId);
    StudentProfileResponse createProfile(Long userId, StudentProfileRequest request);
    StudentProfileResponse updateProfile(Long userId, StudentProfileRequest request);
    StudentProfile getOrCreateProfileEntity(Long userId);
    StudentProfile getProfileEntityByUserId(Long userId);
    void updateProfileCompletion(StudentProfile profile);

    // Skills
    List<StudentSkillResponse> getSkills(Long userId);
    StudentSkillResponse addSkill(Long userId, StudentSkillRequest request);
    StudentSkillResponse updateSkillProficiency(Long userId, Long skillId, StudentSkillRequest request);
    void removeSkill(Long userId, Long skillId);

    // Education
    List<EducationResponse> getEducationList(Long userId);
    EducationResponse addEducation(Long userId, EducationRequest request);
    EducationResponse updateEducation(Long userId, Long educationId, EducationRequest request);
    void deleteEducation(Long userId, Long educationId);

    // Projects
    List<ProjectResponse> getProjectList(Long userId);
    ProjectResponse addProject(Long userId, ProjectRequest request);
    ProjectResponse updateProject(Long userId, Long projectId, ProjectRequest request);
    void deleteProject(Long userId, Long projectId);

    // Certifications
    List<CertificationResponse> getCertificationList(Long userId);
    CertificationResponse addCertification(Long userId, CertificationRequest request);
    CertificationResponse updateCertification(Long userId, Long certificationId, CertificationRequest request);
    void deleteCertification(Long userId, Long certificationId);
}
