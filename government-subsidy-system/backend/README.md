# Government Subsidy / Grant Disbursement Tracking System

An enterprise-grade, full-stack Spring Boot monolithic application for managing the complete lifecycle of government subsidies and grants with MySQL database integration, JWT authentication, role-based authorization, multi-level verification workflows, configurable eligibility scoring, staged disbursements, compliance milestone enforcement, fund utilization tracking, regional analytics, automated monitoring schedulers, and an embedded web dashboard.

---

## 1. Project Overview & Features

### Core Modules
1. **Beneficiary & Scheme Master Data Management**:
   - Beneficiary registration with Aadhaar identity validation, category (`GENERAL`, `OBC`, `SC`, `ST`, `EWS`), KYC verification status, and bank account metadata.
   - Scheme creation, budget caps, grant slabs (`minGrantAmount`, `maxGrantAmount`), and configurable criteria.
2. **Eligibility Scoring & Multi-Level Verification Workflow**:
   - Automated configurable scoring engine evaluating income, age, category, land holding, and disability.
   - Strict status workflow state machine: `DRAFT` → `SUBMITTED` → `ELIGIBILITY_EVALUATED` → `FIELD_VERIFICATION` → `DISTRICT_REVIEW` → `FINANCE_APPROVAL` → `DISBURSEMENT_PLANNED`.
   - Rejection and re-verification routing (`REVERIFICATION_REQUIRED`, `REJECTED`).
   - Risk classification: `LOW`, `FLAGGED` (borderline score), `HIGH_VALUE` (grant >= ₹5,00,000).
3. **Staged Disbursement & Milestone Compliance**:
   - Staged disbursement plans with multi-milestone schedules.
   - Mandatory compliance checks preventing fund release until required conditions are inspected and satisfied.
   - Integration-ready Treasury DBT gateway simulation issuing RBI/PFMS-compliant UTRs.
4. **Fund Utilization & Regional Analytics**:
   - Tracks utilized funds against released funds (`utilizationPercentage = utilized / released * 100`).
   - Ground proof upload and officer verification advancing applications to `COMPLETED`.
   - Comprehensive regional and scheme analytics.
5. **Security, Schedulers & Reporting**:
   - Stateless Spring Security with JWT Bearer tokens and BCrypt password encryption.
   - Role-based authorization (`ROLE_BENEFICIARY`, `ROLE_FIELD_OFFICER`, `ROLE_DISTRICT_OFFICER`, `ROLE_FINANCE_OFFICER`, `ROLE_ADMIN`).
   - Automated `@Scheduled` compliance monitor flagging overdue milestones and unutilized funds.
   - CSV report exports for Schemes, Regions, Applications, Milestones, and Utilizations.

---

## 2. User Roles & Credentials

| Role | Username | Password | Full Name | Primary Responsibilities |
| :--- | :--- | :--- | :--- | :--- |
| **Admin** | `admin` | `Admin@123` | Rajesh Sharma | Scheme creation, criteria configuration, regional budgets, audit logs, analytics |
| **Beneficiary** | `farmer_john` | `User@123` | John Doe | Browse schemes, submit applications, upload documents, submit fund utilizations |
| **Beneficiary 2** | `artisan_priya` | `User@123` | Priya Patel | Browse schemes, submit applications |
| **Field Officer** | `field_officer1` | `Officer@123` | Amit Kumar | Ground verification, applicant inspection, reverification requests |
| **District Officer**| `district_officer1`| `District@123`| Dr. Neha Verma | District review, approve/reject/escalate applications |
| **Finance Officer** | `finance_officer1` | `Finance@123` | Sanjay Gupta | Financial approvals, create staged disbursement plans, release funds via DBT |

---

## 3. Database Schema

The database consists of 14 normalized tables:
- `users`: User credentials, email, phone, status
- `roles`: Role definitions (`ROLE_BENEFICIARY`, `ROLE_FIELD_OFFICER`, `ROLE_DISTRICT_OFFICER`, `ROLE_FINANCE_OFFICER`, `ROLE_ADMIN`)
- `user_roles`: User-role join table
- `regions`: Regional jurisdictions, allocated & utilized budgets
- `beneficiaries`: Identity numbers (Aadhaar), category, income, land holding, bank details, KYC status
- `schemes`: Scheme code, department, total budget, remaining budget, grant slabs, min score
- `eligibility_criteria`: Rule criteria types, comparison operators (`<=`, `>=`, `==`, `IN`), expected values, weight points, mandatory flag
- `subsidy_applications`: Unique application numbers (`SUB-YYYYMMDD-XXXX`), status, risk level, score, applied & approved amounts
- `application_documents`: Attached verification documents and metadata
- `verifications`: Multi-stage verification records (Field, District, Finance) with remarks and decisions
- `disbursement_plans`: Sanctioned plans tracking planned, released, and remaining balances
- `disbursement_milestones`: Sequenced milestones, scheduled amounts, due dates, compliance conditions, release statuses
- `fund_releases`: Direct Benefit Transfer records with transaction reference numbers (UTR), payment modes, and treasury status
- `fund_utilizations`: Ground expenditure receipts, proof documents, and verification records
- `audit_logs`: Immutable audit trail of every lifecycle transition and financial action

---

## 4. How to Configure & Run

### Prerequisites
- Java 21 LTS or Java 17+
- Maven 3.9+
- MySQL 8.x (optional: in-memory H2 profile included for zero-setup execution)

### 1. MySQL Setup (Production / Local MySQL)
1. Ensure MySQL is running on `localhost:3306`.
2. Create the database:
   ```sql
   CREATE DATABASE subsidy_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. Check credentials in `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/subsidy_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   spring.datasource.username=root
   spring.datasource.password=root
   ```

### 2. Running the Application
- **With MySQL (Default)**:
  ```powershell
  mvn spring-boot:run
  ```
- **With Zero-Setup In-Memory Database (`h2` profile)**:
  ```powershell
  java -jar target/government-subsidy-system-1.0.0.jar --spring.profiles.active=h2
  ```

### 3. Accessing the Interactive Web Dashboard
Open your browser and navigate to:
```
http://localhost:8080/
```
The embedded government dashboard provides a **1-Click Quick Demo Role Switcher** at the top allowing instant switching between Admin, Beneficiary, Field Officer, District Officer, and Finance Officer.

---

## 5. Running Automated Tests

Execute the comprehensive test suite with:
```powershell
mvn test
```
All 20 unit and integration tests pass cleanly covering authentication, eligibility evaluation, workflow state machine, staged disbursement, milestone compliance, and fund utilization.
