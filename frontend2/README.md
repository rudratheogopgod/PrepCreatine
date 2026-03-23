# PrepCreatine Frontend 2 (Alternative UI)

This is an alternative or secondary frontend web application for PrepCreatine. It provides a different layout and user experience, often used for testing specific isolated features (like raw Chat JSON parsing) before they are merged into the primary `frontend` application.

## 🛠 Prerequisites
- **Node.js**: v18 or later
- **npm** or **yarn** or **pnpm**
- The PrepCreatine Backend Server must be running locally on `localhost:8080`.

## 🚀 Setup Instructions (Step-by-Step)

### 1. Install Dependencies
Navigate to the `frontend2` directory and install the necessary node modules:
```bash
cd frontend2
npm install
```

### 2. Configure Environment Variables
Create a file named `.env.local` inside the `frontend2` directory:
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
Open your browser and navigate to the port specified in your terminal (usually `http://localhost:3001` if `frontend` is already running on port `3000`).

## Notes on Features
- **Demo User Integration:** Ensures that the `Arjun Sharma` user profile is loaded for chat and history functionality.
- **Chat Formatting:** Uses explicit Server-Sent Events (SSE) parsing to handle markdown, bullet points, and clean streams from the AI APIs.
