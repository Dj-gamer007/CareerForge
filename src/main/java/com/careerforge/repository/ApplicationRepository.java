package com.careerforge.repository;

import com.careerforge.entity.Application;
import com.careerforge.entity.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {

    @EntityGraph(attributePaths = {"job", "job.company", "resume", "studentProfile", "studentProfile.user"})
    Optional<Application> findByIdAndJob_Company_Id(Long id, Long companyId);

    @EntityGraph(attributePaths = {"job", "job.company", "resume"})
    Optional<Application> findByIdAndStudentProfile_Id(Long id, Long studentProfileId);

    boolean existsByStudentProfile_IdAndJob_Id(Long studentProfileId, Long jobId);

    Optional<Application> findByStudentProfile_IdAndJob_Id(Long studentProfileId, Long jobId);

    @EntityGraph(attributePaths = {"job", "job.company", "resume"})
    Page<Application> findAllByStudentProfile_Id(Long studentProfileId, Pageable pageable);

    @EntityGraph(attributePaths = {"job", "job.company", "resume"})
    Page<Application> findAllByStudentProfile_IdAndStatus(Long studentProfileId, ApplicationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"studentProfile", "studentProfile.user", "resume"})
    Page<Application> findAllByJob_IdAndJob_Company_Id(Long jobId, Long companyId, Pageable pageable);

    @EntityGraph(attributePaths = {"studentProfile", "studentProfile.user", "resume"})
    Page<Application> findAllByJob_IdAndJob_Company_IdAndStatus(Long jobId, Long companyId, ApplicationStatus status, Pageable pageable);

    long countByJob_Id(Long jobId);

    long countByJob_IdAndStatus(Long jobId, ApplicationStatus status);

    long countByStudentProfile_Id(Long studentProfileId);
}
