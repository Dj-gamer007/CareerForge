package com.careerforge.service;

import com.careerforge.dto.response.PagedResponse;
import com.careerforge.dto.response.SavedJobResponse;
import org.springframework.data.domain.Pageable;

public interface SavedJobService {

    SavedJobResponse saveJob(Long userId, Long jobId);

    void removeSavedJob(Long userId, Long jobId);

    PagedResponse<SavedJobResponse> getSavedJobs(Long userId, Pageable pageable);

    boolean isJobSaved(Long userId, Long jobId);
}
