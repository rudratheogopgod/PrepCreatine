// All API endpoint paths — never hardcode elsewhere
export const ENDPOINTS = {
  // Auth
  AUTH_SIGNUP: '/api/auth/signup',
  AUTH_LOGIN: '/api/auth/login',
  AUTH_LOGOUT: '/api/auth/logout',
  AUTH_ME: '/api/auth/me',
  AUTH_FORGOT_PASSWORD: '/api/auth/forgot-password',
  AUTH_RESET_PASSWORD: '/api/auth/reset-password',
  AUTH_VERIFY_EMAIL: '/api/auth/verify-email',
  AUTH_GOOGLE: '/api/auth/google',

  // User
  ME: '/api/me',
  ME_CONTEXT: '/api/me/context',
  ME_EXPORT: '/api/me/export-pdf',
  ME_DEACTIVATE: '/api/me',
  ME_CHANGE_PASSWORD: '/api/me/password',

  // Onboarding
  ONBOARDING_QUICK: '/api/onboarding/quick',
  WAITLIST: '/api/waitlist',

  // Roadmap
  ROADMAP: '/api/roadmap',
  ROADMAP_REGENERATE: '/api/roadmap/regenerate',
  ROADMAP_TOPIC_STATUS: (topicId: string) => `/api/roadmap/topics/${topicId}/status`,

  // Syllabus
  SYLLABUS: '/api/syllabus',
  SYLLABUS_TOPIC_STATUS: (topicId: string) => `/api/syllabus/topics/${topicId}/status`,

  // Chat
  CHAT: '/api/chat',
  CHAT_HISTORY: '/api/conversations',
  CHAT_STREAM: '/api/chat',

  // Tests
  TESTS: '/api/tests',
  TEST_SESSION: (id: string) => `/api/tests/${id}`,
  TEST_START: '/api/tests/start',
  TEST_SUBMIT: (id: string) => `/api/tests/${id}/submit`,
  TEST_RESULTS: (id: string) => `/api/tests/${id}/results`,

  // Notes/Sources
  SOURCES: '/api/sources',
  SOURCE: (id: string) => `/api/sources/${id}`,
  SOURCE_CHAT: (id: string) => `/api/sources/${id}/chat`,

  // Analytics
  ANALYTICS: '/api/analytics',
  ANALYTICS_SUMMARY: '/api/analytics/summary',
  ANALYTICS_HEATMAP: '/api/analytics/heatmap',
  ANALYTICS_TOPICS: '/api/analytics/topics',
  ANALYTICS_TEST_PERFORMANCE: '/api/analytics/test-performance',
  PROGRESS_SHARE: '/api/analytics/share-token',
  PUBLIC_PROGRESS: (token: string) => `/api/progress/${token}`,

  // Community
  THREADS: '/api/community/threads',
  THREAD: (id: string) => `/api/community/threads/${id}`,
  THREAD_ANSWERS: (id: string) => `/api/community/threads/${id}/answers`,
  THREAD_UPVOTE: (id: string) => `/api/community/threads/${id}/upvote`,

  // Game
  GAME_GENERATE: '/api/game/generate',
  GAME_SESSION: (id: string) => `/api/game/${id}`,

  // Mentor
  MENTOR_STUDENTS: '/api/mentor/students',
  MENTOR_STUDENT: (id: string) => `/api/mentor/students/${id}`,
  MENTOR_NOTES: (id: string) => `/api/mentor/students/${id}/note`,

  // Notifications
  NOTIFICATIONS: '/api/notifications',
  NOTIFICATIONS_READ: '/api/notifications/read-all',
}
