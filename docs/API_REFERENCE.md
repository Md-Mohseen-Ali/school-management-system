# REST API Reference

Base path: /api/v1  
Transport: JSON over HTTPS  
Authentication: JWT bearer token, except login and actuator health.  
Roles: ADMIN, TEACHER, GUARDIAN, STUDENT.

Send the access token returned by login in the Authorization header:

Authorization: Bearer <access-token>

This reference follows the current Spring controller mappings. For exact field constraints and response members, the DTO record classes under backend/src/main/java/com/schoolmanagement/modules are authoritative.

## Authentication and account profile

| Method | Route | Access | Purpose |
|---|---|---|---|
| POST | /auth/login | Public | Authenticate using email and password; returns access token, token type and expiry |
| GET | /auth/me | Any authenticated role | Current account identity and role-specific profile |
| POST | /account-requests | Public | Submit a TEACHER, GUARDIAN or STUDENT access request; creates no login |
| GET | /account-requests | ADMIN | List access requests; optional `status=PENDING`, `APPROVED` or `REJECTED` |
| POST | /account-requests/{requestId}/approve | ADMIN | Verify and provision a requested account using an administrator-supplied initial password |
| POST | /account-requests/{requestId}/reject | ADMIN | Reject a pending request with a reason |
| POST | /identity/accounts | ADMIN | Directly create TEACHER or GUARDIAN account and role profile |
| GET | /identity/teachers | ADMIN | List active teachers and profile/contact details |
| GET | /identity/guardians | ADMIN | List guardians and profile/contact details |

Teacher account request example:

\`\`\`json
{
  "role": "TEACHER",
  "fullName": "Asha Sharma",
  "email": "asha.teacher@school.example",
  "phone": "9000000001",
  "initialPassword": "Temporary-Password-2026",
  "employeeCode": "TCH-010",
  "hireDate": "2026-11-16"
}
\`\`\`

For a guardian account, set role to GUARDIAN and use optional alternatePhone instead of employeeCode/hireDate.

### Public account-access request

The public form accepts `requestedRole` values `TEACHER`, `GUARDIAN` or `STUDENT`, plus identity/contact fields relevant to that role. A teacher request requires `employeeCode`; a guardian request requires `studentAdmissionNumber` and `relationship`; a student request requires `admissionNumber`, `firstName` and `lastName`. All requests require `fullName` and `email`. No password is collected or stored in the request.

A public submission returns a request reference and `PENDING` status. It does not create an app_user, student, teacher or guardian identity. An administrator reviews the request in **Access requests** and either rejects it with a reason or approves it with an initial password (12–72 characters). Approval runs account/profile creation and any guardian-to-student link in the same transaction. Guardian approval fails unless the admission number resolves to an existing student. Student approval creates the new student admission and login; the administrator must still create the class/section enrollment. Teacher approval creates staff identity; section/subject assignment remains an administrative step.

Approved passwords are BCrypt hashed and never returned. The administrator must communicate initial credentials securely. Email delivery, public request-status lookup, self-service password reset and automatic email invitation are not implemented.

## Academic administration

All routes in this section are ADMIN-only.

| Method | Route | Purpose |
|---|---|---|
| GET, POST | /academic/students | Search/list and register students |
| GET, PUT, DELETE | /academic/students/{id} | View, update and deactivate a student |
| GET, POST | /academic/classes | List and create classes |
| GET, PUT | /academic/classes/{id} | View and update a class |
| GET, POST | /academic/sections | Search/list and create sections |
| GET, PUT | /academic/sections/{id} | View and update a section |
| GET, POST | /academic/subjects | List and create subjects |
| GET, PUT | /academic/subjects/{id} | View and update a subject |
| PATCH | /academic/subjects/{id}/status | Activate/deactivate a subject |
| GET, POST | /academic/enrollments | List and create student enrollments; GET supports `studentId`, `sectionId`, `active` and `attendanceDate` filters |
| GET | /academic/enrollments/{id} | View enrollment detail |
| PATCH | /academic/enrollments/{id}/end | End an enrollment |
| GET, POST | /academic/teacher-assignments | List/create teacher section-subject assignments |
| GET | /academic/teacher-assignments/{id} | View assignment detail |
| PATCH | /academic/teacher-assignments/{id}/status | Activate/deactivate assignment |
| GET, POST | /academic/student-guardians | List links and link guardian to student |
| PATCH | /academic/student-guardians/{studentId}/{guardianId}/primary | Set primary guardian |
| DELETE | /academic/student-guardians/{studentId}/{guardianId} | Remove a guardian link |

Student creation requires admissionNumber, firstName, lastName, email and initialPassword. Password length is 12–72 characters (the service also enforces BCrypt's 72-byte limit). Optional fields include dateOfBirth, admissionDate and phone. Creating a student creates and links a STUDENT account.

Common list filters include search, status, classId, academicYear, sectionId, studentId, teacherId, subjectId, active, guardianId and attendanceDate where supported. The enrollment directory's attendanceDate filter returns enrollments whose start/end interval covers that date; use it for historical attendance registers and examination rosters rather than `active=true`. Spring Page responses accept page, size and sort parameters.

## Attendance

| Method | Route | Access | Purpose |
|---|---|---|---|
| POST | /attendance/sessions | TEACHER | Create a DRAFT session for an assigned section |
| GET | /attendance/sessions | ADMIN, TEACHER (own sessions) | List and filter sessions |
| GET | /attendance/sessions/{sessionId} | ADMIN, TEACHER (own session) | Session detail |
| PUT | /attendance/sessions/{sessionId}/records | TEACHER (own DRAFT session) | Save roster attendance |
| GET | /attendance/sessions/{sessionId}/records | ADMIN, TEACHER (own session) | Read register |
| POST | /attendance/sessions/{sessionId}/submit | TEACHER | Submit complete register |
| POST | /attendance/sessions/{sessionId}/lock | ADMIN | Lock submitted session |
| GET | /attendance/reports/students/{studentId} | ADMIN, TEACHER (assigned section and student enrollment overlap with requested date range required) | Student attendance summary |
| GET | /attendance/reports/sections/{sectionId} | ADMIN, assigned TEACHER | Class attendance summary |
| GET | /teacher/roster/sections | TEACHER | Active assigned section options |
| GET | /teacher/roster?sectionId=&attendanceDate= | TEACHER (assigned section) | Students enrolled on the requested date |
| GET | /student/academics | STUDENT | Own current enrollment, active subjects and assigned teacher contacts |
| GET | /student/attendance/summary | STUDENT | Own attendance summary |
| GET | /student/attendance/history | STUDENT | Own submitted/locked attendance history |
| GET | /guardian/students/{studentId}/attendance/summary | Linked GUARDIAN | Linked student's attendance summary |
| GET | /guardian/students/{studentId}/attendance/history | Linked GUARDIAN | Linked student's submitted/locked attendance history |

Attendance session request:

\`\`\`json
{
  "sectionId": 12,
  "attendanceDate": "2026-11-16",
  "sessionCode": "FULL_DAY",
  "notes": "Morning register"
}
\`\`\`

Attendance records request:

\`\`\`json
{
  "records": [
    {"studentId": 41, "attendanceStatus": "PRESENT", "remarks": ""},
    {"studentId": 42, "attendanceStatus": "ABSENT", "remarks": "Not present"}
  ]
}
\`\`\`

Allowed attendance statuses: PRESENT, ABSENT, LATE and EXCUSED. The service blocks duplicate student IDs in one request, duplicate session keys, students not enrolled on the attendance date, incomplete submission and modification after submission/lock.

Student and guardian date-range routes require ISO-8601 fromDate and toDate query parameters (YYYY-MM-DD). For an individual teacher report, the teacher must be assigned to sectionId and the student must have an enrollment in that section overlapping the requested date range. Unrelated or unknown student IDs receive the same 404 response.

## Examinations, marks and grading

| Method | Route | Access | Purpose |
|---|---|---|---|
| GET, POST | /examinations | ADMIN | List/create examinations |
| GET | /examinations/{id} | ADMIN | Examination detail |
| GET, POST | /examinations/{id}/subjects | ADMIN | List/add subject configuration |
| POST | /examinations/{id}/schedule | ADMIN | Schedule configured examination |
| GET, PUT | /examinations/subjects/{examinationSubjectId}/marks | ADMIN, assigned TEACHER | Read/save student marks |
| POST | /examinations/{id}/complete | ADMIN | Complete after all marks and grading rules are present |
| POST | /examinations/{id}/publish | ADMIN | Publish completed results |
| GET | /examinations/{examId}/report-cards/{studentId} | ADMIN | Student report card |
| GET, POST | /examinations/grading-scales?academicYear= | ADMIN | List/create grading bands |
| GET | /teacher/examinations | TEACHER | Exams with assigned subjects |
| GET | /teacher/examinations/{examId}/subjects | TEACHER | Assigned subjects for an exam |
| GET | /guardian/examinations | GUARDIAN | Published exams for linked students |
| GET | /guardian/examinations/students/{studentId} | Linked GUARDIAN | Published exams for one selected linked student |
| GET | /guardian/examinations/{examId}/students/{studentId}/report-card | Linked GUARDIAN | Linked student's published report card |
| GET | /student/examinations | STUDENT | Own published examinations |
| GET | /student/examinations/{examId}/report-card | STUDENT | Own report card |

Exam creation fields: sectionId, examName, academicYear (YYYY-YYYY), startDate and endDate. Subject configuration fields: subjectId, examinationDate, maxMarks and passMarks. Marks entries contain studentId, marksObtained, attendanceStatus (PRESENT or ABSENT) and optional remarks.

Marks cannot exceed maximum; marks cannot be negative; absent students must receive zero marks. Completion requires continuous grade-scale coverage between 0 and 100 and mark coverage for all students enrolled on each subject date.

Student and guardian published-exam lists are based on enrollment at each configured subject examination date, not only current active enrollment. Historical report cards therefore remain discoverable after a student transfers or leaves the section.\n\nGrading-scale fields: academicYear, grade, minPercentage, maxPercentage and optional remarks.

## Fees and payments

| Method | Route | Access | Purpose |
|---|---|---|---|
| GET, POST | /fees/invoices | ADMIN; GUARDIAN read-only linked issued invoices | List invoices/create draft invoice |
| GET | /fees/invoices/{invoiceId} | ADMIN; linked GUARDIAN | Invoice detail |
| POST | /fees/invoices/{invoiceId}/items | ADMIN | Add item to draft invoice |
| POST | /fees/invoices/{invoiceId}/issue | ADMIN | Issue invoice |
| POST | /fees/invoices/{invoiceId}/cancel | ADMIN | Cancel invoice when no successful payment exists |
| GET, POST | /fees/invoices/{invoiceId}/payments | ADMIN, linked GUARDIAN | Read history/record simulated payment |
| GET | /student/fees/invoices | STUDENT | Own issued invoice list |
| GET | /student/fees/invoices/{invoiceId} | STUDENT | Own issued invoice detail |
| GET | /student/fees/invoices/{invoiceId}/payments | STUDENT | Own invoice payment history |

Invoice request fields: invoiceNumber, studentId, academicYear, issueDate and dueDate. Invoice item fields: description and amount. Payment request fields: amount, paymentMethod and idempotencyKey.

Only SIMULATED payment method is accepted for new payments in the assessment environment. Migration V7 adds administrator-reviewed public account access requests; V6 aligns the database's new-invoice default with the DRAFT-first lifecycle without editing previously applied migrations. The service rejects CASH; it enforces invoice ownership for guardians/students, prevents overpayment and returns an existing payment for an identical idempotent retry. Students have read-only fee access; guardians or administrators record payments.

## Audit trail

| Method | Route | Access | Purpose |
|---|---|---|---|
| GET | /audit-logs | ADMIN | Read paginated audit entries including actor, action, entity, time and old/new JSON values. Material student, academic structure, enrollment, guardian-link, teacher-assignment, attendance, examination, grading, identity and fee mutations are recorded. |

## HTTP error conventions

| Status | Meaning |
|---|---|
| 400 | Validation error or invalid field/date/range |
| 401 | Missing or invalid authentication |
| 403 | Authenticated role lacks route permission or assignment |
| 404 | Record absent or intentionally not visible to this student/guardian |
| 409 | Duplicate, state conflict, incomplete workflow, overpayment or business constraint |

All routes above are relative to /api/v1. Prepend the backend base URL in clients. Dates use YYYY-MM-DD. Monetary values and marks use decimal JSON numbers.
