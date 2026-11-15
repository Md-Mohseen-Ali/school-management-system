-- ============================================
-- V5: Synthetic Demo Data
-- ============================================

-- Demo users. Passwords are BCrypt hashes (cost 12).
INSERT INTO app_users
    (full_name, email, password_hash, phone)
VALUES
    ('School Administrator',
     'admin@school.demo',
     '$2y$12$QveHOlf.d6sLMtYA1YsqDeyL2PU84jzJT8WP9WHje2GBXFdT5yrSi',
     '9000000001'),

    ('Demo Teacher',
     'teacher@school.demo',
     '$2y$12$axIYSNOhODqKtHyTY/d4S.5ft2E8R6pJV7dOsGLeH0smI.3cBOjdy',
     '9000000002'),

    ('Priya Sharma',
     'guardian1@school.demo',
     '$2y$12$S4AvGq0YJ9poDNYJH5x2oOuv5vMZYO95dx/7ErfMRZe5fiKjlAUQG',
     '9000000003'),

    ('Rahul Verma',
     'guardian2@school.demo',
     '$2y$12$/TLu5vQZN9CMMluXNxBlSeC7Nasrlw4vfarkXdiYxB5WRWikF5dya',
     '9000000004'),

    ('Aarav Sharma',
     'student1@school.demo',
     '$2y$12$ryPS9m2m9KwENgHuFLoDIOopePDjb6CQP4YEY6/.ybWIPyKKUlaT.',
     '9000000005'),

    ('Anaya Verma',
     'student2@school.demo',
     '$2y$12$v5HPwEM8dHj3G12/F5jZuubbKAkMRTD6dGABrO9Sjr9PeXU0N.aSy',
     '9000000006');


-- Assign roles
INSERT INTO app_user_roles(user_id, role)
SELECT u.id, seed.role_name
FROM app_users u
JOIN (
    VALUES
        ('admin@school.demo', 'ADMIN'),
        ('teacher@school.demo', 'TEACHER'),
        ('guardian1@school.demo', 'GUARDIAN'),
        ('guardian2@school.demo', 'GUARDIAN'),
        ('student1@school.demo', 'STUDENT'),
        ('student2@school.demo', 'STUDENT')
) AS seed(email, role_name)
ON u.email = seed.email;


-- Teacher profile
INSERT INTO teacher_profiles(user_id, employee_code, hire_date)
SELECT id, 'TCH-001', CURRENT_DATE
FROM app_users
WHERE email = 'teacher@school.demo';


-- Guardian profiles
INSERT INTO guardian_profiles(user_id)
SELECT id
FROM app_users
WHERE email IN (
    'guardian1@school.demo',
    'guardian2@school.demo'
);


-- Academic structure
INSERT INTO school_classes(name, description)
VALUES ('Grade 10', 'Demo Grade 10 class');


INSERT INTO sections(
    school_class_id, section_name, academic_year
)
SELECT id, 'A', '2026-2027'
FROM school_classes
WHERE name = 'Grade 10';


-- Subjects
INSERT INTO subjects(subject_code, name)
VALUES
    ('MATH-10', 'Mathematics'),
    ('SCI-10', 'Science');


-- Students
INSERT INTO students(
    user_id, admission_number,
    first_name, last_name, date_of_birth
)
SELECT id, 'ADM-2026-001', 'Aarav', 'Sharma', '2011-04-12'
FROM app_users
WHERE email = 'student1@school.demo';


INSERT INTO students(
    user_id, admission_number,
    first_name, last_name, date_of_birth
)
SELECT id, 'ADM-2026-002', 'Anaya', 'Verma', '2011-08-21'
FROM app_users
WHERE email = 'student2@school.demo';


-- Link guardians to their respective students
INSERT INTO student_guardians(
    student_id, guardian_id, relationship, is_primary
)
SELECT s.id, g.id, 'PARENT', TRUE
FROM students s
JOIN guardian_profiles g
    ON g.user_id = (
        SELECT id FROM app_users
        WHERE email = 'guardian1@school.demo'
    )
WHERE s.admission_number = 'ADM-2026-001';


INSERT INTO student_guardians(
    student_id, guardian_id, relationship, is_primary
)
SELECT s.id, g.id, 'PARENT', TRUE
FROM students s
JOIN guardian_profiles g
    ON g.user_id = (
        SELECT id FROM app_users
        WHERE email = 'guardian2@school.demo'
    )
WHERE s.admission_number = 'ADM-2026-002';


-- Enroll both students in Grade 10-A
INSERT INTO student_enrollments(student_id, section_id)
SELECT st.id, sec.id
FROM students st
CROSS JOIN sections sec
WHERE st.admission_number IN (
    'ADM-2026-001',
    'ADM-2026-002'
)
AND sec.section_name = 'A'
AND sec.academic_year = '2026-2027';


-- Assign teacher to Mathematics and Science
INSERT INTO teacher_assignments(
    teacher_id, section_id, subject_id
)
SELECT tp.id, sec.id, sub.id
FROM teacher_profiles tp
CROSS JOIN sections sec
CROSS JOIN subjects sub
WHERE tp.employee_code = 'TCH-001'
AND sec.section_name = 'A'
AND sec.academic_year = '2026-2027'
AND sub.subject_code IN ('MATH-10', 'SCI-10');
