package com.careerforge.service;

import com.careerforge.dto.response.ResumeResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResumeService {

    ResumeResponse uploadResume(Long userId, MultipartFile file);

    List<ResumeResponse> getStudentResumes(Long userId);

    ResumeDownloadResult downloadResume(Long userId, Long resumeId);

    ResumeResponse setActiveResume(Long userId, Long resumeId);

    void deleteResume(Long userId, Long resumeId);

    record ResumeDownloadResult(Resource resource, String originalFileName, String contentType) {}
}
