import axios from 'axios'
import { toast } from 'react-hot-toast'
import { logger } from '../utils/logger'
import { useAuthStore } from '../store/authStore'
import { ENDPOINTS } from './endpoints'

const client = axios.create({
baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
withCredentials: true,
timeout: 30000,
headers: { 'Content-Type': 'application/json' },
})

// Request interceptor
client.interceptors.request.use((config) => {
  config.headers['X-Request-ID'] = crypto.randomUUID()

  /** * HACKATHON BYPASS:
   * We retrieve the token from the store, but we don't block the request
   * if it's missing. The backend should also be set to permitAll.
   */
  const token = useAuthStore.getState().token
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }

  return config
})

// Response interceptor
client.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    const status = error.response?.status

    // Prevent infinite loops on auth endpoints
    const isAuthEndpoint = originalRequest.url?.includes('/api/auth/')

    /**
     * HACKATHON BYPASS:
     * Attempt token refresh if a 401 occurs, but DO NOT force a redirect
     * to the login page if it fails. This allows the UI to stay on the
     * current feature page even if the "session" technically expires.
     */
    if (status === 401 && !isAuthEndpoint && !originalRequest._retry) {
      originalRequest._retry = true
      const { refreshToken, setToken } = useAuthStore.getState()

      if (refreshToken) {
        try {
          const res = await axios.post(`${client.defaults.baseURL}${ENDPOINTS.AUTH_REFRESH}`, { refreshToken })
          const { accessToken } = res.data

          setToken(accessToken)
          if (originalRequest.headers) {
            originalRequest.headers['Authorization'] = `Bearer ${accessToken}`
          }
          return client(originalRequest)
        } catch (refreshError) {
          logger.error('Silent refresh failed, continuing as guest', refreshError)
          // We do not call logout() or redirect to /login here for the hackathon
        }
      }
    }

    // Standard error handling - removed the window.location.href = '/login' redirect
    if (status === 403) {
      toast.error('Access restricted, but showing demo content.')
    } else if (status === 500) {
      logger.error('Server Error', error)
    }

    return Promise.reject(error)
  }
)

export default client