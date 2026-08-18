package com.careerforge.entity;

import com.careerforge.entity.enums.ExperienceLevel;
import com.careerforge.entity.enums.JobStatus;
import com.careerforge.entity.enums.JobType;
import com.careerforge.entity.enums.WorkMode;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Stores job posting details, lifecycle state, compensation, and requirements.
 */
@Entity
@Table(name = "jobs", indexes = {
    @Index(name = "idx_jobs_company_id", columnList = "company_id"),
    @Index(name = "idx_jobs_recruiter_id", columnList = "recruiter_id"),
    @Index(name = "idx_jobs_title", columnList = "title"),
    @Index(name = "idx_jobs_slug", columnList = "slug"),
    @Index(name = "idx_jobs_status", columnList = "status"),
    @Index(name = "idx_jobs_work_mode", columnList = "work_mode"),
    @Index(name = "idx_jobs_job_type", columnList = "job_type"),
    @Index(name = "idx_jobs_exp_level", columnList = "experience_level"),
    @Index(name = "idx_jobs_location", columnList = "location"),
    @Index(name = "idx_jobs_created_at", columnList = "created_at"),
    @Index(name = "idx_jobs_deadline", columnList = "deadline")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private RecruiterProfile recruiter;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(nullable = false, length = 150)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", nullable = false, length = 20)
    private WorkMode workMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 20)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level", nullable = false, length = 20)
    private ExperienceLevel experienceLevel;

    @Column(name = "salary_min", precision = 12, scale = 2)
    private BigDecimal salaryMin;

    @Column(name = "salary_max", precision = 12, scale = 2)
    private BigDecimal salaryMax;

    @Builder.Default
    @Column(nullable = false, length = 10)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private JobStatus status = JobStatus.DRAFT;

    @Column(name = "deadline")
    private LocalDateTime deadline;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;
}
