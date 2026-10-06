# ARCHITECTURE.md — System Design Reference

> **Document Type**: System Design — Section 2  
> **Project**: Student Information Management System (SIMS)  
> **Pattern**: Desktop MVC / DAO  
> **Lab**: UIT3361 · UIT3311

---

## 1. Architectural Overview

SIMS follows a **layered desktop MVC (Model-View-Controller) architecture**
augmented by the **DAO (Data Access Object) structural pattern**. The layers
enforce a strict one-way dependency chain:

```
View  →  Controller  →  DAO  →  Database
                  ↘    ↗
                 Model (POJOs)
```

No layer may import from a layer above it.  Specifically:
- `dao.*` must never import `view.*` or `controller.*`
- `model.*` must never import any other `com.sims.*` package
- `view.*` communicates only through `controller.*`; it never calls `dao.*` directly

---

## 2. Layer-by-Layer Breakdown

### 2.1 Model Layer — `com.sims.model`

Plain Java Objects (POJOs) that mirror database table rows 1-to-1.
No SQL, no Swing imports — pure data carriers.

| Class | Maps To | Key Fields |
|---|---|---|
| `User` | `USERS` | userId, username, passwordHash, role (UserRole enum) |
| `Student` | `STUDENT` | studentId, rollNumber, department, year, section |
| `Mark` | `MARKS` | recordId, cat1/2/3Marks, gradePoint; totalMarks computed |
| `Payment` | `PAYMENT` | transactionId, amountDue, amountPaid, paymentStatus |
| `Attendance` | `ATTENDANCE` | logId, studentId, subject, logDate, status |
| `UserRole` | — | Enum: ADMIN, FACULTY, STUDENT, PARENT |

### 2.2 DAO Layer — `com.sims.dao`

Encapsulates all JDBC SQL.  Each DAO class manages one primary table.

```
com.sims.dao
├── UserDAO.java       ← findByUsername(), insert(), deactivate()
├── StudentDAO.java    ← findAll(), findByUserId(), findByRollNumber(), insert(), update()
├── MarksDAO.java      ← findByStudentAndSemester(), insert(), update(), computeGPA/CGPA()
├── PaymentDAO.java    ← findByStudent(), insert(), recordPayment()
└── AttendanceDAO.java ← (Phase 3) findByStudentSubject(), insert(), monthlySummary()
```

**Rules enforced in every DAO**:
1. All queries use `PreparedStatement` (never `Statement`).
2. `PreparedStatement` and `ResultSet` opened in `try-with-resources`.
3. DML methods do NOT commit — transaction control belongs to the controller.

### 2.3 Utility Layer — `com.sims.util`

```
com.sims.util
└── DBConnection.java  ← Singleton JDBC connection manager
```

### 2.4 Factory Layer — `com.sims.factory`

```
com.sims.factory
└── DashboardFactory.java  ← createDashboard(User) → JFrame
```

### 2.5 Controller Layer — `com.sims.controller`

Business logic, input validation, and transaction demarcation.

```
com.sims.controller
├── LoginController.java       ← authenticate(), loginAndOpenDashboard()
├── StudentController.java     ← (Phase 2) registerStudent(), updateProfile()
├── AttendanceController.java  ← (Phase 3) logAttendance(), getMonthlySummary()
├── MarksController.java       ← (Phase 4) saveMarks(), computeGPA(), computeCGPA()
└── PaymentController.java     ← (Phase 5) createInvoice(), processPayment(), generateReceipt()
```

### 2.6 View Layer — `com.sims.view`

All Swing components live here.  Views are thin — they build UI, wire
ActionListeners to controllers, and display returned data.  No SQL allowed.

```
com.sims.view
├── LoginFrame.java          ← (Phase 1) Login form + role routing
├── admin/
│   ├── AdminDashboard.java  ← (Phase 2) Tabbed pane for admin modules
│   └── StudentFormDialog.java
├── faculty/
│   ├── FacultyDashboard.java
│   ├── AttendancePanel.java
│   └── MarksEntryPanel.java
├── student/
│   ├── StudentDashboard.java
│   ├── AttendanceViewPanel.java
│   ├── MarksViewPanel.java
│   └── PaymentPanel.java
└── parent/
    └── ParentDashboard.java
```

---

## 3. Data Flow Diagram — Login Sequence

```
User types credentials
        │
        ▼
   LoginFrame (view)
  [ActionListener fires]  ←── Observer Pattern
        │  calls
        ▼
  LoginController.authenticate(username, password)
        │  calls
        ▼
  UserDAO.findByUsername(username)          [PreparedStatement]
        │  returns Optional<User>
        ▼
  LoginController validates password hash
        │  on success, calls
        ▼
  DashboardFactory.createDashboard(user)    [Factory Method]
        │  returns role-specific JFrame
        ▼
  SwingUtilities.invokeLater(dashboard::setVisible)
```

---

## 4. Design Patterns — Full Academic Documentation

### 4.1 Creational Pattern: Singleton

**Class**: `com.sims.util.DBConnection`

**Intent**: Ensure a class has only one instance and provide a global access point to it.

**Academic Justification**: Opening a new Oracle JDBC connection for every data access operation creates significant overhead — TCP socket establishment, Oracle session allocation, and authentication handshake. In a single-user desktop application, a single shared `Connection` object is both sufficient and optimal. The Singleton pattern guarantees exactly one `DBConnection` instance exists per JVM lifecycle, accessed via `DBConnection.getInstance()`.

**Implementation**: Double-Checked Locking (DCL) with `volatile` field for JDK 5+ memory model compliance.

```
DBConnection (Singleton)
┌─────────────────────────────────────┐
│ - instance: volatile DBConnection   │
│ - connection: Connection            │
├─────────────────────────────────────┤
│ + getInstance(): DBConnection       │  ← synchronized, DCL
│ + getConnection(): Connection       │
│ + reconnect(): void                 │
│ + close(): void                     │
└─────────────────────────────────────┘
```

---

### 4.2 Creational Pattern: Factory Method

**Class**: `com.sims.factory.DashboardFactory`

**Intent**: Define an interface for creating an object, but let subclasses decide which class to instantiate. Here, the "decision" is based on UserRole.

**Academic Justification**: Role-Based Access Control mandates four distinct dashboard experiences. Without a factory, the post-login code would contain a deeply nested `if-else` chain tightly coupling `LoginController` to every dashboard class (`AdminDashboard`, `FacultyDashboard`, etc.). The Factory Method pattern encapsulates this conditional creation behind `createDashboard(User)`, allowing new roles to be added by modifying only the factory — not the login logic (Open/Closed Principle).

```
DashboardFactory
┌──────────────────────────────────────────────────┐
│ + createDashboard(user: User): JFrame            │
│   └── switch(user.getRole())                     │
│         ADMIN   → AdminDashboard(user)           │
│         FACULTY → FacultyDashboard(user)         │
│         STUDENT → StudentDashboard(user)         │
│         PARENT  → ParentDashboard(user)          │
└──────────────────────────────────────────────────┘
```

---

### 4.3 Structural Pattern: DAO (Data Access Object)

**Classes**: `com.sims.dao.UserDAO`, `StudentDAO`, `MarksDAO`, `PaymentDAO`, `AttendanceDAO`

**Intent**: Abstract and encapsulate all access to a data source. The DAO manages the connection with the data source to obtain and store data.

**Academic Justification**: Without DAOs, SQL `PreparedStatement` code would be scattered across controller and view classes, creating tight coupling between UI logic and database schemas. The DAO pattern creates a clean separation boundary: controllers work with Java objects (`Student`, `Mark`), never with `ResultSet` or `PreparedStatement`. This enables the SQL to be changed (e.g., table renamed) without modifying any controller or view.

```
«interface» (conceptual)          Concrete DAO Classes
DataAccessObject                  ┌─────────────────────────┐
├── findBy...()                   │ UserDAO                 │
├── findAll()                     │  findByUsername()       │
├── insert()                      │  insert()               │
└── update()                      │  deactivate()           │
                                  ├─────────────────────────┤
                                  │ StudentDAO              │
                                  │  findAll()              │
                                  │  findByUserId()         │
                                  │  insert() / update()    │
                                  ├─────────────────────────┤
                                  │ MarksDAO                │
                                  │  findByStudentAndSem()  │
                                  │  computeGPA/CGPA()      │
                                  └─────────────────────────┘
```

---

### 4.4 Behavioral Pattern: Observer (Swing Event Handling)

**Implementation**: `java.awt.event.ActionListener` attached to all interactive Swing components.

**Intent**: Define a one-to-many dependency between objects so that when one object changes state, all its dependents are notified and updated automatically.

**Academic Justification**: GUI applications are inherently event-driven. When a user clicks "Submit Grade," the `JButton` (Subject/Observable) notifies its registered `ActionListener` (Observer/Controller method). This decoupling means the button does not know what happens when clicked — it simply broadcasts the event. Multiple observers can be registered to the same button without changing the button's code, satisfying the Open/Closed Principle.

```
Subject (Observable)     Observer
┌──────────────┐         ┌────────────────────────────────┐
│  JButton     │─notify→ │ ActionListener (anonymous /    │
│  "Login"     │         │ lambda in LoginFrame)          │
│              │         │   → loginController.           │
│  JComboBox   │─notify→ │     loginAndOpenDashboard()    │
│  "Semester"  │         └────────────────────────────────┘
└──────────────┘
```

---

## 5. Security Architecture

| Threat | Mitigation |
|---|---|
| SQL Injection | `PreparedStatement` with `?` placeholders — no string concatenation |
| Credential Exposure | Passwords in `.env` file; `.env` is git-ignored; BCrypt hashing in Phase 1 |
| Unauthorised Access | Role checked immediately post-login; dashboard creation gated by `UserRole` |
| Transaction Inconsistency | `autoCommit=false`; explicit `commit()`/`rollback()` in controllers |
