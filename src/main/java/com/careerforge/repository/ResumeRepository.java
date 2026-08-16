package com.careerforge.repository;

import com.careerforge.entity.Resume;
import com.careerforge.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    List<Resume> findAllByStudentProfileOrderByUploadedAtDesc(StudentProfile profile);

    Optional<Resume> findByIdAndStudentProfile(Long id, StudentProfile profile);

    long countByStudentProfile(StudentProfile profile);

    long countByStudentProfileAndIsActiveTrue(StudentProfile profile);

    /** Deactivates all resumes for this profile in one query, used before setting a new active one. */
    @Modifying
    @Query("UPDATE Resume r SET r.isActive = false WHERE r.studentProfile = :profile")
    void deactivateAllByStudentProfile(@Param("profile") StudentProfile profile);
}
