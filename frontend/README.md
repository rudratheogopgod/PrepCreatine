# PrepCreatine Frontend (Next.js)

This is the primary web application interface for PrepCreatine. It provides the dashboards, AI-chat learning paths, and progress analytics. It is built using Next.js, React, Tailwind CSS, and connects directly to the Java Spring Boot Backend APIs.

## Features
- **Student Dashboard:** Real-time analytics, Exam Countdown, and Streaks.
- **Learn Interface:** AI-driven conversational learning paths formatted elegantly from streaming SSE tokens.
- **Tests & Drills:** Full-length dynamic adaptive tests based on weak spots.
- **Analytics:** Data-driven insights mapping topic progress, historical activity, and overall exam readiness score.

---

## 🛠 Prerequisites
- **Node.js**: v18 or later
- **npm** or **yarn** or **pnpm**
- The PrepCreatine Backend Server must be running locally on `localhost:8080`.

## 🚀 Setup Instructions (Step-by-Step)

### 1. Install Dependencies
Navigate to the `frontend` directory and install the necessary node modules:
```bash
cd frontend
npm install
```

### 2. Configure Environment Variables
Create a file named `.env.local` inside the `frontend` directory:
```bash
touch .env.local
```
Add the following line to define your backend API endpoint:
```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

### 3. Run the Development Server
Start the Next.js development server:
```bash
npm run dev
```

### 4. Access the Application
Open your browser and navigate to:
**[http://localhost:3000](http://localhost:3000)**

## 🛡 Authentication (Demo Mode)
For testing and hackathon purposes, the Java backend natively supports a Demo Mode bypass. 
If your backend was launched with the `DEMO_MODE=true` environment variable, simply click the **"Login as Demo User"** button on the Login page. 

This will automatically authenticate you as the default demo user (*Arjun Sharma*) without requiring a username or password.
