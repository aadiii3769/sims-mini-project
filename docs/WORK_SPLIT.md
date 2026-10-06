# WORK_SPLIT.md — Phased Execution Plan

> **Document Type**: Project Execution Plan  
> **Project**: Student Information Management System (SIMS)  
> **Total Phases**: 6 (Phase 1 through Phase 6)

---

## Agent Trigger Contract

To execute any phase, use this exact prompt:

```
Read AGENTS.md, docs/ARCHITECTURE.md, and execute Phase [X]
from docs/WORK_SPLIT.md completely.
```

Replace `[X]` with the phase number (1–6). The agent MUST:
1. Read `AGENTS.md` first (coding standards and rules).
2. Read `docs/ARCHITECTURE.md` (design overview and patterns).
3. Read the target phase section below in full.
4. Implement **every file** listed in the phase — no partial completions.
5. Verify against the phase's **Acceptance Criteria**.

---

## SRS Feature Catalog (Baseline Requirements)

| Module | Feature | Roles |
|---|---|---|
| 1 | User Authentication & Role Selection | All |
| 2 | Admission Management & Student Profiles | Admin |
| 3 | Attendance Logging & Monthly Summaries | Faculty, Student, Parent |
| 4 | Internal Marks Evaluation & GPA/CGPA | Faculty, Student, Parent |
| 5 | Online Fee Invoicing & Payment Processing | Admin, Student |
| 6 | Transcript Generation & Academic Record Archival | Student, Admin |

---

## Phase 1 — User Authentication & Role-Based Dashboard Routing

### Objective & Scope
Implement the login screen, password validation, and role-based dashboard routing. This is the foundation all other phases build upon.

### Files to Create / Modify

| Action | File Path |
|---|---|
| CREATE | `src/main/java/com/sims/view/LoginFrame.java` |
| CREATE | `src/main/java/com/sims/view/SplashScreen.java` |
| MODIFY | `src/main/java/com/sims/Main.java` (wire LoginFrame) |
| MODIFY | `src/main/java/com/sims/controller/LoginController.java` (BCrypt upgrade) |

### Step-by-Step Implementation Flow

1. **Model**: `User`, `UserRole` — already complete (Phase 0). No changes needed.

2. **DAO**: `UserDAO.findByUsername()` — already complete (Phase 0). No changes needed.

3. **Controller — BCrypt upgrade** (`LoginController.java`):
   - Add `org.mindrot:jbcrypt` dependency to `pom.xml`.
   - Replace plain-text comparison with `BCrypt.checkpw(password, user.getPasswordHash())`.
   - Update `db/seed_data.sql` to insert BCrypt hashes (use `BCrypt.hashpw("admin@123", BCrypt.gensalt())`).

4. **View — `SplashScreen.java`**:
   - `JWindow` (no title bar) displayed for 2 seconds on startup.
   - Shows project name, institution name, and loading label.
   - Use `javax.swing.Timer` (not `Thread.sleep`) to dismiss.

5. **View — `LoginFrame.java`**:
   - `JFrame` with `GridBagLayout` form: Username field, Password field (`JPasswordField`), Login button.
   - On button click (ActionListener / Observer): call `loginController.loginAndOpenDashboard(user, pass)`.
   - On `SecurityException`: display `JOptionPane.showMessageDialog(this, "Invalid credentials", "Login Failed", JOptionPane.ERROR_MESSAGE)`.
   - On `IllegalArgumentException` (empty fields): highlight the empty field border in red using `BorderFactory.createLineBorder(Color.RED)`.
   - On `RuntimeException` (DB down): show specific "Database connection failed — is Docker running?" message.
   - Close this frame after successful login: `this.dispose()`.

6. **Main.java update**:
   - Show `SplashScreen` first.
   - After splash timer fires, show `LoginFrame` on EDT.

### Verification & Acceptance Criteria

**Terminal checks**:
```bash
# Verify DB is up
docker exec sims_oracle_db sqlplus system/admin@FREEPDB1 -s <<< "SELECT USERNAME, ROLE FROM USERS;"

# Compile
mvn compile

# Run
mvn exec:java
```

**Manual GUI checks**:
- [ ] Splash screen appears for ~2 seconds with no title bar.
- [ ] Login form appears with Username, Password, and Login button.
- [ ] `admin01` / `admin@123` → opens Admin stub dashboard.
- [ ] `faculty01` / `faculty@123` → opens Faculty stub dashboard.
- [ ] `student01` / `student@123` → opens Student stub dashboard.
- [ ] `parent01` / `parent@123` → opens Parent stub dashboard.
- [ ] Wrong password → "Invalid credentials" dialog.
- [ ] Empty username → red border on username field.

### Definition of Done
All manual GUI checks pass. `mvn compile` exits with code 0.

---

## Phase 2 — Admission Management & Student Profiles (Admin Module)

### Objective & Scope
Build the Admin Dashboard with student registration, profile editing, and search.

### Files to Create / Modify

| Action | File Path |
|---|---|
| CREATE | `src/main/java/com/sims/view/admin/AdminDashboard.java` |
| CREATE | `src/main/java/com/sims/view/admin/StudentListPanel.java` |
| CREATE | `src/main/java/com/sims/view/admin/StudentFormDialog.java` |
| CREATE | `src/main/java/com/sims/controller/StudentController.java` |
| MODIFY | `src/main/java/com/sims/factory/DashboardFactory.java` (replace stub) |

### Step-by-Step Implementation Flow

1. **Model**: `User`, `Student` — already complete. No changes.

2. **DAO**: `UserDAO`, `StudentDAO` — already complete. No changes.

3. **Controller — `StudentController.java`**:
   - `registerStudent(User newUser, Student newStudent)`: inserts USERS row, then STUDENT row, then `conn.commit()`. On any exception → `conn.rollback()`.
   - `updateStudentProfile(Student student)`: calls `studentDAO.update()`, then `conn.commit()`.
   - `searchStudents(String keyword)`: returns filtered `List<Student>` from `studentDAO.findAll()`.
   - `deactivateStudent(long userId)`: calls `userDAO.deactivate()`, then `conn.commit()`.

4. **View — `AdminDashboard.java`**:
   - `JFrame` with `JTabbedPane` containing: "Student Management" tab (more tabs added in later phases).
   - Set `JFrame.setDefaultCloseOperation(EXIT_ON_CLOSE)` with a shutdown hook to call `DBConnection.getInstance().close()`.

5. **View — `StudentListPanel.java`**:
   - `JPanel` with a `JTable` inside `JScrollPane`.
   - `DefaultTableModel` columns: Roll No, Name, Department, Year, Section, Email.
   - Top toolbar: `JTextField` (search), Search button, Add New Student button.
   - Table row double-click → opens `StudentFormDialog` in EDIT mode.
   - "Add New Student" → opens `StudentFormDialog` in CREATE mode.
   - Table populated via `SwingWorker` (never query DB on EDT).

6. **View — `StudentFormDialog.java`**:
   - `JDialog` (modal) with `GridBagLayout` form.
   - Fields: Full Name, Username, Email, Phone, Roll Number, Department (JComboBox), Year (JSpinner 1-5), Section.
   - In CREATE mode: show Password field.
   - In EDIT mode: hide Password field; pre-populate all other fields.
   - Save button → calls `studentController.registerStudent()` or `studentController.updateStudentProfile()`.
   - Input validation: all required fields non-empty; email format check (regex); roll number format check.

### Verification & Acceptance Criteria

**Terminal**:
```bash
mvn compile && mvn exec:java
```

**Manual GUI checks**:
- [ ] Login as `admin01` → Admin Dashboard opens with "Student Management" tab.
- [ ] Student list loads showing 3 seed students.
- [ ] Search "Arun" → filters to show only Arun Kumar.
- [ ] Click "Add New Student" → form dialog opens.
- [ ] Fill form and save → new student appears in table; verify in DB:
  ```sql
  SELECT ROLL_NUMBER, DEPARTMENT FROM STUDENT ORDER BY STUDENT_ID DESC FETCH FIRST 1 ROW ONLY;
  ```
- [ ] Double-click existing student → form pre-populated; edit department; save → DB updated.
- [ ] Empty required field → validation error shown inline (red border).

### Definition of Done
CRUD for students works end-to-end; DB rows persist after application restart.

---

## Phase 3 — Attendance Logging & Monthly Summary (Faculty + Student/Parent Module)

### Objective & Scope
Faculty records daily class attendance; students and parents view attendance history and monthly percentage.

### Files to Create / Modify

| Action | File Path |
|---|---|
| CREATE | `src/main/java/com/sims/dao/AttendanceDAO.java` |
| CREATE | `src/main/java/com/sims/controller/AttendanceController.java` |
| CREATE | `src/main/java/com/sims/view/faculty/FacultyDashboard.java` |
| CREATE | `src/main/java/com/sims/view/faculty/AttendanceEntryPanel.java` |
| CREATE | `src/main/java/com/sims/view/student/AttendanceViewPanel.java` |
| MODIFY | `src/main/java/com/sims/factory/DashboardFactory.java` (replace stubs) |

### Step-by-Step Implementation Flow

1. **DAO — `AttendanceDAO.java`**:
   - `insertOrUpdate(Attendance a)`: use Oracle `MERGE INTO` statement (upsert) keyed on `(STUDENT_ID, SUBJECT, LOG_DATE)`.
   - `findByStudentAndSubject(long studentId, String subject)`: returns `List<Attendance>` ordered by `LOG_DATE DESC`.
   - `getMonthlySummary(long studentId, String subject, int month, int year)`: aggregate query returning present/absent/total counts.
   - All using `PreparedStatement` + `try-with-resources`.

2. **Controller — `AttendanceController.java`**:
   - `markAttendance(List<Attendance> records)`: loops insertOrUpdate for each record, then commits once.
   - `getAttendanceHistory(long studentId, String subject)`: delegates to DAO, returns list.
   - `getMonthlySummary(long studentId, String subject, int month, int year)`: delegates to DAO.
   - `computeAttendancePercentage(int present, int total)`: `(present * 100.0) / total` — computed in Java, not SQL.

3. **View — `AttendanceEntryPanel.java`** (Faculty):
   - `JPanel` with: Subject `JComboBox`, Date `JSpinner` (SpinnerDateModel), student `JTable` with STATUS column as `JComboBox` cell editor (`PRESENT/ABSENT/OD/MEDICAL`).
   - "Save Attendance" button → collects all rows → `attendanceController.markAttendance(records)`.

4. **View — `AttendanceViewPanel.java`** (Student/Parent):
   - Monthly summary: attendance % per subject displayed in `JTable` with colour coding (< 75% → red).
   - Full log table below with filter by subject.

### Verification & Acceptance Criteria

**Manual GUI checks**:
- [ ] Login as `faculty01` → Faculty Dashboard with Attendance Entry tab.
- [ ] Select subject, date → student list loads.
- [ ] Mark attendance, save → verify in DB: `SELECT STATUS FROM ATTENDANCE WHERE LOG_DATE = SYSDATE;`
- [ ] Login as `student01` → Attendance view shows history.
- [ ] Monthly summary correctly shows percentage (e.g., 4 present / 5 total = 80%).
- [ ] Student with < 75% attendance shown in red.

### Definition of Done
Attendance log persists. Monthly percentages compute correctly.

---

## Phase 4 — Internal Marks, GPA/CGPA Tabulation (Faculty + Student Module)

### Objective & Scope
Faculty enters CAT marks; system computes totals, grade points, GPA per semester, and cumulative CGPA.

### Files to Create / Modify

| Action | File Path |
|---|---|
| CREATE | `src/main/java/com/sims/controller/MarksController.java` |
| CREATE | `src/main/java/com/sims/view/faculty/MarksEntryPanel.java` |
| CREATE | `src/main/java/com/sims/view/student/MarksViewPanel.java` |
| MODIFY | `src/main/java/com/sims/view/admin/AdminDashboard.java` (add Marks tab) |

### Step-by-Step Implementation Flow

1. **DAO**: `MarksDAO` — already complete (Phase 0). No changes needed.

2. **Controller — `MarksController.java`**:
   - `saveMarks(Mark mark)`: computes `totalMarks = CAT1+CAT2+CAT3`; derives `gradePoint` using scale:
     ```
     ≥ 91 → 10.0 (O)
     81–90 → 9.0 (A+)
     71–80 → 8.0 (A)
     61–70 → 7.0 (B+)
     51–60 → 6.0 (B)
     < 50  → 0.0 (F)
     ```
   - Calls `marksDAO.insert()` or `marksDAO.update()` (check by RECORD_ID), then commits.
   - `getSemesterGPA(long studentId, int semester)`: delegates to `marksDAO.computeGPA()`.
   - `getCGPA(long studentId)`: delegates to `marksDAO.computeCGPA()`.
   - `getLetterGrade(double gradePoint)`: pure utility method, no DB call.

3. **View — `MarksEntryPanel.java`** (Faculty):
   - `JPanel` with: Semester `JComboBox`, Subject `JTextField`, Student search, CAT1/CAT2/CAT3 `JSpinner` (0–50).
   - Live total preview label: updates as spinners change (no DB call — pure Java arithmetic).
   - "Save" button → `marksController.saveMarks(mark)`.

4. **View — `MarksViewPanel.java`** (Student):
   - Per-semester `JTable`: Subject | CAT1 | CAT2 | CAT3 | Total | Grade | Grade Point.
   - Footer: "Semester GPA: X.X" and "CGPA: X.X".
   - Failing rows (`gradePoint = 0`) highlighted in red.

### Verification & Acceptance Criteria

**Manual GUI checks**:
- [ ] Login as `faculty01` → enter CAT marks for a student → save.
- [ ] Verify DB: `SELECT CAT1_MARKS, GRADE_POINT FROM MARKS WHERE STUDENT_ID = 101;`
- [ ] Login as `student01` → Marks tab shows CAT scores, total, and grade.
- [ ] GPA shown matches `SELECT AVG(GRADE_POINT) FROM MARKS WHERE STUDENT_ID=101 AND SEMESTER=5;`
- [ ] CGPA shown matches `SELECT AVG(GRADE_POINT) FROM MARKS WHERE STUDENT_ID=101;`

### Definition of Done
Grade computation logic verified against scale. GPA/CGPA match DB aggregates.

---

## Phase 5 — Fee Invoicing & Payment Processing (Admin + Student Module)

### Objective & Scope
Administrators create fee invoices; students make payments; system updates status and generates receipt IDs.

### Files to Create / Modify

| Action | File Path |
|---|---|
| CREATE | `src/main/java/com/sims/controller/PaymentController.java` |
| CREATE | `src/main/java/com/sims/view/admin/FeeManagementPanel.java` |
| CREATE | `src/main/java/com/sims/view/student/PaymentPanel.java` |
| CREATE | `src/main/java/com/sims/view/student/ReceiptDialog.java` |

### Step-by-Step Implementation Flow

1. **DAO**: `PaymentDAO` — already complete (Phase 0). No changes.

2. **Controller — `PaymentController.java`**:
   - `createInvoice(Payment invoice)`: validates `amountDue > 0`, calls `paymentDAO.insert()`, commits.
   - `processPayment(long transactionId, BigDecimal amountPaid, String mode)`:
     - Validates `amountPaid > 0`.
     - Generates receipt number: `"RCPT-" + year + "-" + String.format("%06d", transactionId)`.
     - Calls `paymentDAO.recordPayment(transactionId, amountPaid, amountDue, mode, receiptNo)`, commits.
     - Returns generated receipt number.
   - `getStudentFees(long studentId)`: delegates to `paymentDAO.findByStudent()`.
   - `getTotalOutstanding(long studentId)`: sums `getOutstandingBalance()` across all PENDING/PARTIAL records.

3. **View — `FeeManagementPanel.java`** (Admin):
   - Student search; select student; view existing invoices in `JTable`.
   - "Create Invoice" button → dialog with FeeType `JComboBox`, AmountDue `JTextField`, DueDate `JSpinner`.

4. **View — `PaymentPanel.java`** (Student):
   - `JTable` showing all fee records (type, due, paid, outstanding, status, due date).
   - PENDING/PARTIAL rows: "Pay Now" button in a custom `TableCellRenderer/Editor`.
   - Click "Pay Now" → prompts for amount and mode via `JOptionPane` → calls `paymentController.processPayment()`.
   - On success → refresh table → open `ReceiptDialog`.

5. **View — `ReceiptDialog.java`**:
   - Modal `JDialog` displaying receipt details in a clean formatted layout.
   - "Print Receipt" button → `java.awt.Desktop.getPrinter()` or `JTextArea` print simulation.

### Verification & Acceptance Criteria

**Manual GUI checks**:
- [ ] Login as `admin01` → create a HOSTEL fee invoice for student01 for ₹15,000.
- [ ] Login as `student01` → Payment tab shows new PENDING invoice.
- [ ] Click "Pay Now" → enter ₹15,000 / ONLINE → status becomes PAID.
- [ ] Receipt dialog shows receipt number matching DB.
- [ ] Verify: `SELECT PAYMENT_STATUS, RECEIPT_NUMBER FROM PAYMENT WHERE STUDENT_ID=101;`

### Definition of Done
Fee lifecycle (PENDING → PAID) works end-to-end. Receipt number persists in DB.

---

## Phase 6 — Transcript Generation & Academic Record Archival (Student + Admin Module)

### Objective & Scope
Generate a complete academic transcript (all semesters, all subjects, GPA per semester, CGPA) viewable in-app and printable.

### Files to Create / Modify

| Action | File Path |
|---|---|
| CREATE | `src/main/java/com/sims/view/student/TranscriptPanel.java` |
| CREATE | `src/main/java/com/sims/view/admin/TranscriptAdminPanel.java` |
| CREATE | `src/main/java/com/sims/controller/TranscriptController.java` |

### Step-by-Step Implementation Flow

1. **Controller — `TranscriptController.java`**:
   - `generateTranscript(long studentId)`: calls `marksDAO.findAllByStudent()`, groups by semester using `Map<Integer, List<Mark>>`.
   - `computeSemesterSummary(List<Mark> semMarks)`: returns `Map<String, Object>` with GPA, total credits, pass/fail count.
   - `exportTranscriptText(long studentId)`: generates a formatted `String` suitable for `JTextArea` or print.

2. **View — `TranscriptPanel.java`** (Student):
   - Semester-wise `JTable` sections separated by semester headers.
   - Footer summary: per-semester GPA column + overall CGPA row.
   - "Download as Text" button → saves to file via `JFileChooser`.
   - "Print" button → `java.awt.print.PrinterJob`.

3. **View — `TranscriptAdminPanel.java`** (Admin):
   - Student search → select → "View Transcript" → opens `TranscriptPanel` in a modal dialog.
   - "Print All" batch option for examiners.

### Verification & Acceptance Criteria

**Manual GUI checks**:
- [ ] Login as `student01` → Transcript tab shows Semester 5 marks with GPA.
- [ ] CGPA displayed matches `SELECT AVG(GRADE_POINT) FROM MARKS WHERE STUDENT_ID=101;`
- [ ] "Download as Text" → file saved to chosen path.
- [ ] Login as `admin01` → search student01 → view transcript → correct data displayed.

### Definition of Done
Transcript accurately reflects all DB records. Download generates a non-empty file. CGPA verified against DB aggregate.
