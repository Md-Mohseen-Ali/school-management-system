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

- **Academic administration:** student registration with a linked student login, classes, sections, subjects, enrollment, teacher assignment, guardian relationship management, and administrator-managed teacher/guardian account provisioning.
- **Account access:** public role-specific requests for teachers, guardians and students, with a pending-review queue and administrator-only approval/rejection. A login is created only after approval.
- **Attendance:** session creation, enrollment-date validation, duplicate prevention, attendance marking, draft/submitted/locked transitions, class reports, and student/guardian attendance history and summaries.
- **Examinations:** exam scheduling, subject configuration, configurable grading scales, marks validation, completion/publication workflow, and report cards for administrators, students and linked guardians.
- **Fees:** itemized invoices, simulated-only payment records, idempotency handling, payment history, guardian payment recording, student read-only invoice/fee summaries, and outstanding-balance reconciliation.
- **Role-specific workspaces:** dashboards and dedicated screens for administrators, teachers, guardians and students. The student portal includes current class/section, active subjects and assigned teacher contacts, attendance history, fee statements/payment history and published results. Profiles show account identity plus role-specific student enrollment, teacher assignments, linked children or admin management scope.
- **Security:** JWT authentication, BCrypt password hashing, server-side role checks, teacher assignment checks, student self-service ownership checks, and guardian-to-student relationship checks.
- **Audit and errors:** structured audit events for student, academic structure, enrollment, family-link, teacher-assignment, attendance, examination, grading, identity and fee mutations; standardized JSON errors for validation, authorization and business conflicts.

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

## Technical reference documents

- [Entity Relationship Diagram (ERD)](docs/ERD.md) — tables, relationships and integrity constraints from Flyway migrations.
- [REST API Reference](docs/API_REFERENCE.md) — role access, routes, request fields and HTTP error conventions.
- [Requirements and Delivery Tracker](PROJECT_TRACKER.md) — Algions acceptance coverage, role journeys, test evidence and remaining submission deliverables.

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
| Public account request | `POST /api/v1/account-requests` |
| Admin request queue / approve / reject | `GET /api/v1/account-requests`, `POST /api/v1/account-requests/{requestId}/approve`, `POST /api/v1/account-requests/{requestId}/reject` (administrator only) |
| Students | `/api/v1/academic/students` |
| Classes | `/api/v1/academic/classes` |
| Sections | `/api/v1/academic/sections` |
| Subjects | `/api/v1/academic/subjects` |
| Teachers | `GET /api/v1/identity/teachers` |
| Guardians | `GET /api/v1/identity/guardians` |
| Provision teacher/guardian account | `POST /api/v1/identity/accounts` (administrator only) |
| Teacher assignments | `/api/v1/academic/teacher-assignments` |
| Enrollments | `/api/v1/academic/enrollments` |
| Student-guardian links | `/api/v1/academic/student-guardians` |
| Attendance sessions | `/api/v1/attendance/sessions` |
| Examinations | `/api/v1/examinations` |
| Audit logs | `GET /api/v1/audit-logs` |
| Teacher assigned sections and roster | `/api/v1/teacher/roster` |
| Teacher assigned examinations | `/api/v1/teacher/examinations` |
| Guardian linked students | `GET /api/v1/guardian/students` |
| Guardian child attendance | `GET /api/v1/guardian/students/{studentId}/attendance/summary`, `GET /api/v1/guardian/students/{studentId}/attendance/history` |
| Guardian published examinations | `GET /api/v1/guardian/examinations`, `GET /api/v1/guardian/examinations/students/{studentId}` |
| Student profile | `GET /api/v1/student/profile` |
| Student current academics, active subjects and teachers | `GET /api/v1/student/academics` |
| Student attendance summary/history | `GET /api/v1/student/attendance/summary`, `GET /api/v1/student/attendance/history` |
| Student issued invoices | `GET /api/v1/student/fees/invoices`, `GET /api/v1/student/fees/invoices/{invoiceId}` |
| Student published examinations | `GET /api/v1/student/examinations` |

The backend also exposes attendance reports, examination marks/report cards, fee invoices and payment-history operations. See the controllers in `backend/src/main/java/com/schoolmanagement/modules/` for request details.

## Verification commands

Backend compile and tests:

```powershell
Set-Location .\backend
.\mvnw.cmd clean verify
```

Frontend production build:

```powershell
Set-Location ..\frontend
npm ci
npm run build
```

The GitHub Actions workflow `.github/workflows/isolated-e2e.yml` starts a disposable PostgreSQL 17 database, applies Flyway migrations V1–V7 and seed data, starts the backend and frontend, then runs attendance, examination, fee, self-service, account-provisioning, public account-request approval/rejection, student-payment-history and Chromium role-workspace checks. The latest verified feature revision passed 249 API assertions across seven suites and 37 browser checks across the public request flow and all four roles. The database is destroyed with the runner. Exact run evidence is recorded in [PROJECT_TRACKER.md](PROJECT_TRACKER.md).

Do not treat an earlier test run as proof for a later commit. Use the workflow run associated with the exact commit being submitted. The separate production smoke suite is read-only and checks the deployed Vercel/Render endpoints; it is not a substitute for isolated workflow tests.

## Security and data handling

- Local credentials and JWT signing secrets belong in `.env`, never in source control.
- `.gitignore` excludes `.env` files, `node_modules`, frontend build output and Maven `target` directories.
- Demo records are fabricated. Do not commit actual student, guardian or staff personal information.
- The demo passwords above are public assessment credentials and must not be used outside the local demo.

## Known limitations / review before submission

- The isolated E2E suite covers core business lifecycles and the documented privacy regressions; additional boundary, high-volume, mobile viewport and assistive-technology tests remain worthwhile.
- The feature branch has not been merged or production-deployed. Re-run the read-only production smoke test after deployment with each demo role.
- Student self-service requires a linked AppUser account. New student registration now creates that login as part of the registration transaction; the administrator must communicate the initial password securely.
- Public requests are untrusted until verified by an administrator. There is no automated email/OTP validation, CAPTCHA or application-level rate limiter; production exposure should be protected by an edge/WAF rate limit and the school must verify identity off-band.
- Teacher approval creates the account/profile, but a teacher still needs section/subject assignment. Guardian approval requires a valid existing admission number and creates the guardian-student link. Student approval creates a new admission/login; admin still needs to enroll the student.
- The public confirmation page displays a request reference but does not expose a public status lookup; applicants contact the school office for updates.
- Student fee screens are read-only; guardians or administrators record simulated payments. No real payment processor is connected.
- No self-service password reset/email delivery is included; temporary credentials are communicated by the school administrator.
- A consolidated OpenAPI specification, ERD, 3–5 minute walkthrough video and short technical report remain separate submission deliverables. Track their completion in [PROJECT_TRACKER.md](PROJECT_TRACKER.md).

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

## 16. Deployment architecture (Vercel + Render + Neon)

The intended assessment deployment is split across three providers:

- **Frontend:** React/Vite deployed to Vercel. Vercel project Root Directory is `frontend`, build command is `npm run build`, and output directory is `dist`.
- **Backend:** Spring Boot deployed to Render as a Docker Web Service using `backend/Dockerfile`.
- **Database:** PostgreSQL hosted on Neon. Use the direct, non-pooler connection URL for the backend's Flyway migration startup.

Set `VITE_API_BASE_URL` in Vercel to the Render backend origin (for example `https://your-api.onrender.com`, no trailing slash). This variable is embedded at frontend build time and is not a secret. Do not place database credentials or the JWT signing secret in Vercel.

Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` in Render's backend environment settings. Keep `sslmode=require` in the Neon JDBC URL. Set `CORS_ALLOWED_ORIGINS` to the exact Vercel production origin (for example `https://your-project.vercel.app`, no trailing slash), then redeploy the backend.

`frontend/vercel.json` provides an SPA fallback rewrite to `index.html` for client-side routes. Full dashboard, authentication and role workflows must be checked again against the deployed URLs before sharing the live deployment.

See `DEPLOYMENT_VERCEL_RENDER_NEON.md` for step-by-step configuration and verification instructions.
