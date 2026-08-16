package com.careerforge.controller;

import com.careerforge.dto.request.*;
import com.careerforge.dto.response.*;
import com.careerforge.security.UserPrincipal;
import com.careerforge.service.ResumeService;
import com.careerforge.service.StudentProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    private final StudentProfileService studentProfileService;
    private final ResumeService resumeService;

    // ==========================================
    // 1. Profile Endpoints
    // ==========================================

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<StudentProfileResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        StudentProfileResponse profile = studentProfileService.getProfileByUserId(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Student profile retrieved successfully", profile));
    }

    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<StudentProfileResponse>> createProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody StudentProfileRequest request) {
        StudentProfileResponse profile = studentProfileService.createProfile(userPrincipal.getId(), request);
        return new ResponseEntity<>(ApiResponse.success("Student profile created successfully", profile), HttpStatus.CREATED);
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<StudentProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody StudentProfileRequest request) {
        StudentProfileResponse profile = studentProfileService.updateProfile(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Student profile updated successfully", profile));
    }

    // ==========================================
    // 2. Skills Endpoints
    // ==========================================

    @GetMapping("/skills")
    public ResponseEntity<ApiResponse<List<StudentSkillResponse>>> getSkills(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<StudentSkillResponse> skills = studentProfileService.getSkills(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Student skills retrieved successfully", skills));
    }

    @PostMapping("/skills")
    public ResponseEntity<ApiResponse<StudentSkillResponse>> addSkill(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody StudentSkillRequest request) {
        StudentSkillResponse skill = studentProfileService.addSkill(userPrincipal.getId(), request);
        return new ResponseEntity<>(ApiResponse.success("Skill added to profile successfully", skill), HttpStatus.CREATED);
    }

    @PutMapping("/skills/{skillId}")
    public ResponseEntity<ApiResponse<StudentSkillResponse>> updateSkillProficiency(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long skillId,
            @Valid @RequestBody StudentSkillRequest request) {
        StudentSkillResponse skill = studentProfileService.updateSkillProficiency(userPrincipal.getId(), skillId, request);
        return ResponseEntity.ok(ApiResponse.success("Skill proficiency updated successfully", skill));
    }

    @DeleteMapping("/skills/{skillId}")
    public ResponseEntity<ApiResponse<Void>> removeSkill(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long skillId) {
        studentProfileService.removeSkill(userPrincipal.getId(), skillId);
        return ResponseEntity.ok(ApiResponse.success("Skill removed from profile successfully", null));
    }

    // ==========================================
    // 3. Education Endpoints
    // ==========================================

    @GetMapping("/education")
    public ResponseEntity<ApiResponse<List<EducationResponse>>> getEducationList(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<EducationResponse> list = studentProfileService.getEducationList(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Education records retrieved successfully", list));
    }

    @PostMapping("/education")
    public ResponseEntity<ApiResponse<EducationResponse>> addEducation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody EducationRequest request) {
        EducationResponse education = studentProfileService.addEducation(userPrincipal.getId(), request);
        return new ResponseEntity<>(ApiResponse.success("Education record added successfully", education), HttpStatus.CREATED);
    }

    @PutMapping("/education/{id}")
    public ResponseEntity<ApiResponse<EducationResponse>> updateEducation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody EducationRequest request) {
        EducationResponse education = studentProfileService.updateEducation(userPrincipal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Education record updated successfully", education));
    }

    @DeleteMapping("/education/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEducation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        studentProfileService.deleteEducation(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Education record deleted successfully", null));
    }

    // ==========================================
    // 4. Projects Endpoints
    // ==========================================

    @GetMapping("/projects")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getProjectList(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<ProjectResponse> list = studentProfileService.getProjectList(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Project records retrieved successfully", list));
    }

    @PostMapping("/projects")
    public ResponseEntity<ApiResponse<ProjectResponse>> addProject(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ProjectRequest request) {
        ProjectResponse project = studentProfileService.addProject(userPrincipal.getId(), request);
        return new ResponseEntity<>(ApiResponse.success("Project added successfully", project), HttpStatus.CREATED);
    }

    @PutMapping("/projects/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request) {
        ProjectResponse project = studentProfileService.updateProject(userPrincipal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Project updated successfully", project));
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        studentProfileService.deleteProject(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Project deleted successfully", null));
    }

    // ==========================================
    // 5. Certifications Endpoints
    // ==========================================

    @GetMapping("/certifications")
    public ResponseEntity<ApiResponse<List<CertificationResponse>>> getCertificationList(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<CertificationResponse> list = studentProfileService.getCertificationList(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Certification records retrieved successfully", list));
    }

    @PostMapping("/certifications")
    public ResponseEntity<ApiResponse<CertificationResponse>> addCertification(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CertificationRequest request) {
        CertificationResponse certification = studentProfileService.addCertification(userPrincipal.getId(), request);
        return new ResponseEntity<>(ApiResponse.success("Certification added successfully", certification), HttpStatus.CREATED);
    }

    @PutMapping("/certifications/{id}")
    public ResponseEntity<ApiResponse<CertificationResponse>> updateCertification(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody CertificationRequest request) {
        CertificationResponse certification = studentProfileService.updateCertification(userPrincipal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Certification updated successfully", certification));
    }

    @DeleteMapping("/certifications/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCertification(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        studentProfileService.deleteCertification(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Certification deleted successfully", null));
    }

    // ==========================================
    // 6. Resume Management Endpoints
    // ==========================================

    @PostMapping(value = "/resumes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ResumeResponse>> uploadResume(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("file") MultipartFile file) {
        ResumeResponse resume = resumeService.uploadResume(userPrincipal.getId(), file);
        return new ResponseEntity<>(ApiResponse.success("Resume uploaded successfully", resume), HttpStatus.CREATED);
    }

    @GetMapping("/resumes")
    public ResponseEntity<ApiResponse<List<ResumeResponse>>> getResumes(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<ResumeResponse> list = resumeService.getStudentResumes(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Resumes retrieved successfully", list));
    }

    @GetMapping("/resumes/{id}/download")
    public ResponseEntity<Resource> downloadResume(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        ResumeService.ResumeDownloadResult result = resumeService.downloadResume(userPrincipal.getId(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.originalFileName() + "\"")
                .body(result.resource());
    }

    @PutMapping("/resumes/{id}/active")
    public ResponseEntity<ApiResponse<ResumeResponse>> setActiveResume(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        ResumeResponse resume = resumeService.setActiveResume(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Resume set as active successfully", resume));
    }

    @DeleteMapping("/resumes/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteResume(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        resumeService.deleteResume(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Resume deleted successfully", null));
    }
}
