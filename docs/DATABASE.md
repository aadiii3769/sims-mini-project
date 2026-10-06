# DATABASE.md — Relational Schema & Normalization Proof

> **Document Type**: System Design — Database Section  
> **Standard**: Relational Algebra / Normal Forms (Codd's 1NF–3NF)  
> **Project**: Student Information Management System (SIMS)  
> **Lab**: UIT3311 (Database Technology Laboratory)

---

## 1. Entity-Relationship Overview

```
USERS (1) ─────────────────────────────── (1) STUDENT
   │                                             │
   │ (plays role FACULTY)                        │ (1)
   │                                             │
   ├─── (∞) ATTENDANCE ◄──────────────── (∞) ──┘
   │
   └─── (∞) MARKS ◄────────────────────── (∞) STUDENT
                                                 │
                                      (∞) PAYMENT
```

### ER Relationships

| Relationship | Cardinality | FK Column | On Delete |
|---|---|---|---|
| USERS → STUDENT | One-to-One | `STUDENT.USER_ID` | CASCADE |
| STUDENT → ATTENDANCE | One-to-Many | `ATTENDANCE.STUDENT_ID` | CASCADE |
| USERS (Faculty) → ATTENDANCE | One-to-Many | `ATTENDANCE.FACULTY_ID` | RESTRICT |
| STUDENT → MARKS | One-to-Many | `MARKS.STUDENT_ID` | CASCADE |
| USERS (Faculty) → MARKS | One-to-Many | `MARKS.FACULTY_ID` | RESTRICT |
| STUDENT → PAYMENT | One-to-Many | `PAYMENT.STUDENT_ID` | CASCADE |
| USERS (Parent) → STUDENT | One-to-Many | `STUDENT.PARENT_USER_ID` | SET NULL |

---

## 2. Table Schemas

### 2.1 USERS

| Column | Type | Constraint | Notes |
|---|---|---|---|
| USER_ID | NUMBER | PK, NOT NULL | Sequence-generated surrogate key |
| USERNAME | VARCHAR2(50) | UNIQUE, NOT NULL | Login identifier |
| PASSWORD_HASH | VARCHAR2(255) | NOT NULL | BCrypt hash (plain-text in seed only) |
| FULL_NAME | VARCHAR2(100) | NOT NULL | Display name |
| EMAIL | VARCHAR2(100) | UNIQUE, NOT NULL | Contact + notification |
| PHONE | VARCHAR2(15) | — | Optional contact |
| ROLE | VARCHAR2(10) | CHECK IN ('ADMIN','FACULTY','STUDENT','PARENT') | RBAC role |
| IS_ACTIVE | NUMBER(1) | DEFAULT 1, CHECK IN (0,1) | Soft-delete flag |
| CREATED_AT | TIMESTAMP | DEFAULT SYSTIMESTAMP | Audit trail |

### 2.2 STUDENT

| Column | Type | Constraint | Notes |
|---|---|---|---|
| STUDENT_ID | NUMBER | PK, NOT NULL | Sequence-generated |
| USER_ID | NUMBER | FK → USERS, UNIQUE | Links to login credentials |
| ROLL_NUMBER | VARCHAR2(20) | UNIQUE, NOT NULL | Institution roll no. |
| DEPARTMENT | VARCHAR2(80) | NOT NULL | e.g. "Computer Science and Engineering" |
| YEAR | NUMBER(1) | CHECK 1-5 | Academic year of study |
| SECTION | VARCHAR2(5) | — | Class section |
| DATE_OF_BIRTH | DATE | — | Optional for profile |
| ADDRESS | VARCHAR2(300) | — | Permanent address |
| PARENT_USER_ID | NUMBER | FK → USERS (nullable) | Optional parent link |

### 2.3 ATTENDANCE

| Column | Type | Constraint | Notes |
|---|---|---|---|
| LOG_ID | NUMBER | PK, NOT NULL | Sequence-generated |
| STUDENT_ID | NUMBER | FK → STUDENT | Which student |
| FACULTY_ID | NUMBER | FK → USERS | Who marked attendance |
| SUBJECT | VARCHAR2(80) | NOT NULL | Subject name + code |
| LOG_DATE | DATE | NOT NULL | Date of the class |
| STATUS | VARCHAR2(10) | CHECK IN ('PRESENT','ABSENT','OD','MEDICAL') | Attendance status |
| REMARKS | VARCHAR2(200) | — | Optional faculty note |
| — | — | UNIQUE(STUDENT_ID, SUBJECT, LOG_DATE) | Prevent duplicate entries |

### 2.4 MARKS

| Column | Type | Constraint | Notes |
|---|---|---|---|
| RECORD_ID | NUMBER | PK, NOT NULL | Sequence-generated |
| STUDENT_ID | NUMBER | FK → STUDENT | Whose marks |
| FACULTY_ID | NUMBER | FK → USERS | Who entered marks |
| SUBJECT | VARCHAR2(80) | NOT NULL | Subject name + code |
| SEMESTER | NUMBER(2) | CHECK 1-10 | Academic semester |
| CAT1_MARKS | NUMBER(5,2) | CHECK 0-50 | First assessment |
| CAT2_MARKS | NUMBER(5,2) | CHECK 0-50 | Second assessment |
| CAT3_MARKS | NUMBER(5,2) | CHECK 0-50 | Third assessment |
| GRADE_POINT | NUMBER(3,1) | CHECK 0-10 | 10-point GPA scale |
| ACADEMIC_YEAR | VARCHAR2(10) | NOT NULL | e.g. "2025-26" |
| — | — | UNIQUE(STUDENT_ID, SUBJECT, SEMESTER, ACADEMIC_YEAR) | Prevent duplicate |

> **Note**: `TOTAL_MARKS` is intentionally **excluded** from the table.
> It is a transitive dependency (`TOTAL = CAT1 + CAT2 + CAT3`) and storing it
> would violate 3NF. The value is computed in `MarksDAO.mapRow()`.

### 2.5 PAYMENT

| Column | Type | Constraint | Notes |
|---|---|---|---|
| TRANSACTION_ID | NUMBER | PK, NOT NULL | Sequence-generated |
| STUDENT_ID | NUMBER | FK → STUDENT | Which student |
| FEE_TYPE | VARCHAR2(50) | CHECK IN ('TUITION','HOSTEL','EXAM','BUS','OTHER') | Fee category |
| AMOUNT_DUE | NUMBER(10,2) | CHECK >= 0 | Total fee owed |
| AMOUNT_PAID | NUMBER(10,2) | DEFAULT 0, CHECK >= 0 | Amount received |
| PAYMENT_STATUS | VARCHAR2(10) | CHECK IN ('PAID','PENDING','PARTIAL','WAIVED') | Current status |
| PAYMENT_DATE | DATE | — | Nullable until payment made |
| DUE_DATE | DATE | NOT NULL | Payment deadline |
| PAYMENT_MODE | VARCHAR2(20) | CHECK IN ('ONLINE','CASH','DD','WAIVER') or NULL | How paid |
| RECEIPT_NUMBER | VARCHAR2(30) | — | System-generated receipt ID |
| REMARKS | VARCHAR2(200) | — | Admin notes |

> **Note**: `OUTSTANDING_BALANCE` (`AMOUNT_DUE - AMOUNT_PAID`) is computed
> at runtime in `Payment.getOutstandingBalance()` — not stored, to avoid 3NF violation.

---

## 3. Formal Normalization — Academic Proof (UNF → 1NF → 2NF → 3NF)

---

### 3.1 Normalization of STUDENT Data

#### 3.1.1 Unnormalized Form (UNF)

Imagine an initial flat spreadsheet capturing student data:

```
StudentRecord(StudentID, RollNo, StudentName, Email, Phone, Password,
              Dept, Year, Section, ParentName, ParentEmail, ParentPhone,
              Subject1, CAT1_S1, CAT2_S1, CAT3_S1,
              Subject2, CAT1_S2, CAT2_S2, CAT3_S2, ...)
```

**Problems**: Repeating groups for subjects/marks; multi-valued cells.

#### 3.1.2 First Normal Form (1NF)

**Rule**: Every cell must contain an atomic (indivisible) value; no repeating groups.

**Applied transformation**: Separate student identity from repeating subject/mark groups.

```
STUDENT_FLAT(StudentID, RollNo, StudentName, Email, Phone, Password,
             Dept, Year, Section, ParentName, ParentEmail, ParentPhone)
```

Repeating subject marks moved to a separate flat table:
```
MARKS_FLAT(StudentID, Subject, CAT1, CAT2, CAT3, Semester, AcadYear)
```

**1NF achieved**: All columns atomic; no repeating groups.

#### 3.1.3 Second Normal Form (2NF)

**Rule**: Must be in 1NF, and every non-key attribute must depend on the **whole** primary key (no partial dependencies). Applies only when the PK is composite.

**Analysis of `STUDENT_FLAT`**: PK = `{StudentID}` (single-column) → 2NF is automatically satisfied.

**Analysis of `MARKS_FLAT`**: PK = `{StudentID, Subject, Semester, AcadYear}` (composite).
- `CAT1`, `CAT2`, `CAT3` depend on the full composite key ✅
- No partial dependencies identified.

**2NF achieved** for both tables.

#### 3.1.4 Third Normal Form (3NF)

**Rule**: Must be in 2NF, and no non-key attribute may **transitively** depend on the PK (i.e., no non-key → non-key dependency).

**Issue found in `STUDENT_FLAT`**:
- `StudentID → StudentName, Email, Phone, Password, Role` (authentication data)
- `StudentID → ParentName, ParentEmail, ParentPhone` (parent data)
- `ParentName → ParentEmail, ParentPhone` — **transitive dependency** through a non-key attribute

**Resolution**: Decompose into separate tables, each focused on one entity:

```
USERS(USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE, IS_ACTIVE)
STUDENT(STUDENT_ID, USER_ID [FK], ROLL_NUMBER, DEPT, YEAR, SECTION, DOB, ADDRESS, PARENT_USER_ID [FK])
```

Parent data is a separate USERS row with `ROLE='PARENT'`, linked by `STUDENT.PARENT_USER_ID`.

**3NF achieved**: No transitive dependencies remain in STUDENT.

---

### 3.2 Normalization of MARKS

#### UNF Issue
```
MARKS_RAW(StudentID, StudentName, Subject, CAT1, CAT2, CAT3, TotalMarks, GradePoint, AcadYear)
```
`TotalMarks` = `CAT1 + CAT2 + CAT3` — **transitive/derived dependency** on other non-key columns.

#### 3NF Resolution
`TOTAL_MARKS` excluded from the table. It is computed in `MarksDAO.mapRow()`:
```java
m.setTotalMarks(rs.getDouble("CAT1_MARKS") + rs.getDouble("CAT2_MARKS") + rs.getDouble("CAT3_MARKS"));
```
**3NF achieved**: No derived attribute stored in the database.

---

### 3.3 Normalization of PAYMENT

#### UNF Issue
```
PAYMENT_RAW(TransID, StudentID, StudentName, FeeType, AmtDue, AmtPaid,
            OutstandingBalance, Status, ReceiptNo, ...)
```
`OutstandingBalance` = `AmtDue - AmtPaid` — derived from two other columns (transitive via arithmetic).

#### 3NF Resolution
`OUTSTANDING_BALANCE` excluded. Computed at runtime: `Payment.getOutstandingBalance()`.
**3NF achieved**.

---

### 3.4 Normalization of ATTENDANCE

#### UNF Issue
```
ATTEND_RAW(LogID, StudentID, StudentName, Subject, Date, Status, FacultyName, ...)
```
`FacultyName` depends on `FACULTY_ID`, not on `LOG_ID` — **transitive dependency**.

#### 3NF Resolution
`FacultyName` excluded from ATTENDANCE. Obtained via JOIN in DAO query:
```sql
JOIN USERS u ON ATTENDANCE.FACULTY_ID = u.USER_ID
```
**3NF achieved**: ATTENDANCE stores only foreign keys to USERS and STUDENT; names retrieved by join.

---

## 4. Normalization Summary Table

| Table | Issue in UNF | 1NF Fix | 2NF Status | 3NF Fix |
|---|---|---|---|---|
| USERS | Mixed parent + student data | Atomic columns; one row per user | Automatic (single PK) | Split parent into separate USERS row |
| STUDENT | Parent name/email embedded | Separate table | Automatic (single PK) | FK to USERS for parent |
| MARKS | TotalMarks stored | Repeating groups removed | Full composite PK | TotalMarks removed; computed in DAO |
| PAYMENT | OutstandingBalance stored | Atomic columns | Automatic (single PK) | OutstandingBalance removed; computed in model |
| ATTENDANCE | FacultyName embedded | Atomic columns | Full composite key satisfied | FacultyName removed; retrieved via JOIN |

---

## 5. Indexes

| Index | Table | Columns | Purpose |
|---|---|---|---|
| IDX_STUDENT_USER | STUDENT | USER_ID | Fast user→student lookup at login |
| IDX_ATTEND_STUDENT | ATTENDANCE | STUDENT_ID, LOG_DATE | Monthly summary queries |
| IDX_MARKS_STUDENT | MARKS | STUDENT_ID, SEMESTER | Semester transcript generation |
| IDX_PAYMENT_STUDENT | PAYMENT | STUDENT_ID, PAYMENT_STATUS | Fee dashboard filtering |
| IDX_USERS_ROLE | USERS | ROLE | Admin user management list |
