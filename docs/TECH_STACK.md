# TECH_STACK.md — Software and Hardware Specification

> **Document Type**: System Requirements Specification (SRS) — Section 1  
> **Standard**: IEEE 830-1998 SRS format (adapted for academic lab report)  
> **Project**: Student Information Management System (SIMS)  
> **Lab**: UIT3361 · UIT3311

---

## 1. Hardware Requirements

### 1.1 Minimum Configuration (Development)

| Component | Minimum Specification |
|---|---|
| Processor | Intel Core i3 / AMD Ryzen 3 (2.0 GHz, dual-core) |
| RAM | 8 GB DDR4 |
| Storage | 20 GB free disk space (10 GB for Docker Oracle image) |
| Display | 1366 × 768 resolution |
| Network | Localhost loopback only (no internet required at runtime) |

### 1.2 Recommended Configuration

| Component | Recommended Specification |
|---|---|
| Processor | Intel Core i5 / AMD Ryzen 5 (3.0+ GHz, quad-core) |
| RAM | 16 GB DDR4 |
| Storage | 50 GB SSD |
| Display | 1920 × 1080 resolution |

---

## 2. Software Requirements

### 2.1 Development Environment

| Software | Version | Purpose | License |
|---|---|---|---|
| Java Development Kit (JDK) | 17 LTS (Eclipse Temurin / Oracle) | Compilation, runtime | GPL v2 / Oracle BCL |
| Apache Maven | 3.9+ | Build, dependency management | Apache 2.0 |
| Docker Desktop | 24+ | Oracle container orchestration | Docker Subscription |
| Git | 2.40+ | Version control | GPL v2 |
| IDE (recommended) | IntelliJ IDEA 2024+ / VS Code | Development | Free Community / MIT |

### 2.2 Runtime Dependencies (managed by Maven `pom.xml`)

| Artifact | Group ID | Version | Purpose |
|---|---|---|---|
| `ojdbc11` | `com.oracle.database.jdbc` | 23.4.0.24.05 | Oracle JDBC thin driver |
| `dotenv-java` | `io.github.cdimascio` | 5.2.2 | `.env` credential loading |

### 2.3 Database

| Component | Specification |
|---|---|
| DBMS | Oracle Database Free 23ai (formerly XE) |
| Docker Image | `gvenzl/oracle-free:slim` |
| Pluggable DB (PDB) | `FREEPDB1` |
| Port | `1521` (TCP, mapped to host) |
| Connection String | `jdbc:oracle:thin:@localhost:1521/FREEPDB1` |
| Schema Owner | `SYSTEM` (dev) / `SIMS_APP` (app user, provisioned by Docker) |

### 2.4 Java Libraries (JDK Built-In — No External Dependency)

| Package / Class | Usage |
|---|---|
| `javax.swing.*` | All GUI components (JFrame, JPanel, JTable, JDialog) |
| `java.awt.*` | Layout managers, events, rendering |
| `java.sql.*` | JDBC interfaces (Connection, PreparedStatement, ResultSet) |
| `java.time.*` | LocalDate, LocalDateTime for date mapping |
| `java.math.BigDecimal` | Currency-precision fee amounts |

---

## 3. Design Patterns — Academic Classification

| Pattern | Category (GoF) | Implemented In | Rationale |
|---|---|---|---|
| Singleton | Creational | `com.sims.util.DBConnection` | Single shared JDBC connection; avoids repeated TCP/Oracle session overhead |
| Factory Method | Creational | `com.sims.factory.DashboardFactory` | Decouples login from role-specific UI instantiation; supports Open/Closed Principle |
| DAO (Data Access Object) | Structural | `com.sims.dao.*` | Separates SQL from business logic; enables query changes without touching UI |
| Observer | Behavioral | `ActionListener` on all Swing widgets | Decouples event source (button click) from event handler (controller method) |

---

## 4. Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                  Presentation Layer                  │
│          Java Swing (JFrame / JPanel / JTable)       │
│            com.sims.view / com.sims.factory          │
└────────────────────────┬────────────────────────────┘
                         │  ActionListener (Observer)
┌────────────────────────▼────────────────────────────┐
│                  Controller Layer                     │
│   LoginController · StudentController · MarksCtrl   │
│             com.sims.controller                      │
└────────────────────────┬────────────────────────────┘
                         │  POJO objects
┌────────────────────────▼────────────────────────────┐
│                     DAO Layer                        │
│  UserDAO · StudentDAO · MarksDAO · PaymentDAO        │
│             com.sims.dao                             │
└────────────────────────┬────────────────────────────┘
                         │  PreparedStatement / ResultSet
┌────────────────────────▼────────────────────────────┐
│                Oracle Database (Docker)              │
│   USERS · STUDENT · ATTENDANCE · MARKS · PAYMENT    │
└─────────────────────────────────────────────────────┘
```

---

## 5. Non-Functional Requirements

| Requirement | Specification |
|---|---|
| Performance | All DB queries complete within 2 seconds on local Docker |
| Security | Passwords hashed (BCrypt, Phase 1); all queries use PreparedStatement |
| Scalability | Desktop single-user application; no concurrent session requirement |
| Portability | Runs on Windows 10/11, macOS 12+, Ubuntu 22.04+ with JDK 17 |
| Maintainability | MVC/DAO separation; every pattern documented in Javadoc |
