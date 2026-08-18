package com.careerforge.repository;

import com.careerforge.entity.SavedJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SavedJobRepository extends JpaRepository<SavedJob, Long> {

    @EntityGraph(attributePaths = {"job", "job.company"})
    Optional<SavedJob> findByStudentProfile_IdAndJob_Id(Long studentProfileId, Long jobId);

    boolean existsByStudentProfile_IdAndJob_Id(Long studentProfileId, Long jobId);

    @EntityGraph(attributePaths = {"job", "job.company"})
    Page<SavedJob> findAllByStudentProfile_Id(Long studentProfileId, Pageable pageable);

    @Modifying
    void deleteByStudentProfile_IdAndJob_Id(Long studentProfileId, Long jobId);

    long countByStudentProfile_Id(Long studentProfileId);
}
