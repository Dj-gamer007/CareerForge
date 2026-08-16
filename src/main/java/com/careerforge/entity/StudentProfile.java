package com.careerforge.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Stores a student's extended profile information. One-to-one with User.
 * The profileCompletionPercentage is calculated and persisted server-side;
 * the client never sets it directly.
 */
@Entity
@Table(name = "student_profiles", indexes = {
    @Index(name = "idx_sp_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(length = 25)
    private String phone;

    @Column(length = 100)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "education_summary", columnDefinition = "TEXT")
    private String educationSummary;

    @Column(name = "github_url", length = 255)
    private String githubUrl;

    @Column(name = "linkedin_url", length = 255)
    private String linkedinUrl;

    @Column(name = "portfolio_url", length = 255)
    private String portfolioUrl;

    /**
     * Calculated server-side. Ranges 0–100. Never set directly by the client.
     */
    @Builder.Default
    @Column(name = "profile_completion_percentage", nullable = false)
    private Integer profileCompletionPercentage = 0;
}
