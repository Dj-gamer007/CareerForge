package com.careerforge.repository;

import com.careerforge.entity.Project;
import com.careerforge.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByStudentProfileOrderByStartDateDesc(StudentProfile profile);

    Optional<Project> findByIdAndStudentProfile(Long id, StudentProfile profile);

    long countByStudentProfile(StudentProfile profile);
}
