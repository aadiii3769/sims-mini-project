# TASK: Phase 0 — Foundation Setup, Documentation Blueprints & Database Provisioning

## 1. ACADEMIC COURSE CONTEXT & EXACT REQUIREMENTS
You are scaffolding a repository for an academic lab project combining two university laboratory papers:
- **UIT3361**: Object-Oriented Programming Using Java[cite: 1]
- **UIT3311**: Database Technology Laboratory[cite: 1, 2]
- **Assignment**: Exercise 10 — Standalone Java Application with Database Connectivity (Student Information Management System)[cite: 1, 2]
- **Reference Syllabus Topics**:
  - Java Application Development: AWT, Swing, Console Applications, GUI Applications (AWT, Swing, JavaFX), Database Applications (JDBC), Event Handling, Exception Handling, Input Validation, and OOP Design Patterns[cite: 1].
- **Final Evaluation Deliverables**:
  - Live application demonstration before the Model Examination[cite: 1].
  - Comprehensive project report covering:
    1. System Requirements (hardware and software specifications)[cite: 2]
    2. System Design (front-end and back-end architecture)[cite: 2]
    3. Core Modules Implemented[cite: 2]
    4. Code Snippets highlighting key functionality[cite: 1]
    5. Screenshots of the working application
    6. GitHub Repository Link for source code

---

## 2. PROBLEM STATEMENT
**Student Information Management System (SIMS)**[cite: 2]:

Develop a standalone Java application for educational institutions that automates academic administration[cite: 1, 2]:
- **Role-Based Access Control (RBAC)**: Supports Administrator, Faculty, Student, and Parent roles with tailored views and permissions[cite: 2].
- **Admissions & Profile Management**: Administrators register new students, edit profiles, and maintain academic records[cite: 2].
- **Attendance Tracking**: Faculty records hourly/daily class attendance, while students/parents view attendance history[cite: 2].
- **Internal Marks & Semester Results**: Faculty logs continuous assessment test (CAT) scores, and the system computes aggregates, GPA/CGPA, and generates downloadable transcripts[cite: 2].
- **Fee Payment Processing**: Calculates outstanding balances, processes online payments, and generates printable digital receipts[cite: 2].
- **Technology Stack**: Java Swing desktop GUI, JDBC connectivity via `PreparedStatement`, and an Oracle Database running inside a local Docker container[cite: 1, 2].
- **Mandatory Academic Requirement**: Formal relational database normalization (1NF, 2NF, 3NF) with documented academic justifications[cite: 2].

---

## 3. CORE ARCHITECTURAL CONSTRAINTS & CODING PHILOSOPHY
- **No Over-Engineering**: Do NOT use Spring, Spring Boot, Hibernate, JPA, or complex ORM frameworks[cite: 1]. The evaluation is strictly on foundational Java OOP concepts, Swing GUI, and core JDBC[cite: 1].
- **Back-End Pattern**: Standard Desktop MVC / DAO architecture[cite: 1]:
  - `model/`: Plain Java POJOs (getters, setters, constructors) mapping directly to database entities (`User`, `Student`, `Mark`, `Payment`)[cite: 2].
  - `dao/`: Pure JDBC using `java.sql.*` interfaces (`Connection`, `PreparedStatement`, `ResultSet`)[cite: 1]. Explicit resource management using `try-with-resources`.
  - `util/`: Singleton `DBConnection.java` reading connection properties securely from `.env` or local configuration.
  - `controller/`: Lightweight business logic, validation, and transaction demarcation (`commit`/`rollback`).
  - `view/`: Swing views (`JFrame`, `JDialog`, `JPanel`, `JTable`)[cite: 1].
- **Front-End & UI Standards**:
  - **No Bizarre "AI UI"**: Avoid messy custom-painted gradients or absolute/null layouts (`setBounds`).
  - Use native **System Look and Feel** (`UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())`).
  - Use standard, robust Swing layout managers (`BorderLayout`, `GridBagLayout`, `FlowLayout`, nested `JPanel` with consistent padding/borders via `BorderFactory.createEmptyBorder(...)`)[cite: 1].
  - Clean, readable table rendering (`JTable` inside `JScrollPane`) and standard dialogs (`JOptionPane`).
  - Strict UI Threading: Always launch and mutate UI components on the Event Dispatch Thread using `SwingUtilities.invokeLater`.
- **Build System**: Maven (`pom.xml`) managing:
  - JDK 17 baseline[cite: 2].
  - Oracle JDBC Driver: `com.oracle.database.jdbc:ojdbc11`.
  - `dotenv-java` (for clean `.env` loading, keeping credentials out of code).

---

## 4. DESIGN PATTERNS REQUIREMENT & ACADEMIC JUSTIFICATION
To fulfill the evaluation requirement for **Creational, Structural, and Behavioral Design Patterns**[cite: 1], the codebase must explicitly implement and document the following patterns:

### 1. Creational Pattern: Singleton Pattern
- **Implementation**: `com.sims.util.DBConnection`
- **Academic Justification**: Opening and closing database connections repeatedly creates severe resource overhead. The Singleton pattern ensures that only one single instance of the database connection manager exists across the JVM lifecycle, providing a globally accessible, thread-safe access point to the Oracle JDBC connection[cite: 1].

### 2. Creational Pattern: Factory Method Pattern
- **Implementation**: `com.sims.factory.DashboardFactory`
- **Academic Justification**: The SRS mandates explicit Role-Based Access Control (Admin, Faculty, Student, Parent)[cite: 2]. The Factory pattern encapsulates conditional creation logic (`createDashboard(UserRole role)`), decoupling user authentication from the instantiation of role-specific UI dashboards (`AdminDashboard`, `FacultyDashboard`, etc.)[cite: 2].

### 3. Structural Pattern: Data Access Object (DAO) Pattern
- **Implementation**: `com.sims.dao.*` (`StudentDAO`, `MarksDAO`, `PaymentDAO`)
- **Academic Justification**: Prevents tight coupling between GUI components and SQL queries[cite: 1]. By hiding low-level JDBC calls (`PreparedStatement`, `ResultSet`) behind DAO interfaces, the application decouples data persistence logic from business and presentation layers[cite: 1].

### 4. Behavioral Pattern: Observer Pattern (Swing Event Handling)
- **Implementation**: `java.awt.event.ActionListener` attached to Swing GUI elements (`JButton`, `JComboBox`)[cite: 1].
- **Academic Justification**: Handles asynchronous UI interactions[cite: 1]. When a user triggers an action (e.g., clicking "Submit Grade" or "Pay Fees"), the UI acts as the Subject notifying registered Observer listeners, decoupling event generation from event handling[cite: 1, 2].

---

## 5. REPOSITORY STRUCTURE RULES
Keep root clean and place architectural blueprints in `docs/`:

.
├── .env.example
├── .gitignore
├── AGENTS.md
├── README.md
├── docker-compose.yml
├── pom.xml
├── db/
│   ├── schema.sql
│   └── seed_data.sql
├── docs/
│   ├── ARCHITECTURE.md
│   ├── DATABASE.md
│   ├── TECH_STACK.md
│   └── WORK_SPLIT.md
└── src/
    └── main/
        └── java/
            └── com/
                └── sims/
                    ├── Main.java
                    ├── model/
                    ├── dao/
                    ├── view/
                    ├── controller/
                    ├── factory/
                    └── util/

---

## 6. REQUIRED DELIVERABLES TO GENERATE (PHASE 0)

### 1. `docker-compose.yml`
- Containerized Oracle Database using `gvenzl/oracle-free:slim` (lightweight Oracle XE/Free image).
- Map standard Oracle SQL ports (`1521:1521`).
- Configure persistent volume mapping, database service (`FREEPDB1`), and environment credentials.
- Automate execution of `db/schema.sql` on container startup via entrypoint initialization scripts (`/container-entrypoint-initdb.d`).

### 2. `db/schema.sql`
- Complete, runnable DDL script for Oracle Database based on SRS specifications[cite: 2]:
  - Tables: `USERS`, `STUDENT`, `ATTENDANCE`, `MARKS`, `PAYMENT`[cite: 2].
  - Primary keys (`STUDENT_ID`, `RECORD_ID`, `TRANSACTION_ID`, `LOG_ID`)[cite: 2].
  - Foreign keys with referential integrity[cite: 2].
  - Check constraints for roles (`ADMIN`, `FACULTY`, `STUDENT`, `PARENT`), marks scales, and transaction statuses (`PAID`, `PENDING`)[cite: 2].
- Include `db/seed_data.sql` with realistic initial data (Admin, Faculty, Student users, test grades, fee structures) for immediate live testing[cite: 2].

### 3. `.env.example` & `.gitignore`
- `.env.example`: `DB_URL=jdbc:oracle:thin:@localhost:1521/FREEPDB1`, `DB_USER=system`, `DB_PASSWORD=admin`.
- `.gitignore`: Standard Java, Maven, IDE (`.idea`, `.vscode`), OS files, and `.env`.

### 4. `pom.xml`
- Minimal, clean Maven configuration targeting Java 17+[cite: 2].
- Dependencies: `com.oracle.database.jdbc:ojdbc11`, `io.github.cdimascio:dotenv-java`.
- Configured `exec-maven-plugin` to launch `com.sims.Main`.

### 5. `AGENTS.md` (Root)
- Operating protocol for coding agents working on subsequent execution phases.
- Rules on JDBC connection safety (using `try-with-resources`), `PreparedStatement` usage (preventing SQL injection), Swing threading rules (`SwingUtilities.invokeLater`), zero over-engineering enforcement, and modular development[cite: 1].

### 6. `README.md` (Root)
- Project overview, prerequisites (JDK 17, Docker Desktop, Maven), and quick-start instructions[cite: 2]:
  - Running Oracle DB in Docker.
  - Executing SQL scripts via SQL*Plus or entrypoint.
  - Compiling and executing the application via `mvn exec:java`.

### 7. Documentation in `docs/`
- **`docs/TECH_STACK.md`**:
  - Software and hardware specification tables conforming to IEEE SRS / academic report standards[cite: 2].
- **`docs/ARCHITECTURE.md`**:
  - Layered MVC/DAO architecture breakdown with ASCII data-flow diagrams[cite: 1].
  - Explicit documentation of Creational (Singleton, Factory), Structural (DAO), and Behavioral (Observer) design patterns with academic justifications[cite: 1].
- **`docs/DATABASE.md`**:
  - Relational schema descriptions and ER relationship mappings[cite: 2].
  - **Formal Normalization Justification**: Step-by-step academic breakdown from UNF through 1NF, 2NF, and 3NF for `STUDENT`, `MARKS`, `PAYMENT`, and `ATTENDANCE` tables[cite: 2].
- **`docs/WORK_SPLIT.md`**:
  - **Standardized Agent Trigger Contract**: Prompt template to execute each phase (e.g., `"Read AGENTS.md, docs/ARCHITECTURE.md, and execute Phase [X] from docs/WORK_SPLIT.md completely."`).
  - **SRS Feature Catalog (Baseline Requirements)**[cite: 2]:
    - Module 1: User Authentication & Role Selection (Admin, Faculty, Student, Parent)[cite: 2].
    - Module 2: Admission Management & Student Profiles[cite: 2].
    - Module 3: Attendance Logging & Monthly Summaries[cite: 2].
    - Module 4: Internal Marks Evaluation & GPA/CGPA Tabulation[cite: 2].
    - Module 5: Online Fee Invoicing & Payment Processing[cite: 2].
    - Module 6: Transcript Generation & Academic Record Archival[cite: 2].
  - **Balanced Phased Execution Plan (Phase 1 to Phase N)**:
    - Structured step-by-step phases executable within context limits.
    - **Mandatory Anatomy of Every Phase**:
      1. **Objective & Scope**: Module goals[cite: 2].
      2. **Files to Create / Modify**: Explicit package paths (e.g., `src/main/java/com/sims/dao/StudentDAO.java`).
      3. **Step-by-Step Implementation Flow**: Model -> DAO (`PreparedStatement`) -> Controller -> Swing View[cite: 1].
      4. **Verification & Acceptance Criteria**: Terminal verification harness and manual Swing GUI checks[cite: 1, 2].
      5. **Definition of Done**: Clean checkpoint before phase completion.

---

## 7. EXECUTION INSTRUCTION
Generate all listed files and directories with complete, syntactically correct, and immediately usable contents. Do not output placeholders, omitted sections, or deferred TODOs.