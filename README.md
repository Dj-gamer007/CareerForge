# CareerForge

CareerForge is a full-stack career and recruitment management platform connecting students/job seekers, recruiters, and administrators in one application.

## 🚀 Deployment

- **Frontend:** Vercel
- **Backend:** Railway
- **Database:** MySQL
- **Backend:** Spring Boot 3.2.5 / Java 17

Production credentials and private environment files are not stored in this repository.

## ✨ Features

### Student / Job Seeker
- Registration and secure login
- JWT authentication
- Browse and search jobs
- View job details
- Upload and manage resumes
- Apply for jobs
- Track application status and history
- Receive notifications
- Manage profile

### Recruiter
- Secure recruiter authentication
- Company management
- Create and manage job postings
- Manage job lifecycle
- View applicants
- Review applications and resumes
- Update application statuses
- Recruitment workflow tracking
- Notifications

### Administrator
- Role-based administrative access
- User management
- Company and verification management
- Platform activity management
- Audit information

### Security
- Spring Security
- JWT access and refresh tokens
- BCrypt password hashing
- Role-based authorization
- Stateless authentication
- Production CORS restrictions
- Environment-based secrets and database credentials
- Production-safe error responses
- File upload limits

## 🏗️ Architecture

```text
Users
  │
  ▼
Vercel (React + Vite)
  │ HTTPS / REST API
  ▼
Railway (Spring Boot + Java 17)
  │ JDBC
  ▼
Production MySQL
```

## 🧰 Technology Stack

### Frontend
- React
- TypeScript
- Vite
- React Router
- Zustand
- Axios
- React Hook Form
- Zod
- Tailwind CSS

### Backend
- Java 17
- Spring Boot 3.2.5
- Spring Web
- Spring Data JPA
- Spring Security
- Bean Validation
- JJWT
- Lombok
- Maven

### Database & Deployment
- MySQL
- H2 for tests
- Vercel
- Railway
- Docker configuration

## 📁 Project Structure

```text
CareerForge/
├── careerforge-frontend/          # React + TypeScript frontend
├── src/                            # Spring Boot backend
│   ├── main/java/com/careerforge/
│   ├── main/resources/
│   └── test/
├── init-db/                        # Database initialization
├── scripts/                        # Operational scripts
├── nginx/                          # Nginx resources
├── docs/                           # Documentation
├── docker-compose.prod.yml
├── Dockerfile.backend
├── pom.xml
├── .gitignore
└── README.md
```

## 🔑 Environment Variables

Production values must be configured through the hosting platform and must not be committed to Git.

### Backend

| Variable | Purpose |
|---|---|
| `DB_URL` | MySQL JDBC connection URL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | JWT signing secret |
| `JWT_EXPIRATION_MS` | Access-token lifetime |
| `JWT_REFRESH_EXPIRATION_MS` | Refresh-token lifetime |
| `SERVER_PORT` | Server port |
| `STORAGE_LOCAL_DIR` | Resume storage directory |
| `STORAGE_MAX_FILE_SIZE` | Maximum file size |

### Frontend

```env
VITE_API_BASE_URL=https://<your-production-backend-domain>
```

Do not commit real `.env` files.

## 💻 Local Development

### Prerequisites

- Java 17
- Maven
- Node.js and npm
- MySQL

### Backend

Clone the repository:

```bash
git clone https://github.com/dhanush-j-dev/CareerForge.git
cd CareerForge
```

Configure the required local environment variables:

```env
DB_URL=jdbc:mysql://localhost:3306/careerforge_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=<your-local-password>
JWT_SECRET=<your-local-development-secret>
```

Start the backend:

```bash
mvn spring-boot:run
```

Default local backend:

```text
http://localhost:8080
```

### Frontend

```bash
cd careerforge-frontend
npm install
npm run dev
```

## 🧪 Testing

Build the backend without running tests:

```bash
mvn clean package -DskipTests
```

Run backend tests:

```bash
mvn test
```

Build the frontend:

```bash
cd careerforge-frontend
npm install
npm run build
```

## 🌐 Production Deployment

The current deployment flow is:

```text
GitHub
 ├── Frontend → Vercel
 │
 └── Backend → Railway
                 │
                 ▼
              MySQL
```

### Backend

The Spring Boot backend runs on Railway using the `prod` Spring profile.

Production configuration reads database credentials, JWT secrets, and other settings from environment variables.

### Frontend

The React/Vite frontend is deployed on Vercel and configured with the production backend API URL.

### CORS

Spring Security CORS is configured to restrict browser requests to the intended production frontend origin rather than allowing arbitrary origins.

## 📄 Resume Uploads

CareerForge supports resume uploads with a maximum configured file size of 5 MB.

Storage is configurable through:

```text
STORAGE_LOCAL_DIR
```

Production storage should use persistent storage appropriate for the hosting environment.

## 🔒 Security & Git Hygiene

The repository ignores sensitive and generated files including:

```text
.env
.env.prod
*.sql
*.class
target/
node_modules/
dist/
uploads/
logs/
```

Example environment files may be committed when they contain placeholders only.

Never commit:
- Production passwords
- JWT secrets
- API keys
- Private keys or certificates
- Production `.env` files
- Database backups
- Build artifacts

## 🗄️ Database

CareerForge uses MySQL for production application data and H2 for automated tests.

Database initialization resources are located under:

```text
init-db/
```

Database backups are treated as private operational files and are excluded from Git.

## 🔄 Application Flow

### Authentication

```text
Register / Login
       │
       ▼
Spring Security
       │
       ▼
JWT Access + Refresh Tokens
       │
       ▼
Authenticated API Requests
```

### Student

```text
Browse Jobs
    ↓
View Job
    ↓
Upload Resume
    ↓
Apply
    ↓
Recruiter Review
    ↓
Application Status
    ↓
Notification
```

### Recruiter

```text
Company
   ↓
Create Job
   ↓
Receive Applications
   ↓
Review Candidates
   ↓
Update Application Status
   ↓
Candidate Notification
```

## 🔌 API

Backend REST APIs are organized under:

```text
/api/v1
```

Major areas include:

```text
/api/v1/auth
/api/v1/jobs
/api/v1/companies
```

Additional application, profile, resume, notification, recruitment, and administration endpoints are implemented under the backend controllers.

For the exact current endpoint implementation, see:

```text
src/main/java/com/careerforge/controller/
```

## ✅ Production Validation

The deployed application has been end-to-end tested.

Validated areas include:

- Frontend-to-backend communication
- Login and authentication
- JWT flow
- Resume upload
- Production database connectivity
- Resume storage persistence
- Production Spring profile
- CORS behavior
- Student workflows
- Recruiter workflows
- Application workflows
- Git repository hygiene
- Environment/secrets separation

## 🛠️ Useful Git Commands

```bash
git status
git log --oneline -10
git ls-files
git status --ignored --short
```

Check for accidentally committed values:

```bash
git grep -n -i "root1234"
git grep -n -i "secret:"
git grep -n -i "api_key"
git grep -n -i "apikey"
```

## 👨‍💻 Author

**Dhanush J**

Java Developer | Spring Boot | REST APIs

GitHub: https://github.com/dhanush-j-dev

## 📌 Project Status

**CareerForge is deployed and production-tested.**

The project demonstrates:

- Full-stack React development
- Spring Boot REST API development
- MySQL integration
- JWT authentication
- Spring Security
- Role-based access control
- Resume management
- Recruitment workflows
- Application lifecycle management
- Notifications
- Audit logging
- Production CORS
- Environment-based configuration
- Cloud deployment
- End-to-end production validation

## 📜 License

Add a project license here if one is selected for the repository.
