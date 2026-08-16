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
- **Database**: MySQL 8.0 with JPA Auditing & Indexing
- **Validation & Exception Handling**: Bean Validation (`@Valid`), Custom `@RestControllerAdvice` Global Exception Handler with unified `ApiResponse<T>` and `ErrorResponse` wrappers
- **File Storage**: StorageService abstraction (`LocalStorageServiceImpl` for development, isolated from MySQL)

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
# Clean & run test suite (Phase 1 + Phase 2)
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

#### Profile
- `GET /api/v1/students/profile` — Get authenticated student's profile & completion percentage
- `POST /api/v1/students/profile` — Create student profile
- `PUT /api/v1/students/profile` — Update student profile

#### Skills
- `GET /api/v1/students/skills` — List student's skills and proficiency levels
- `POST /api/v1/students/skills` — Add a skill to student profile (duplicate protected)
- `PUT /api/v1/students/skills/{skillId}` — Update skill proficiency (`BEGINNER`, `INTERMEDIATE`, `ADVANCED`, `EXPERT`)
- `DELETE /api/v1/students/skills/{skillId}` — Remove skill from profile

#### Education
- `GET /api/v1/students/education` — List education history
- `POST /api/v1/students/education` — Add education record
- `PUT /api/v1/students/education/{id}` — Update education record (ownership enforced)
- `DELETE /api/v1/students/education/{id}` — Delete education record (ownership enforced)

#### Projects
- `GET /api/v1/students/projects` — List student projects
- `POST /api/v1/students/projects` — Add project record
- `PUT /api/v1/students/projects/{id}` — Update project record (ownership enforced)
- `DELETE /api/v1/students/projects/{id}` — Delete project record (ownership enforced)

#### Certifications
- `GET /api/v1/students/certifications` — List certifications
- `POST /api/v1/students/certifications` — Add certification record
- `PUT /api/v1/students/certifications/{id}` — Update certification record (ownership enforced)
- `DELETE /api/v1/students/certifications/{id}` — Delete certification record (ownership enforced)

#### Resumes
- `POST /api/v1/students/resumes` — Upload PDF resume (`multipart/form-data`, max 5MB)
- `GET /api/v1/students/resumes` — List uploaded resumes metadata
- `GET /api/v1/students/resumes/{id}/download` — Download resume binary PDF
- `PUT /api/v1/students/resumes/{id}/active` — Set resume as active
- `DELETE /api/v1/students/resumes/{id}` — Delete resume from disk and database
