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
- **File Storage**: StorageService abstraction (Local File System implementation for development)

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
# Clean & compile
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

## Core API Endpoints (Phase 1 Foundation)

- `GET /api/v1/health` — System Health Check
- `POST /api/v1/auth/register` — User Registration (`ROLE_STUDENT` / `ROLE_RECRUITER`)
- `POST /api/v1/auth/login` — User Authentication (returns Access Token + Refresh Token)
- `POST /api/v1/auth/refresh` — Access Token Renewal
- `POST /api/v1/auth/logout` — Revoke Refresh Token
- `GET /api/v1/auth/me` — Authenticated User Profile metadata
