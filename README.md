# README — Student Information Management System (SIMS)

> **Lab Assignment**: Exercise 10 — Standalone Java Application with Database Connectivity  
> **Courses**: UIT3361 (Object-Oriented Programming Using Java) · UIT3311 (Database Technology Laboratory)

---

## Overview

SIMS is a **Java Swing desktop application** that automates academic administration for educational institutions. It supports four user roles (Administrator, Faculty, Student, Parent) with RBAC-controlled dashboards, and connects to an **Oracle Database Free** instance running in a local Docker container via pure JDBC.

### Key Features

| Module | Description |
|---|---|
| User Authentication | Login with role-based dashboard routing |
| Admission Management | Register, edit, and search student profiles |
| Attendance Tracking | Faculty logs attendance; students/parents view history |
| Internal Marks (CAT) | Faculty enters scores; system computes GPA/CGPA |
| Fee Payment | Invoice creation, online payment simulation, receipt generation |
| Transcript Generation | Downloadable semester-wise academic records |

---

## Prerequisites

| Tool | Version | Purpose |
|---|---|---|
| JDK | 17+ | Compilation and runtime |
| Apache Maven | 3.9+ | Dependency management and build |
| Docker Desktop | Latest | Runs Oracle Database container |
| Git | Any | Version control |

---

## Quick Start

### Step 1 — Clone the repository

```bash
git clone <repository-url>
cd student-information-management-system
```

### Step 2 — Configure credentials

```bash
# Copy the example file and fill in your values
cp .env.example .env
```

The default `.env` values match the Docker Compose configuration and work
out-of-the-box for local development:

```
DB_URL=jdbc:oracle:thin:@localhost:1521/FREEPDB1
DB_USER=system
DB_PASSWORD=admin
```

### Step 3 — Start Oracle Database in Docker

```bash
# Start the container in detached mode
docker compose up -d

# Monitor initialisation (Oracle takes ~2 minutes on first boot)
docker logs -f sims_oracle_db
```

Wait until you see:
```
DATABASE IS READY TO USE!
```

The `db/schema.sql` and `db/seed_data.sql` scripts run **automatically** on
first container start via the `/container-entrypoint-initdb.d` mount.

### Step 4 — (Optional) Verify schema manually

```bash
# Connect via SQL*Plus inside the container
docker exec -it sims_oracle_db sqlplus system/admin@FREEPDB1

# Inside SQL*Plus:
SELECT TABLE_NAME FROM USER_TABLES;
SELECT USERNAME, ROLE FROM USERS;
EXIT;
```

### Step 5 — Build and run the application

```bash
# Compile all sources and resolve dependencies
mvn compile

# Launch the Swing application
mvn exec:java
```

---

## Project Structure

```
.
├── .env.example          ← Credential template (copy to .env)
├── .gitignore
├── AGENTS.md             ← Coding agent operating protocol
├── README.md             ← This file
├── docker-compose.yml    ← Oracle Database container config
├── pom.xml               ← Maven build descriptor
├── db/
│   ├── schema.sql        ← Oracle DDL (auto-run on container start)
│   └── seed_data.sql     ← Demo users and records
├── docs/
│   ├── ARCHITECTURE.md   ← MVC/DAO design + pattern justifications
│   ├── DATABASE.md       ← ER diagram + 3NF normalization proof
│   ├── TECH_STACK.md     ← Hardware/software specification tables
│   └── WORK_SPLIT.md     ← Phased execution plan for all modules
└── src/
    └── main/java/com/sims/
        ├── Main.java
        ├── model/         ← User, Student, Mark, Payment, Attendance POJOs
        ├── dao/           ← UserDAO, StudentDAO, MarksDAO, PaymentDAO
        ├── controller/    ← LoginController (+ phase 1–5 controllers)
        ├── factory/       ← DashboardFactory (Factory Method pattern)
        ├── view/          ← Swing frames and panels (implemented per phase)
        └── util/          ← DBConnection (Singleton pattern)
```

---

## Default Login Credentials (Seed Data)

| Role | Username | Password |
|---|---|---|
| Administrator | `admin01` | `admin@123` |
| Faculty | `faculty01` | `faculty@123` |
| Student | `student01` | `student@123` |
| Parent | `parent01` | `parent@123` |

---

## Building a Fat JAR (optional)

```bash
mvn package
java -jar target/student-information-management-system-1.0.0-SNAPSHOT.jar
```

---

## Technology Stack

See [`docs/TECH_STACK.md`](docs/TECH_STACK.md) for full specification tables.

| Layer | Technology |
|---|---|
| Language | Java 17 |
| GUI Framework | Java Swing (javax.swing) |
| Database | Oracle Database Free 23ai (Docker) |
| JDBC Driver | ojdbc11 (com.oracle.database.jdbc) |
| Build Tool | Apache Maven 3.9+ |
| Configuration | dotenv-java (io.github.cdimascio) |
| Containerisation | Docker / Docker Compose |

---

## Design Patterns Implemented

| Pattern | Category | Class |
|---|---|---|
| Singleton | Creational | `com.sims.util.DBConnection` |
| Factory Method | Creational | `com.sims.factory.DashboardFactory` |
| DAO | Structural | `com.sims.dao.*` |
| Observer | Behavioral | `ActionListener` in all View classes |

---

## GitHub Repository

```
https://github.com/aadiii3769/sims-mini-project.git
```
