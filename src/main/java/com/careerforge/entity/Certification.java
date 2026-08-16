package com.careerforge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Represents a professional certification held by a student.
 * expiryDate may be null for certifications that do not expire.
 */
@Entity
@Table(name = "certifications", indexes = {
    @Index(name = "idx_cert_student_profile", columnList = "student_profile_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "issuing_organization", length = 150)
    private String issuingOrganization;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    /** Nullable: null means the certification does not expire. */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "credential_id", length = 100)
    private String credentialId;

    @Column(name = "credential_url", length = 255)
    private String credentialUrl;
}
