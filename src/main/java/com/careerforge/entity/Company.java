package com.careerforge.entity;

import com.careerforge.entity.enums.CompanyVerificationStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a registered hiring company on CareerForge.
 * Newly registered companies default to PENDING awaiting administrator verification.
 */
@Entity
@Table(name = "companies", indexes = {
    @Index(name = "idx_company_name", columnList = "name"),
    @Index(name = "idx_company_slug", columnList = "slug"),
    @Index(name = "idx_company_industry", columnList = "industry"),
    @Index(name = "idx_company_status", columnList = "verification_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 160)
    private String slug;

    @Column(length = 255)
    private String website;

    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    private String industry;

    @Column(name = "company_size", length = 50)
    private String companySize;

    @Column(length = 150)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    @Builder.Default
    private CompanyVerificationStatus verificationStatus = CompanyVerificationStatus.PENDING;
}
