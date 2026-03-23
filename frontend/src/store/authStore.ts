import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import client from '../api/client'

export interface AuthUser {
  id: string
  email: string
  fullName: string
  role: 'STUDENT' | 'MENTOR' | 'ADMIN'
  onboardingComplete: boolean
  avatarUrl?: string
}

interface AuthStore {
  user: AuthUser | null
  token: string | null
  refreshToken: string | null
  isAuthenticated: boolean
  setAuth: (user: AuthUser, token: string, refreshToken: string) => void
  setToken: (token: string) => void
  setUser: (user: AuthUser | null) => void
  logout: () => void
}

/**
 * HACKATHON BYPASS:
 * We initialize the store with a Guest User and set isAuthenticated to true.
 * This allows you to view all pages without actually logging in.
 */
const MOCK_USER: AuthUser = {
  id: 'hackathon-dev-id',
  email: 'guest@example.com',
  fullName: 'Guest Developer',
  role: 'STUDENT',
  onboardingComplete: true,
};

export const useAuthStore = create<AuthStore>()(
  persist(
    (set) => ({
      // Default state for Hackathon: Logged in by default
      user: MOCK_USER,
      token: 'mock-access-token',
      refreshToken: 'mock-refresh-token',
      isAuthenticated: true,

      setAuth: (user, token, refreshToken) => {
        client.defaults.headers.common['Authorization'] = `Bearer ${token}`
        set({ user, token, refreshToken, isAuthenticated: true })
      },

      setToken: (token) => {
        client.defaults.headers.common['Authorization'] = `Bearer ${token}`
        set({ token })
      },

      setUser: (user) => set({ user, isAuthenticated: true }), // Keep true for demo

      logout: () => {
        // For a hackathon demo, you might want logout to just reset to mock
        // rather than clearing everything, so the app remains usable.
        delete client.defaults.headers.common['Authorization']
        set({
          user: MOCK_USER,
          token: 'mock-access-token',
          refreshToken: 'mock-refresh-token',
          isAuthenticated: true
        })
      },
    }),
    {
      name: 'prepcreatine-auth',
      onRehydrateStorage: () => (state) => {
        // Ensure the mock token is in headers even after a page refresh
        if (state?.token) {
          client.defaults.headers.common['Authorization'] = `Bearer ${state.token}`
        }
      },
    }
  )
)