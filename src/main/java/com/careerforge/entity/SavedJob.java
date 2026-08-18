package com.careerforge.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Stores bookmarked jobs for student profiles.
 */
@Entity
@Table(
    name = "saved_jobs",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_saved_job_student_job", columnNames = {"student_profile_id", "job_id"})
    },
    indexes = {
        @Index(name = "idx_saved_job_student_id", columnList = "student_profile_id"),
        @Index(name = "idx_saved_job_job_id", columnList = "job_id"),
        @Index(name = "idx_saved_job_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedJob extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;
}
