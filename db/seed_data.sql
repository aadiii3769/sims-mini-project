-- =============================================================================
-- SIMS – Seed / Demo Data
-- File   : db/seed_data.sql
-- Target : Oracle Database Free (FREEPDB1)
-- Run    : After schema.sql — automatically via /container-entrypoint-initdb.d
--          OR manually:  sqlplus system/admin@localhost:1521/FREEPDB1 @seed_data.sql
-- =============================================================================
-- NOTE: Passwords are stored as plain-text ONLY in this seed script for
--       development / evaluation convenience.  Phase 1 implementation will
--       replace these with BCrypt hashes inside DBConnection bootstrap logic.
-- =============================================================================

-- ── 1. USERS ─────────────────────────────────────────────────────────────────
-- Inserts are ordered: ADMIN → FACULTY → STUDENT → PARENT
-- USER_IDs are explicitly set to small fixed values to make FK references below
-- predictable without relying on sims_seq order.

-- Administrator
INSERT INTO USERS (USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES (1, 'admin01', 'admin@123', 'Dr. Ramesh Krishnamurthy',
        'admin@sims.edu', '9876543210', 'ADMIN');

-- Faculty
INSERT INTO USERS (USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES (2, 'faculty01', 'faculty@123', 'Prof. Meenakshi Sundaram',
        'meenakshi@sims.edu', '9876541111', 'FACULTY');

INSERT INTO USERS (USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES (3, 'faculty02', 'faculty@123', 'Prof. Vijayaraghavan K.',
        'vijay@sims.edu', '9876542222', 'FACULTY');

-- Students
INSERT INTO USERS (USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES (10, 'student01', 'student@123', 'Arun Kumar S.',
        'arun.kumar@student.sims.edu', '9123456781', 'STUDENT');

INSERT INTO USERS (USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES (11, 'student02', 'student@123', 'Priya Lakshmi R.',
        'priya.lakshmi@student.sims.edu', '9123456782', 'STUDENT');

INSERT INTO USERS (USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES (12, 'student03', 'student@123', 'Mohammed Farhan A.',
        'farhan@student.sims.edu', '9123456783', 'STUDENT');

-- Parents (linked to students below)
INSERT INTO USERS (USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES (20, 'parent01', 'parent@123', 'Suresh Kumar (Parent of Arun)',
        'suresh.kumar@gmail.com', '9988776655', 'PARENT');

INSERT INTO USERS (USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES (21, 'parent02', 'parent@123', 'Rajamani Lakshmi (Parent of Priya)',
        'rajamani.l@gmail.com', '9988776644', 'PARENT');

-- ── 2. STUDENT profiles ───────────────────────────────────────────────────────
INSERT INTO STUDENT (STUDENT_ID, USER_ID, ROLL_NUMBER, DEPARTMENT, YEAR, SECTION,
                     DATE_OF_BIRTH, ADDRESS, PARENT_USER_ID)
VALUES (101, 10, '22CS001', 'Computer Science and Engineering',
        3, 'A', DATE '2004-06-15',
        '42, Gandhi Nagar, Chennai - 600 042', 20);

INSERT INTO STUDENT (STUDENT_ID, USER_ID, ROLL_NUMBER, DEPARTMENT, YEAR, SECTION,
                     DATE_OF_BIRTH, ADDRESS, PARENT_USER_ID)
VALUES (102, 11, '22CS002', 'Computer Science and Engineering',
        3, 'A', DATE '2004-09-22',
        '7, Anna Salai, Coimbatore - 641 001', 21);

INSERT INTO STUDENT (STUDENT_ID, USER_ID, ROLL_NUMBER, DEPARTMENT, YEAR, SECTION,
                     DATE_OF_BIRTH, ADDRESS, PARENT_USER_ID)
VALUES (103, 12, '22IT001', 'Information Technology',
        3, 'B', DATE '2004-03-10',
        '15, Park Street, Madurai - 625 001', NULL);

-- ── 3. ATTENDANCE records (last 5 school days) ────────────────────────────────
-- Subject: Object-Oriented Programming (CS3391) — Faculty ID 2

INSERT INTO ATTENDANCE (STUDENT_ID, FACULTY_ID, SUBJECT, LOG_DATE, STATUS)
VALUES (101, 2, 'Object-Oriented Programming (CS3391)', DATE '2026-09-29', 'PRESENT');
INSERT INTO ATTENDANCE (STUDENT_ID, FACULTY_ID, SUBJECT, LOG_DATE, STATUS)
VALUES (101, 2, 'Object-Oriented Programming (CS3391)', DATE '2026-09-30', 'PRESENT');
INSERT INTO ATTENDANCE (STUDENT_ID, FACULTY_ID, SUBJECT, LOG_DATE, STATUS)
VALUES (101, 2, 'Object-Oriented Programming (CS3391)', DATE '2026-10-01', 'ABSENT');
INSERT INTO ATTENDANCE (STUDENT_ID, FACULTY_ID, SUBJECT, LOG_DATE, STATUS)
VALUES (101, 2, 'Object-Oriented Programming (CS3391)', DATE '2026-10-02', 'PRESENT');
INSERT INTO ATTENDANCE (STUDENT_ID, FACULTY_ID, SUBJECT, LOG_DATE, STATUS)
VALUES (101, 2, 'Object-Oriented Programming (CS3391)', DATE '2026-10-03', 'OD');

INSERT INTO ATTENDANCE (STUDENT_ID, FACULTY_ID, SUBJECT, LOG_DATE, STATUS)
VALUES (102, 2, 'Object-Oriented Programming (CS3391)', DATE '2026-09-29', 'PRESENT');
INSERT INTO ATTENDANCE (STUDENT_ID, FACULTY_ID, SUBJECT, LOG_DATE, STATUS)
VALUES (102, 2, 'Object-Oriented Programming (CS3391)', DATE '2026-09-30', 'ABSENT');
INSERT INTO ATTENDANCE (STUDENT_ID, FACULTY_ID, SUBJECT, LOG_DATE, STATUS)
VALUES (102, 2, 'Object-Oriented Programming (CS3391)', DATE '2026-10-01', 'PRESENT');

-- Subject: Database Technology (IT3301) — Faculty ID 3
INSERT INTO ATTENDANCE (STUDENT_ID, FACULTY_ID, SUBJECT, LOG_DATE, STATUS)
VALUES (103, 3, 'Database Technology (IT3301)', DATE '2026-09-29', 'PRESENT');
INSERT INTO ATTENDANCE (STUDENT_ID, FACULTY_ID, SUBJECT, LOG_DATE, STATUS)
VALUES (103, 3, 'Database Technology (IT3301)', DATE '2026-09-30', 'PRESENT');
INSERT INTO ATTENDANCE (STUDENT_ID, FACULTY_ID, SUBJECT, LOG_DATE, STATUS)
VALUES (103, 3, 'Database Technology (IT3301)', DATE '2026-10-01', 'MEDICAL');

-- ── 4. MARKS records ─────────────────────────────────────────────────────────
-- Semester 5, Academic Year 2025-26

-- Arun Kumar — CS3391 OOP
INSERT INTO MARKS (STUDENT_ID, FACULTY_ID, SUBJECT, SEMESTER, CAT1_MARKS,
                   CAT2_MARKS, CAT3_MARKS, GRADE_POINT, ACADEMIC_YEAR)
VALUES (101, 2, 'Object-Oriented Programming (CS3391)', 5,
        42.5, 38.0, 45.0, 8.5, '2025-26');

-- Arun Kumar — CS3392 Computer Networks
INSERT INTO MARKS (STUDENT_ID, FACULTY_ID, SUBJECT, SEMESTER, CAT1_MARKS,
                   CAT2_MARKS, CAT3_MARKS, GRADE_POINT, ACADEMIC_YEAR)
VALUES (101, 3, 'Computer Networks (CS3392)', 5,
        36.0, 40.0, 38.5, 7.5, '2025-26');

-- Priya Lakshmi — CS3391 OOP
INSERT INTO MARKS (STUDENT_ID, FACULTY_ID, SUBJECT, SEMESTER, CAT1_MARKS,
                   CAT2_MARKS, CAT3_MARKS, GRADE_POINT, ACADEMIC_YEAR)
VALUES (102, 2, 'Object-Oriented Programming (CS3391)', 5,
        48.0, 46.5, 49.0, 9.5, '2025-26');

-- Mohammed Farhan — IT3301 Database Technology
INSERT INTO MARKS (STUDENT_ID, FACULTY_ID, SUBJECT, SEMESTER, CAT1_MARKS,
                   CAT2_MARKS, CAT3_MARKS, GRADE_POINT, ACADEMIC_YEAR)
VALUES (103, 3, 'Database Technology (IT3301)', 5,
        40.0, 37.0, 43.5, 8.0, '2025-26');

-- ── 5. PAYMENT records ───────────────────────────────────────────────────────

-- Arun Kumar — Tuition fee PAID
INSERT INTO PAYMENT (STUDENT_ID, FEE_TYPE, AMOUNT_DUE, AMOUNT_PAID,
                     PAYMENT_STATUS, PAYMENT_DATE, DUE_DATE,
                     PAYMENT_MODE, RECEIPT_NUMBER)
VALUES (101, 'TUITION', 45000.00, 45000.00, 'PAID',
        DATE '2026-07-05', DATE '2026-07-31',
        'ONLINE', 'RCPT-2026-001001');

-- Arun Kumar — Exam fee PENDING
INSERT INTO PAYMENT (STUDENT_ID, FEE_TYPE, AMOUNT_DUE, AMOUNT_PAID,
                     PAYMENT_STATUS, PAYMENT_DATE, DUE_DATE,
                     PAYMENT_MODE, RECEIPT_NUMBER)
VALUES (101, 'EXAM', 1500.00, 0, 'PENDING',
        NULL, DATE '2026-10-31',
        NULL, NULL);

-- Priya Lakshmi — Tuition fee PAID
INSERT INTO PAYMENT (STUDENT_ID, FEE_TYPE, AMOUNT_DUE, AMOUNT_PAID,
                     PAYMENT_STATUS, PAYMENT_DATE, DUE_DATE,
                     PAYMENT_MODE, RECEIPT_NUMBER)
VALUES (102, 'TUITION', 45000.00, 45000.00, 'PAID',
        DATE '2026-07-03', DATE '2026-07-31',
        'ONLINE', 'RCPT-2026-001002');

-- Priya Lakshmi — Hostel fee PARTIAL
INSERT INTO PAYMENT (STUDENT_ID, FEE_TYPE, AMOUNT_DUE, AMOUNT_PAID,
                     PAYMENT_STATUS, PAYMENT_DATE, DUE_DATE,
                     PAYMENT_MODE, RECEIPT_NUMBER, REMARKS)
VALUES (102, 'HOSTEL', 30000.00, 15000.00, 'PARTIAL',
        DATE '2026-07-20', DATE '2026-08-15',
        'CASH', 'RCPT-2026-001003',
        'First instalment paid; balance due by Aug 15');

-- Mohammed Farhan — Tuition fee PENDING
INSERT INTO PAYMENT (STUDENT_ID, FEE_TYPE, AMOUNT_DUE, AMOUNT_PAID,
                     PAYMENT_STATUS, PAYMENT_DATE, DUE_DATE)
VALUES (103, 'TUITION', 45000.00, 0, 'PENDING',
        NULL, DATE '2026-07-31');

COMMIT;
