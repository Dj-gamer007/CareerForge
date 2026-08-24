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

    public static Specification<Application> buildStudentSpecification(Long studentProfileId, ApplicationStatus status) {
        return buildStudentSpecification(studentProfileId, status, null);
    }

    public static Specification<Application> buildStudentSpecification(Long studentProfileId, ApplicationStatus status, String tab) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory student profile scoping
            predicates.add(cb.equal(root.get("studentProfile").get("id"), studentProfileId));

            // 2. Milestone Tab Semantics (Naukri-style persistent milestones)
            if (StringUtils.hasText(tab)) {
                String normalizedTab = tab.trim().toUpperCase();
                switch (normalizedTab) {
                    case "APPLIED" -> {
                        // All applications submitted by student belong to Applied tab permanently
                    }
                    case "SHORTLISTED" -> {
                        // Reached the shortlisted milestone
                        Predicate shortlistedNotNull = cb.isNotNull(root.get("shortlistedAt"));
                        Predicate statusShortlisted = cb.equal(root.get("status"), ApplicationStatus.SHORTLISTED);
                        Predicate statusInterview = cb.equal(root.get("status"), ApplicationStatus.INTERVIEW_SCHEDULED);
                        Predicate statusAccepted = cb.equal(root.get("status"), ApplicationStatus.ACCEPTED);
                        predicates.add(cb.or(shortlistedNotNull, statusShortlisted, statusInterview, statusAccepted));
                    }
                    case "INTERVIEW", "INTERVIEW_SCHEDULED" -> {
                        // Current interview pipeline: applications currently in INTERVIEW_SCHEDULED status
                        predicates.add(cb.equal(root.get("status"), ApplicationStatus.INTERVIEW_SCHEDULED));
                    }
                    case "ALL" -> {
                        // All applications
                    }
                }
            }

            // 3. Current Status Filter (single source of truth for exact current status)
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
