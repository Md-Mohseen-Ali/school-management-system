# Algions School Management System — Requirements & Delivery Tracker

Last reviewed: 2026-11-19  
Assignment: Algions AI Labs Private Limited — Full-Stack Web Development Internship Project: School Management System  
Working branch: fix/security-enrollment-and-docs  
Production branch: main (role-portal and UI-refresh releases merged; this security-hardening branch is under review)

Status meanings: **Verified** = passed the current feature-branch build or automated test; **Implemented** = code is present but not fully validated; **Pending** = not delivered or checked yet.

## 1. Algions requirement coverage

| Requirement | Implementation | Status |
|---|---|---|
| Admin manages students, guardians, teachers, classes, sections, subjects, assignments and enrollment | CRUD, direct provisioning and public requests for all three portal roles, with admin-only approve/reject | Verified by current API/browser E2E |
| Daily attendance, duplicate prevention, student/class summaries | Teacher roster scoped to assignment and enrollment date; session DRAFT → SUBMITTED → LOCKED; student, guardian and teacher reports | Verified: attendance and self-service E2E |
| Exams, marks, grading and report cards | Exam setup/scheduling; teacher-assigned marks; grade bands; complete/publish transitions; admin/student/guardian report cards | Verified: examination E2E |
| Fees, invoices, payments and balances | Draft/itemize/issue/cancel; simulated-only payment; idempotency; admin/guardian payment history; student read-only invoice/payment history | Verified: fee and student payment-history E2E |
| RBAC and data privacy | Role-level route security, teacher assignment checks, teacher individual-report enrollment checks, student self-ownership and guardian/student relationship checks | Verified by negative, cross-role and unrelated-student report E2E tests |
| Reporting and audit | Admin audit workspace, class attendance reports, student and guardian summaries; structured audit events across academic, attendance, examination, grading, identity and financial mutations | Verified by audit-event assertions in attendance, exam, fee and self-service E2E suites |
| User-friendly role workspaces | Admin request queue and student current class/section, active subjects and assigned teacher workspace alongside attendance, fees and results | Verified by current Chromium workflow |
| Profile screen | Account identity/contact/status plus student enrollment, teacher assignments, guardian child/class context and admin management scope | Verified by API and Chromium E2E |
| Search/filter/pagination | Existing academic/operational search and paging; student/guardian attendance date filters; invoice search | Core paths exercised; exhaustive filter-combination tests still pending |
| Accessible/responsive UI | Semantic forms, accessible labels and responsive styles added for profiles, people directory and portals | Implemented; dedicated viewport/screen-reader audit pending |
| Database integrity and migrations | PostgreSQL schema via Flyway; V8 disables the fixed demo Admin password, adds persisted identity-verification attestation and hashed PostgreSQL rate-limit counters | V8 migration pending isolated verification |
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
- [x] Review public teacher, guardian and student access requests; approve/reject and provision only after verification.

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
- [x] View the current academic overview and subject/teacher directory.
- [x] New student admission creates a login that can authenticate and load the self-service profile.
- [x] Student workspace browser checks.
- [x] View current class/section, active subjects and assigned teacher contacts in My academics.

## 2.1 Correctness and audit hardening (2026-11-16)

- [x] Teacher individual attendance reports require both an active assignment to the requested section and an enrollment overlap for the student/date range. Unrelated or unknown students return the same 404 response.
- [x] Administrator enrollment listings accept an `attendanceDate` filter that returns the roster valid on that date. Admin attendance registers and exam mark-entry rosters use the attendance/subject date rather than today's active enrollment.
- [x] Student and guardian published-exam lists use enrollment on each configured subject date. Historical results remain discoverable after a transfer or ended enrollment.
- [x] Enrollment start/end defaults use the school's `Asia/Kolkata` timezone.
- [x] Flyway V6 changes the database default for new fee invoices to `DRAFT` without editing previously applied migrations.
- [x] Audit events cover student create/update/deactivation, class/section/subject changes, enrollments, guardian links, teacher assignments, attendance lifecycle, examination setup/marks/publication, grading scales, identity provisioning and fee operations.
- [x] Teacher examination discovery no longer scans every examination and executes per-examination/per-subject assignment checks; the repository query scopes directly to active assigned subjects.
- [x] Regression E2E tests cover unrelated-student report access, historical attendance roster, historical student/guardian exam discovery and audit event persistence.


## 2.2 Account access and student learning workspace (2026-11-16)

- [x] Public login screen offers **Request account access** for TEACHER, GUARDIAN and STUDENT. It collects role-specific identity/admission details and never asks for a password.
- [x] Public submissions remain untrusted PENDING rows in `account_access_requests`; no user/profile/login exists before admin approval. ADMIN requests are rejected.
- [x] Admin-only Access requests workspace supports Pending, Approved, Rejected and All filters, approve/reject actions, review notes and initial password input only during approval.
- [x] Approval creates teacher identity, guardian identity plus a verified student relationship, or student admission plus login inside one transaction. Invalid guardian admission rolls back the profile and login. Admin still enrolls new students.
- [x] Duplicate/pending email conflicts use the same public response to reduce account enumeration, and a partial unique index allows one pending request per email across roles.
- [x] Student My academics shows current enrollment, class, section, academic year, active subjects and assigned teacher name/email. Existing My attendance, My fees and My results remain available.
- [x] Profiles expose account details and role-specific student enrollment, teacher assignments (including inactive teacher identity/status), guardian child/class context and admin management scope.
- [x] API/browser E2E covers request transitions, role restrictions, credential non-disclosure, transactional rollback, student academic scoping and all four profiles.
- [x] Public account requests use PostgreSQL-backed per-email and per-source-address throttling. Admin approval requires an explicit off-band identity-verification attestation and a required verification note, recorded on the request.
- [!] Automated email/OTP verification, CAPTCHA, public status lookup, invitation mail, password reset and profile-edit workflow are not included. The verification gate is a manual school-admin control, not automated proof of identity.

## 3. Current feature-branch verification

**Verified current candidate**

Tested code and test commit: f94bcc38fdca4478b43ed71f11ebd3d1047d53c2  
Full-stack workflow: https://github.com/Md-Mohseen-Ali/school-management-system/actions/runs/43805660743  
Build workflow: https://github.com/Md-Mohseen-Ali/school-management-system/actions/runs/43805660708

### Automated API workflow assertions

| Suite | Result |
|---|---:|
| Attendance lifecycle, date-aware historical roster and attendance audit | 33 / 33 |
| Examination, report cards, historical visibility and examination audit | 57 / 57 |
| Fees and simulated payments | 37 / 37 |
| Student/guardian/teacher self-service, privacy, history, profiles and student academics | 61 / 61 |
| Teacher/guardian/student direct account provisioning | 16 / 16 |
| Public account requests, administrator decisions and transactional rollback | 40 / 40 |
| Student payment history | 5 / 5 |
| **Total API assertions** | **249 / 249** |

### Browser checks

37 browser checks passed across the public request form and four roles. They cover role-specific request fields, the admin request queue, account/profile workspaces, student academics, results, attendance and fees. No uncaught browser JavaScript exceptions were reported.

- Admin: student/class/section/subject/enrollment/assignment/family-link/attendance/exam/fee/people/access-request/audit/profile workspaces.
- Teacher: attendance, examinations, assigned-class reports and profile.
- Guardian: linked-student report card, invoice payment history, student attendance and family profile.
- Student: current academics/subjects/teachers, published results, own attendance, own fees/payment history workspace and student profile.

### Build and environment

- Backend Maven package/compile: passed.
- Flyway migrations V1–V7 against disposable PostgreSQL 17: passed.
- Frontend Vite production build: passed.
- Chromium installation and browser run: passed.
- Test data is created only in the disposable GitHub Actions database; the database is destroyed after the run.
- Previous production read-only smoke test was 40/40 on the earlier deployment baseline. This feature branch has not yet been merged or production-smoke-tested.

The tested code and test commit is f94bcc38fdca4478b43ed71f11ebd3d1047d53c2. The tracker synchronization after it is documentation-only. The full-stack workflow is configured at .github/workflows/isolated-e2e.yml; production smoke tests are read-only and not a substitute for this isolated write workflow.

## 4. Algions submission deliverables

| Deliverable | Status |
|---|---|
| Public source repository | Available |
| README with stack, setup, environment and demo instructions | Updated |
| Environment example and secret exclusion | Present |
| Database migrations and synthetic seed data | Present (Flyway V1–V7) |
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
- Admin may directly provision teacher/guardian accounts or approve public requests. No login is created before approval; the administrator supplies the initial password and shares it securely. Passwords are BCrypt hashed and never returned.
- Student approval creates the admission and login. Admin must separately enroll the student into a class/section and share initial credentials securely.
- No email/OTP verification, public request-status lookup, email delivery, self-service password reset or profile-edit workflow is included. Applicants contact the school office for status updates; profile corrections are handled by the administrator.
- There is no built-in CAPTCHA or application rate limiter on the public request route. Add edge/WAF rate limiting and off-band identity verification before internet-facing production use.
- Teacher accounts still need active section/subject assignments; guardians need student links before family data appears.
- No production data is modified by the isolated E2E workflow.
- Responsive CSS has been added; dedicated mobile/tablet viewport, screen-reader and keyboard-only audits are still recommended.

## 6. Change control

- [x] Feature work isolated from main.
- [x] Backend build and isolated PostgreSQL startup.
- [x] Frontend production build.
- [x] All seven API E2E suites pass on the current candidate (249/249 assertions).
- [x] Public request form, admin request queue and all four role workspaces pass 37 browser checks.
- [x] Flyway V1–V7 startup/migration and backend compile pass on the current candidate.
- [x] Test logs reviewed and test-selector failures corrected.
- [x] ERD and API reference added.
- [x] Tracker updated with exact test evidence.
- [ ] Open/complete pull request review.
- [ ] Merge/deploy only after review.
- [ ] Re-run read-only production smoke tests after deployment and update live demo status.
- [x] Record and upload the 3–5 minute walkthrough video.
- [ ] Finish the short technical report/submission form.


## 7. Security review follow-up (2026-11-19)

- [x] Remove the public Admin password from the frontend login helper and README; leave the email as the seeded assessment identity.
- [x] Flyway V8 disables the seeded Admin account. A private `DEMO_ADMIN_PASSWORD` backend environment value re-enables it with a newly BCrypt-hashed password on startup; without the variable the seeded Admin remains disabled.
- [x] Add persistent PostgreSQL-backed request throttling (3 attempts per normalized email and 30 attempts per connection source address per 15-minute window); only SHA-256 rate keys are stored.
- [x] Require a boolean off-band identity-verification attestation plus a required verification note before an administrator can approve a public account request; persist and expose the attestation.
- [x] Select student profile/academic enrollment using the Asia/Kolkata current date, excluding future-dated and expired enrollments.
- [x] Update README and Render deployment instructions for the new controls.
- [ ] Run isolated PostgreSQL/API/browser workflows and production smoke tests against the resulting release.
- [!] Manual identity attestation is enforced; automated email/OTP verification is still a known limitation. No actual email address ownership check is performed.
