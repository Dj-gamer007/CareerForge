package com.careerforge.entity;

import com.careerforge.entity.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores student job applications, snapshots resume and match score at application time,
 * and maintains the recruiter ATS lifecycle state.
 */
@Entity
@Table(
    name = "applications",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_application_student_job", columnNames = {"student_profile_id", "job_id"})
    },
    indexes = {
        @Index(name = "idx_app_student_profile_id", columnList = "student_profile_id"),
        @Index(name = "idx_app_job_id", columnList = "job_id"),
        @Index(name = "idx_app_status", columnList = "status"),
        @Index(name = "idx_app_job_status", columnList = "job_id, status"),
        @Index(name = "idx_app_job_score", columnList = "job_id, match_score_at_application"),
        @Index(name = "idx_app_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = true)
    private Resume resume;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Column(name = "match_score_at_application", precision = 5, scale = 2, nullable = false)
    private BigDecimal matchScoreAtApplication;

    @Column(name = "recruiter_notes", columnDefinition = "TEXT")
    private String recruiterNotes;

    @Column(name = "interview_scheduled_at")
    private LocalDateTime interviewScheduledAt;

    @Column(name = "shortlisted_at")
    private LocalDateTime shortlistedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ApplicationStatusHistory> statusHistory = new ArrayList<>();
}
