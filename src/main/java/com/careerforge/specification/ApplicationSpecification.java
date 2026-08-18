package com.careerforge.specification;

import com.careerforge.entity.Application;
import com.careerforge.entity.Job;
import com.careerforge.entity.StudentProfile;
import com.careerforge.entity.User;
import com.careerforge.entity.enums.ApplicationStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ApplicationSpecification {

    public static Specification<Application> buildRecruiterSpecification(
            Long jobId,
            Long companyId,
            ApplicationStatus status,
            BigDecimal minScore,
            BigDecimal maxScore,
            String search) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory company scoping and job scoping
            Join<Application, Job> jobJoin = root.join("job", JoinType.INNER);
            predicates.add(cb.equal(jobJoin.get("id"), jobId));
            predicates.add(cb.equal(jobJoin.get("company").get("id"), companyId));

            // 2. Status filter
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 3. Min score filter
            if (minScore != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("matchScoreAtApplication"), minScore));
            }

            // 4. Max score filter
            if (maxScore != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("matchScoreAtApplication"), maxScore));
            }

            // 5. Candidate search filter (first name, last name, email)
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase().trim() + "%";
                Join<Application, StudentProfile> profileJoin = root.join("studentProfile", JoinType.LEFT);
                Join<StudentProfile, User> userJoin = profileJoin.join("user", JoinType.LEFT);

                Predicate firstNameMatch = cb.like(cb.lower(profileJoin.get("firstName")), pattern);
                Predicate lastNameMatch = cb.like(cb.lower(profileJoin.get("lastName")), pattern);
                Predicate emailMatch = cb.like(cb.lower(userJoin.get("email")), pattern);

                predicates.add(cb.or(firstNameMatch, lastNameMatch, emailMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
