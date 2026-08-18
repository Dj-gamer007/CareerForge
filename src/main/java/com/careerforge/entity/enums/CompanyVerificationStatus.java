package com.careerforge.entity.enums;

/**
 * Verification status of a company on CareerForge.
 * In Phase 3, default on creation is VERIFIED for recruiter operational autonomy;
 * Admin moderation capabilities will be integrated in Phase 6.
 */
public enum CompanyVerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED
}
