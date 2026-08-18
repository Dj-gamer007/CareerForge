package com.careerforge.service;

import com.careerforge.dto.response.SkillMatchResponse;
import com.careerforge.entity.JobSkill;
import com.careerforge.entity.Skill;
import com.careerforge.entity.StudentSkill;
import com.careerforge.entity.enums.SkillProficiency;
import com.careerforge.repository.JobSkillRepository;
import com.careerforge.repository.StudentSkillRepository;
import com.careerforge.service.impl.SkillMatchingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillMatchingServiceTest {

    @Mock
    private StudentSkillRepository studentSkillRepository;

    @Mock
    private JobSkillRepository jobSkillRepository;

    @InjectMocks
    private SkillMatchingServiceImpl skillMatchingService;

    private Skill javaSkill;
    private Skill springBootSkill;
    private Skill reactSkill;
    private Skill dockerSkill;

    @BeforeEach
    void setUp() {
        javaSkill = Skill.builder().id(1L).name("Java").category("Backend").build();
        springBootSkill = Skill.builder().id(2L).name("Spring Boot").category("Backend").build();
        reactSkill = Skill.builder().id(3L).name("React").category("Frontend").build();
        dockerSkill = Skill.builder().id(4L).name("Docker").category("DevOps").build();
    }

    @Test
    @DisplayName("1. Job with zero skills should return 100.00% score and eligible = true")
    void testZeroJobSkills_Returns100Percent() {
        List<StudentSkill> studentSkills = List.of(
                StudentSkill.builder().skill(javaSkill).proficiency(SkillProficiency.ADVANCED).build()
        );

        SkillMatchResponse response = skillMatchingService.calculateMatch(studentSkills, Collections.emptyList());

        assertThat(response.getOverallScore()).isEqualByComparingTo("100.00");
        assertThat(response.isEligible()).isTrue();
        assertThat(response.getTotalJobSkillsCount()).isEqualTo(0);
        assertThat(response.getMatchedSkills()).isEmpty();
    }

    @Test
    @DisplayName("2. Candidate with zero skills applying to job with skills should return 0.00% and eligible = false")
    void testCandidateWithNoSkills_ReturnsZeroPercent() {
        List<JobSkill> jobSkills = List.of(
                JobSkill.builder().skill(javaSkill).isRequired(true).minimumProficiency(SkillProficiency.INTERMEDIATE).build(),
                JobSkill.builder().skill(reactSkill).isRequired(false).minimumProficiency(SkillProficiency.BEGINNER).build()
        );

        SkillMatchResponse response = skillMatchingService.calculateMatch(Collections.emptyList(), jobSkills);

        assertThat(response.getOverallScore()).isEqualByComparingTo("0.00");
        assertThat(response.isEligible()).isFalse();
        assertThat(response.getMatchedRequiredCount()).isEqualTo(0);
        assertThat(response.getMatchedOptionalCount()).isEqualTo(0);
        assertThat(response.getMissingRequiredSkills()).hasSize(1);
        assertThat(response.getMissingOptionalSkills()).hasSize(1);
    }

    @Test
    @DisplayName("3. 100% perfect match: candidate meets or exceeds all required and optional proficiencies")
    void testPerfectMatch_Returns100Percent() {
        List<JobSkill> jobSkills = List.of(
                JobSkill.builder().skill(javaSkill).isRequired(true).minimumProficiency(SkillProficiency.ADVANCED).build(),
                JobSkill.builder().skill(springBootSkill).isRequired(true).minimumProficiency(SkillProficiency.INTERMEDIATE).build(),
                JobSkill.builder().skill(dockerSkill).isRequired(false).minimumProficiency(SkillProficiency.BEGINNER).build()
        );

        List<StudentSkill> studentSkills = List.of(
                StudentSkill.builder().skill(javaSkill).proficiency(SkillProficiency.EXPERT).build(), // 4 vs 3 -> multiplier 1.0 (capped)
                StudentSkill.builder().skill(springBootSkill).proficiency(SkillProficiency.ADVANCED).build(), // 3 vs 2 -> multiplier 1.0
                StudentSkill.builder().skill(dockerSkill).proficiency(SkillProficiency.BEGINNER).build() // 1 vs 1 -> multiplier 1.0
        );

        SkillMatchResponse response = skillMatchingService.calculateMatch(studentSkills, jobSkills);

        assertThat(response.getOverallScore()).isEqualByComparingTo("100.00");
        assertThat(response.isEligible()).isTrue();
        assertThat(response.getMatchedRequiredCount()).isEqualTo(2);
        assertThat(response.getTotalRequiredCount()).isEqualTo(2);
        assertThat(response.getMatchedOptionalCount()).isEqualTo(1);
        assertThat(response.getTotalOptionalCount()).isEqualTo(1);
        assertThat(response.getMissingRequiredSkills()).isEmpty();
        assertThat(response.getMissingOptionalSkills()).isEmpty();
    }

    @Test
    @DisplayName("4. 0% match: candidate has skills but none match the job requirements")
    void testZeroMatchWithUnrelatedSkills_ReturnsZero() {
        List<JobSkill> jobSkills = List.of(
                JobSkill.builder().skill(javaSkill).isRequired(true).minimumProficiency(SkillProficiency.ADVANCED).build()
        );

        List<StudentSkill> studentSkills = List.of(
                StudentSkill.builder().skill(reactSkill).proficiency(SkillProficiency.EXPERT).build()
        );

        SkillMatchResponse response = skillMatchingService.calculateMatch(studentSkills, jobSkills);

        assertThat(response.getOverallScore()).isEqualByComparingTo("0.00");
        assertThat(response.isEligible()).isFalse();
        assertThat(response.getMatchedSkills()).isEmpty();
        assertThat(response.getMissingRequiredSkills()).hasSize(1);
    }

    @Test
    @DisplayName("5. Partial proficiency: candidate has skill but at lower proficiency than required")
    void testPartialProficiency_CalculatesMultiplierAccurately() {
        // Job requires Java (ADVANCED = 3, weight 2.0)
        // Total weight = 2.0
        // Student has Java (BEGINNER = 1) -> multiplier = 1/3 = 0.3333...
        // Earned = 2.0 * (1/3) = 0.6666...
        // Score = (0.6666... / 2.0) * 100 = 33.33%
        List<JobSkill> jobSkills = List.of(
                JobSkill.builder().skill(javaSkill).isRequired(true).minimumProficiency(SkillProficiency.ADVANCED).build()
        );

        List<StudentSkill> studentSkills = List.of(
                StudentSkill.builder().skill(javaSkill).proficiency(SkillProficiency.BEGINNER).build()
        );

        SkillMatchResponse response = skillMatchingService.calculateMatch(studentSkills, jobSkills);

        assertThat(response.getOverallScore()).isEqualByComparingTo("33.33");
        assertThat(response.isEligible()).isFalse(); // 33.33% < 50%
        assertThat(response.getMatchedRequiredCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("6. Required vs Optional weighting: Required skills have weight 2.0, Optional skills have weight 1.0")
    void testRequiredVsOptionalWeighting() {
        // Job has:
        // - Java (Required, weight 2.0, min ADVANCED = 3)
        // - Docker (Optional, weight 1.0, min ADVANCED = 3)
        // Total weight = 3.0
        //
        // Case A: Student has only Docker (Optional) at ADVANCED (multiplier 1.0)
        // Earned = 1.0 * 1.0 = 1.0. Score = (1.0 / 3.0) * 100 = 33.33%.
        // Eligible = false (score < 50% AND 0 required skills matched).
        List<JobSkill> jobSkills = List.of(
                JobSkill.builder().skill(javaSkill).isRequired(true).minimumProficiency(SkillProficiency.ADVANCED).build(),
                JobSkill.builder().skill(dockerSkill).isRequired(false).minimumProficiency(SkillProficiency.ADVANCED).build()
        );

        List<StudentSkill> studentSkillsOnlyOptional = List.of(
                StudentSkill.builder().skill(dockerSkill).proficiency(SkillProficiency.ADVANCED).build()
        );

        SkillMatchResponse responseA = skillMatchingService.calculateMatch(studentSkillsOnlyOptional, jobSkills);
        assertThat(responseA.getOverallScore()).isEqualByComparingTo("33.33");
        assertThat(responseA.isEligible()).isFalse();
        assertThat(responseA.getMatchedRequiredCount()).isEqualTo(0);
        assertThat(responseA.getMatchedOptionalCount()).isEqualTo(1);

        // Case B: Student has only Java (Required) at ADVANCED (multiplier 1.0)
        // Earned = 2.0 * 1.0 = 2.0. Score = (2.0 / 3.0) * 100 = 66.67%.
        // Eligible = true (score >= 50% AND 1 required skill matched).
        List<StudentSkill> studentSkillsOnlyRequired = List.of(
                StudentSkill.builder().skill(javaSkill).proficiency(SkillProficiency.ADVANCED).build()
        );

        SkillMatchResponse responseB = skillMatchingService.calculateMatch(studentSkillsOnlyRequired, jobSkills);
        assertThat(responseB.getOverallScore()).isEqualByComparingTo("66.67");
        assertThat(responseB.isEligible()).isTrue();
        assertThat(responseB.getMatchedRequiredCount()).isEqualTo(1);
        assertThat(responseB.getMatchedOptionalCount()).isEqualTo(0);
        assertThat(responseB.getMissingOptionalSkills()).hasSize(1);
    }

    @Test
    @DisplayName("7. Mixed required and optional with partial proficiency and exact 2-decimal rounding")
    void testMixedSkills_ComplexCalculation() {
        // Job:
        // 1. Java (Required, wt 2.0, req EXPERT = 4)
        // 2. Spring Boot (Required, wt 2.0, req ADVANCED = 3)
        // 3. React (Optional, wt 1.0, req ADVANCED = 3)
        // 4. Docker (Optional, wt 1.0, req INTERMEDIATE = 2)
        // Total weight = 2.0 + 2.0 + 1.0 + 1.0 = 6.0
        //
        // Student:
        // 1. Java: ADVANCED (3) -> multiplier 3/4 = 0.75 -> contrib 2.0 * 0.75 = 1.50
        // 2. Spring Boot: EXPERT (4) -> multiplier min(1.0, 4/3) = 1.0 -> contrib 2.0 * 1.0 = 2.00
        // 3. React: missing -> contrib 0.00
        // 4. Docker: BEGINNER (1) -> multiplier 1/2 = 0.50 -> contrib 1.0 * 0.50 = 0.50
        //
        // Total earned = 1.50 + 2.00 + 0.00 + 0.50 = 4.00
        // Raw score = (4.00 / 6.00) * 100 = 66.6666... -> 66.67%
        // isEligible = true (66.67 >= 50% and matched required = 2 >= 1)
        List<JobSkill> jobSkills = List.of(
                JobSkill.builder().skill(javaSkill).isRequired(true).minimumProficiency(SkillProficiency.EXPERT).build(),
                JobSkill.builder().skill(springBootSkill).isRequired(true).minimumProficiency(SkillProficiency.ADVANCED).build(),
                JobSkill.builder().skill(reactSkill).isRequired(false).minimumProficiency(SkillProficiency.ADVANCED).build(),
                JobSkill.builder().skill(dockerSkill).isRequired(false).minimumProficiency(SkillProficiency.INTERMEDIATE).build()
        );

        List<StudentSkill> studentSkills = List.of(
                StudentSkill.builder().skill(javaSkill).proficiency(SkillProficiency.ADVANCED).build(),
                StudentSkill.builder().skill(springBootSkill).proficiency(SkillProficiency.EXPERT).build(),
                StudentSkill.builder().skill(dockerSkill).proficiency(SkillProficiency.BEGINNER).build()
        );

        SkillMatchResponse response = skillMatchingService.calculateMatch(studentSkills, jobSkills);

        assertThat(response.getOverallScore()).isEqualByComparingTo("66.67");
        assertThat(response.isEligible()).isTrue();
        assertThat(response.getMatchedRequiredCount()).isEqualTo(2);
        assertThat(response.getTotalRequiredCount()).isEqualTo(2);
        assertThat(response.getMatchedOptionalCount()).isEqualTo(1);
        assertThat(response.getTotalOptionalCount()).isEqualTo(2);
        assertThat(response.getMissingOptionalSkills()).hasSize(1);
        assertThat(response.getMissingOptionalSkills().get(0).getSkillName()).isEqualTo("React");
    }

    @Test
    @DisplayName("8. Repository retrieval integration: calculateMatchForStudentAndJob")
    void testCalculateMatchForStudentAndJob() {
        List<StudentSkill> studentSkills = List.of(
                StudentSkill.builder().skill(javaSkill).proficiency(SkillProficiency.ADVANCED).build()
        );
        List<JobSkill> jobSkills = List.of(
                JobSkill.builder().skill(javaSkill).isRequired(true).minimumProficiency(SkillProficiency.ADVANCED).build()
        );

        when(studentSkillRepository.findAllByStudentProfile_IdWithSkill(10L)).thenReturn(studentSkills);
        when(jobSkillRepository.findAllByJob_IdWithSkill(200L)).thenReturn(jobSkills);

        SkillMatchResponse response = skillMatchingService.calculateMatchForStudentAndJob(10L, 200L);

        assertThat(response.getOverallScore()).isEqualByComparingTo("100.00");
        assertThat(response.isEligible()).isTrue();
        assertThat(response.getMatchedSkills()).hasSize(1);
    }
}
