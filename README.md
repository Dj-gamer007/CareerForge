# CareerForge — Intelligent Career & Recruitment Management Platform

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Security](https://img.shields.io/badge/Spring%20Security-JWT%20RBAC-blue.svg)](https://spring.io/projects/spring-security)
[![Database](https://img.shields.io/badge/Database-MySQL%208.0-blue.svg)](https://www.mysql.com/)
[![Tests](https://img.shields.io/badge/Tests-200%20Passed-success.svg)](https://github.com/)

CareerForge is an enterprise-grade full-stack recruitment and career management platform built with Java 17, Spring Boot 3.x, MySQL, and modern security patterns. The system connects students and recruiters through an intelligent, deterministic skill-matching engine, automated applicant tracking (ATS), job lifecycle state machines, resume storage abstractions, real-time platform notifications, administrative governance, company verification, content moderation, append-only security audit trails, and high-performance database-aggregated platform analytics.

---

## Technical Stack & Architecture

- **Backend Framework**: Java 17, Spring Boot 3.2.5, Spring MVC, Spring Data JPA, Hibernate ORM
- **Security & RBAC**: Spring Security 6, Stateless JWT (Access Tokens + Database-backed Refresh Tokens with Rotation), BCrypt Password Hashing, Method-level Security (`@PreAuthorize`)
- **Database Layer**: MySQL 8.0 with JPA Auditing (`BaseEntity`), Compound Indexing, JPA Specifications, and `@EntityGraph` N+1 query prevention
- **Validation & Exception Handling**: Jakarta Bean Validation (`@Valid`), Custom `@RestControllerAdvice` Global Exception Handler with unified `ApiResponse<T>` and `ErrorResponse` standards
- **Storage Abstraction**: `StorageService` interface (`LocalStorageServiceImpl` for local development, isolated from database metadata)
- **Deterministic Skill-Matching Engine**: Multi-factor candidate-job compatibility scoring with weighted required/optional skills, proficiency scaling, and threshold eligibility rules
- **Applicant Tracking System (ATS)**: Multi-stage application pipeline with strict state machine transitions, interview scheduling/rescheduling, recruiter evaluation notes, and candidate resume streaming
- **Administrative Governance & Operations**: Admin RBAC, user management with self-disablement protection, company verification lifecycle, job content moderation, append-only audit trail with `Propagation.REQUIRES_NEW` transaction isolation, and server-side platform analytics engine

---

## Implemented Modules & Features

### 1. Core Authentication & Token Engine (Phase 1)
- User registration and login for `ROLE_STUDENT` and `ROLE_RECRUITER` personas.
- Short-lived HMAC-SHA512 (HS512) JWT access tokens (24h) and long-lived refresh tokens (7d).
- Token rotation on refresh to prevent replay attacks; explicit logout token revocation.
- Global exception handling and structured error responses.

### 2. Student Profile & Resume Storage (Phase 2)
- Dynamic profile completion calculation (0–100%) tracking personal bio, education, skills, projects, certifications, and active resume.
- Granular sub-resource management (Education, Projects, Certifications, Skills) with strict student ownership isolation.
- PDF resume upload with MIME/extension/size validation, version tracking, and active resume designation.

### 3. Recruiter, Company & Job Lifecycle Management (Phase 3)
- Hiring company registration with automatic recruiter-admin assignment.
- Verified company directory search and public profile discovery.
- Comprehensive **Job Lifecycle State Machine**:
  - `DRAFT` $\rightarrow$ `PUBLISHED` (requires minimum skills, valid future deadline, and verified company)
  - `PUBLISHED` $\rightarrow$ `DRAFT` (pause/unpublish)
  - `PUBLISHED` $\rightarrow$ `CLOSED` (stop accepting applications)
  - `CLOSED` $\rightarrow$ `PUBLISHED` (reopen job; requires verified company)
  - `DRAFT`/`CLOSED` $\rightarrow$ `ARCHIVED`
- Dynamic multi-criteria public job discovery using JPA Specifications with batched skill loading.
- Cross-company ownership isolation preventing recruiters from viewing or mutating jobs posted by another company.

### 4. Skill Matching Engine (Phase 4B)
- Deterministic compatibility scoring formula:
  $$\text{Score} = \left( \frac{\sum (W_k \times \mu_k)}{\sum W_j} \right) \times 100$$
  - Required skills weight: $2.0$, Optional skills weight: $1.0$.
  - Proficiency levels: `BEGINNER` (1), `INTERMEDIATE` (2), `ADVANCED` (3), `EXPERT` (4).
  - Multiplier: $\mu_k = \min\left(1.0, \frac{\text{studentProficiency}}{\text{jobRequiredProficiency}}\right)$.
  - Eligibility rule: $\text{Score} \ge 50.00\% \land (\text{totalRequired} == 0 \lor \text{matchedRequired} \ge 1)$.
- Real-time candidate match preview endpoint (`GET /api/v1/students/jobs/{id}/match-preview`).

### 5. Notifications & Saved Jobs (Phase 4C)
- User notification inbox with unread count tracking (`/api/v1/notifications/unread-count`).
- Real-time notification dispatch on application submission, status transitions, interview invitations, company verification decisions, and job moderation actions.
- Bulk and single mark-as-read endpoints with strict user scoping.
- Student saved jobs/bookmarks ecosystem with duplicate prevention and batched skill retrieval.

### 6. Student Applications & Recruiter ATS Pipeline (Phase 4D)
- Application submission with active resume resolution and profile completion guard ($\ge 30\%$).
- Exact snapshotting of `matchScoreAtApplication` persisted immutably at submission time.
- Candidate self-withdrawal supported from `APPLIED` or `UNDER_REVIEW` states.
- **Application State Machine**:
  - `APPLIED` $\rightarrow$ `UNDER_REVIEW` (records `reviewedAt`), `WITHDRAWN`
  - `UNDER_REVIEW` $\rightarrow$ `SHORTLISTED`, `WITHDRAWN`
  - `SHORTLISTED` $\rightarrow$ `INTERVIEW_SCHEDULED` (requires future timestamp), `REJECTED`
  - `INTERVIEW_SCHEDULED` $\rightarrow$ `ACCEPTED`, `REJECTED`, `INTERVIEW_SCHEDULED` (rescheduling)
  - Terminal states (`ACCEPTED`, `REJECTED`, `WITHDRAWN`) are immutable.
- Recruiter internal evaluation notes (`recruiterNotes`) kept strictly private from candidate responses.
- Secure recruiter candidate resume download restricted to the hiring company.
- Multi-criteria applicant search via `ApplicationSpecification` (jobId, companyId, status, minScore, maxScore, candidate search).

### 7. Administrative Governance & Operations (Phase 5)
- **Admin User Management & RBAC Foundation (Phase 5A)**:
  - Paginated user directory with multi-criteria filtering (`search`, `role`, `enabled`, date ranges).
  - Deep user inspection with profile aggregation (`AdminUserDetailResponse`).
  - Account status management (`enable`/`disable`) with mandatory reason and strict self-disablement protection.
  - Authentication-level enforcement of disabled accounts across login and active JWT validation.
- **Company Verification & Job Moderation (Phase 5B)**:
  - Company verification state machine (`PENDING` $\leftrightarrow$ `VERIFIED` $\leftrightarrow$ `REJECTED`) with mandatory administrative reason and recruiter notification dispatch.
  - Job moderation state machine (`PUBLISHED` $\rightarrow$ `CLOSED` [force-close], `PUBLISHED`/`DRAFT`/`CLOSED` $\rightarrow$ `ARCHIVED` [force-archive], `CLOSED`/`ARCHIVED` $\rightarrow$ `DRAFT` [return for correction]).
  - Verification guard blocking recruiters of unverified companies from publishing or reopening jobs.
- **Append-Only Audit Logging & Security Event Trail (Phase 5C)**:
  - Immutable `AuditLog` entity with indexed snapshot fields and zero mutable business entity foreign keys.
  - Transaction-isolated failure auditing (`Propagation.REQUIRES_NEW`) ensuring administrative mutation rejections and self-disable attempts survive business rollbacks.
  - Spring Security `AuthenticationAuditEventListener` capturing `ADMIN_LOGIN_SUCCESS` and `ADMIN_LOGIN_FAILURE` (restricted to existing `ROLE_ADMIN` accounts).
  - Strict JSON allowlist sanitization preventing leakage of passwords, JWTs, stack traces, and binary blobs.
- **Platform Analytics Engine (Phase 5D)**:
  - Database-level `COUNT` / `GROUP BY` aggregations via `MetricCountDto` constructor projections with zero JVM entity collection hydration.
  - Platform overview KPIs, current application funnel status breakdown, job marketplace distributions, company ecosystem metrics, user demographics, and bounded time-series trends ($1 \le \text{days} \le 365$).
  - Full enum map zero-filling (all enum constants populated with `0L` for deterministic API responses).

---

## Environment Variables Configuration

| Variable | Description | Default (Dev) |
| :--- | :--- | :--- |
| `DB_URL` | MySQL Connection JDBC URL | `jdbc:mysql://localhost:3306/careerforge_db?...` |
| `DB_USERNAME` | Database username | `root` |
| `DB_PASSWORD` | Database password | `Set via environment variable` |
| `JWT_SECRET` | HMAC-SHA512 (HS512) Base64 encoded secret key | Default 512-bit key |
| `JWT_EXPIRATION_MS` | Access Token Validity Duration (ms) | `86400000` (24 Hours) |
| `JWT_REFRESH_EXPIRATION_MS` | Refresh Token Validity Duration (ms) | `604800000` (7 Days) |
| `STORAGE_LOCAL_DIR` | Local resume file upload directory | `./uploads/resumes` |
| `STORAGE_MAX_FILE_SIZE` | Maximum upload file size in bytes (5MB) | `5242880` |
| `SERVER_PORT` | HTTP Server Port | `8080` |

---

## Development Setup & Running Locally

### Prerequisites
1. Java 17+ installed (`java -version`)
2. Apache Maven 3.9+ installed
3. MySQL 8.0 database service running locally on port 3306

### Step 1: Start MySQL Database
Ensure MySQL is running. The application automatically creates `careerforge_db` if it does not exist (`createDatabaseIfNotExist=true`).

### Step 2: Build & Run Test Suite
```bash
# Run complete test suite (200 unit, integration, and E2E tests)
./tools/apache-maven-3.9.9/bin/mvn.cmd clean test

# Run Phase 5 specific test suites
./tools/apache-maven-3.9.9/bin/mvn.cmd test "-Dtest=Admin*Test"
```

### Step 3: Run the Backend Application
```bash
./tools/apache-maven-3.9.9/bin/mvn.cmd spring-boot:run
```

---

## Development Seed Accounts (DO NOT USE IN PRODUCTION)

Upon startup, `DataInitializer` populates standard technical skills and 3 development-only accounts:

| Role | Email | Default Dev Password |
| :--- | :--- | :--- |
| **ADMIN** | `admin@careerforge.local` | `DevPass123!` |
| **RECRUITER** | `recruiter@careerforge.local` | `DevPass123!` |
| **STUDENT** | `student@careerforge.local` | `DevPass123!` |

---

## Complete API Directory

### 1. Authentication & Health (`/api/v1/auth`, `/api/v1/health`)
- `GET /api/v1/health` — System health check (`PERMIT_ALL`)
- `POST /api/v1/auth/register` — Register user (`ROLE_STUDENT` / `ROLE_RECRUITER`)
- `POST /api/v1/auth/login` — Login user (returns Access Token + Refresh Token)
- `POST /api/v1/auth/refresh` — Refresh expired access token using valid refresh token
- `POST /api/v1/auth/logout` — Revoke refresh token
- `GET /api/v1/auth/me` — Get authenticated user details

### 2. Student Profile & Sub-resources (`/api/v1/students` — Role: `ROLE_STUDENT`)
- `GET /api/v1/students/profile` — Get profile with completion %
- `POST /api/v1/students/profile` — Create student profile
- `PUT /api/v1/students/profile` — Update student profile
- `GET/POST /api/v1/students/skills` — List and add profile skills
- `PUT/DELETE /api/v1/students/skills/{skillId}` — Update or remove profile skill
- `GET/POST /api/v1/students/education` — List and add education history
- `PUT/DELETE /api/v1/students/education/{id}` — Update or delete education
- `GET/POST /api/v1/students/projects` — List and add project portfolio
- `PUT/DELETE /api/v1/students/projects/{id}` — Update or delete project
- `GET/POST /api/v1/students/certifications` — List and add certifications
- `PUT/DELETE /api/v1/students/certifications/{id}` — Update or delete certification
- `POST /api/v1/students/resumes` — Upload PDF resume
- `GET /api/v1/students/resumes` — List resume metadata
- `GET /api/v1/students/resumes/{id}/download` — Download resume binary PDF
- `PUT /api/v1/students/resumes/{id}/active` — Set active resume
- `DELETE /api/v1/students/resumes/{id}` — Delete resume

### 3. Student Applications & Saved Jobs (`/api/v1/students` — Role: `ROLE_STUDENT`)
- `POST /api/v1/students/applications` — Submit application with score snapshotting
- `GET /api/v1/students/applications` — List own applications with status filtering & pagination
- `GET /api/v1/students/applications/{id}` — View application details + live skill match analysis
- `PATCH /api/v1/students/applications/{id}/withdraw` — Self-withdraw application
- `GET /api/v1/students/jobs/{jobId}/match-preview` — Real-time skill match preview before applying
- `POST /api/v1/students/saved-jobs/{jobId}` — Bookmark published job
- `GET /api/v1/students/saved-jobs` — List bookmarked jobs with pagination
- `DELETE /api/v1/students/saved-jobs/{jobId}` — Remove job from bookmarks
- `GET /api/v1/students/saved-jobs/{jobId}/check` — Check if job is bookmarked

### 4. Recruiter & Company Management (`/api/v1/recruiters`, `/api/v1/companies`)
- `GET/POST/PUT /api/v1/recruiters/profile` — Recruiter profile management (`ROLE_RECRUITER`)
- `POST /api/v1/companies` — Register company and assign admin (`ROLE_RECRUITER`)
- `GET /api/v1/companies/my-company` — View affiliated company (`ROLE_RECRUITER`)
- `PUT /api/v1/companies/my-company` — Update company profile (`ROLE_RECRUITER`)
- `GET /api/v1/companies/{id}` — View public company profile (`PERMIT_ALL`)
- `GET /api/v1/companies/slug/{slug}` — View public company profile by slug (`PERMIT_ALL`)
- `GET /api/v1/companies` — Browse verified companies directory (`PERMIT_ALL`)

### 5. Recruiter Job Management & State Machine (`/api/v1/recruiters/jobs` — Role: `ROLE_RECRUITER`)
- `POST /api/v1/recruiters/jobs` — Create draft job with required/optional skills
- `GET /api/v1/recruiters/jobs` — List company jobs with status filtering & pagination
- `GET /api/v1/recruiters/jobs/{id}` — Get company job details with skills
- `PUT /api/v1/recruiters/jobs/{id}` — Update company job & required skills
- `PATCH /api/v1/recruiters/jobs/{id}/publish` — Transition `DRAFT`/`CLOSED` $\rightarrow$ `PUBLISHED` (requires verified company)
- `PATCH /api/v1/recruiters/jobs/{id}/unpublish` — Transition `PUBLISHED` $\rightarrow$ `DRAFT`
- `PATCH /api/v1/recruiters/jobs/{id}/close` — Transition `PUBLISHED` $\rightarrow$ `CLOSED`
- `PATCH /api/v1/recruiters/jobs/{id}/reopen` — Transition `CLOSED` $\rightarrow$ `PUBLISHED` (requires verified company)
- `PATCH /api/v1/recruiters/jobs/{id}/archive` — Transition `DRAFT`/`CLOSED` $\rightarrow$ `ARCHIVED`
- `DELETE /api/v1/recruiters/jobs/{id}` — Delete job (`DRAFT` or `ARCHIVED` only)

### 6. Recruiter Applicant Tracking System (ATS) (`/api/v1/recruiters` — Role: `ROLE_RECRUITER`)
- `GET /api/v1/recruiters/jobs/{jobId}/applications` — List applicants with status/score/keyword filtering & pagination
- `GET /api/v1/recruiters/applications/{id}` — View complete candidate dossier + skill breakdown
- `PATCH /api/v1/recruiters/applications/{id}/status` — Transition application status & schedule/reschedule interview
- `PATCH /api/v1/recruiters/applications/{id}/notes` — Update internal evaluation notes
- `GET /api/v1/recruiters/applications/{id}/resume/download` — Stream candidate resume PDF

### 7. Public Job Discovery (`/api/v1/jobs` — Role: `PERMIT_ALL`)
- `GET /api/v1/jobs` — Dynamic search across published jobs (keyword, location, workMode, jobType, experienceLevel, salary, skills, company)
- `GET /api/v1/jobs/{id}` — View published job details with skills
- `GET /api/v1/jobs/slug/{slug}` — View published job details by slug

### 8. Notifications (`/api/v1/notifications` — Role: `IS_AUTHENTICATED`)
- `GET /api/v1/notifications` — List paginated notifications (newest first)
- `GET /api/v1/notifications/unread-count` — Get current unread notification counter
- `PATCH /api/v1/notifications/{id}/read` — Mark single notification as read
- `PATCH /api/v1/notifications/read-all` — Mark all user notifications as read

### 9. Admin User Management (`/api/v1/admin/users` — Role: `ROLE_ADMIN`)
- `GET /api/v1/admin/users` — List and search users with role, enabled, keyword, and date filters
- `GET /api/v1/admin/users/{id}` — Inspect detailed user account with student/recruiter profile aggregation
- `PATCH /api/v1/admin/users/{id}/status` — Enable or disable user account (with mandatory reason and self-disablement protection)

### 10. Admin Company Verification & Moderation (`/api/v1/admin/companies` — Role: `ROLE_ADMIN`)
- `GET /api/v1/admin/companies` — List and search companies with verification status and keyword filtering
- `GET /api/v1/admin/companies/{id}` — Inspect detailed company dossier, recruiter roster, and active job count
- `PATCH /api/v1/admin/companies/{id}/verification` — Approve or reject company verification (with mandatory reason and recruiter alert)

### 11. Admin Job Content Moderation (`/api/v1/admin/jobs` — Role: `ROLE_ADMIN`)
- `GET /api/v1/admin/jobs` — List and filter all platform jobs by status, company, work mode, and keyword
- `GET /api/v1/admin/jobs/{id}` — Inspect full job details, required skills, and applicant count
- `PATCH /api/v1/admin/jobs/{id}/moderate` — Moderate job status (`FORCE_CLOSE`, `FORCE_ARCHIVE`, `RETURN_TO_DRAFT`)

### 12. Admin Security & Audit Trail (`/api/v1/admin/audit-logs` — Role: `ROLE_ADMIN`)
- `GET /api/v1/admin/audit-logs` — Paginated query of security audit events with multi-criteria filtering
- `GET /api/v1/admin/audit-logs/{id}` — Inspect complete audit event details including allowlisted state changes and client context

### 13. Admin Platform Analytics Engine (`/api/v1/admin/analytics` — Role: `ROLE_ADMIN`)
- `GET /api/v1/admin/analytics/overview` — Executive summary KPIs across users, companies, jobs, and applications
- `GET /api/v1/admin/analytics/applications/funnel` — Application lifecycle stage breakdown and conversion rates (with optional job/company/date filters)
- `GET /api/v1/admin/analytics/jobs` — Job marketplace distributions across status, work mode, job type, and experience level
- `GET /api/v1/admin/analytics/companies` — Company ecosystem distributions, verification breakdown, and recruiter metrics
- `GET /api/v1/admin/analytics/users` — User demographics, role distributions, and account health ratios
- `GET /api/v1/admin/analytics/trends` — Daily time-series activity trends for registrations, job postings, and applications ($1 \le \text{days} \le 365$)

---

## Test Suite Baseline & Verification Status

```
[INFO] Results:
[INFO] 
[INFO] Tests run: 200, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

- **Phase 1**: Core Backend, JWT Security, Token Refresh, Data Seeding (24 tests)
- **Phase 2**: Student Profile, Education, Projects, Certifications, Resume Storage (16 tests)
- **Phase 3**: Recruiter Profiles, Company Directory, Job State Machine, Public Search (24 tests)
- **Phase 4A–4D**: Application Domain, Skill Matching Engine, Notifications, Saved Jobs, ATS Pipeline (35 tests)
- **Phase 4E**: End-to-End Workflow Integration Test Suite (4 comprehensive multi-scenario tests)
- **Phase 5A**: Admin User Management, RBAC, and Self-Disablement Protection (23 tests)
- **Phase 5B**: Company Verification, Admin Job Moderation, and Publish Guards (31 tests)
- **Phase 5C**: Append-Only Security Audit Logging and Transaction Isolation (22 tests)
- **Phase 5D**: Admin Platform Analytics Engine and Database-Level Aggregations (21 tests)
- **Total Automated Test Count**: **200 Tests Passing (0 Failures, 0 Errors, 0 Skipped)**
