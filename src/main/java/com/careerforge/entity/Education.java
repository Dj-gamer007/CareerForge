package com.careerforge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Represents a single education record belonging to a student's profile.
 * A student may hold multiple education entries (undergraduate, postgraduate, etc.).
 * When currentlyStudying is true, endDate may be null.
 */
@Entity
@Table(name = "education", indexes = {
    @Index(name = "idx_edu_student_profile", columnList = "student_profile_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Education extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @Column(nullable = false, length = 150)
    private String institution;

    @Column(length = 100)
    private String degree;

    @Column(name = "field_of_study", length = 100)
    private String fieldOfStudy;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Builder.Default
    @Column(name = "currently_studying", nullable = false)
    private boolean currentlyStudying = false;

    @Column(name = "grade_or_gpa", length = 50)
    private String gradeOrGpa;
}
