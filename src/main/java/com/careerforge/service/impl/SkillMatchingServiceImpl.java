package com.careerforge.service.impl;

import com.careerforge.dto.response.MissingSkillDto;
import com.careerforge.dto.response.SkillMatchDetailDto;
import com.careerforge.dto.response.SkillMatchResponse;
import com.careerforge.entity.JobSkill;
import com.careerforge.entity.StudentSkill;
import com.careerforge.entity.enums.SkillProficiency;
import com.careerforge.repository.JobSkillRepository;
import com.careerforge.repository.StudentSkillRepository;
import com.careerforge.service.SkillMatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillMatchingServiceImpl implements SkillMatchingService {

    private static final double REQUIRED_SKILL_WEIGHT = 2.0;
    private static final double OPTIONAL_SKILL_WEIGHT = 1.0;

    private final StudentSkillRepository studentSkillRepository;
    private final JobSkillRepository jobSkillRepository;

    @Override
    public SkillMatchResponse calculateMatch(List<StudentSkill> studentSkills, List<JobSkill> jobSkills) {
        if (jobSkills == null || jobSkills.isEmpty()) {
            return SkillMatchResponse.builder()
                    .overallScore(BigDecimal.valueOf(100.00).setScale(2, RoundingMode.HALF_UP))
                    .matchedRequiredCount(0)
                    .totalRequiredCount(0)
                    .matchedOptionalCount(0)
                    .totalOptionalCount(0)
                    .totalJobSkillsCount(0)
                    .totalStudentSkillsCount(studentSkills != null ? studentSkills.size() : 0)
                    .isEligible(true)
                    .matchedSkills(Collections.emptyList())
                    .missingRequiredSkills(Collections.emptyList())
                    .missingOptionalSkills(Collections.emptyList())
                    .build();
        }

        // Map student skills by skill ID
        Map<Long, StudentSkill> studentSkillMap = new HashMap<>();
        if (studentSkills != null) {
            for (StudentSkill ss : studentSkills) {
                if (ss.getSkill() != null && ss.getSkill().getId() != null) {
                    studentSkillMap.put(ss.getSkill().getId(), ss);
                }
            }
        }

        double totalJobWeight = 0.0;
        double totalEarnedScore = 0.0;

        int totalRequiredCount = 0;
        int matchedRequiredCount = 0;
        int totalOptionalCount = 0;
        int matchedOptionalCount = 0;

        List<SkillMatchDetailDto> matchedSkills = new ArrayList<>();
        List<MissingSkillDto> missingRequiredSkills = new ArrayList<>();
        List<MissingSkillDto> missingOptionalSkills = new ArrayList<>();

        for (JobSkill js : jobSkills) {
            double weight = js.isRequired() ? REQUIRED_SKILL_WEIGHT : OPTIONAL_SKILL_WEIGHT;
            totalJobWeight += weight;

            if (js.isRequired()) {
                totalRequiredCount++;
            } else {
                totalOptionalCount++;
            }

            Long skillId = (js.getSkill() != null) ? js.getSkill().getId() : null;
            StudentSkill studentSkill = (skillId != null) ? studentSkillMap.get(skillId) : null;

            if (studentSkill != null) {
                // Matched skill
                if (js.isRequired()) {
                    matchedRequiredCount++;
                } else {
                    matchedOptionalCount++;
                }

                int studentProficiencyVal = getProficiencyValue(studentSkill.getProficiency());
                int requiredProficiencyVal = getProficiencyValue(js.getMinimumProficiency());

                double multiplier = Math.min(1.0, (double) studentProficiencyVal / requiredProficiencyVal);
                double contribution = weight * multiplier;
                totalEarnedScore += contribution;

                matchedSkills.add(SkillMatchDetailDto.builder()
                        .skillId(skillId)
                        .skillName(js.getSkill() != null ? js.getSkill().getName() : "")
                        .category(js.getSkill() != null ? js.getSkill().getCategory() : "")
                        .isRequired(js.isRequired())
                        .requiredProficiency(js.getMinimumProficiency())
                        .studentProficiency(studentSkill.getProficiency())
                        .proficiencyMultiplier(BigDecimal.valueOf(multiplier).setScale(2, RoundingMode.HALF_UP))
                        .skillWeight(BigDecimal.valueOf(weight).setScale(1, RoundingMode.HALF_UP))
                        .effectiveScoreContribution(BigDecimal.valueOf(contribution).setScale(2, RoundingMode.HALF_UP))
                        .build());
            } else {
                // Missing skill
                MissingSkillDto missingDto = MissingSkillDto.builder()
                        .skillId(skillId)
                        .skillName(js.getSkill() != null ? js.getSkill().getName() : "")
                        .category(js.getSkill() != null ? js.getSkill().getCategory() : "")
                        .isRequired(js.isRequired())
                        .requiredProficiency(js.getMinimumProficiency())
                        .build();

                if (js.isRequired()) {
                    missingRequiredSkills.add(missingDto);
                } else {
                    missingOptionalSkills.add(missingDto);
                }
            }
        }

        double rawScore = (totalJobWeight > 0) ? (totalEarnedScore / totalJobWeight) * 100.0 : 100.0;
        BigDecimal overallScore = BigDecimal.valueOf(rawScore).setScale(2, RoundingMode.HALF_UP);

        // Eligibility check: score >= 50% AND (if there are required skills, at least 1 is matched)
        boolean isEligible = overallScore.compareTo(BigDecimal.valueOf(50.00)) >= 0
                && (totalRequiredCount == 0 || matchedRequiredCount >= 1);

        return SkillMatchResponse.builder()
                .overallScore(overallScore)
                .matchedRequiredCount(matchedRequiredCount)
                .totalRequiredCount(totalRequiredCount)
                .matchedOptionalCount(matchedOptionalCount)
                .totalOptionalCount(totalOptionalCount)
                .totalJobSkillsCount(jobSkills.size())
                .totalStudentSkillsCount(studentSkills != null ? studentSkills.size() : 0)
                .isEligible(isEligible)
                .matchedSkills(matchedSkills)
                .missingRequiredSkills(missingRequiredSkills)
                .missingOptionalSkills(missingOptionalSkills)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SkillMatchResponse calculateMatchForStudentAndJob(Long studentProfileId, Long jobId) {
        List<StudentSkill> studentSkills = studentSkillRepository.findAllByStudentProfile_IdWithSkill(studentProfileId);
        List<JobSkill> jobSkills = jobSkillRepository.findAllByJob_IdWithSkill(jobId);
        return calculateMatch(studentSkills, jobSkills);
    }

    private int getProficiencyValue(SkillProficiency proficiency) {
        if (proficiency == null) {
            return 1;
        }
        return switch (proficiency) {
            case BEGINNER -> 1;
            case INTERMEDIATE -> 2;
            case ADVANCED -> 3;
            case EXPERT -> 4;
        };
    }
}
