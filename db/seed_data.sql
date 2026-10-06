-- =============================================================================
-- SIMS – Seed / Demo Data
-- File   : db/seed_data.sql
-- Target : Oracle Database Free (FREEPDB1)
-- Run    : After schema.sql — automatically via /container-entrypoint-initdb.d
--          OR manually:  sqlplus system/admin@localhost:1521/FREEPDB1 @seed_data.sql
-- =============================================================================
-- Phase 1: Passwords stored as BCrypt hashes (cost factor 10).
--   admin01   / admin@123    → $2a$10$YCcqhSEVJA1YDe1o0JoaL.5pGDqfs4JBsOusLI89JF4qzqeCDjxjm
--   faculty01 / faculty@123  → $2a$10$pI0WT1QZe2EkX2bJpRrJK.gopC8Ib.0Pp78K6FPFPT5n510IlLjzO
--   student*  / student@123  → $2a$10$vEbjDQRTlmHZvoYz8FjAXuevtLMvEIBb0fX65R7e9SrGjOLSDbgV.
--   parent*   / parent@123   → $2a$10$TiimBtoeVSGdP.6FRlJKL.59iBdzq6/LKoVR4M/QB7xKgrDUjNiZm
-- =============================================================================
SET SQLBLANKLINES ON;
SET DEFINE OFF;
-- =============================================================================

-- ── 1. USERS ─────────────────────────────────────────────────────────────────
-- USER_IDs are explicitly set to small fixed values to make FK references
-- in STUDENT/ATTENDANCE/MARKS/PAYMENT predictable.

-- Administrator (password: admin@123)
INSERT INTO USERS (USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES (1, 'admin01',
        '$2a$10$YCcqhSEVJA1YDe1o0JoaL.5pGDqfs4JBsOusLI89JF4qzqeCDjxjm',
        'Dr. Ramesh Krishnamurthy',
        'admin@sims.edu', '9876543210', 'ADMIN');

-- Faculty (password: faculty@123)
INSERT INTO USERS (USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES (2, 'faculty01',
        '$2a$10$pI0WT1QZe2EkX2bJpRrJK.gopC8Ib.0Pp78K6FPFPT5n510IlLjzO',
        'Prof. Meenakshi Sundaram',
        'meenakshi@sims.edu', '9876541111', 'FACULTY');

INSERT INTO USERS (USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES (3, 'faculty02',
        '$2a$10$pI0WT1QZe2EkX2bJpRrJK.gopC8Ib.0Pp78K6FPFPT5n510IlLjzO',
        'Prof. Vijayaraghavan K.',
        'vijay@sims.edu', '9876542222', 'FACULTY');

-- Students (password: student@123)
INSERT INTO USERS (USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES (10, 'student01',
        '$2a$10$vEbjDQRTlmHZvoYz8FjAXuevtLMvEIBb0fX65R7e9SrGjOLSDbgV.',
        'Arun Kumar S.',
        'arun.kumar@student.sims.edu', '9123456781', 'STUDENT');

INSERT INTO USERS (USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES (11, 'student02',
        '$2a$10$vEbjDQRTlmHZvoYz8FjAXuevtLMvEIBb0fX65R7e9SrGjOLSDbgV.',
        'Priya Lakshmi R.',
        'priya.lakshmi@student.sims.edu', '9123456782', 'STUDENT');

INSERT INTO USERS (USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES (12, 'student03',
        '$2a$10$vEbjDQRTlmHZvoYz8FjAXuevtLMvEIBb0fX65R7e9SrGjOLSDbgV.',
        'Mohammed Farhan A.',
        'farhan@student.sims.edu', '9123456783', 'STUDENT');

INSERT INTO USERS (USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES (13, 'student04',
        '$2a$10$vEbjDQRTlmHZvoYz8FjAXuevtLMvEIBb0fX65R7e9SrGjOLSDbgV.',
        'Ananya Venkatesh',
        'ananya.v@student.sims.edu', '9123456784', 'STUDENT');

INSERT INTO USERS (USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES (14, 'student05',
        '$2a$10$vEbjDQRTlmHZvoYz8FjAXuevtLMvEIBb0fX65R7e9SrGjOLSDbgV.',
        'Karthik Subramanian',
        'karthik.s@student.sims.edu', '9123456785', 'STUDENT');

INSERT INTO USERS (USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES (15, 'student06',
        '$2a$10$vEbjDQRTlmHZvoYz8FjAXuevtLMvEIBb0fX65R7e9SrGjOLSDbgV.',
        'Sneha Murugan',
        'sneha.m@student.sims.edu', '9123456786', 'STUDENT');

INSERT INTO USERS (USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES (16, 'student07',
        '$2a$10$vEbjDQRTlmHZvoYz8FjAXuevtLMvEIBb0fX65R7e9SrGjOLSDbgV.',
        'Rohit R. Menon',
        'rohit.menon@student.sims.edu', '9123456787', 'STUDENT');

INSERT INTO USERS (USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES (17, 'student08',
        '$2a$10$vEbjDQRTlmHZvoYz8FjAXuevtLMvEIBb0fX65R7e9SrGjOLSDbgV.',
        'Divya Balachandran',
        'divya.bala@student.sims.edu', '9123456788', 'STUDENT');

INSERT INTO USERS (USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES (18, 'student09',
        '$2a$10$vEbjDQRTlmHZvoYz8FjAXuevtLMvEIBb0fX65R7e9SrGjOLSDbgV.',
        'Vigneshwaran K.',
        'vignesh.k@student.sims.edu', '9123456789', 'STUDENT');

INSERT INTO USERS (USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES (19, 'student10',
        '$2a$10$vEbjDQRTlmHZvoYz8FjAXuevtLMvEIBb0fX65R7e9SrGjOLSDbgV.',
        'Keerthana Natarajan',
        'keerthana.n@student.sims.edu', '9123456790', 'STUDENT');

-- Parents (password: parent@123)
INSERT INTO USERS (USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES (20, 'parent01',
        '$2a$10$TiimBtoeVSGdP.6FRlJKL.59iBdzq6/LKoVR4M/QB7xKgrDUjNiZm',
        'Suresh Kumar (Parent of Arun)',
        'suresh.kumar@gmail.com', '9988776655', 'PARENT');

INSERT INTO USERS (USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES (21, 'parent02',
        '$2a$10$TiimBtoeVSGdP.6FRlJKL.59iBdzq6/LKoVR4M/QB7xKgrDUjNiZm',
        'Rajamani Lakshmi (Parent of Priya)',
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

INSERT INTO STUDENT (STUDENT_ID, USER_ID, ROLL_NUMBER, DEPARTMENT, YEAR, SECTION,
                     DATE_OF_BIRTH, ADDRESS, PARENT_USER_ID)
VALUES (104, 13, '22CS003', 'Computer Science and Engineering',
        3, 'A', DATE '2004-11-05',
        '18, Temple Road, Mylapore, Chennai - 600 004', NULL);

INSERT INTO STUDENT (STUDENT_ID, USER_ID, ROLL_NUMBER, DEPARTMENT, YEAR, SECTION,
                     DATE_OF_BIRTH, ADDRESS, PARENT_USER_ID)
VALUES (105, 14, '23IT001', 'Information Technology',
        2, 'B', DATE '2005-04-18',
        '5/12, Lake View Road, Madurai - 625 020', NULL);

INSERT INTO STUDENT (STUDENT_ID, USER_ID, ROLL_NUMBER, DEPARTMENT, YEAR, SECTION,
                     DATE_OF_BIRTH, ADDRESS, PARENT_USER_ID)
VALUES (106, 15, '23EC001', 'Electronics and Communication',
        2, 'A', DATE '2005-08-30',
        '29, Cross Cut Road, Coimbatore - 641 012', NULL);

INSERT INTO STUDENT (STUDENT_ID, USER_ID, ROLL_NUMBER, DEPARTMENT, YEAR, SECTION,
                     DATE_OF_BIRTH, ADDRESS, PARENT_USER_ID)
VALUES (107, 16, '24AI001', 'Artificial Intelligence and Data Science',
        1, 'A', DATE '2006-01-22',
        '88, Beach Road, Thiruvananthapuram - 695 001', NULL);

INSERT INTO STUDENT (STUDENT_ID, USER_ID, ROLL_NUMBER, DEPARTMENT, YEAR, SECTION,
                     DATE_OF_BIRTH, ADDRESS, PARENT_USER_ID)
VALUES (108, 17, '21ME001', 'Mechanical Engineering',
        4, 'A', DATE '2003-07-14',
        '102, Nehru Street, Salem - 636 007', NULL);

INSERT INTO STUDENT (STUDENT_ID, USER_ID, ROLL_NUMBER, DEPARTMENT, YEAR, SECTION,
                     DATE_OF_BIRTH, ADDRESS, PARENT_USER_ID)
VALUES (109, 18, '22CS004', 'Computer Science and Engineering',
        3, 'B', DATE '2004-12-01',
        '14, VOC Nagar, Tirunelveli - 627 002', NULL);

INSERT INTO STUDENT (STUDENT_ID, USER_ID, ROLL_NUMBER, DEPARTMENT, YEAR, SECTION,
                     DATE_OF_BIRTH, ADDRESS, PARENT_USER_ID)
VALUES (110, 19, '23EC002', 'Electronics and Communication',
        2, 'B', DATE '2005-09-19',
        '77, Anna Nagar West, Chennai - 600 040', NULL);

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
