import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { toast } from 'react-hot-toast'
import { motion } from 'framer-motion'
import { AlertCircle, Loader2 } from 'lucide-react'
import Logo from '../../components/layout/Logo'
import client from '../../api/client'
import { ENDPOINTS } from '../../api/endpoints'
import { useAuthStore } from '../../store/authStore'
import { logger } from '../../utils/logger'

const C = {
  surface: '#f5f6f7', surfaceLow: '#eff1f2', surfaceLowest: '#ffffff',
  onSurface: '#2c2f30', onSurfaceVariant: '#595c5d', outline: '#757778', outlineVariant: '#abadae',
  primary: '#00628c', primaryContainer: '#34b5fa', error: '#b31b25',
}
const GRADIENT = `linear-gradient(135deg, ${C.primary} 0%, ${C.primaryContainer} 100%)`

const loginSchema = z.object({
  email: z.string().email('Please enter a valid email address.').max(254),
  password: z.string().min(1, 'Please enter your password.').max(128),
})
type LoginForm = z.infer<typeof loginSchema>

function AuthInput({
  label, type = 'text', placeholder, error, id,
  ...props
}: React.InputHTMLAttributes<HTMLInputElement> & { label: string; error?: string }) {
  return (
    <div>
      <label htmlFor={id} style={{ display: 'block', fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: C.outline, marginBottom: 7 }}>
        {label}
      </label>
      <input
        id={id}
        type={type}
        placeholder={placeholder}
        style={{
          width: '100%', boxSizing: 'border-box',
          background: C.surfaceLow, border: `1px solid rgba(171,173,174,0.2)`,
          borderRadius: 12, padding: '11px 16px',
          fontFamily: 'Manrope,sans-serif', fontSize: 14, color: C.onSurface,
          outline: 'none', transition: 'all 0.15s',
          ...(error ? { borderColor: C.error, background: '#fff5f5' } : {}),
        }}
        onFocus={e => { if (!error) { e.target.style.background = C.surfaceLowest; e.target.style.boxShadow = `0 0 0 2px rgba(52,181,250,0.35)` } }}
        onBlur={e => { e.target.style.background = error ? '#fff5f5' : C.surfaceLow; e.target.style.boxShadow = 'none' }}
        {...props}
      />
      {error && (
        <p style={{ display: 'flex', alignItems: 'center', gap: 5, marginTop: 6, fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.error }}>
          <AlertCircle size={12} /> {error}
        </p>
      )}
    </div>
  )
}

export default function Login() {
  const navigate = useNavigate()
  const setAuth = useAuthStore(s => s.setAuth)

  const { register, handleSubmit, formState: { errors } } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
  })

  const mutation = useMutation({
  mutationFn: async (data: LoginForm) => {
      const res = await client.post(ENDPOINTS.AUTH_LOGIN, {
        email: data.email.trim().toLowerCase(),
        password: data.password
      })
      return res.data
    },
    onSuccess: (data) => {
      // Backend returns { accessToken, refreshToken, user }
      const { user, accessToken, refreshToken } = data
      setAuth(user, accessToken, refreshToken) // Pass both tokens to the store
      if (!user?.onboardingComplete) navigate('/onboarding')
      else navigate('/home')
    },
    onError: (err: unknown) => {
      const status = (err as { response?: { status?: number } })?.response?.status
      toast.error(status === 401 ? 'Incorrect email or password.' : 'Could not log you in. Please try again.')
      logger.error('[Auth] Login failure', err)
    },
  })

  return (
    <div style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: `linear-gradient(160deg, #e8f3ff 0%, #f0eeff 40%, ${C.surface} 70%)`,
      padding: '40px 16px', position: 'relative', overflow: 'hidden',
    }}>
      {/* Background blobs */}
      <div style={{ position: 'absolute', top: -120, left: -120, width: 420, height: 420, borderRadius: '50%', background: 'radial-gradient(circle, rgba(52,181,250,0.25) 0%, transparent 70%)', filter: 'blur(60px)', pointerEvents: 'none' }} />
      <div style={{ position: 'absolute', bottom: -100, right: -100, width: 360, height: 360, borderRadius: '50%', background: 'radial-gradient(circle, rgba(175,166,255,0.25) 0%, transparent 70%)', filter: 'blur(60px)', pointerEvents: 'none' }} />

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.45 }}
        style={{ width: '100%', maxWidth: 420, position: 'relative', zIndex: 1 }}
      >
        {/* Logo */}
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 28 }}><Logo /></div>

        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: 28 }}>
          <h1 style={{ fontSize: 26, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: '0 0 6px' }}>
            Welcome back
          </h1>
          <p style={{ fontSize: 14, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, margin: 0 }}>
            Log in to continue your study session
          </p>
        </div>

        {/* Card */}
        <div style={{
          background: 'rgba(255,255,255,0.92)', backdropFilter: 'blur(20px)', WebkitBackdropFilter: 'blur(20px)',
          borderRadius: 24, padding: 28, border: '1px solid rgba(171,173,174,0.18)',
          boxShadow: '0 20px 48px rgba(0,98,140,0.08)',
          marginBottom: 16,
        }}>
          {/* Google OAuth */}
          <a href={`${import.meta.env.VITE_API_BASE_URL}/api/auth/google`} style={{ textDecoration: 'none' }}>
            <button
              style={{
                width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 12,
                padding: '11px 16px', borderRadius: 14, cursor: 'pointer',
                background: C.surfaceLowest, border: '1px solid rgba(171,173,174,0.25)',
                fontFamily: 'Manrope,sans-serif', fontSize: 14, fontWeight: 600, color: C.onSurface,
                transition: 'all 0.15s', marginBottom: 20,
              }}
              onMouseEnter={e => { (e.currentTarget as HTMLButtonElement).style.background = C.surfaceLow; (e.currentTarget as HTMLButtonElement).style.borderColor = 'rgba(171,173,174,0.4)' }}
              onMouseLeave={e => { (e.currentTarget as HTMLButtonElement).style.background = C.surfaceLowest; (e.currentTarget as HTMLButtonElement).style.borderColor = 'rgba(171,173,174,0.25)' }}
            >
              <svg viewBox="0 0 24 24" style={{ width: 18, height: 18, flexShrink: 0 }}>
                <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
                <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
                <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
                <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
              </svg>
              Continue with Google
            </button>
          </a>

          {/* Divider */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 20 }}>
            <div style={{ flex: 1, height: 1, background: 'rgba(171,173,174,0.25)' }} />
            <span style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.outlineVariant }}>or</span>
            <div style={{ flex: 1, height: 1, background: 'rgba(171,173,174,0.25)' }} />
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit(d => mutation.mutate(d))} noValidate style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <AuthInput id="login-email" label="Email address" type="email" placeholder="you@example.com" autoComplete="email" error={errors.email?.message} {...register('email')} />
            <AuthInput id="login-password" label="Password" type="password" placeholder="Your password" autoComplete="current-password" error={errors.password?.message} {...register('password')} />

            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <Link to="/forgot-password" style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.primary, textDecoration: 'none', fontWeight: 600 }}>
                Forgot your password?
              </Link>
            </div>

            <button
              type="submit"
              disabled={mutation.isPending}
              style={{
                width: '100%', padding: '13px', borderRadius: 100, border: 'none', cursor: mutation.isPending ? 'not-allowed' : 'pointer',
                background: mutation.isPending ? 'rgba(0,98,140,0.5)' : GRADIENT,
                color: '#fff', fontFamily: 'Manrope,sans-serif', fontSize: 14, fontWeight: 700,
                display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
                boxShadow: '0 4px 16px rgba(0,98,140,0.25)', transition: 'all 0.15s',
              }}
              onMouseEnter={e => { if (!mutation.isPending) (e.currentTarget as HTMLButtonElement).style.opacity = '0.9' }}
              onMouseLeave={e => { (e.currentTarget as HTMLButtonElement).style.opacity = '1' }}
            >
              {mutation.isPending && <Loader2 size={15} style={{ animation: 'spin 1s linear infinite' }} />}
              Log in
            </button>
          </form>
        </div>

        <p style={{ textAlign: 'center', fontSize: 13, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant }}>
          New to PrepCreatine?{' '}
          <Link to="/signup" style={{ color: C.primary, fontWeight: 700, textDecoration: 'none' }}>Create a free account</Link>
        </p>
      </motion.div>
    </div>
  )
}
