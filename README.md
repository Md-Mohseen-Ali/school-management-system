# School Management System

**ALGIONS AI LABS PRIVATE LIMITED — Full-Stack Web Development Internship Project**

A full-stack school administration application built with Java 17, Spring Boot, PostgreSQL and React. It includes academic administration, attendance, examinations, fee invoicing, role-based access control and audit logging.

## Technology stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 4.1.2 |
| API | Spring MVC REST APIs |
| Security | Spring Security, JWT, BCrypt |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL 17 |
| Migrations | Flyway |
| Frontend | React, Vite |
| Local database | Docker Compose |
| Backend build | Maven Wrapper |
| Frontend packages | npm |

## Implemented functionality

- **Academic administration:** students, classes, sections, subjects, teacher and guardian directories, teacher assignments, student enrollments, and student-guardian relationships.
- **Attendance:** sessions, student attendance records, enrollment-date validation, draft/submitted/locked workflow, duplicate prevention within a session, and student/section attendance reports.
- **Examinations:** exam scheduling, exam subjects, configurable grading scales, marks validation, completion/publication workflow, and report cards.
- **Fees:** invoices, invoice line items, payment records, idempotency handling, payment history, and outstanding-balance calculations. Review the configured payment methods against the assessment's simulated-payment-only rule before final submission.
- **Security:** JWT authentication, BCrypt password hashing, server-side role checks, teacher assignment checks, and guardian-to-student privacy checks.
- **Audit and errors:** audit events for selected fee operations and standardized JSON error responses, including tested 400, 401 and 403 cases.

## Architecture

The backend is a modular monolith with layered Controller–Service–Repository separation. The frontend communicates with the REST API using HTTP/JSON. Flyway manages database schema changes.

```text
React + Vite frontend
        | HTTP / JSON
        v
Spring Boot REST API
  |-- Security / JWT
  |-- Identity
  |-- Academic
  |-- Attendance
  |-- Examination
  |-- Fees
  |-- Reporting
  |-- Audit / Common
        |
        v
Spring Data JPA / Hibernate
        |
        v
PostgreSQL 17 (Flyway migrations)
```

## Repository layout

```text
school-management-system/
├── backend/
│   ├── src/main/java/com/schoolmanagement/
│   ├── src/main/resources/application.yaml
│   ├── src/main/resources/db/migration/
│   ├── src/test/
│   ├── pom.xml
│   └── mvnw.cmd
├── frontend/
│   ├── src/
│   ├── package.json
│   └── vite.config.js
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

## Prerequisites

- JDK 17
- Docker Desktop
- Node.js and npm
- GitHub Desktop (or Git)

## Local setup

### 1. Configure environment

At the repository root, copy the example file and edit the new `.env` with local values:

```powershell
Copy-Item .env.example .env
```

Use a strong, randomly generated Base64 JWT secret. Generate one in PowerShell:

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

Set `POSTGRES_PASSWORD` and `DB_PASSWORD` to the same local password. Do not commit `.env`.

### 2. Start PostgreSQL

From the repository root:

```powershell
docker compose up -d
docker compose ps
```

The default host port is `5433`. The Compose service uses the persistent Docker volume `school_management_data`. Do not use `docker compose down -v` unless you intentionally want to delete local database data.

### 3. Start backend

Open a PowerShell terminal:

```powershell
Set-Location .\backend
.\mvnw.cmd spring-boot:run
```

The API runs at `http://localhost:8080`. Flyway applies migrations at startup. Health check:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

### 4. Start frontend

Open a second PowerShell terminal:

```powershell
Set-Location .\frontend
npm install
npm run dev
```

Open `http://localhost:5173`. Vite proxies `/api` and `/actuator` to `http://localhost:8080` during local development.

### 5. Production frontend build

From `frontend`:

```powershell
npm run build
```

The output is written to `frontend/dist/` and is intentionally excluded from Git.

## Demo accounts

The local Flyway demo seed includes these fabricated accounts:

| Role | Email | Password |
|---|---|---|
| Administrator | `admin@school.demo` | `Admin@2026School` |
| Teacher | `teacher@school.demo` | `Teacher@2026School` |
| Guardian 1 | `guardian1@school.demo` | `Guardian@2026School` |
| Guardian 2 | `guardian2@school.demo` | `Guardian2@2026School` |
| Student 1 | `student1@school.demo` | `Student@2026School` |
| Student 2 | `student2@school.demo` | `Student2@2026School` |

These accounts are for local assessment demonstrations only. Do not reuse the passwords for real accounts.

## Authentication and representative API routes

Login:

```http
POST /api/v1/auth/login
Content-Type: application/json
```

Send the demo email and password in the JSON body. Use the returned access token on protected requests:

```http
Authorization: Bearer <access-token>
```

Representative routes:

| Feature | Route |
|---|---|
| Current user | `GET /api/v1/auth/me` |
| Students | `/api/v1/academic/students` |
| Classes | `/api/v1/academic/classes` |
| Sections | `/api/v1/academic/sections` |
| Subjects | `/api/v1/academic/subjects` |
| Teachers | `GET /api/v1/identity/teachers` |
| Guardians | `GET /api/v1/identity/guardians` |
| Teacher assignments | `/api/v1/academic/teacher-assignments` |
| Enrollments | `/api/v1/academic/enrollments` |
| Student-guardian links | `/api/v1/academic/student-guardians` |
| Attendance sessions | `/api/v1/attendance/sessions` |
| Examinations | `/api/v1/examinations` |
| Audit logs | `GET /api/v1/audit-logs` |

The backend also exposes attendance reports, examination marks/report cards, fee invoices and payment-history operations. See the controllers in `backend/src/main/java/com/schoolmanagement/modules/` for request details.

## Verification commands

Backend compile:

```powershell
Set-Location .\backend
.\mvnw.cmd clean compile
```

Backend tests:

```powershell
.\mvnw.cmd test
```

Frontend production build:

```powershell
Set-Location ..\frontend
npm run build
```

Manual verification performed during development includes administrator login, live admin data views, teacher access restrictions, unauthenticated 401 responses, forbidden 403 responses, attendance session workflow, examination marks/report card calculation, guardian linked-student privacy, fee balance/idempotency checks, and audit-log retrieval.

**Testing limitation:** The current automated test suite is not yet a comprehensive integration suite for all workflows. Do not interpret manual checks as automated test coverage. Run `mvnw.cmd test` on the submitted revision and disclose the actual result.

## Security and data handling

- Local credentials and JWT signing secrets belong in `.env`, never in source control.
- `.gitignore` excludes `.env` files, `node_modules`, frontend build output and Maven `target` directories.
- Demo records are fabricated. Do not commit actual student, guardian or staff personal information.
- The demo passwords above are public assessment credentials and must not be used outside the local demo.

## Known limitations / review before submission

- Automated integration tests and negative/conflict tests need further coverage.
- Admin frontend read views have been checked; verify frontend create/update operations and each role-specific UI before claiming them as fully tested.
- Confirm payment behavior strictly follows the assignment's simulated-payment-only requirement. The current backend has a cash-payment path that should be reviewed or removed if that requirement is strict.
- API reference/OpenAPI documentation, ERD and deployment are separate deliverables and should only be marked complete once produced and checked.

## Demonstration outline

1. Start PostgreSQL, backend and frontend.
2. Sign in as administrator and show the dashboard and academic lists.
3. Demonstrate attendance and reports.
4. Demonstrate exam setup, marks and report card.
5. Demonstrate fee invoice/payment history.
6. Sign in as teacher and show role restriction/assigned-section behavior.
7. Sign in as guardian and show linked-student privacy.
8. Show a real unauthorized/forbidden response.

Only record workflows that succeed on the final submitted revision.

## Assessment context

Prepared for the ALGIONS AI LABS PRIVATE LIMITED Full-Stack Web Development Internship Project submission. External dependencies retain their own licenses. AI assistance used during development must be disclosed honestly in the submission form.
