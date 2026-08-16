package com.careerforge.repository;

import com.careerforge.entity.Certification;
import com.careerforge.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificationRepository extends JpaRepository<Certification, Long> {

    List<Certification> findAllByStudentProfileOrderByIssueDateDesc(StudentProfile profile);

    Optional<Certification> findByIdAndStudentProfile(Long id, StudentProfile profile);

    long countByStudentProfile(StudentProfile profile);
}
