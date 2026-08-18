package com.careerforge.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Stores a recruiter's profile information and their company affiliation.
 */
@Entity
@Table(name = "recruiter_profiles", indexes = {
    @Index(name = "idx_recruiter_user_id", columnList = "user_id"),
    @Index(name = "idx_recruiter_company_id", columnList = "company_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruiterProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(length = 25)
    private String phone;

    @Column(nullable = false, length = 100)
    private String designation;

    @Column(length = 100)
    private String department;

    @Builder.Default
    @Column(name = "is_company_admin", nullable = false)
    private boolean isCompanyAdmin = false;
}
