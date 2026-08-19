package com.careerforge.repository;

import com.careerforge.entity.Job;
import com.careerforge.entity.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    @EntityGraph(attributePaths = {"company", "recruiter"})
    Optional<Job> findByIdAndCompany_Id(Long id, Long companyId);

    @EntityGraph(attributePaths = {"company", "recruiter"})
    Optional<Job> findBySlug(String slug);

    @EntityGraph(attributePaths = {"company"})
    Page<Job> findAllByCompany_Id(Long companyId, Pageable pageable);

    @EntityGraph(attributePaths = {"company"})
    Page<Job> findAllByCompany_IdAndStatus(Long companyId, JobStatus status, Pageable pageable);

    boolean existsBySlug(String slug);

    long countByCompany_Id(Long companyId);

    long countByCompany_IdAndStatus(Long companyId, JobStatus status);

    long countByStatus(JobStatus status);

    long countByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT new com.careerforge.dto.response.analytics.MetricCountDto(j.status, COUNT(j)) FROM Job j " +
            "WHERE (:from IS NULL OR j.createdAt >= :from) AND (:to IS NULL OR j.createdAt <= :to) GROUP BY j.status")
    java.util.List<com.careerforge.dto.response.analytics.MetricCountDto<JobStatus>> countJobsGroupedByStatus(
            @org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from,
            @org.springframework.data.repository.query.Param("to") java.time.LocalDateTime to);

    @org.springframework.data.jpa.repository.Query("SELECT new com.careerforge.dto.response.analytics.MetricCountDto(j.workMode, COUNT(j)) FROM Job j " +
            "WHERE (:from IS NULL OR j.createdAt >= :from) AND (:to IS NULL OR j.createdAt <= :to) GROUP BY j.workMode")
    java.util.List<com.careerforge.dto.response.analytics.MetricCountDto<com.careerforge.entity.enums.WorkMode>> countJobsGroupedByWorkMode(
            @org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from,
            @org.springframework.data.repository.query.Param("to") java.time.LocalDateTime to);

    @org.springframework.data.jpa.repository.Query("SELECT new com.careerforge.dto.response.analytics.MetricCountDto(j.jobType, COUNT(j)) FROM Job j " +
            "WHERE (:from IS NULL OR j.createdAt >= :from) AND (:to IS NULL OR j.createdAt <= :to) GROUP BY j.jobType")
    java.util.List<com.careerforge.dto.response.analytics.MetricCountDto<com.careerforge.entity.enums.JobType>> countJobsGroupedByJobType(
            @org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from,
            @org.springframework.data.repository.query.Param("to") java.time.LocalDateTime to);

    @org.springframework.data.jpa.repository.Query("SELECT new com.careerforge.dto.response.analytics.MetricCountDto(j.experienceLevel, COUNT(j)) FROM Job j " +
            "WHERE (:from IS NULL OR j.createdAt >= :from) AND (:to IS NULL OR j.createdAt <= :to) GROUP BY j.experienceLevel")
    java.util.List<com.careerforge.dto.response.analytics.MetricCountDto<com.careerforge.entity.enums.ExperienceLevel>> countJobsGroupedByExperienceLevel(
            @org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from,
            @org.springframework.data.repository.query.Param("to") java.time.LocalDateTime to);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(j) FROM Job j WHERE (:from IS NULL OR j.createdAt >= :from) AND (:to IS NULL OR j.createdAt <= :to)")
    long countJobsWithDateRange(
            @org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from,
            @org.springframework.data.repository.query.Param("to") java.time.LocalDateTime to);
}
