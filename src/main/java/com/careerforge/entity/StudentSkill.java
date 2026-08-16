package com.careerforge.entity;

import com.careerforge.entity.enums.SkillProficiency;
import jakarta.persistence.*;
import lombok.*;

/**
 * Join entity representing a skill on a student's profile, enriched with
 * a self-assessed proficiency level. The unique constraint prevents a student
 * from adding the same Skill more than once.
 */
@Entity
@Table(
    name = "student_skills",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_skill", columnNames = {"student_profile_id", "skill_id"})
    },
    indexes = {
        @Index(name = "idx_ss_student_profile", columnList = "student_profile_id"),
        @Index(name = "idx_ss_skill",            columnList = "skill_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSkill extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SkillProficiency proficiency;
}
