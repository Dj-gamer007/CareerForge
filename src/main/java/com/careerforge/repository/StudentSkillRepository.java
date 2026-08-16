package com.careerforge.repository;

import com.careerforge.entity.StudentProfile;
import com.careerforge.entity.StudentSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentSkillRepository extends JpaRepository<StudentSkill, Long> {

    /**
     * Fetch all skills for a student profile, eagerly loading the Skill entity
     * to avoid N+1 queries when mapping to DTOs.
     */
    @Query("SELECT ss FROM StudentSkill ss JOIN FETCH ss.skill WHERE ss.studentProfile = :profile")
    List<StudentSkill> findAllByStudentProfileWithSkill(@Param("profile") StudentProfile profile);

    Optional<StudentSkill> findByStudentProfileAndSkill_Id(StudentProfile profile, Long skillId);

    boolean existsByStudentProfileAndSkill_Id(StudentProfile profile, Long skillId);

    long countByStudentProfile(StudentProfile profile);
}
