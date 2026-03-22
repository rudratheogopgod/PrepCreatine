import './index.css'
import React from 'react'
import ReactDOM from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Toaster } from 'react-hot-toast'
import App from './App.tsx'

// ── Sentry (init BEFORE any React renders, per FDD §13) ──────────────────────
import * as Sentry from '@sentry/react'
Sentry.init({
  dsn: import.meta.env.VITE_SENTRY_DSN ?? '',
  environment: import.meta.env.MODE,
  tracesSampleRate: 0.1,        // 10% of transactions for perf monitoring
  replaysSessionSampleRate: 0.0, // disable replay (privacy)
  enabled: !!import.meta.env.VITE_SENTRY_DSN, // only when DSN is configured
})

// Initialize QueryClient
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error: unknown) => {
        // Do not retry 4xx errors
        const status = (error as { response?: { status?: number } })?.response?.status
        if (status && status < 500) return false
        return failureCount < 2
      },
      retryDelay: (failureCount) => Math.min(1000 * 2 ** failureCount, 30000),
      staleTime: 60 * 1000, // 1 minute default
    },
  },
})

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
      <Toaster
        position="top-right"
        toastOptions={{
          duration: 3000,
          style: {
            background: '#fff',
            color: '#334155',
            borderRadius: '12px',
            boxShadow: '0 10px 15px -3px rgba(0,0,0,0.1)',
            fontSize: '14px',
            fontFamily: 'Inter, sans-serif',
            borderLeft: '4px solid #0EA5E9',
          },
          success: {
            duration: 3000,
            style: { borderLeft: '4px solid #22c55e' },
          },
          error: {
            duration: 5000,
            style: { borderLeft: '4px solid #ef4444' },
          },
        }}
      />
    </QueryClientProvider>
  </React.StrictMode>,
)
