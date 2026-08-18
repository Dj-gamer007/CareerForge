package com.careerforge.service;

import com.careerforge.dto.response.SkillMatchResponse;
import com.careerforge.entity.JobSkill;
import com.careerforge.entity.StudentSkill;

import java.util.List;

public interface SkillMatchingService {

    /**
     * Pure deterministic matching evaluation between a list of student skills and job skills.
     */
    SkillMatchResponse calculateMatch(List<StudentSkill> studentSkills, List<JobSkill> jobSkills);

    /**
     * Calculates match score and gap analysis for a student profile and a job by their IDs.
     */
    SkillMatchResponse calculateMatchForStudentAndJob(Long studentProfileId, Long jobId);
}
