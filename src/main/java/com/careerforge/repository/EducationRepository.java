package com.careerforge.repository;

import com.careerforge.entity.Education;
import com.careerforge.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EducationRepository extends JpaRepository<Education, Long> {

    List<Education> findAllByStudentProfileOrderByStartDateDesc(StudentProfile profile);

    /** Used for ownership verification: returns empty if the id does not belong to this profile. */
    Optional<Education> findByIdAndStudentProfile(Long id, StudentProfile profile);

    long countByStudentProfile(StudentProfile profile);
}
