package com.careerforge.entity;

import com.careerforge.entity.enums.SkillProficiency;
import jakarta.persistence.*;
import lombok.*;

/**
 * Associates required or optional skills with a Job posting along with minimum required proficiency.
 */
@Entity
@Table(
    name = "job_skills",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_job_skill", columnNames = {"job_id", "skill_id"})
    },
    indexes = {
        @Index(name = "idx_job_skill_job_id", columnList = "job_id"),
        @Index(name = "idx_job_skill_skill_id", columnList = "skill_id"),
        @Index(name = "idx_job_skill_required", columnList = "is_required")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobSkill extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Builder.Default
    @Column(name = "is_required", nullable = false)
    private boolean isRequired = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "minimum_proficiency", nullable = false, length = 20)
    @Builder.Default
    private SkillProficiency minimumProficiency = SkillProficiency.INTERMEDIATE;
}
