package com.careerforge.specification;

import com.careerforge.entity.RecruiterProfile;
import com.careerforge.entity.StudentProfile;
import com.careerforge.entity.User;
import com.careerforge.entity.enums.Role;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class UserSpecification {

    public static Specification<User> buildAdminUserSpecification(String search, Role role, Boolean enabled) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Role filter
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }

            // 2. Enabled status filter
            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }

            // 3. Keyword Search (User.email, StudentProfile.firstName, StudentProfile.lastName, RecruiterProfile.firstName, RecruiterProfile.lastName)
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase().trim() + "%";
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), pattern);

                // Subquery for StudentProfile name match
                Subquery<Long> studentSubquery = query.subquery(Long.class);
                Root<StudentProfile> spRoot = studentSubquery.from(StudentProfile.class);
                studentSubquery.select(spRoot.get("user").get("id"));
                Predicate studentNameMatch = cb.or(
                        cb.like(cb.lower(spRoot.get("firstName")), pattern),
                        cb.like(cb.lower(spRoot.get("lastName")), pattern)
                );
                studentSubquery.where(cb.and(cb.equal(spRoot.get("user").get("id"), root.get("id")), studentNameMatch));

                // Subquery for RecruiterProfile name match
                Subquery<Long> recruiterSubquery = query.subquery(Long.class);
                Root<RecruiterProfile> rpRoot = recruiterSubquery.from(RecruiterProfile.class);
                recruiterSubquery.select(rpRoot.get("user").get("id"));
                Predicate recruiterNameMatch = cb.or(
                        cb.like(cb.lower(rpRoot.get("firstName")), pattern),
                        cb.like(cb.lower(rpRoot.get("lastName")), pattern)
                );
                recruiterSubquery.where(cb.and(cb.equal(rpRoot.get("user").get("id"), root.get("id")), recruiterNameMatch));

                predicates.add(cb.or(emailMatch, cb.exists(studentSubquery), cb.exists(recruiterSubquery)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
