# BloodConnect — Blood Donation Management System
**3rd-Year Academic Final-Year Project**

BloodConnect is a lightweight, academic three-tier blood donation management web application designed to connect blood donors, hospitals, and healthcare administrators.

---

## 1. System Architecture

```text
HTML5 + Vanilla CSS + JavaScript (Fetch API)
                   ↓
      Spring Boot 3 REST API
                   ↓
            MySQL 8 Database
```

### Three Portals:
1. **Customer / Donor Portal** (`donor.html`): Register, view profile & 90-day cooldown status, schedule appointments at approved hospitals, and track history.
2. **Hospital Portal** (`hospital.html`): View real-time inventory of all 8 blood groups (`A+`, `A-`, `B+`, `B-`, `AB+`, `AB-`, `O+`, `O-`), process appointments (+1 unit upon completion), and broadcast/fulfill blood requests.
3. **Admin Portal** (`admin.html`): Verify and approve hospital licenses, monitor system blood reserves, and manage user accounts.

---

## 2. Technology Stack

- **Frontend:** Plain HTML5, CSS3, Vanilla JavaScript (Modern Fetch API, Session Storage). No Node.js / React / Vite required.
- **Backend:** Java (JDK 26 runtime, source/target 21), Spring Boot 3.4.3, Spring Web, Spring Data JPA, Spring Security Crypto (BCrypt).
- **Database:** MySQL 8.0 (`bloodconnect_db`).
- **Build Tool:** Maven Wrapper (`mvnw.cmd`).

---

## 3. Database Setup (MySQL 8.0)

1. Open **MySQL Workbench** or MySQL CLI.
2. Execute the initialization script located at:
   ```sql
   database/schema.sql
   ```
   This creates `bloodconnect_db`, defines the 6 core tables (`users`, `donors`, `hospitals`, `blood_inventory`, `donations`, `blood_requests`), and seeds the default administrator.

3. Verify `backend/src/main/resources/application.properties` credentials match your local MySQL installation:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/bloodconnect_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   spring.datasource.username=root
   spring.datasource.password=root
   ```

---

## 4. Default Demonstration Credentials

| Role | Email | Password | Status |
| :--- | :--- | :--- | :--- |
| **System Admin** | `admin@bloodconnect.com` | `Admin@123` | Active & Verified |

*(New donors can be registered directly through the UI. New hospitals can register through the UI and are approved from the Admin Portal).*

---

## 5. How to Run

### Step 1: Start the Backend (Spring Boot)
Open a terminal in the `backend` folder:
```powershell
cd "C:\Blood Donation System\backend"
.\mvnw.cmd spring-boot:run
```
The REST API will start on:
```text
http://localhost:8080
```

### Step 2: Open the Frontend
Because the frontend is plain HTML/CSS/JS, no npm installation or build step is needed:
- Open `frontend/index.html` directly in your browser, or
- Use VS Code Live Server / Python HTTP server:
  ```powershell
  cd "C:\Blood Donation System\frontend"
  python -m http.server 5500
  ```
  and visit `http://localhost:5500`.

---

## 6. Project Size Summary

- **Backend Java files:** 23 files
- **Frontend HTML files:** 6 files
- **Frontend CSS files:** 1 file
- **Frontend JS files:** 1 file
- **Database tables:** 6 tables
