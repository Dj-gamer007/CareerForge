# CareerForge — Intelligent Career & Recruitment Management Platform

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Security](https://img.shields.io/badge/Spring%20Security-JWT%20RBAC-blue.svg)](https://spring.io/projects/spring-security)
[![Database](https://img.shields.io/badge/Database-MySQL%208.0-blue.svg)](https://www.mysql.com/)

CareerForge is a production-grade full-stack recruitment and career management platform built with Java 17, Spring Boot 3.x, MySQL, and React + TypeScript.

---

## Technical Stack & Architecture

- **Backend Framework**: Java 17, Spring Boot 3.2.5, Spring MVC, Spring Data JPA, Hibernate
- **Security & Auth**: Spring Security 6, JWT (Access Tokens + Refresh Tokens), BCrypt Password Hashing, Role-Based Access Control (`ROLE_STUDENT`, `ROLE_RECRUITER`, `ROLE_ADMIN`)
- **Database**: MySQL 8.0 with JPA Auditing, Compound Indexing, and JPA Specifications
- **Validation & Exception Handling**: Bean Validation (`@Valid`), Custom `@RestControllerAdvice` Global Exception Handler with unified `ApiResponse<T>` and `ErrorResponse` wrappers
- **File Storage**: StorageService abstraction (`LocalStorageServiceImpl` for development, isolated from MySQL)
- **Job Discovery**: Dynamic multi-criteria search with Spring Data JPA Specifications and N+1 query prevention

---

## Environment Variables Configuration

| Variable | Description | Default (Dev) |
| :--- | :--- | :--- |
| `DB_URL` | MySQL Connection JDBC URL | `jdbc:mysql://localhost:3306/careerforge_db?...` |
| `DB_USERNAME` | Database username | `root` |
| `DB_PASSWORD` | Database password | `root1234` |
| `JWT_SECRET` | HMAC-SHA512 (HS512) Secret key for signing JWTs | Base64 Encoded Secret Key |
| `JWT_EXPIRATION_MS` | Access Token Validity Duration (ms) | `86400000` (24 Hours) |
| `JWT_REFRESH_EXPIRATION_MS` | Refresh Token Validity Duration (ms) | `604800000` (7 Days) |
| `STORAGE_LOCAL_DIR` | Local resume file upload directory | `./uploads/resumes` |
| `STORAGE_MAX_FILE_SIZE` | Maximum upload file size in bytes (5MB) | `5242880` |
| `SERVER_PORT` | HTTP Server Port | `8080` |

---

## Development Setup & Running Locally

### Prerequisites
1. Java 17+ installed (`java -version`)
2. MySQL 8.0 database service running locally on port 3306

### Step 1: Create MySQL Database
Ensure MySQL is running. The application will automatically attempt to create `careerforge_db` if it does not exist (`createDatabaseIfNotExist=true`).

### Step 2: Build & Run Backend
```bash
# Clean & run full test suite (Phase 1 + Phase 2 + Phase 3)
./tools/apache-maven-3.9.9/bin/mvn.cmd clean test

# Run Spring Boot Application
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

## API Endpoints

### 1. Authentication & System (Phase 1)
- `GET /api/v1/health` — System Health Check
- `POST /api/v1/auth/register` — User Registration (`ROLE_STUDENT` / `ROLE_RECRUITER`)
- `POST /api/v1/auth/login` — User Authentication (returns Access Token + Refresh Token)
- `POST /api/v1/auth/refresh` — Access Token Renewal
- `POST /api/v1/auth/logout` — Revoke Refresh Token
- `GET /api/v1/auth/me` — Authenticated User Profile metadata

### 2. Student Module (Phase 2 — requires `ROLE_STUDENT`)
- `GET /api/v1/students/profile` — Get authenticated student's profile & completion %
- `POST /api/v1/students/profile` — Create student profile
- `PUT /api/v1/students/profile` — Update student profile
- `GET /api/v1/students/skills` — List student skills and proficiencies
- `POST /api/v1/students/skills` — Add skill to profile (duplicate protected)
- `PUT /api/v1/students/skills/{skillId}` — Update skill proficiency
- `DELETE /api/v1/students/skills/{skillId}` — Remove skill from profile
- `GET/POST /api/v1/students/education` — Education management
- `PUT/DELETE /api/v1/students/education/{id}` — Education item modification (ownership enforced)
- `GET/POST /api/v1/students/projects` — Projects management
- `PUT/DELETE /api/v1/students/projects/{id}` — Project item modification (ownership enforced)
- `GET/POST /api/v1/students/certifications` — Certifications management
- `PUT/DELETE /api/v1/students/certifications/{id}` — Certification item modification (ownership enforced)
- `POST /api/v1/students/resumes` — Upload PDF resume (MIME/ext/size validated, stored on filesystem)
- `GET /api/v1/students/resumes` — List resume metadata
- `GET /api/v1/students/resumes/{id}/download` — Download resume binary PDF
- `PUT /api/v1/students/resumes/{id}/active` — Set resume as active
- `DELETE /api/v1/students/resumes/{id}` — Delete resume from disk and database

### 3. Recruiter Profile & Company Module (Phase 3)
- `GET/POST/PUT /api/v1/recruiters/profile` — Recruiter profile management (`ROLE_RECRUITER`)
- `POST /api/v1/companies` — Register hiring company (`ROLE_RECRUITER`, creator becomes company admin)
- `GET /api/v1/companies/my-company` — View recruiter's affiliated company (`ROLE_RECRUITER`)
- `PUT /api/v1/companies/my-company` — Update company profile (`ROLE_RECRUITER`, admin checked)
- `GET /api/v1/companies/{id}` — View public company profile (`PERMIT_ALL`)
- `GET /api/v1/companies/slug/{slug}` — View public company profile by slug (`PERMIT_ALL`)
- `GET /api/v1/companies` — Browse/search verified companies directory with pagination (`PERMIT_ALL`)

### 4. Recruiter Job Management & State Machine (Phase 3 — requires `ROLE_RECRUITER`)
- `POST /api/v1/recruiters/jobs` — Create job posting draft with required/optional skills
- `GET /api/v1/recruiters/jobs` — List company jobs with status filtering & pagination
- `GET /api/v1/recruiters/jobs/{id}` — Get complete company job details (ownership enforced)
- `PUT /api/v1/recruiters/jobs/{id}` — Update company job & required skills (ownership enforced)
- `PATCH /api/v1/recruiters/jobs/{id}/publish` — Transition `DRAFT`/`CLOSED` $\rightarrow$ `PUBLISHED`
- `PATCH /api/v1/recruiters/jobs/{id}/unpublish` — Transition `PUBLISHED` $\rightarrow$ `DRAFT`
- `PATCH /api/v1/recruiters/jobs/{id}/close` — Transition `PUBLISHED` $\rightarrow$ `CLOSED`
- `PATCH /api/v1/recruiters/jobs/{id}/reopen` — Transition `CLOSED` $\rightarrow$ `PUBLISHED`
- `PATCH /api/v1/recruiters/jobs/{id}/archive` — Transition `DRAFT`/`CLOSED` $\rightarrow$ `ARCHIVED`
- `DELETE /api/v1/recruiters/jobs/{id}` — Delete job (`DRAFT` or `ARCHIVED` only)

### 5. Candidate & Public Job Discovery (Phase 3 — `PERMIT_ALL`)
- `GET /api/v1/jobs` — Dynamic multi-criteria job search (keyword, location, workMode, jobType, experienceLevel, salaryMin, salaryMax, skillIds, companyId, pagination, sorting)
- `GET /api/v1/jobs/{id}` — Get single published job details + required skills
- `GET /api/v1/jobs/slug/{slug}` — Get single published job details by slug
