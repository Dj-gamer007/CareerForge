package com.careerforge.repository;

import com.careerforge.entity.Job;
import com.careerforge.entity.JobSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobSkillRepository extends JpaRepository<JobSkill, Long> {

    @Query("SELECT js FROM JobSkill js JOIN FETCH js.skill WHERE js.job = :job")
    List<JobSkill> findAllByJobWithSkill(@Param("job") Job job);

    @Query("SELECT js FROM JobSkill js JOIN FETCH js.skill WHERE js.job.id IN :jobIds")
    List<JobSkill> findAllByJob_IdInWithSkill(@Param("jobIds") Collection<Long> jobIds);

    Optional<JobSkill> findByJobAndSkill_Id(Job job, Long skillId);

    boolean existsByJobAndSkill_Id(Job job, Long skillId);

    @Modifying
    @Query("DELETE FROM JobSkill js WHERE js.job = :job")
    void deleteAllByJob(@Param("job") Job job);

    long countByJob(Job job);

    long countByJobAndIsRequiredTrue(Job job);
}
