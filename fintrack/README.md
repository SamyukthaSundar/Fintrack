# ⚡ FinTrack — Collaborative Expense Engine

**UE23CS352B — Object Oriented Analysis & Design | Mini Project**

> A production-grade Spring Boot web application for collaborative expense tracking, debt simplification, and settlement verification.

---

## 👥 Team

| Member | Role | Major Feature | Minor Feature |
|---|---|---|---|
| **Saanvi Kakkar** | Identity & Optimization Lead | Intelligent Debt Simplification Engine (Graph Algorithm) | User/Group Orchestrator + Admin Audit |
| **Samyuktha S** | Automation & Strategy Lead | OCR-Powered Expense Orchestrator (Tess4J) | Multi-Strategy Split Logic (Strategy Pattern) |
| **Sanika Gupta** | Settlement & Analytics Lead | Verified Settlement Workflow (State Pattern) | Visual Analytics & PDF Export (Chart.js + iText) |

---

## 🏗️ Architecture

**Strict MVC** (Model → View → Controller) via Spring Boot + Thymeleaf

```
HTTP Request
    ↓
Controller  (handles HTTP, delegates to service)
    ↓
Service     (business logic, publishes events)
    ↓
Repository  (Spring Data JPA → MySQL)
    ↓
Model       (JPA Entities)
    ↓
View        (Thymeleaf templates)
```

---

## 🎨 Design Patterns Implemented

| Pattern | Type | Owner | Where |
|---|---|---|---|
| **Strategy** | Behavioral | Samyuktha S | `SplitStrategy` interface + 4 implementations |
| **Observer** | Behavioral | Samyuktha/Saanvi | `FinTrackEvent` + `NotificationEventListener` |
| **State** | Behavioral | Sanika Gupta | `Settlement.submit/verify/reject()` |
| **Repository** | Structural | All | Spring Data JPA repositories |
| **MVC** | Architectural | All | Spring MVC (enforced by framework) |

---

## 📐 Design Principles

| Principle | Applied In |
|---|---|
| **SRP** | Each service class has one responsibility |
| **OCP** | New split strategies via `SplitStrategy` without changing `ExpenseService` |
| **DIP** | Controllers depend on service interfaces, not implementations |
| **DRY** | `getCurrentUser()`, OCR extraction reused across services |

---

## 📁 Folder Structure

```
fintrack/
├── pom.xml
└── src/main/
    ├── java/com/fintrack/
    │   ├── FinTrackApplication.java
    │   ├── config/
    │   │   ├── JpaConfig.java
    │   │   ├── SecurityConfig.java          ← Saanvi
    │   │   └── WebMvcConfig.java
    │   ├── controller/
    │   │   ├── AuthController.java          ← Saanvi
    │   │   ├── DashboardController.java     ← Saanvi
    │   │   ├── GroupController.java         ← Saanvi
    │   │   ├── ExpenseController.java       ← Samyuktha
    │   │   ├── SettlementController.java    ← Sanika
    │   │   ├── AnalyticsController.java     ← Sanika
    │   │   ├── AdminController.java         ← Saanvi
    │   │   └── NotificationController.java
    │   ├── model/
    │   │   ├── User.java                    ← Saanvi
    │   │   ├── Group.java                   ← Saanvi
    │   │   ├── GroupMember.java             ← Saanvi
    │   │   ├── Expense.java                 ← Samyuktha
    │   │   ├── ExpenseSplit.java            ← Samyuktha
    │   │   ├── Settlement.java              ← Sanika (State Pattern)
    │   │   ├── Notification.java            ← Observer target
    │   │   └── AuditLog.java               ← Saanvi
    │   ├── repository/                      (Spring Data JPA)
    │   ├── service/
    │   │   ├── strategy/
    │   │   │   ├── SplitStrategy.java       ← Samyuktha (Strategy interface)
    │   │   │   ├── EqualSplitStrategy.java
    │   │   │   ├── PercentageSplitStrategy.java
    │   │   │   ├── ExactSplitStrategy.java
    │   │   │   └── WeightedSplitStrategy.java
    │   │   └── impl/
    │   │       ├── UserServiceImpl.java     ← Saanvi
    │   │       ├── GroupServiceImpl.java    ← Saanvi
    │   │       ├── DebtSimplificationService.java  ← Saanvi (MAJOR)
    │   │       ├── ExpenseServiceImpl.java  ← Samyuktha (MAJOR + OCR)
    │   │       ├── SettlementServiceImpl.java ← Sanika (MAJOR)
    │   │       ├── AnalyticsService.java   ← Sanika (MINOR)
    │   │       └── AuditService.java       ← Saanvi (Admin)
    │   ├── observer/
    │   │   ├── FinTrackEvent.java           ← Observer: Event
    │   │   └── NotificationEventListener.java ← Observer: Listener
    │   └── dto/
    └── resources/
        ├── application.properties
        ├── schema.sql                       ← Full DDL
        ├── static/
        │   ├── css/main.css                 ← Dark/Light mode
        │   └── js/main.js                   ← Theme toggle, OCR, Charts
        └── templates/
            ├── auth/          login, register
            ├── dashboard/     home, notifications
            ├── group/         list, new, detail (+ debt simplification)
            ├── expense/       list, new (+ OCR upload)
            ├── settlement/    list (+ state flow UI)
            ├── analytics/     dashboard (Chart.js + PDF export)
            └── admin/         dashboard (audit logs)
```

---

## 🗄️ Database Schema

Tables: `users`, `groups_table`, `group_members`, `expenses`, `expense_splits`, `settlements`, `notifications`, `audit_logs`

Run `schema.sql` to create all tables and seed the admin user.

---

## 🚀 Windows Setup & Execution Guide

### Prerequisites

Install the following on Windows:

| Tool | Version | Download |
|---|---|---|
| Java JDK | 17+ | https://adoptium.net |
| Maven | 3.9+ | https://maven.apache.org/download.cgi |
| MySQL | 8.0+ | https://dev.mysql.com/downloads/installer |
| Git | Latest | https://git-scm.com |

Verify installations:
```cmd
java -version
mvn -version
mysql --version
```

---

### Step 1 — MySQL Database Setup

Open MySQL command line or MySQL Workbench:

```sql
-- Create database and tables
SOURCE path\to\fintrack\src\main\resources\schema.sql;

-- Verify
USE fintrack_db;
SHOW TABLES;
-- Expected: audit_logs, expense_splits, expenses, group_members, groups_table, notifications, settlements, users
```

**Default Admin Credentials** (seeded by schema.sql):
- Username: `admin`
- Password: `admin123`

---

### Step 2 — Configure Database Connection

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/fintrack_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root        # ← your MySQL username
spring.datasource.password=root        # ← your MySQL password
```

---

### Step 3 — (Optional) Tesseract OCR Setup

For real OCR bill scanning (otherwise mock OCR is used automatically):

1. Download Tesseract installer: https://github.com/UB-Mannheim/tesseract/wiki
2. Install to `C:\Program Files\Tesseract-OCR`
3. Create `tessdata/` folder in project root
4. Copy `eng.traineddata` from Tesseract install to `tessdata/`
5. Set environment variable:
```cmd
setx TESSDATA_PREFIX "C:\path\to\fintrack\tessdata"
```

> **Without Tesseract**: The OCR feature uses a built-in mock that returns sample receipt text. All other features work fully without Tesseract.

---

### Step 4 — Build the Project

Open Command Prompt in the project root:

```cmd
cd C:\path\to\fintrack
mvn clean package -DskipTests
```

Expected output:
```
[INFO] BUILD SUCCESS
[INFO] BUILD: fintrack-1.0.0.jar
```

---

### Step 5 — Run the Application

```cmd
mvn spring-boot:run
```

Or run the JAR directly:
```cmd
java -jar target\fintrack-1.0.0.jar
```

Expected console output:
```
╔═══════════════════════════════════════════╗
║   FinTrack - Collaborative Expense Engine ║
║   UE23CS352B  |  Spring Boot 3.2          ║
║   Running at: http://localhost:8080        ║
╚═══════════════════════════════════════════╝
```

---

### Step 6 — Access the Application

Open browser: **http://localhost:8080**

| URL | Description |
|---|---|
| `/auth/login` | Login page |
| `/auth/register` | Register new user |
| `/dashboard` | Main dashboard |
| `/groups` | Group management |
| `/groups/{id}` | Group detail + Debt Simplification |
| `/expenses/group/{id}` | Expense list |
| `/expenses/group/{id}/new` | Add expense (with OCR) |
| `/settlements/group/{id}` | Settlement workflow |
| `/analytics/group/{id}` | Charts + PDF export |
| `/admin` | Admin panel (ADMIN role only) |
| `/actuator/health` | System health (public) |

---

### Step 7 — Quick Demo Flow

```
1. Register 3 users (Saanvi, Samyuktha, Sanika)
2. Login as Saanvi → Create a group "Goa Trip"
3. Add Samyuktha and Sanika to the group
4. Add expenses:
   - "Hotel" ₹6000 paid by Saanvi, EQUAL split
   - "Food" ₹1500 paid by Samyuktha, PERCENTAGE split
   - "Transport" ₹900 paid by Sanika, WEIGHTED split
5. View Group Detail → Debt Simplification shows minimal transactions
6. Samyuktha initiates settlement → submits payment proof → Saanvi verifies
7. View Analytics → Bar chart + Doughnut chart + Export PDF
8. Login as admin → View audit logs
```

---

### Common Issues

| Issue | Fix |
|---|---|
| `Access denied for user 'root'@'localhost'` | Update username/password in application.properties |
| `Table 'fintrack_db.users' doesn't exist` | Run schema.sql against MySQL |
| `Port 8080 already in use` | Add `server.port=8081` to application.properties |
| `OCR not working` | Expected — mock OCR is used; set up Tesseract for real OCR |
| `JDK version error` | Ensure Java 17+ is installed and JAVA_HOME is set |

Set JAVA_HOME on Windows:
```cmd
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17"
setx PATH "%PATH%;%JAVA_HOME%\bin"
```

---

## 🌗 Dark / Light Mode

The UI defaults to **deep black (#000000) with neon green (#00c896) accents**.

Click **"☀️ Light Mode"** button in the top-right navbar to switch. Preference is saved to `localStorage` and persists across sessions.

---

## 📊 UML Summary

- **Use Case Diagram**: User auth, group management, expense creation, settlement workflow, analytics
- **Class Diagram**: 8 entities with full relationships
- **Activity Diagrams**: Expense creation flow, Settlement state flow, Debt simplification algorithm, OCR scan flow
- **State Diagram**: Settlement states — `PENDING → SUBMITTED → VERIFIED / REJECTED`

---

*FinTrack — UE23CS352B OOAD Mini Project | Team: Saanvi Kakkar, Samyuktha S, Sanika Gupta*
