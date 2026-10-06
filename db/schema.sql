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

-- ── Safety: drop tables in dependency order (child → parent) ─────────────────
BEGIN
    FOR t IN (
        SELECT table_name FROM user_tables
        WHERE table_name IN (
            'PAYMENT','MARKS','ATTENDANCE','STUDENT','USERS'
        )
        ORDER BY DECODE(table_name,
            'PAYMENT',1,'MARKS',2,'ATTENDANCE',3,'STUDENT',4,'USERS',5)
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
    PASSWORD_HASH VARCHAR2(255)  NOT NULL,   -- BCrypt hash (Phase 1)
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
-- Table: STUDENT
-- Purpose: Academic profile and enrolment data for students.
-- 3NF status: Every non-key column (DEPT, YEAR, SECTION, PARENT_USER_ID)
--   depends solely on STUDENT_ID (PK) and on no other non-key column.
-- =============================================================================
CREATE TABLE STUDENT (
    STUDENT_ID    NUMBER         DEFAULT sims_seq.NEXTVAL  NOT NULL,
    USER_ID       NUMBER         NOT NULL,   -- FK → USERS (role=STUDENT)
    ROLL_NUMBER   VARCHAR2(20)   NOT NULL,
    DEPARTMENT    VARCHAR2(80)   NOT NULL,
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
    CONSTRAINT FK_STUDENT_PARENT    FOREIGN KEY (PARENT_USER_ID)
                                        REFERENCES USERS(USER_ID) ON DELETE SET NULL,
    CONSTRAINT CHK_STUDENT_YEAR     CHECK       (YEAR BETWEEN 1 AND 5)
);

COMMENT ON TABLE  STUDENT                IS 'Academic enrolment record linked to a USERS row with role=STUDENT.';
COMMENT ON COLUMN STUDENT.PARENT_USER_ID IS 'Optional link to a PARENT-role USERS row.';

-- =============================================================================
-- Table: ATTENDANCE
-- Purpose: Hourly / daily class attendance log per student per subject.
-- 3NF status: LOG_ID is the sole key; STUDENT_ID+SUBJECT+LOG_DATE determines
--   STATUS — no transitive dependency on non-key columns.
-- =============================================================================
CREATE TABLE ATTENDANCE (
    LOG_ID        NUMBER         DEFAULT sims_seq.NEXTVAL  NOT NULL,
    STUDENT_ID    NUMBER         NOT NULL,   -- FK → STUDENT
    FACULTY_ID    NUMBER         NOT NULL,   -- FK → USERS (role=FACULTY)
    SUBJECT       VARCHAR2(80)   NOT NULL,
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
    -- Prevent duplicate log for same student + subject on same date
    CONSTRAINT UQ_ATTEND_ENTRY        UNIQUE      (STUDENT_ID, SUBJECT, LOG_DATE)
);

COMMENT ON TABLE ATTENDANCE IS 'Per-subject daily attendance record logged by faculty.';

-- =============================================================================
-- Table: MARKS
-- Purpose: Continuous assessment (CAT) scores per student per subject.
-- 3NF status: RECORD_ID is PK; TOTAL_MARKS is derived (CAT1+CAT2+CAT3) —
--   computed at the application layer (controller) to avoid transitive dependency.
-- =============================================================================
CREATE TABLE MARKS (
    RECORD_ID     NUMBER         DEFAULT sims_seq.NEXTVAL  NOT NULL,
    STUDENT_ID    NUMBER         NOT NULL,   -- FK → STUDENT
    FACULTY_ID    NUMBER         NOT NULL,   -- FK → USERS (role=FACULTY)
    SUBJECT       VARCHAR2(80)   NOT NULL,
    SEMESTER      NUMBER(2)      NOT NULL,
    CAT1_MARKS    NUMBER(5,2)    DEFAULT 0,
    CAT2_MARKS    NUMBER(5,2)    DEFAULT 0,
    CAT3_MARKS    NUMBER(5,2)    DEFAULT 0,
    -- TOTAL_MARKS intentionally omitted: computed = CAT1+CAT2+CAT3 in controller.
    -- Grade-point stored for persistence; computed from total in controller.
    GRADE_POINT   NUMBER(3,1),
    ACADEMIC_YEAR VARCHAR2(10)   NOT NULL,   -- e.g. "2025-26"

    -- ── Constraints ──────────────────────────────────────────────────────────
    CONSTRAINT PK_MARKS              PRIMARY KEY (RECORD_ID),
    CONSTRAINT FK_MARKS_STUDENT      FOREIGN KEY (STUDENT_ID)
                                         REFERENCES STUDENT(STUDENT_ID) ON DELETE CASCADE,
    CONSTRAINT FK_MARKS_FACULTY      FOREIGN KEY (FACULTY_ID)
                                         REFERENCES USERS(USER_ID),
    CONSTRAINT UQ_MARKS_ENTRY        UNIQUE      (STUDENT_ID, SUBJECT, SEMESTER, ACADEMIC_YEAR),
    CONSTRAINT CHK_MARKS_CAT1        CHECK       (CAT1_MARKS BETWEEN 0 AND 50),
    CONSTRAINT CHK_MARKS_CAT2        CHECK       (CAT2_MARKS BETWEEN 0 AND 50),
    CONSTRAINT CHK_MARKS_CAT3        CHECK       (CAT3_MARKS BETWEEN 0 AND 50),
    CONSTRAINT CHK_MARKS_GRADE_PT    CHECK       (GRADE_POINT BETWEEN 0 AND 10),
    CONSTRAINT CHK_MARKS_SEM         CHECK       (SEMESTER BETWEEN 1 AND 10)
);

COMMENT ON TABLE  MARKS             IS 'CAT marks per subject per semester. Totals computed by controller.';
COMMENT ON COLUMN MARKS.GRADE_POINT IS '10-point scale grade stored for GPA/CGPA computation.';

-- =============================================================================
-- Table: PAYMENT
-- Purpose: Fee invoice and payment transaction record.
-- 3NF status: TRANSACTION_ID is PK; AMOUNT_PAID depends only on
--   TRANSACTION_ID, not on any other non-key attribute.
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
CREATE INDEX IDX_STUDENT_USER     ON STUDENT    (USER_ID);
CREATE INDEX IDX_ATTEND_STUDENT   ON ATTENDANCE (STUDENT_ID, LOG_DATE);
CREATE INDEX IDX_MARKS_STUDENT    ON MARKS      (STUDENT_ID, SEMESTER);
CREATE INDEX IDX_PAYMENT_STUDENT  ON PAYMENT    (STUDENT_ID, PAYMENT_STATUS);
CREATE INDEX IDX_USERS_ROLE       ON USERS      (ROLE);

COMMIT;
