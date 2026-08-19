package com.careerforge.repository;

import com.careerforge.entity.RecruiterProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecruiterProfileRepository extends JpaRepository<RecruiterProfile, Long> {

    @EntityGraph(attributePaths = {"company", "user"})
    Optional<RecruiterProfile> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);

    @EntityGraph(attributePaths = {"user", "company"})
    List<RecruiterProfile> findAllByCompany_Id(Long companyId);

    @EntityGraph(attributePaths = {"company", "user"})
    List<RecruiterProfile> findAllByUser_IdIn(Collection<Long> userIds);

    @EntityGraph(attributePaths = {"user", "company"})
    List<RecruiterProfile> findAllByCompany_IdIn(Collection<Long> companyIds);

    long countByCompany_Id(Long companyId);
}
