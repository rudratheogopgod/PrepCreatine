# PrepCreatine Backend (Spring Boot)

This directory contains the source code for the PrepCreatine Java Spring Boot backend application.

## 🏗 Architecture
The backend is built using:
- **Java 21+** with Virtual Threads enabled for massive concurrency.
- **Spring Boot 3.x** for the core REST API framework.
- **PostgreSQL** for the primary relational database.
- **Spring Security + JWT** for robust stateless authentication.
- **Flyway** for automated database migrations (`V1__...sql`).
- **SSE (Server-Sent Events)** for real-time AI capabilities (e.g. chat streaming).

## 🚀 Local Setup Instructions (Step-by-Step)

### 1. Database Configuration
Ensure PostgreSQL is running on your machine (default port 5432).
You must create the database before starting the application:
```sql
CREATE DATABASE prepcreatine;
```

### 2. Environment Variables
In the **root** directory of the project, create a `.env` file (you can copy `.env.example`).
Ensure the following variables are correctly set:
```env
DATABASE_URL=jdbc:postgresql://localhost:5432/prepcreatine
DB_USERNAME=postgres
DB_PASSWORD=your_password
JWT_SECRET=super_secret_jwt_key_that_is_long_enough
```
*Optional: Add `GROQ_API_KEY` or `GEMINI_API_KEY` to enable AI generation instead of fallbacks.*

### 3. Running the Application
Open a terminal in the project's root directory (one level up from this folder) and run the Maven wrapper:

**On Mac/Linux:**
```bash
./mvnw spring-boot:run
```

**On Windows:**
```cmd
.\mvnw.cmd spring-boot:run
```

### 4. Demo Mode (Optional)
To bypass JWT security constraints for rapid UI testing, you can enable demo mode:
- **Windows:** `$env:DEMO_MODE="true"; .\mvnw.cmd spring-boot:run`
- **Mac/Linux:** `DEMO_MODE=true ./mvnw spring-boot:run`

This bypasses login and automatically authenticates all requests as the master Demo User.

### 5. Automated Seeding
On startup, the backend will automatically:
- Run Flyway migrations to build the schema.
- Seed the database with mock users and NCERT exam data sources.
- Run a 10-step startup self-test.

The API will be available at `http://localhost:8080/api`.
