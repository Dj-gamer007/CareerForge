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
}
