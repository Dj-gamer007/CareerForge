package com.careerforge.specification;

import com.careerforge.entity.Company;
import com.careerforge.entity.Job;
import com.careerforge.entity.JobSkill;
import com.careerforge.entity.Skill;
import com.careerforge.entity.enums.JobStatus;
import com.careerforge.entity.enums.JobType;
import com.careerforge.entity.enums.WorkMode;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class AdminJobSpecification {

    public static Specification<Job> buildAdminJobSpecification(
            String search,
            JobStatus status,
            Long companyId,
            WorkMode workMode,
            JobType jobType
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Job, Company> companyJoin = root.join("company", JoinType.LEFT);

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (companyId != null) {
                predicates.add(cb.equal(companyJoin.get("id"), companyId));
            }

            if (workMode != null) {
                predicates.add(cb.equal(root.get("workMode"), workMode));
            }

            if (jobType != null) {
                predicates.add(cb.equal(root.get("jobType"), jobType));
            }

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase().trim() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                Predicate companyNameMatch = cb.like(cb.lower(companyJoin.get("name")), pattern);

                Subquery<Long> skillKeywordSubquery = query.subquery(Long.class);
                Root<JobSkill> jsRoot = skillKeywordSubquery.from(JobSkill.class);
                Join<JobSkill, Skill> skillJoin = jsRoot.join("skill", JoinType.INNER);
                skillKeywordSubquery.select(jsRoot.get("id"))
                        .where(
                                cb.equal(jsRoot.get("job"), root),
                                cb.like(cb.lower(skillJoin.get("name")), pattern)
                        );
                Predicate skillMatch = cb.exists(skillKeywordSubquery);

                predicates.add(cb.or(titleMatch, descMatch, companyNameMatch, skillMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
