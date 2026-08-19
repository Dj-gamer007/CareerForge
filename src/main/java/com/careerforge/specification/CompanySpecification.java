package com.careerforge.specification;

import com.careerforge.entity.Company;
import com.careerforge.entity.enums.CompanyVerificationStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class CompanySpecification {

    public static Specification<Company> buildAdminCompanySpecification(String search, CompanyVerificationStatus verificationStatus, String industry) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (verificationStatus != null) {
                predicates.add(cb.equal(root.get("verificationStatus"), verificationStatus));
            }

            if (StringUtils.hasText(industry)) {
                predicates.add(cb.equal(cb.lower(root.get("industry")), industry.toLowerCase().trim()));
            }

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase().trim() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                Predicate locationMatch = cb.like(cb.lower(root.get("location")), pattern);
                Predicate industryMatch = cb.like(cb.lower(root.get("industry")), pattern);
                predicates.add(cb.or(nameMatch, locationMatch, industryMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
