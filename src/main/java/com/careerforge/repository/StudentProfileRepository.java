package com.careerforge.repository;

import com.careerforge.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    Optional<StudentProfile> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);

    List<StudentProfile> findAllByUser_IdIn(Collection<Long> userIds);
}
