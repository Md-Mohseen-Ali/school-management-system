# School Management System — Entity Relationship Diagram

This diagram is based on the Flyway schema in the backend migration directory. It describes the current PostgreSQL relational model. Migration V6 aligns the `fee_invoices.status` database default with the existing DRAFT-first application lifecycle. Migration V7 adds a separate public access-request table; it creates no user relationship until an administrator approves a request.

## Logical data model

\`\`\`mermaid
erDiagram
    APP_USERS ||--o{ APP_USER_ROLES : has
    APP_USERS ||--o| TEACHER_PROFILES : teacher_account
    APP_USERS ||--o| GUARDIAN_PROFILES : guardian_account
    APP_USERS o|--o| STUDENTS : student_portal_account
    APP_USERS o|--o{ ACCOUNT_ACCESS_REQUESTS : reviews

    SCHOOL_CLASSES ||--o{ SECTIONS : contains
    SECTIONS ||--o{ STUDENT_ENROLLMENTS : has
    STUDENTS ||--o{ STUDENT_ENROLLMENTS : enrolled
    STUDENTS ||--o{ STUDENT_GUARDIANS : linked
    GUARDIAN_PROFILES ||--o{ STUDENT_GUARDIANS : represents

    TEACHER_PROFILES ||--o{ TEACHER_ASSIGNMENTS : assigned
    SECTIONS ||--o{ TEACHER_ASSIGNMENTS : assigned_to
    SUBJECTS ||--o{ TEACHER_ASSIGNMENTS : teaches

    TEACHER_PROFILES ||--o{ ATTENDANCE_SESSIONS : marks
    SECTIONS ||--o{ ATTENDANCE_SESSIONS : attendance_for
    ATTENDANCE_SESSIONS ||--o{ ATTENDANCE_RECORDS : contains
    STUDENTS ||--o{ ATTENDANCE_RECORDS : attendance_of

    SECTIONS ||--o{ EXAMINATIONS : schedules
    EXAMINATIONS ||--o{ EXAMINATION_SUBJECTS : configures
    SUBJECTS ||--o{ EXAMINATION_SUBJECTS : assessed_as
    EXAMINATION_SUBJECTS ||--o{ EXAMINATION_MARKS : records
    STUDENTS ||--o{ EXAMINATION_MARKS : receives

    STUDENTS ||--o{ FEE_INVOICES : billed
    APP_USERS o|--o{ FEE_INVOICES : creates
    FEE_INVOICES ||--o{ FEE_INVOICE_ITEMS : itemizes
    FEE_INVOICES ||--o{ FEE_PAYMENTS : paid_by
    APP_USERS o|--o{ FEE_PAYMENTS : receives
    APP_USERS o|--o{ AUDIT_LOGS : acts
\`\`\`

## Tables and responsibilities

| Table | Purpose | Key relationships |
|---|---|---|
| app_users | Login identity, name, email, password hash, phone, enabled flag | Referenced by role links, staff/guardian profiles, optional student profile, invoice creator, payment receiver and audit actor |
| app_user_roles | User-to-role assignments | Composite key: user_id + role |
| account_access_requests | Untrusted public request data, requested role, review state and administrator decision | Optional reviewer reference to app_users; applicants are not app_users until approval |
| teacher_profiles | Staff-specific employee code, hire date and active state | One-to-one with app_users |
| guardian_profiles | Guardian-specific alternate phone | One-to-one with app_users |
| students | Admission identity, name, dates and academic status | Optional one-to-one login; enrollments, family links, attendance, marks and invoices |
| school_classes | Class catalogue | One-to-many sections |
| sections | Class section and academic year | Class, enrollments, teacher assignments, attendance sessions and examinations |
| subjects | Subject catalogue and active state | Teacher assignments and exam subject definitions |
| student_guardians | Student-to-guardian relationship | Composite key: student_id + guardian_id |
| student_enrollments | Student placement with start/end dates | Student and section |
| teacher_assignments | Teacher/section/subject responsibility | Teacher, section and subject |
| attendance_sessions | Daily register and workflow state | Section and teacher who marked the session |
| attendance_records | Student attendance status and remarks | Session and student |
| examinations | Examination timetable and publication state | Section |
| examination_subjects | Subject date, max marks and pass marks for an exam | Examination and subject |
| grading_scales | Academic-year percentage bands | Selected by academic year and result percentage |
| examination_marks | Student mark, presence and remarks | Exam subject and student |
| fee_invoices | Issued/draft/cancelled student invoice | Student and creating user |
| fee_invoice_items | Itemized fee amounts | Invoice |
| fee_payments | Simulated payment, idempotency and payment status | Invoice and optional receiving user |
| audit_logs | Actor, action, entity, request metadata and before/after JSON | Optional actor user |

## Important integrity constraints

- Email is unique case-insensitively.
- A student can have at most one active enrollment at a time.
- A teacher can have one active assignment for a teacher/section/subject tuple.
- Student/guardian links are unique and a student can have at most one primary guardian.
- An attendance session is unique per section, attendance date and session code.
- An attendance record is unique per session/student pair.
- An examination is unique per section, exam name and academic year.
- An exam subject is unique per examination/subject pair.
- Student marks are unique per examination subject/student pair, non-negative, and absent students must have zero marks.
- Invoice numbers, payment references and idempotency keys are unique.
- Invoice due date cannot precede issue date; invoice items and payment amounts must be positive.

## Design notes

1. Account access requests store applicant data separately from authenticated identities. Approval creates the appropriate user/profile transactionally; rejected or pending requests do not create a login.
2. The students table keeps user_id nullable to allow historical/imported records without a portal login. The current student-registration API requires email and initial password and creates the linked student login as part of registration.
3. Grading scales are selected by academic year and percentage; they do not belong to a single examination row.
4. The database enum constraint contains the legacy CASH payment method, but the payment service rejects CASH for new transactions. Assessment payments must be simulated.
5. This is a logical ERD; the Flyway SQL migrations remain the source of truth for exact SQL data types, indexes and constraints.
