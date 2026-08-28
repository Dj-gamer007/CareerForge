-- CareerForge Production Schema Initialization Script
-- Target Database: MySQL 8.0+
-- Generated for Phase 7 Production Deployment

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `email` VARCHAR(150) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `role` ENUM('ROLE_STUDENT','ROLE_RECRUITER','ROLE_ADMIN') NOT NULL,
    `enabled` BIT(1) NOT NULL DEFAULT 1,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    INDEX `idx_users_email` (`email`),
    INDEX `idx_users_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Refresh Tokens Table
CREATE TABLE IF NOT EXISTS `refresh_tokens` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `token` VARCHAR(255) NOT NULL UNIQUE,
    `expiry_date` TIMESTAMP NOT NULL,
    `revoked` BIT(1) NOT NULL DEFAULT 0,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_refresh_tokens_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    INDEX `idx_ref_token` (`token`),
    INDEX `idx_ref_token_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Student Profiles Table
CREATE TABLE IF NOT EXISTS `student_profiles` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL UNIQUE,
    `first_name` VARCHAR(50) NOT NULL,
    `last_name` VARCHAR(50) NOT NULL,
    `phone` VARCHAR(25) NULL,
    `location` VARCHAR(100) NULL,
    `bio` TEXT NULL,
    `education_summary` TEXT NULL,
    `github_url` VARCHAR(255) NULL,
    `linkedin_url` VARCHAR(255) NULL,
    `portfolio_url` VARCHAR(255) NULL,
    `profile_completion_percentage` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    CONSTRAINT `fk_student_profiles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    INDEX `idx_sp_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Skills Table
CREATE TABLE IF NOT EXISTS `skills` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL UNIQUE,
    `category` VARCHAR(50) NOT NULL,
    INDEX `idx_skills_name` (`name`),
    INDEX `idx_skills_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Student Skills Join Table
CREATE TABLE IF NOT EXISTS `student_skills` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_profile_id` BIGINT NOT NULL,
    `skill_id` BIGINT NOT NULL,
    `proficiency` ENUM('BEGINNER','INTERMEDIATE','ADVANCED','EXPERT') NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    CONSTRAINT `uk_student_skill` UNIQUE (`student_profile_id`, `skill_id`),
    CONSTRAINT `fk_student_skills_profile` FOREIGN KEY (`student_profile_id`) REFERENCES `student_profiles` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_student_skills_skill` FOREIGN KEY (`skill_id`) REFERENCES `skills` (`id`) ON DELETE RESTRICT,
    INDEX `idx_ss_student_profile` (`student_profile_id`),
    INDEX `idx_ss_skill` (`skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Education Table
CREATE TABLE IF NOT EXISTS `education` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_profile_id` BIGINT NOT NULL,
    `institution` VARCHAR(150) NOT NULL,
    `degree` VARCHAR(100) NULL,
    `field_of_study` VARCHAR(100) NULL,
    `start_date` DATE NULL,
    `end_date` DATE NULL,
    `currently_studying` BIT(1) NOT NULL DEFAULT 0,
    `grade_or_gpa` VARCHAR(50) NULL,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    CONSTRAINT `fk_education_student_profile` FOREIGN KEY (`student_profile_id`) REFERENCES `student_profiles` (`id`) ON DELETE CASCADE,
    INDEX `idx_edu_student_profile` (`student_profile_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Projects Table
CREATE TABLE IF NOT EXISTS `projects` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_profile_id` BIGINT NOT NULL,
    `title` VARCHAR(100) NOT NULL,
    `description` TEXT NULL,
    `technologies` VARCHAR(500) NULL,
    `project_url` VARCHAR(255) NULL,
    `github_url` VARCHAR(255) NULL,
    `start_date` DATE NULL,
    `end_date` DATE NULL,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    CONSTRAINT `fk_projects_student_profile` FOREIGN KEY (`student_profile_id`) REFERENCES `student_profiles` (`id`) ON DELETE CASCADE,
    INDEX `idx_proj_student_profile` (`student_profile_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Certifications Table
CREATE TABLE IF NOT EXISTS `certifications` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_profile_id` BIGINT NOT NULL,
    `name` VARCHAR(150) NOT NULL,
    `issuing_organization` VARCHAR(150) NULL,
    `issue_date` DATE NULL,
    `expiry_date` DATE NULL,
    `credential_id` VARCHAR(100) NULL,
    `credential_url` VARCHAR(255) NULL,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    CONSTRAINT `fk_certifications_student_profile` FOREIGN KEY (`student_profile_id`) REFERENCES `student_profiles` (`id`) ON DELETE CASCADE,
    INDEX `idx_cert_student_profile` (`student_profile_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Resumes Metadata Table
CREATE TABLE IF NOT EXISTS `resumes` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_profile_id` BIGINT NOT NULL,
    `original_file_name` VARCHAR(255) NOT NULL,
    `stored_file_name` VARCHAR(255) NOT NULL UNIQUE,
    `storage_path` VARCHAR(512) NOT NULL,
    `content_type` VARCHAR(50) NOT NULL,
    `file_size` BIGINT NOT NULL,
    `version` INT NOT NULL DEFAULT 1,
    `is_active` BIT(1) NOT NULL DEFAULT 0,
    `uploaded_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_resumes_student_profile` FOREIGN KEY (`student_profile_id`) REFERENCES `student_profiles` (`id`) ON DELETE CASCADE,
    INDEX `idx_resume_student_profile` (`student_profile_id`),
    INDEX `idx_resume_active` (`student_profile_id`, `is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Companies Table
CREATE TABLE IF NOT EXISTS `companies` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(150) NOT NULL UNIQUE,
    `slug` VARCHAR(160) NOT NULL UNIQUE,
    `website` VARCHAR(255) NULL,
    `logo_url` VARCHAR(255) NULL,
    `description` TEXT NULL,
    `industry` VARCHAR(100) NOT NULL,
    `company_size` VARCHAR(50) NULL,
    `location` VARCHAR(150) NULL,
    `verification_status` ENUM('PENDING','VERIFIED','REJECTED') NOT NULL DEFAULT 'PENDING',
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    INDEX `idx_company_name` (`name`),
    INDEX `idx_company_slug` (`slug`),
    INDEX `idx_company_industry` (`industry`),
    INDEX `idx_company_status` (`verification_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. Recruiter Profiles Table
CREATE TABLE IF NOT EXISTS `recruiter_profiles` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL UNIQUE,
    `company_id` BIGINT NULL,
    `first_name` VARCHAR(50) NOT NULL,
    `last_name` VARCHAR(50) NOT NULL,
    `phone` VARCHAR(25) NULL,
    `designation` VARCHAR(100) NOT NULL,
    `department` VARCHAR(100) NULL,
    `is_company_admin` BIT(1) NOT NULL DEFAULT 0,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    CONSTRAINT `fk_recruiter_profiles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_recruiter_profiles_company` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE SET NULL,
    INDEX `idx_recruiter_user_id` (`user_id`),
    INDEX `idx_recruiter_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. Jobs Table
CREATE TABLE IF NOT EXISTS `jobs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `company_id` BIGINT NOT NULL,
    `recruiter_id` BIGINT NOT NULL,
    `title` VARCHAR(150) NOT NULL,
    `slug` VARCHAR(180) NOT NULL UNIQUE,
    `description` TEXT NOT NULL,
    `location` VARCHAR(150) NOT NULL,
    `work_mode` ENUM('REMOTE','HYBRID','ONSITE') NOT NULL,
    `job_type` ENUM('FULL_TIME','PART_TIME','INTERNSHIP','CONTRACT') NOT NULL,
    `experience_level` ENUM('ENTRY_LEVEL','MID_LEVEL','SENIOR_LEVEL','LEAD','EXECUTIVE') NOT NULL,
    `salary_min` DECIMAL(12, 2) NULL,
    `salary_max` DECIMAL(12, 2) NULL,
    `currency` VARCHAR(10) NOT NULL DEFAULT 'INR',
    `status` ENUM('DRAFT','PUBLISHED','CLOSED','ARCHIVED') NOT NULL DEFAULT 'DRAFT',
    `deadline` DATETIME(6) NULL,
    `published_at` DATETIME(6) NULL,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    CONSTRAINT `fk_jobs_company` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_jobs_recruiter` FOREIGN KEY (`recruiter_id`) REFERENCES `recruiter_profiles` (`id`) ON DELETE RESTRICT,
    INDEX `idx_jobs_company_id` (`company_id`),
    INDEX `idx_jobs_recruiter_id` (`recruiter_id`),
    INDEX `idx_jobs_title` (`title`),
    INDEX `idx_jobs_slug` (`slug`),
    INDEX `idx_jobs_status` (`status`),
    INDEX `idx_jobs_work_mode` (`work_mode`),
    INDEX `idx_jobs_job_type` (`job_type`),
    INDEX `idx_jobs_exp_level` (`experience_level`),
    INDEX `idx_jobs_location` (`location`),
    INDEX `idx_jobs_created_at` (`created_at`),
    INDEX `idx_jobs_deadline` (`deadline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. Job Skills Join Table
CREATE TABLE IF NOT EXISTS `job_skills` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `job_id` BIGINT NOT NULL,
    `skill_id` BIGINT NOT NULL,
    `is_required` BIT(1) NOT NULL DEFAULT 1,
    `minimum_proficiency` ENUM('BEGINNER','INTERMEDIATE','ADVANCED','EXPERT') NOT NULL DEFAULT 'INTERMEDIATE',
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    CONSTRAINT `uk_job_skill` UNIQUE (`job_id`, `skill_id`),
    CONSTRAINT `fk_job_skills_job` FOREIGN KEY (`job_id`) REFERENCES `jobs` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_job_skills_skill` FOREIGN KEY (`skill_id`) REFERENCES `skills` (`id`) ON DELETE RESTRICT,
    INDEX `idx_job_skill_job_id` (`job_id`),
    INDEX `idx_job_skill_skill_id` (`skill_id`),
    INDEX `idx_job_skill_required` (`is_required`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. Applications Table
CREATE TABLE IF NOT EXISTS `applications` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_profile_id` BIGINT NOT NULL,
    `job_id` BIGINT NOT NULL,
    `resume_id` BIGINT NULL,
    `status` ENUM('APPLIED', 'UNDER_REVIEW', 'SHORTLISTED', 'INTERVIEW_SCHEDULED', 'ACCEPTED', 'REJECTED', 'WITHDRAWN') NOT NULL DEFAULT 'APPLIED',
    `cover_letter` TEXT NULL,
    `match_score_at_application` DECIMAL(5, 2) NOT NULL,
    `recruiter_notes` TEXT NULL,
    `interview_scheduled_at` DATETIME(6) NULL,
    `shortlisted_at` DATETIME(6) NULL,
    `reviewed_at` DATETIME(6) NULL,
    `withdrawn_at` DATETIME(6) NULL,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    CONSTRAINT `uk_application_student_job` UNIQUE (`student_profile_id`, `job_id`),
    CONSTRAINT `fk_applications_student_profile` FOREIGN KEY (`student_profile_id`) REFERENCES `student_profiles` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_applications_job` FOREIGN KEY (`job_id`) REFERENCES `jobs` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_applications_resume` FOREIGN KEY (`resume_id`) REFERENCES `resumes` (`id`) ON DELETE SET NULL,
    INDEX `idx_app_student_profile_id` (`student_profile_id`),
    INDEX `idx_app_job_id` (`job_id`),
    INDEX `idx_app_status` (`status`),
    INDEX `idx_app_job_status` (`job_id`, `status`),
    INDEX `idx_app_job_score` (`job_id`, `match_score_at_application`),
    INDEX `idx_app_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 15. Application Status History Table
CREATE TABLE IF NOT EXISTS `application_status_history` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `application_id` BIGINT NOT NULL,
    `from_status` ENUM('APPLIED', 'UNDER_REVIEW', 'SHORTLISTED', 'INTERVIEW_SCHEDULED', 'ACCEPTED', 'REJECTED', 'WITHDRAWN') NULL,
    `to_status` ENUM('APPLIED', 'UNDER_REVIEW', 'SHORTLISTED', 'INTERVIEW_SCHEDULED', 'ACCEPTED', 'REJECTED', 'WITHDRAWN') NOT NULL,
    `changed_at` DATETIME(6) NOT NULL,
    `changed_by` VARCHAR(50) NOT NULL,
    `reason` TEXT NULL,
    `notes` TEXT NULL,
    CONSTRAINT `fk_app_status_history_application` FOREIGN KEY (`application_id`) REFERENCES `applications` (`id`) ON DELETE CASCADE,
    INDEX `idx_ash_application_id` (`application_id`),
    INDEX `idx_ash_app_id_changed_at` (`application_id`, `changed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 16. Saved Jobs Table
CREATE TABLE IF NOT EXISTS `saved_jobs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_profile_id` BIGINT NOT NULL,
    `job_id` BIGINT NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    CONSTRAINT `uk_saved_job_student_job` UNIQUE (`student_profile_id`, `job_id`),
    CONSTRAINT `fk_saved_jobs_student_profile` FOREIGN KEY (`student_profile_id`) REFERENCES `student_profiles` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_saved_jobs_job` FOREIGN KEY (`job_id`) REFERENCES `jobs` (`id`) ON DELETE CASCADE,
    INDEX `idx_saved_job_student_id` (`student_profile_id`),
    INDEX `idx_saved_job_job_id` (`job_id`),
    INDEX `idx_saved_job_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 16. Notifications Table
CREATE TABLE IF NOT EXISTS `notifications` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `actor_user_id` BIGINT NULL,
    `actor_name` VARCHAR(150) NULL,
    `title` VARCHAR(150) NOT NULL,
    `message` TEXT NOT NULL,
    `type` VARCHAR(50) NOT NULL,
    `related_entity_type` VARCHAR(50) NULL,
    `related_entity_id` BIGINT NULL,
    `is_read` BIT(1) NOT NULL DEFAULT 0,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    CONSTRAINT `fk_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_notifications_actor_user` FOREIGN KEY (`actor_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    INDEX `idx_notif_user` (`user_id`),
    INDEX `idx_notif_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 17. Audit Logs Table
CREATE TABLE IF NOT EXISTS `audit_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `actor_user_id` BIGINT NULL,
    `actor_email` VARCHAR(150) NOT NULL,
    `actor_role` VARCHAR(50) NOT NULL,
    `target_entity_id` BIGINT NULL,
    `event_type` ENUM('USER_STATUS_UPDATED', 'USER_SELF_DISABLE_REJECTED', 'COMPANY_VERIFICATION_UPDATED', 'JOB_MODERATION_PERFORMED', 'JOB_PUBLISH_GUARD_BLOCKED', 'ADMIN_LOGIN_SUCCESS', 'ADMIN_LOGIN_FAILURE') NOT NULL,
    `target_entity_type` ENUM('USER', 'COMPANY', 'JOB', 'APPLICATION', 'AUTH') NOT NULL,
    `status` ENUM('SUCCESS','FAILURE') NOT NULL,
    `ip_address` VARCHAR(45) NULL,
    `user_agent` VARCHAR(255) NULL,
    `reason` TEXT NULL,
    `details` TEXT NULL,
    `created_at` DATETIME(6) NOT NULL,
    INDEX `idx_audit_logs_created_at` (`created_at`),
    INDEX `idx_audit_logs_event_type` (`event_type`),
    INDEX `idx_audit_logs_actor_user_id` (`actor_user_id`),
    INDEX `idx_audit_logs_target` (`target_entity_type`, `target_entity_id`),
    INDEX `idx_audit_logs_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
