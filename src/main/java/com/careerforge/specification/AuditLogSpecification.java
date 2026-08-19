package com.careerforge.specification;

import com.careerforge.entity.AuditLog;
import com.careerforge.entity.enums.AuditEventType;
import com.careerforge.entity.enums.AuditStatus;
import com.careerforge.entity.enums.AuditTargetType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditLogSpecification {

    public static Specification<AuditLog> buildSpecification(
            String search,
            AuditEventType eventType,
            AuditTargetType targetEntityType,
            AuditStatus status,
            Long actorUserId,
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (eventType != null) {
                predicates.add(cb.equal(root.get("eventType"), eventType));
            }

            if (targetEntityType != null) {
                predicates.add(cb.equal(root.get("targetEntityType"), targetEntityType));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (actorUserId != null) {
                predicates.add(cb.equal(root.get("actorUserId"), actorUserId));
            }

            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
            }

            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
            }

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase().trim() + "%";
                Predicate actorEmailMatch = cb.like(cb.lower(root.get("actorEmail")), pattern);
                Predicate targetIdMatch = cb.like(cb.lower(root.get("targetIdentifier")), pattern);
                Predicate reasonMatch = cb.like(cb.lower(root.get("reason")), pattern);
                predicates.add(cb.or(actorEmailMatch, targetIdMatch, reasonMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
