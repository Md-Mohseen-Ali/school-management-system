# Algions School Management System — Requirements & Delivery Tracker

Last reviewed: 2026-11-16  
Assignment: Algions AI Labs Private Limited — Full-Stack Web Development Internship Project: School Management System  
Working branch: `feature/role-portals-profile-and-tracker`  
Production branch: `main` (feature work is **not merged**)

> Status meanings: **Verified** = confirmed by a completed build/test or inspected working code; **Implemented — pending verification** = code is on the feature branch but has not yet passed the current branch's test run; **Pending** = not delivered yet.

## 1. Requirement coverage

| Algions requirement | Current implementation | Status |
|---|---|---|
| Student, guardian and teacher administration | Student CRUD; guardian linking; teacher assignment; new teacher/guardian account provisioning | Implemented — pending verification |
| Classes, sections and subjects | Admin CRUD and relationships | Verified on previous main-branch E2E |
| Student enrollment | Create/list/end enrollment with date-aware enrollment validation | Verified on previous main-branch E2E |
| Teacher assignment by section and subject | Admin assignment management; teacher routes filter by active assignments | Verified on previous main-branch E2E |
| Daily attendance | Create session, load roster, mark attendance, save, submit, admin lock | Verified on previous main-branch E2E |
| Duplicate attendance prevention | Database uniqueness plus service-level conflict checks; duplicate student IDs rejected | Verified on previous main-branch E2E |
| Class and student attendance reports | Existing admin/teacher reporting; added student and guardian self-service reports; teacher class-report screen | Implemented — pending verification |
| Examinations and marks | Create exam, configure subjects, schedule, enter marks, complete and publish | Verified on previous main-branch E2E |
| Configurable grading and report cards | Grading rules and result calculation; admin, guardian and student report-card APIs | Verified on previous main-branch E2E |
| Fee invoices and payments | Draft, itemize, issue, simulated payments, history, cancellation rules, reconciliation | Verified on previous main-branch E2E |
| Student fee visibility | New read-only student invoice list/detail, own invoices only | Implemented — pending verification |
| Role-based access control | Server-side authorization on routes and service-level record ownership checks | Verified on previous main-branch E2E; new routes pending verification |
| Guardian privacy | Linked-student filtering; unlinked access returns not-found; added attendance privacy checks | Previous report-card/fee privacy verified; new attendance checks pending |
| Dashboard/reporting | Admin dashboard; new teacher, guardian and student metrics and quick links | Implemented — pending verification |
| Audit trail | Business audit entries for tracked changes, searchable admin view | Verified on previous main-branch E2E |
| User-friendly account profile | Replaced raw JSON profile with personal, academic, staff or family information and account status | Implemented — pending verification |
| Accessible, responsive UI | Existing responsive layouts; new profile, people-directory, filter and portal styles added | Implemented — visual/responsive verification pending |
| Search, filters and pagination | Existing academic tables and operational filters; new portal date filters | Partially verified; new filters pending |
| Predictable validation and errors | Bean validation, service checks, error responses, conflict handling | Verified for previous core workflows; new endpoints pending |

## 2. Role-by-role journey tracker

### Administrator
- [x] Manage students, classes, sections and subjects.
- [x] Manage enrollments, teacher assignments and guardian links.
- [ ] Create teacher accounts with a staff profile and initial password.
- [ ] Create guardian accounts with a guardian profile and initial password.
- [ ] Create students with linked student login credentials.
- [x] Manage attendance lifecycle, exams, grading, fee invoices and audit trail.
- [ ] Review expanded profile and People Directory in the browser.

### Teacher
- [x] See only assigned sections and exams.
- [x] Take attendance for assigned sections.
- [ ] See date-filtered class attendance summaries and student-level aggregates.
- [ ] Review teaching assignments in the profile.
- [ ] Browser-check all new teacher routes/screens.

### Guardian
- [x] View only linked students.
- [x] View linked students' published report cards.
- [x] View linked invoices and payment history.
- [ ] View attendance summary and history for linked children.
- [ ] Review linked children in the account profile.
- [ ] Browser-check family attendance and profile for multiple children.

### Student
- [x] View own published examination results.
- [ ] View own attendance rate, date-filtered history and status.
- [ ] View own issued invoices, fee totals, paid amount and outstanding balance.
- [ ] View own admission, class, section and enrollment profile.
- [ ] Create a new student record with a usable linked student login.
- [ ] Browser-check every student workspace and empty state.

## 3. Verification tracker

| Check | Previous baseline | Current feature branch |
|---|---|---|
| Backend build | Passed | Pending |
| Frontend production build | Passed | Pending |
| Attendance workflow E2E | 22/22 passed on prior baseline | Pending rerun |
| Examination/report-card E2E | 54/54 passed on prior baseline | Pending rerun |
| Fee/payment E2E | 37/37 passed on prior baseline | Pending rerun |
| Student/guardian/teacher self-service API tests | Not previously present | Added; pending run |
| Staff/student account provisioning tests | Not previously present | Added; pending run |
| Browser role-workspace checks | Admin, Teacher, Guardian, Student core screens passed on prior baseline | Expanded; pending run |
| Production read-only smoke tests | 40/40 passed on prior baseline | No production changes made by this branch |
| Cross-role negative tests | Core role and guardian privacy cases passed on prior baseline | Expanded cases pending |

Previous evidence (before this feature branch):
- Full-stack E2E: https://github.com/Md-Mohseen-Ali/school-management-system/actions/runs/43754742332
- Production smoke: https://github.com/Md-Mohseen-Ali/school-management-system/actions/runs/43754742358

Those runs validate the previous main/test baseline, **not** the current feature branch.

## 4. Algions submission deliverables

| Deliverable | State |
|---|---|
| Public source repository | Available |
| README with stack/setup/environment and demo instructions | Present; re-review after feature changes |
| Environment example and secret exclusion | Present; secrets remain out of source |
| Database migrations and synthetic seed data | Present (Flyway V1–V5) |
| ERD/schema document | Needs final verification / inclusion in submission package |
| API specification (OpenAPI or equivalent) | Needs a single consolidated, current specification |
| Automated tests and evidence | Core API/browser E2E baseline available; feature-branch tests pending |
| Working demo URLs / local-run steps | Vercel and Render URLs available; production smoke baseline passed |
| 3–5 minute walkthrough video | Pending |
| Short technical report | Pending |
| Final known-limitations and AI-use/originality declaration | Pending final review |
| Final submission package/form | Pending final review |

## 5. Known constraints to keep visible

- Payment handling is simulated only. Do not integrate real payment processing for this assessment.
- Student invoices are read-only; guardian/admin handles recording simulated payment.
- New staff/student accounts require the administrator to communicate the initial password securely; the application must never show or log it after account creation.
- New student account creation and the account-profile response must be verified against the isolated database before merging.
- New guardian attendance queries must continue to verify the student–guardian relationship in the backend.
- No production database writes are part of the E2E workflow. New test records are created only in a disposable PostgreSQL instance.

## 6. Change control

- [x] Feature work isolated from `main`.
- [ ] Backend compilation and frontend build on feature branch.
- [ ] All API workflow suites pass on feature branch.
- [ ] All four role browser tests pass on feature branch.
- [ ] Review test logs and inspect any failed assertion.
- [ ] Update this tracker with exact run links and counts.
- [ ] Open a pull request for review.
- [ ] Merge/deploy only after the feature-branch checks pass.
