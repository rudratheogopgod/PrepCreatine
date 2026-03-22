import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { lazy, Suspense } from 'react'
import { AnimatePresence } from 'framer-motion'
import AppShell from './components/layout/AppShell'
import ErrorBoundary from './components/layout/ErrorBoundary'
import { useAuthStore } from './store/authStore'

// === Lazy imports for all pages ===
// Public
const Landing = lazy(() => import('./pages/landing/Landing'))
const Signup = lazy(() => import('./pages/auth/Signup'))
const Login = lazy(() => import('./pages/auth/Login'))
const ForgotPassword = lazy(() => import('./pages/auth/ForgotPassword'))
const ResetPassword = lazy(() => import('./pages/auth/ResetPassword'))
const VerifyEmail = lazy(() => import('./pages/auth/VerifyEmail'))

// Onboarding
const Onboarding = lazy(() => import('./pages/onboarding/Onboarding'))
const QuickStudy = lazy(() => import('./pages/onboarding/QuickStudy'))

// App pages
const Home = lazy(() => import('./pages/home/Home'))
const Learn = lazy(() => import('./pages/learn/Learn'))
const Roadmap = lazy(() => import('./pages/roadmap/Roadmap'))
const Syllabus = lazy(() => import('./pages/syllabus/Syllabus'))
const TestConfig = lazy(() => import('./pages/test/TestConfig'))
const ExamSession = lazy(() => import('./pages/test/ExamSession'))
const TestResults = lazy(() => import('./pages/test/TestResults'))
const Game = lazy(() => import('./pages/game/Game'))
const Notes = lazy(() => import('./pages/notes/Notes'))
const SourceViewer = lazy(() => import('./pages/notes/SourceViewer'))
const Analytics = lazy(() => import('./pages/analytics/Analytics'))
const Community = lazy(() => import('./pages/community/Community'))
const Thread = lazy(() => import('./pages/community/Thread'))
const Settings = lazy(() => import('./pages/settings/Settings'))
const MentorDashboard = lazy(() => import('./pages/mentor/MentorDashboard'))
const StudentDetail = lazy(() => import('./pages/mentor/StudentDetail'))
const NotificationsList = lazy(() => import('./pages/notifications/NotificationsList'))

// Public / system
const PublicProgress = lazy(() => import('./pages/progress/PublicProgress'))
const NotFound = lazy(() => import('./pages/system/NotFound'))
const Offline = lazy(() => import('./pages/system/Offline'))
const Privacy = lazy(() => import('./pages/system/Privacy'))
const Terms = lazy(() => import('./pages/system/Terms'))
const About = lazy(() => import('./pages/system/About'))

// Fallback for Suspense
const PageLoader = () => (
  <div className="flex h-screen items-center justify-center bg-gray-50 dark:bg-slate-950">
    <div className="h-8 w-8 rounded-full border-2 border-sky-500 border-t-transparent animate-spin" />
  </div>
)

// Protected route wrapper
function Protected({ children }: { children: React.ReactNode }) {
  //const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  //if (!isAuthenticated) return <Navigate to="/login" replace />
  return <>{children}</>;
}

export default function App() {
  return (
    <BrowserRouter>
      <ErrorBoundary>
        <Suspense fallback={<PageLoader />}>
          <AnimatePresence mode="wait">
            <Routes>
              {/* Public routes */}
              <Route path="/" element={<Landing />} />
              <Route path="/signup" element={<Signup />} />
              <Route path="/login" element={<Login />} />
              <Route path="/forgot-password" element={<ForgotPassword />} />
              <Route path="/reset-password/:token" element={<ResetPassword />} />
              <Route path="/verify-email/:token" element={<VerifyEmail />} />
              <Route path="/progress/:shareToken" element={<PublicProgress />} />
              <Route path="/privacy" element={<Privacy />} />
              <Route path="/terms" element={<Terms />} />
              <Route path="/about" element={<About />} />

              {/* Onboarding — no AppShell */}
              <Route path="/onboarding" element={
                <Protected><Onboarding /></Protected>
              } />
              <Route path="/onboarding/quick" element={
                <Protected><QuickStudy /></Protected>
              } />

              {/* Offline fallback */}
              <Route path="/offline" element={<Offline />} />

              {/* Exam session — full screen, no AppShell */}
              <Route path="/test/session/:id" element={
                <Protected><ExamSession /></Protected>
              } />

              {/* Game — full screen, no AppShell */}
              <Route path="/game" element={
                <Protected><Game /></Protected>
              } />

              {/* Authenticated pages with AppShell */}
              <Route element={<Protected><AppShell /></Protected>}>
                <Route path="/home" element={<Home />} />
                <Route path="/learn" element={<Learn />} />
                <Route path="/roadmap" element={<Roadmap />} />
                <Route path="/syllabus" element={<Syllabus />} />
                <Route path="/test" element={<TestConfig />} />
                <Route path="/test/results/:id" element={<TestResults />} />
                <Route path="/notes" element={<Notes />} />
                <Route path="/notes/:sourceId" element={<SourceViewer />} />
                <Route path="/analytics" element={<Analytics />} />
                <Route path="/community" element={<Community />} />
                <Route path="/community/:threadId" element={<Thread />} />
                <Route path="/settings" element={<Settings />} />
                <Route path="/mentor" element={<MentorDashboard />} />
                <Route path="/mentor/student/:id" element={<StudentDetail />} />
                <Route path="/notifications" element={<NotificationsList />} />
              </Route>

              {/* 404 */}
              <Route path="*" element={<NotFound />} />
            </Routes>
          </AnimatePresence>
        </Suspense>
      </ErrorBoundary>
    </BrowserRouter>
  )
}
