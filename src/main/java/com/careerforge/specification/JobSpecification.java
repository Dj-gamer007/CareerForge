package com.careerforge.specification;

import com.careerforge.dto.request.JobSearchCriteria;
import com.careerforge.entity.Company;
import com.careerforge.entity.Job;
import com.careerforge.entity.JobSkill;
import com.careerforge.entity.Skill;
import com.careerforge.entity.enums.JobStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class JobSpecification {

    public static Specification<Job> buildPublicSpecification(JobSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Only PUBLISHED jobs for public/candidate discovery
            predicates.add(cb.equal(root.get("status"), JobStatus.PUBLISHED));

            // 2. Keyword filter across title, description, company name, and job skills (both required & optional)
            if (StringUtils.hasText(criteria.getKeyword())) {
                String pattern = "%" + criteria.getKeyword().toLowerCase().trim() + "%";
                Join<Job, Company> companyJoin = root.join("company", JoinType.LEFT);
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                Predicate companyMatch = cb.like(cb.lower(companyJoin.get("name")), pattern);

                Subquery<Long> skillKeywordSubquery = query.subquery(Long.class);
                Root<JobSkill> jsRoot = skillKeywordSubquery.from(JobSkill.class);
                Join<JobSkill, Skill> skillJoin = jsRoot.join("skill", JoinType.INNER);
                skillKeywordSubquery.select(jsRoot.get("id"))
                        .where(
                                cb.equal(jsRoot.get("job"), root),
                                cb.like(cb.lower(skillJoin.get("name")), pattern)
                        );
                Predicate skillMatch = cb.exists(skillKeywordSubquery);

                predicates.add(cb.or(titleMatch, descMatch, companyMatch, skillMatch));
            }

            // 3. Location / Remote filter
            if (StringUtils.hasText(criteria.getLocation())) {
                String loc = criteria.getLocation().toLowerCase().trim();
                if ("remote".equalsIgnoreCase(loc)) {
                    predicates.add(cb.or(
                            cb.equal(root.get("workMode"), com.careerforge.entity.enums.WorkMode.REMOTE),
                            cb.like(cb.lower(root.get("location")), "%remote%")
                    ));
                } else {
                    predicates.add(cb.like(cb.lower(root.get("location")), "%" + loc + "%"));
                }
            }

            // 4. Work modes filter
            if (criteria.getWorkModes() != null && !criteria.getWorkModes().isEmpty()) {
                predicates.add(root.get("workMode").in(criteria.getWorkModes()));
            }

            // 5. Job types filter
            if (criteria.getJobTypes() != null && !criteria.getJobTypes().isEmpty()) {
                predicates.add(root.get("jobType").in(criteria.getJobTypes()));
            }

            // 6. Experience levels filter
            if (criteria.getExperienceLevels() != null && !criteria.getExperienceLevels().isEmpty()) {
                predicates.add(root.get("experienceLevel").in(criteria.getExperienceLevels()));
            }

            // 7. Salary range filters
            if (criteria.getSalaryMin() != null) {
                predicates.add(cb.or(
                        cb.greaterThanOrEqualTo(root.get("salaryMax"), criteria.getSalaryMin()),
                        cb.greaterThanOrEqualTo(root.get("salaryMin"), criteria.getSalaryMin())
                ));
            }
            if (criteria.getSalaryMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("salaryMin"), criteria.getSalaryMax()));
            }

            // 8. Company ID filter
            if (criteria.getCompanyId() != null) {
                predicates.add(cb.equal(root.get("company").get("id"), criteria.getCompanyId()));
            }

            // Skill IDs filter - ALL selected skills must exist on the job
            if (criteria.getSkillIds() != null && !criteria.getSkillIds().isEmpty()) {
                List<Long> skillIds = criteria.getSkillIds().stream()
                .distinct()
                .toList();

                Subquery<Long> skillSubquery = query.subquery(Long.class);
                Root<JobSkill> jsRoot = skillSubquery.from(JobSkill.class);

                skillSubquery.select(cb.countDistinct(jsRoot.get("skill").get("id")))
                .where(
                    cb.equal(jsRoot.get("job"), root),
                    jsRoot.get("skill").get("id").in(skillIds)
                );
                predicates.add(cb.equal(skillSubquery, (long) skillIds.size()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
