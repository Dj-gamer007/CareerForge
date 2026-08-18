package com.careerforge.entity.enums;

/**
 * State machine lifecycle status of a Job posting on CareerForge.
 * DRAFT: Initial work-in-progress, not visible in public search.
 * PUBLISHED: Active and searchable by candidates.
 * CLOSED: Applications stopped; visible in recruiter history.
 * ARCHIVED: Permanently archived from active pipelines.
 */
public enum JobStatus {
    DRAFT,
    PUBLISHED,
    CLOSED,
    ARCHIVED
}
