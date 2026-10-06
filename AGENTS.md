# AGENTS.md — Operating Protocol for Coding Agents

> **READ THIS FILE FIRST before writing any code.**
> This document defines non-negotiable coding standards for every agent
> working on the SIMS project.  Violating these rules will produce code
> that fails evaluation criteria.

---

## 1. Project Identity

| Field | Value |
|---|---|
| Project | Student Information Management System (SIMS) |
| Lab | UIT3361 (OOP Java) + UIT3311 (Database Technology) |
| Stack | Java 17 · Java Swing · Oracle JDBC (ojdbc11) · Oracle DB (Docker) |
| Build | Maven (`pom.xml`) — run with `mvn exec:java` |
| Entry point | `com.sims.Main` |

---

## 2. Architectural Law — No Over-Engineering

> **FORBIDDEN technologies**: Spring, Spring Boot, Hibernate, JPA, MyBatis,
> Guice, CDI, Lombok, MapStruct, Quarkus, Micronaut.

The evaluation panel grades on **core Java OOP, raw JDBC, and Swing**.
Adding frameworks signals the student cannot implement foundational concepts.

**Permitted third-party libraries** (already in `pom.xml`):
- `com.oracle.database.jdbc:ojdbc11` — JDBC thin driver
- `io.github.cdimascio:dotenv-java` — `.env` loading only

---

## 3. Package Structure — Never Deviate

```
com.sims
├── Main.java
├── model/       ← POJOs only (User, Student, Mark, Payment, Attendance)
├── dao/         ← JDBC only (UserDAO, StudentDAO, MarksDAO, PaymentDAO, AttendanceDAO)
├── controller/  ← Business logic + transaction control
├── factory/     ← DashboardFactory (Factory Method pattern)
├── view/        ← Swing frames / panels / dialogs
└── util/        ← DBConnection (Singleton pattern)
```

---

## 4. JDBC Safety Rules (MANDATORY)

### 4.1 Always use `PreparedStatement` — Never `Statement`

```java
// ✅ CORRECT
String sql = "SELECT * FROM USERS WHERE USERNAME = ?";
try (PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setString(1, username);
    try (ResultSet rs = ps.executeQuery()) { ... }
}

// ❌ FORBIDDEN — SQL injection vulnerability
String sql = "SELECT * FROM USERS WHERE USERNAME = '" + username + "'";
Statement st = conn.createStatement();
```

### 4.2 Always use `try-with-resources`

Every `PreparedStatement` and `ResultSet` MUST be opened in a
`try-with-resources` block.  Never close them manually in `finally` blocks.

```java
// ✅ CORRECT
try (PreparedStatement ps = conn.prepareStatement(sql);
     ResultSet rs = ps.executeQuery()) {
    while (rs.next()) { ... }
}   // ps and rs auto-closed here
```

### 4.3 Transaction Demarcation — Controller's Responsibility

- `DBConnection` sets `autoCommit = false`.
- DAO methods perform DML (`INSERT`, `UPDATE`, `DELETE`) but **do NOT commit**.
- The **Controller** issues `conn.commit()` after all related DAOs succeed, or
  `conn.rollback()` on any exception.

```java
// ✅ CORRECT pattern in a Controller
Connection conn = DBConnection.getInstance().getConnection();
try {
    userDAO.insert(user);
    studentDAO.insert(student);
    conn.commit();           // both inserts succeed atomically
} catch (SQLException e) {
    conn.rollback();
    throw new RuntimeException("Registration failed: " + e.getMessage(), e);
}
```

### 4.4 Never Swallow `SQLException`

Always propagate or wrap `SQLException`.  Never catch and ignore.

```java
// ❌ FORBIDDEN
try { ... } catch (SQLException e) { /* silent */ }

// ✅ CORRECT
try { ... } catch (SQLException e) {
    throw new RuntimeException("Operation failed", e);
}
```

---

## 5. Swing Threading Rules (MANDATORY)

> **All Swing component creation and mutation MUST happen on the Event
> Dispatch Thread (EDT).**

### 5.1 Application startup

```java
// In Main.java — ALWAYS wrap in invokeLater
SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
```

### 5.2 Long-running tasks (DB queries)

Never query the database on the EDT. Use `SwingWorker`:

```java
new SwingWorker<List<Student>, Void>() {
    @Override protected List<Student> doInBackground() throws Exception {
        return studentDAO.findAll();    // runs off EDT
    }
    @Override protected void done() {
        try {
            tableModel.setData(get()); // runs on EDT
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage());
        }
    }
}.execute();
```

### 5.3 UI Mutations from background threads

```java
// ✅ CORRECT
SwingUtilities.invokeLater(() -> label.setText("Done"));

// ❌ FORBIDDEN — may cause rendering corruption
label.setText("Done");   // from a non-EDT thread
```

---

## 6. UI Layout Rules — No Null/Absolute Layouts

```java
// ❌ FORBIDDEN
panel.setLayout(null);
component.setBounds(10, 20, 100, 30);

// ✅ CORRECT — use standard layout managers
JPanel panel = new JPanel(new BorderLayout());
JPanel form  = new JPanel(new GridBagLayout());
form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
```

**Approved layout managers**: `BorderLayout`, `GridBagLayout`, `FlowLayout`,
`BoxLayout`, `GridLayout`.  For complex forms, nest `JPanel` containers.

**Look and Feel**: always set system L&F in `Main.java` before creating any
Swing component:
```java
UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
```

---

## 7. Design Patterns — Document Every Implementation

Every class implementing a design pattern MUST include a Javadoc section:
```
<h2>Design Pattern – [Category]: [Pattern Name]</h2>
<p><b>Academic Justification</b>: ...</p>
```

Required patterns:

| Category | Pattern | Class |
|---|---|---|
| Creational | Singleton | `com.sims.util.DBConnection` |
| Creational | Factory Method | `com.sims.factory.DashboardFactory` |
| Structural | DAO | `com.sims.dao.*` |
| Behavioral | Observer | `ActionListener` in all View classes |

---

## 8. Phase Execution Protocol

To execute a phase:
```
Read AGENTS.md, docs/ARCHITECTURE.md, and execute Phase [X]
from docs/WORK_SPLIT.md completely.
```

Before writing any code for a phase:
1. Read `AGENTS.md` (this file).
2. Read `docs/ARCHITECTURE.md` for the full design picture.
3. Read the target phase section in `docs/WORK_SPLIT.md`.
4. Implement **all** files listed in the phase — no partial completions.
5. Verify against the phase's **Acceptance Criteria** before declaring done.

---

## 9. Commit Hygiene

- Commit after each phase is fully working.
- Commit message format: `feat(phaseN): <short description>`
- Never commit `.env` (it is git-ignored).
- Always commit `.env.example` with placeholder values only.
