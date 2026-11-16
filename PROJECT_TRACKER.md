# Algions School Management System — Requirements & Delivery Tracker

Last reviewed: 2026-11-16  
Assignment: Algions AI Labs Private Limited — Full-Stack Web Development Internship Project: School Management System  
Working branch: feature/role-portals-profile-and-tracker  
Production branch: main (feature is not merged/deployed)

Status meanings: **Verified** = passed the current feature-branch build or automated test; **Implemented** = code is present but not fully validated; **Pending** = not delivered or checked yet.

## 1. Algions requirement coverage

| Requirement | Implementation | Status |
|---|---|---|
| Admin manages students, guardians, teachers, classes, sections, subjects, assignments and enrollment | CRUD, relationships, new Teacher/Guardian account provisioning and student account created alongside student admission | Verified by branch build and account-provisioning E2E |
| Daily attendance, duplicate prevention, student/class summaries | Teacher roster scoped to assignment and enrollment date; session DRAFT → SUBMITTED → LOCKED; student, guardian and teacher reports | Verified: attendance and self-service E2E |
| Exams, marks, grading and report cards | Exam setup/scheduling; teacher-assigned marks; grade bands; complete/publish transitions; admin/student/guardian report cards | Verified: examination E2E |
| Fees, invoices, payments and balances | Draft/itemize/issue/cancel; simulated-only payment; idempotency; admin/guardian payment history; student read-only invoice/payment history | Verified: fee and student payment-history E2E |
| RBAC and data privacy | Role-level route security, teacher assignment checks, teacher individual-report enrollment checks, student self-ownership and guardian/student relationship checks | Verified by negative, cross-role and unrelated-student report E2E tests |
| Reporting and audit | Admin audit workspace, class attendance reports, student and guardian summaries; structured audit events across academic, attendance, examination, grading, identity and financial mutations | Verified by audit-event assertions in attendance, exam, fee and self-service E2E suites |
| User-friendly role workspaces | Separate Admin, Teacher, Guardian and Student navigation, role-specific dashboard metrics and quick links | Verified by Chromium role-workspace test |
| Profile screen | No raw JSON display; real authenticated account identity, contact, role and role-specific school/family details | Verified by Chromium role-workspace test |
| Search/filter/pagination | Existing academic/operational search and paging; student/guardian attendance date filters; invoice search | Core paths exercised; exhaustive filter-combination tests still pending |
| Accessible/responsive UI | Semantic forms, accessible labels and responsive styles added for profiles, people directory and portals | Implemented; dedicated viewport/screen-reader audit pending |
| Database integrity and migrations | PostgreSQL schema managed by Flyway; uniqueness, FK, indexes and service-level business constraints | Verified by isolated PostgreSQL startup/migration and workflow E2E |
| Documentation | README, ERD and consolidated API reference | Added; final editorial review pending |

## 2. Role journey tracker

### Administrator
- [x] Student, class, section and subject management.
- [x] Enrollment, teacher assignment and guardian linking.
- [x] Create teacher account with staff profile and initial password.
- [x] Create guardian account with guardian profile and initial password.
- [x] Register a student with a linked student login.
- [x] Attendance, examination, grading, invoices, simulated payments and audit trail.
- [x] People directory and expanded administrator profile.

### Teacher
- [x] See only assigned sections and exams.
- [x] Take attendance for the assigned roster.
- [x] Review assigned-section attendance summaries and student aggregates.
- [x] Review employee code and teaching assignments in profile.
- [x] Teacher workspace browser checks.

### Guardian
- [x] See only linked students.
- [x] View published report cards with exam choices filtered by selected child.
- [x] View linked student's attendance summary and submitted/locked register history.
- [x] View linked invoices and simulated payment history.
- [x] Review linked children in profile.
- [x] Guardian workspace browser checks.

### Student
- [x] View own published results and report cards.
- [x] View own attendance rate and date-filtered history.
- [x] View own issued invoice totals, paid amount, balance and invoice items.
- [x] View own simulated payment history (read-only).
- [x] View admission identity, class, section and enrollment in profile.
- [x] New student admission creates a login that can authenticate and load the self-service profile.
- [x] Student workspace browser checks.

## 2.1 Correctness and audit hardening (2026-11-16)

- [x] Teacher individual attendance reports require both an active assignment to the requested section and an enrollment overlap for the student/date range. Unrelated or unknown students return the same 404 response.
- [x] Administrator enrollment listings accept an `attendanceDate` filter that returns the roster valid on that date. Admin attendance registers and exam mark-entry rosters use the attendance/subject date rather than today's active enrollment.
- [x] Student and guardian published-exam lists use enrollment on each configured subject date. Historical results remain discoverable after a transfer or ended enrollment.
- [x] Enrollment start/end defaults use the school's `Asia/Kolkata` timezone.
- [x] Flyway V6 changes the database default for new fee invoices to `DRAFT` without editing previously applied migrations.
- [x] Audit events cover student create/update/deactivation, class/section/subject changes, enrollments, guardian links, teacher assignments, attendance lifecycle, examination setup/marks/publication, grading scales, identity provisioning and fee operations.
- [x] Teacher examination discovery no longer scans every examination and executes per-examination/per-subject assignment checks; the repository query scopes directly to active assigned subjects.
- [x] Regression E2E tests cover unrelated-student report access, historical attendance roster, historical student/guardian exam discovery and audit event persistence.

## 3. Current feature-branch verification

**Verified candidate**

Tested code commit: 2daa58c3c8205edf8cd37d3125dcec66c0eebded  
Full workflow: https://github.com/Md-Mohseen-Ali/school-management-system/actions/runs/43759111375

### Automated API workflow assertions

| Suite | Result |
|---|---:|
| Attendance lifecycle, date-aware historical roster and attendance audit | 33 / 33 |
| Examination, report cards, historical visibility and examination audit | 57 / 57 |
| Fees and simulated payments | 37 / 37 |
| Student/guardian/teacher self-service, privacy, historical exams and academic audit | 55 / 55 |
| Teacher/guardian/student account provisioning | 16 / 16 |
| Student payment history | 5 / 5 |
| **Total API assertions** | **203 / 203** |

### Browser checks

34 browser assertions passed across the four roles. They covered role sign-in, visible navigation, workspace loading, selected report-card/payment-history interactions and profile views. The test also confirmed no uncaught browser JavaScript exceptions.

- Admin: student/class/section/subject/enrollment/assignment/family-link/attendance/exam/fee/people/audit/profile workspaces.
- Teacher: attendance, examinations, assigned-class reports and profile.
- Guardian: linked-student report card, invoice payment history, student attendance and family profile.
- Student: published results, own attendance, own fees/payment history workspace and student profile.

### Build and environment

- Backend Maven package/compile: passed.
- Flyway migrations V1–V6 against disposable PostgreSQL 17: passed.
- Frontend Vite production build: passed.
- Chromium installation and browser run: passed.
- Test data is created only in the disposable GitHub Actions database; the database is destroyed after the run.
- Previous production read-only smoke test was 40/40 on the earlier deployment baseline. This feature branch has not yet been merged or production-smoke-tested.

The full-stack feature workflow is configured at .github/workflows/isolated-e2e.yml. The production smoke workflow is read-only and is not a substitute for the isolated write-workflow tests.

## 4. Algions submission deliverables

| Deliverable | Status |
|---|---|
| Public source repository | Available |
| README with stack, setup, environment and demo instructions | Updated |
| Environment example and secret exclusion | Present |
| Database migrations and synthetic seed data | Present (Flyway V1–V5) |
| ERD/schema document | Added: docs/ERD.md |
| Consolidated API reference | Added: docs/API_REFERENCE.md |
| Requirements/test/delivery tracker | This file |
| Automated workflow and test evidence | Current full-stack run above |
| Working demo URLs/local-run steps | Existing Vercel/Render deployments; production verification after merge still pending |
| 3–5 minute walkthrough video | Pending |
| Short technical report | Pending |
| Final known-limitations and AI-use/originality declaration | Pending final review |
| Final submission package/form | Pending |

## 5. Known limits and operational notes

- Payment handling is simulated only. No real payment gateway or transfer is integrated.
- Student invoices and payment history are read-only; guardian or administrator handles recording simulated payments.
- Admin provisions teacher/guardian accounts and communicates the initial password securely. Passwords are BCrypt hashed and are never returned after creation.
- Student admission requires login credentials. The administrator must communicate the initial password securely.
- No email-delivery, self-service password reset or profile-edit workflow is included. Account corrections are handled by the school administrator.
- Teacher accounts still need active section/subject assignments; guardians need student links before family data appears.
- No production data is modified by the isolated E2E workflow.
- Responsive CSS has been added; dedicated mobile/tablet viewport, screen-reader and keyboard-only audits are still recommended.

## 6. Change control

- [x] Feature work isolated from main.
- [x] Backend build and isolated PostgreSQL startup.
- [x] Frontend production build.
- [x] All six API E2E suites pass on the hardened revision (203/203 assertions).
- [x] Browser tests pass for all four roles (34/34 assertions).
- [x] Test logs reviewed and test-selector failures corrected.
- [x] ERD and API reference added.
- [x] Tracker updated with exact test evidence.
- [ ] Open/complete pull request review.
- [ ] Merge/deploy only after review.
- [ ] Re-run read-only production smoke tests after deployment and update live demo status.
- [ ] Record the 3–5 minute walkthrough and finish the technical report/submission form.
