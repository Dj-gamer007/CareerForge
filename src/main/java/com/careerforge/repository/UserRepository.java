package com.careerforge.repository;

import com.careerforge.dto.response.analytics.MetricCountDto;
import com.careerforge.entity.User;
import com.careerforge.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(Role role);

    List<User> findAllByRole(Role role);

    List<User> findAllByRoleAndEnabledTrue(Role role);

    long countByEnabled(boolean enabled);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT new com.careerforge.dto.response.analytics.MetricCountDto(u.role, COUNT(u)) FROM User u GROUP BY u.role")
    List<MetricCountDto<Role>> countUsersGroupedByRole();
}
