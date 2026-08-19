package com.careerforge.repository;

import com.careerforge.entity.Company;
import com.careerforge.entity.enums.CompanyVerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long>, JpaSpecificationExecutor<Company> {

    Optional<Company> findByNameIgnoreCase(String name);

    Optional<Company> findBySlug(String slug);

    boolean existsByNameIgnoreCase(String name);

    boolean existsBySlug(String slug);

    Page<Company> findAllByVerificationStatus(CompanyVerificationStatus status, Pageable pageable);

    Page<Company> findAllByVerificationStatusAndNameContainingIgnoreCase(
            CompanyVerificationStatus status, String name, Pageable pageable);

    long countByVerificationStatus(CompanyVerificationStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT new com.careerforge.dto.response.analytics.MetricCountDto(c.verificationStatus, COUNT(c)) FROM Company c GROUP BY c.verificationStatus")
    java.util.List<com.careerforge.dto.response.analytics.MetricCountDto<CompanyVerificationStatus>> countCompaniesGroupedByVerificationStatus();

    @org.springframework.data.jpa.repository.Query("SELECT new com.careerforge.dto.response.analytics.MetricCountDto(c.companySize, COUNT(c)) FROM Company c WHERE c.companySize IS NOT NULL GROUP BY c.companySize")
    java.util.List<com.careerforge.dto.response.analytics.MetricCountDto<String>> countCompaniesGroupedBySize();
}
