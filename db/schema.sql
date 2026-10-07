-- =============================================================================
-- SIMS – Oracle Database DDL Schema
-- File   : db/schema.sql
-- Target : Oracle Database Free (FREEPDB1)
-- Run    : Automatically via /container-entrypoint-initdb.d on first Docker boot
--          OR manually:  sqlplus system/admin@localhost:1521/FREEPDB1 @schema.sql
-- =============================================================================
-- Normalization note: all tables satisfy 3NF — see docs/DATABASE.md for the
-- step-by-step academic justification (UNF → 1NF → 2NF → 3NF).
-- =============================================================================
SET SQLBLANKLINES ON;
SET DEFINE OFF;
-- =============================================================================

-- ── Safety: drop tables in dependency order (child → parent) ─────────────────
BEGIN
    FOR t IN (
        SELECT table_name FROM user_tables
        WHERE table_name IN (
            'FACULTY_COURSE_ALLOCATION','MARKS','ATTENDANCE','PAYMENT',
            'FACULTY','COURSE','STUDENT','DEPARTMENT','USERS'
        )
        ORDER BY DECODE(table_name,
            'FACULTY_COURSE_ALLOCATION', 1,
            'MARKS',                     2,
            'ATTENDANCE',                3,
            'PAYMENT',                   4,
            'FACULTY',                   5,
            'COURSE',                    6,
            'STUDENT',                   7,
            'DEPARTMENT',                8,
            'USERS',                     9)
    ) LOOP
        EXECUTE IMMEDIATE 'DROP TABLE ' || t.table_name || ' CASCADE CONSTRAINTS';
    END LOOP;
END;
/

-- ── Sequence: shared surrogate key generator ──────────────────────────────────
DROP SEQUENCE IF EXISTS sims_seq;
CREATE SEQUENCE sims_seq
    START WITH 1000
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- =============================================================================
-- Table: USERS
-- Purpose: Stores authentication credentials and role for every system actor.
-- 3NF status: No partial or transitive dependencies; ROLE is a property of
--   the user row only, not of any non-key attribute.
-- =============================================================================
CREATE TABLE USERS (
    USER_ID       NUMBER         DEFAULT sims_seq.NEXTVAL  NOT NULL,
    USERNAME      VARCHAR2(50)   NOT NULL,
    PASSWORD_HASH VARCHAR2(255)  NOT NULL,   -- BCrypt hash
    FULL_NAME     VARCHAR2(100)  NOT NULL,
    EMAIL         VARCHAR2(100)  NOT NULL,
    PHONE         VARCHAR2(15),
    ROLE          VARCHAR2(10)   NOT NULL,
    IS_ACTIVE     NUMBER(1)      DEFAULT 1   NOT NULL,
    CREATED_AT    TIMESTAMP      DEFAULT SYSTIMESTAMP,

    -- ── Constraints ──────────────────────────────────────────────────────────
    CONSTRAINT PK_USERS          PRIMARY KEY (USER_ID),
    CONSTRAINT UQ_USERS_USERNAME UNIQUE      (USERNAME),
    CONSTRAINT UQ_USERS_EMAIL    UNIQUE      (EMAIL),
    CONSTRAINT CHK_USERS_ROLE    CHECK       (ROLE IN ('ADMIN','FACULTY','STUDENT','PARENT')),
    CONSTRAINT CHK_USERS_ACTIVE  CHECK       (IS_ACTIVE IN (0,1))
);

COMMENT ON TABLE  USERS               IS 'Authentication and role registry for all system users.';
COMMENT ON COLUMN USERS.PASSWORD_HASH IS 'BCrypt-hashed password. Plain-text NEVER stored.';
COMMENT ON COLUMN USERS.ROLE          IS 'RBAC role: ADMIN | FACULTY | STUDENT | PARENT';

-- =============================================================================
-- Table: DEPARTMENT
-- Purpose: Academic departments (CSE, IT, ECE, MECH, etc.).
-- =============================================================================
CREATE TABLE DEPARTMENT (
    DEPT_ID       NUMBER         DEFAULT sims_seq.NEXTVAL  NOT NULL,
    DEPT_NAME     VARCHAR2(100)  NOT NULL,
    DEPT_CODE     VARCHAR2(10)   NOT NULL,

    -- ── Constraints ──────────────────────────────────────────────────────────
    CONSTRAINT PK_DEPARTMENT       PRIMARY KEY (DEPT_ID),
    CONSTRAINT UQ_DEPT_CODE        UNIQUE      (DEPT_CODE)
);

COMMENT ON TABLE  DEPARTMENT           IS 'Academic department master table.';
COMMENT ON COLUMN DEPARTMENT.DEPT_CODE IS 'Unique acronym: CSE | IT | ECE | MECH';

-- =============================================================================
-- Table: COURSE
-- Purpose: Academic subjects offered across semesters and departments.
-- Semester 1 is common foundation across all departments (DEPT_ID is NULL).
-- Semesters 2 to 8 are department-specific.
-- =============================================================================
CREATE TABLE COURSE (
    COURSE_ID     NUMBER         DEFAULT sims_seq.NEXTVAL  NOT NULL,
    COURSE_NAME   VARCHAR2(150)  NOT NULL,
    COURSE_CODE   VARCHAR2(20)   NOT NULL,
    CREDITS       NUMBER(2)      DEFAULT 3 NOT NULL,
    SEMESTER      NUMBER(2)      NOT NULL,
    DEPT_ID       NUMBER,                    -- FK → DEPARTMENT (NULL for Sem 1 common)

    -- ── Constraints ──────────────────────────────────────────────────────────
    CONSTRAINT PK_COURSE           PRIMARY KEY (COURSE_ID),
    CONSTRAINT UQ_COURSE_CODE      UNIQUE      (COURSE_CODE),
    CONSTRAINT FK_COURSE_DEPT      FOREIGN KEY (DEPT_ID)
                                       REFERENCES DEPARTMENT(DEPT_ID) ON DELETE SET NULL,
    CONSTRAINT CHK_COURSE_SEM      CHECK       (SEMESTER BETWEEN 1 AND 8),
    CONSTRAINT CHK_COURSE_CREDITS  CHECK       (CREDITS > 0)
);

COMMENT ON TABLE  COURSE         IS 'Academic curriculum courses. Sem 1 has NULL DEPT_ID (common foundation).';
COMMENT ON COLUMN COURSE.DEPT_ID IS 'Nullable FK: NULL indicates common foundational course for all departments.';

-- =============================================================================
-- Table: FACULTY
-- Purpose: Faculty profiles mapped to academic departments.
-- =============================================================================
CREATE TABLE FACULTY (
    FACULTY_ID    NUMBER         DEFAULT sims_seq.NEXTVAL  NOT NULL,
    USER_ID       NUMBER         NOT NULL,   -- FK → USERS (role=FACULTY)
    DEPT_ID       NUMBER         NOT NULL,   -- FK → DEPARTMENT
    DESIGNATION   VARCHAR2(100)  NOT NULL,

    -- ── Constraints ──────────────────────────────────────────────────────────
    CONSTRAINT PK_FACULTY          PRIMARY KEY (FACULTY_ID),
    CONSTRAINT UQ_FACULTY_USER     UNIQUE      (USER_ID),
    CONSTRAINT FK_FACULTY_USER     FOREIGN KEY (USER_ID)
                                       REFERENCES USERS(USER_ID) ON DELETE CASCADE,
    CONSTRAINT FK_FACULTY_DEPT     FOREIGN KEY (DEPT_ID)
                                       REFERENCES DEPARTMENT(DEPT_ID)
);

COMMENT ON TABLE  FACULTY             IS 'Faculty academic profile linked to USERS and DEPARTMENT.';
COMMENT ON COLUMN FACULTY.DESIGNATION IS 'Academic rank e.g. Professor, Associate Professor, Assistant Professor.';

-- =============================================================================
-- Table: FACULTY_COURSE_ALLOCATION
-- Purpose: Mappings of faculty members to taught courses per academic year.
-- =============================================================================
CREATE TABLE FACULTY_COURSE_ALLOCATION (
    ALLOCATION_ID NUMBER         DEFAULT sims_seq.NEXTVAL  NOT NULL,
    FACULTY_ID    NUMBER         NOT NULL,   -- FK → FACULTY
    COURSE_ID     NUMBER         NOT NULL,   -- FK → COURSE
    ACADEMIC_YEAR VARCHAR2(10)   NOT NULL,   -- e.g. '2025-26'

    -- ── Constraints ──────────────────────────────────────────────────────────
    CONSTRAINT PK_FCA              PRIMARY KEY (ALLOCATION_ID),
    CONSTRAINT FK_FCA_FACULTY      FOREIGN KEY (FACULTY_ID)
                                       REFERENCES FACULTY(FACULTY_ID) ON DELETE CASCADE,
    CONSTRAINT FK_FCA_COURSE       FOREIGN KEY (COURSE_ID)
                                       REFERENCES COURSE(COURSE_ID) ON DELETE CASCADE,
    CONSTRAINT UQ_FCA_ALLOC        UNIQUE      (FACULTY_ID, COURSE_ID, ACADEMIC_YEAR)
);

COMMENT ON TABLE FACULTY_COURSE_ALLOCATION IS 'Faculty-to-course allocation registry for academic terms.';

-- =============================================================================
-- Table: STUDENT
-- Purpose: Academic profile and enrolment data for students.
-- 3NF status: Every non-key column depends solely on STUDENT_ID (PK).
-- =============================================================================
CREATE TABLE STUDENT (
    STUDENT_ID    NUMBER         DEFAULT sims_seq.NEXTVAL  NOT NULL,
    USER_ID       NUMBER         NOT NULL,   -- FK → USERS (role=STUDENT)
    ROLL_NUMBER   VARCHAR2(20)   NOT NULL,
    DEPARTMENT    VARCHAR2(80)   NOT NULL,
    DEPT_ID       NUMBER,                    -- FK → DEPARTMENT
    YEAR          NUMBER(1)      NOT NULL,
    SECTION       VARCHAR2(5),
    DATE_OF_BIRTH DATE,
    ADDRESS       VARCHAR2(300),
    PARENT_USER_ID NUMBER,                  -- FK → USERS (role=PARENT), nullable

    -- ── Constraints ──────────────────────────────────────────────────────────
    CONSTRAINT PK_STUDENT           PRIMARY KEY (STUDENT_ID),
    CONSTRAINT UQ_STUDENT_ROLL      UNIQUE      (ROLL_NUMBER),
    CONSTRAINT UQ_STUDENT_USER      UNIQUE      (USER_ID),
    CONSTRAINT FK_STUDENT_USER      FOREIGN KEY (USER_ID)
                                        REFERENCES USERS(USER_ID) ON DELETE CASCADE,
    CONSTRAINT FK_STUDENT_DEPT      FOREIGN KEY (DEPT_ID)
                                        REFERENCES DEPARTMENT(DEPT_ID) ON DELETE SET NULL,
    CONSTRAINT FK_STUDENT_PARENT    FOREIGN KEY (PARENT_USER_ID)
                                        REFERENCES USERS(USER_ID) ON DELETE SET NULL,
    CONSTRAINT CHK_STUDENT_YEAR     CHECK       (YEAR BETWEEN 1 AND 5)
);

COMMENT ON TABLE  STUDENT                IS 'Academic enrolment record linked to a USERS row with role=STUDENT.';
COMMENT ON COLUMN STUDENT.PARENT_USER_ID IS 'Optional link to a PARENT-role USERS row.';

-- =============================================================================
-- Table: ATTENDANCE
-- Purpose: Hourly / daily class attendance log per student per subject.
-- =============================================================================
CREATE TABLE ATTENDANCE (
    LOG_ID        NUMBER         DEFAULT sims_seq.NEXTVAL  NOT NULL,
    STUDENT_ID    NUMBER         NOT NULL,   -- FK → STUDENT
    FACULTY_ID    NUMBER         NOT NULL,   -- FK → USERS (role=FACULTY)
    SUBJECT       VARCHAR2(150)  NOT NULL,
    LOG_DATE      DATE           NOT NULL,
    STATUS        VARCHAR2(10)   NOT NULL,   -- PRESENT | ABSENT | OD | MEDICAL
    REMARKS       VARCHAR2(200),

    -- ── Constraints ──────────────────────────────────────────────────────────
    CONSTRAINT PK_ATTENDANCE          PRIMARY KEY (LOG_ID),
    CONSTRAINT FK_ATTEND_STUDENT      FOREIGN KEY (STUDENT_ID)
                                          REFERENCES STUDENT(STUDENT_ID) ON DELETE CASCADE,
    CONSTRAINT FK_ATTEND_FACULTY      FOREIGN KEY (FACULTY_ID)
                                          REFERENCES USERS(USER_ID),
    CONSTRAINT CHK_ATTEND_STATUS      CHECK       (STATUS IN ('PRESENT','ABSENT','OD','MEDICAL')),
    CONSTRAINT UQ_ATTEND_ENTRY        UNIQUE      (STUDENT_ID, SUBJECT, LOG_DATE)
);

COMMENT ON TABLE ATTENDANCE IS 'Per-subject daily attendance record logged by faculty.';

-- =============================================================================
-- Table: MARKS
-- Purpose: Continuous assessment tests (CAT1, CAT2, Assignment), internal total,
--          semester grade, and completion status per student per course.
-- =============================================================================
CREATE TABLE MARKS (
    RECORD_ID        NUMBER         DEFAULT sims_seq.NEXTVAL  NOT NULL,
    STUDENT_ID       NUMBER         NOT NULL,   -- FK → STUDENT
    COURSE_ID        NUMBER         NOT NULL,   -- FK → COURSE
    CAT1_MARKS       NUMBER(5,2)    DEFAULT 0,
    CAT2_MARKS       NUMBER(5,2)    DEFAULT 0,
    ASSIGNMENT_MARKS NUMBER(5,2)    DEFAULT 0,
    TOTAL_INTERNAL   NUMBER(5,2)    DEFAULT 0,
    SEMESTER_GRADE   VARCHAR2(10),              -- e.g. 'O', 'A+', 'A', 'B+', 'B', 'F'
    GRADE_POINT      NUMBER(3,1)    DEFAULT 0,  -- 0.0 to 10.0 scale
    SEMESTER_NO      NUMBER(2)      NOT NULL,
    IS_COMPLETED     NUMBER(1)      DEFAULT 0   NOT NULL,  -- 0=Active CATs, 1=Completed Transcript
    FACULTY_ID       NUMBER,                    -- FK → USERS (role=FACULTY)
    ACADEMIC_YEAR    VARCHAR2(10)   DEFAULT '2025-26',

    -- ── Constraints ──────────────────────────────────────────────────────────
    CONSTRAINT PK_MARKS              PRIMARY KEY (RECORD_ID),
    CONSTRAINT FK_MARKS_STUDENT      FOREIGN KEY (STUDENT_ID)
                                         REFERENCES STUDENT(STUDENT_ID) ON DELETE CASCADE,
    CONSTRAINT FK_MARKS_COURSE       FOREIGN KEY (COURSE_ID)
                                         REFERENCES COURSE(COURSE_ID) ON DELETE CASCADE,
    CONSTRAINT FK_MARKS_FACULTY      FOREIGN KEY (FACULTY_ID)
                                         REFERENCES USERS(USER_ID),
    CONSTRAINT UQ_MARKS_ENTRY        UNIQUE      (STUDENT_ID, COURSE_ID, SEMESTER_NO),
    CONSTRAINT CHK_MARKS_CAT1        CHECK       (CAT1_MARKS BETWEEN 0 AND 50),
    CONSTRAINT CHK_MARKS_CAT2        CHECK       (CAT2_MARKS BETWEEN 0 AND 50),
    CONSTRAINT CHK_MARKS_ASSG        CHECK       (ASSIGNMENT_MARKS BETWEEN 0 AND 50),
    CONSTRAINT CHK_MARKS_TOTAL       CHECK       (TOTAL_INTERNAL >= 0),
    CONSTRAINT CHK_MARKS_GRADE_PT    CHECK       (GRADE_POINT BETWEEN 0 AND 10),
    CONSTRAINT CHK_MARKS_SEM_NO      CHECK       (SEMESTER_NO BETWEEN 1 AND 8),
    CONSTRAINT CHK_MARKS_COMPLETED   CHECK       (IS_COMPLETED IN (0, 1))
);

COMMENT ON TABLE  MARKS              IS 'Internal CAT and semester assessment marks linked to COURSE.';
COMMENT ON COLUMN MARKS.IS_COMPLETED IS '0 for current active semester CATs, 1 for completed historical semesters.';

-- =============================================================================
-- Table: PAYMENT
-- Purpose: Fee invoice and payment transaction record.
-- =============================================================================
CREATE TABLE PAYMENT (
    TRANSACTION_ID  NUMBER         DEFAULT sims_seq.NEXTVAL  NOT NULL,
    STUDENT_ID      NUMBER         NOT NULL,   -- FK → STUDENT
    FEE_TYPE        VARCHAR2(50)   NOT NULL,   -- TUITION | HOSTEL | EXAM | BUS | OTHER
    AMOUNT_DUE      NUMBER(10,2)   NOT NULL,
    AMOUNT_PAID     NUMBER(10,2)   DEFAULT 0,
    PAYMENT_STATUS  VARCHAR2(10)   NOT NULL,   -- PAID | PENDING | PARTIAL | WAIVED
    PAYMENT_DATE    DATE,
    DUE_DATE        DATE           NOT NULL,
    PAYMENT_MODE    VARCHAR2(20),              -- ONLINE | CASH | DD | WAIVER
    RECEIPT_NUMBER  VARCHAR2(30),
    REMARKS         VARCHAR2(200),

    -- ── Constraints ──────────────────────────────────────────────────────────
    CONSTRAINT PK_PAYMENT              PRIMARY KEY (TRANSACTION_ID),
    CONSTRAINT FK_PAYMENT_STUDENT      FOREIGN KEY (STUDENT_ID)
                                           REFERENCES STUDENT(STUDENT_ID) ON DELETE CASCADE,
    CONSTRAINT CHK_PAYMENT_STATUS      CHECK       (PAYMENT_STATUS IN ('PAID','PENDING','PARTIAL','WAIVED')),
    CONSTRAINT CHK_PAYMENT_FEE_TYPE    CHECK       (FEE_TYPE IN ('TUITION','HOSTEL','EXAM','BUS','OTHER')),
    CONSTRAINT CHK_PAYMENT_MODE        CHECK       (PAYMENT_MODE IN ('ONLINE','CASH','DD','WAIVER') OR PAYMENT_MODE IS NULL),
    CONSTRAINT CHK_PAYMENT_AMOUNT_DUE  CHECK       (AMOUNT_DUE  >= 0),
    CONSTRAINT CHK_PAYMENT_AMOUNT_PAID CHECK       (AMOUNT_PAID >= 0)
);

COMMENT ON TABLE  PAYMENT                IS 'Fee invoice and transaction log per student.';
COMMENT ON COLUMN PAYMENT.RECEIPT_NUMBER IS 'System-generated receipt number printed on the PDF receipt.';

-- =============================================================================
-- Indexes (frequently joined / filtered columns)
-- =============================================================================
CREATE INDEX IDX_STUDENT_DEPT     ON STUDENT    (DEPT_ID);
CREATE INDEX IDX_COURSE_DEPT_SEM  ON COURSE     (DEPT_ID, SEMESTER);
CREATE INDEX IDX_FACULTY_DEPT     ON FACULTY    (DEPT_ID);
CREATE INDEX IDX_FCA_FACULTY      ON FACULTY_COURSE_ALLOCATION (FACULTY_ID);
CREATE INDEX IDX_MARKS_STUD_SEM   ON MARKS      (STUDENT_ID, SEMESTER_NO);
CREATE INDEX IDX_MARKS_COURSE     ON MARKS      (COURSE_ID);
CREATE INDEX IDX_ATTEND_STUDENT   ON ATTENDANCE (STUDENT_ID, LOG_DATE);
CREATE INDEX IDX_PAYMENT_STUDENT  ON PAYMENT    (STUDENT_ID, PAYMENT_STATUS);
CREATE INDEX IDX_USERS_ROLE       ON USERS      (ROLE);

COMMIT;
