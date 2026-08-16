package com.careerforge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Represents a personal or academic project on a student's profile.
 * Technologies is stored as a free-form comma-separated string for flexibility;
 * full skill-linking can be added in a later phase if required.
 */
@Entity
@Table(name = "projects", indexes = {
    @Index(name = "idx_proj_student_profile", columnList = "student_profile_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Comma-separated list of technologies used (e.g. "Java, Spring Boot, MySQL"). */
    @Column(length = 500)
    private String technologies;

    @Column(name = "project_url", length = 255)
    private String projectUrl;

    @Column(name = "github_url", length = 255)
    private String githubUrl;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;
}
