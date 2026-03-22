import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { CheckCircle, Mail, AlertCircle, Loader2 } from 'lucide-react'
import { motion } from 'framer-motion'
import Logo from '../../components/layout/Logo'
import client from '../../api/client'
import { ENDPOINTS } from '../../api/endpoints'

const C = {
  surface: '#f5f6f7', surfaceLow: '#eff1f2', surfaceLowest: '#ffffff',
  onSurface: '#2c2f30', onSurfaceVariant: '#595c5d', outline: '#757778', outlineVariant: '#abadae',
  primary: '#00628c', primaryContainer: '#34b5fa', error: '#b31b25',
}
const GRADIENT = `linear-gradient(135deg, ${C.primary} 0%, ${C.primaryContainer} 100%)`

const schema = z.object({ email: z.string().email('Please enter a valid email address.').max(254) })

export default function ForgotPassword() {
  const [sent, setSent] = useState(false)

  const { register, handleSubmit, formState: { errors } } = useForm({
    resolver: zodResolver(schema),
  })

  const mutation = useMutation({
    mutationFn: async (data: { email: string }) => {
      await client.post(ENDPOINTS.AUTH_FORGOT_PASSWORD, { email: data.email.trim().toLowerCase() })
    },
    onSettled: () => setSent(true),
  })

  const wrapper = (children: React.ReactNode) => (
    <div style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: `linear-gradient(160deg, #e8f3ff 0%, #f0eeff 40%, ${C.surface} 70%)`,
      padding: '40px 16px', position: 'relative', overflow: 'hidden',
    }}>
      <div style={{ position: 'absolute', top: -100, left: -100, width: 380, height: 380, borderRadius: '50%', background: 'radial-gradient(circle, rgba(52,181,250,0.22) 0%, transparent 70%)', filter: 'blur(60px)', pointerEvents: 'none' }} />
      <div style={{ position: 'absolute', bottom: -100, right: -100, width: 320, height: 320, borderRadius: '50%', background: 'radial-gradient(circle, rgba(175,166,255,0.22) 0%, transparent 70%)', filter: 'blur(60px)', pointerEvents: 'none' }} />
      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4 }}
        style={{ width: '100%', maxWidth: 400, position: 'relative', zIndex: 1 }}
      >
        {children}
      </motion.div>
    </div>
  )

  if (sent) {
    return wrapper(
      <div style={{ textAlign: 'center' }}>
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 28 }}><Logo /></div>
        <div style={{
          background: 'rgba(255,255,255,0.92)', backdropFilter: 'blur(20px)', WebkitBackdropFilter: 'blur(20px)',
          borderRadius: 24, padding: '40px 32px', border: '1px solid rgba(171,173,174,0.18)',
          boxShadow: '0 20px 48px rgba(0,98,140,0.08)',
        }}>
          <div style={{ width: 64, height: 64, borderRadius: '50%', background: 'rgba(22,101,52,0.08)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 20px' }}>
            <CheckCircle size={32} color="#166534" />
          </div>
          <h1 style={{ fontSize: 22, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: '0 0 10px' }}>Check your inbox</h1>
          <p style={{ fontSize: 14, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, lineHeight: 1.6, margin: '0 0 24px' }}>
            If that email is registered, we&apos;ve sent a reset link. Check your inbox and spam folder.
          </p>
          <Link to="/login" style={{ fontSize: 14, fontFamily: 'Manrope,sans-serif', color: C.primary, fontWeight: 700, textDecoration: 'none' }}>
            ← Back to log in
          </Link>
        </div>
      </div>
    )
  }

  return wrapper(
    <>
      <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 28 }}><Logo /></div>
      <div style={{ textAlign: 'center', marginBottom: 24 }}>
        <div style={{ width: 52, height: 52, borderRadius: '50%', background: 'rgba(0,98,140,0.08)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 14px' }}>
          <Mail size={24} color={C.primary} />
        </div>
        <h1 style={{ fontSize: 24, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: '0 0 6px' }}>Forgot your password?</h1>
        <p style={{ fontSize: 14, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, margin: 0 }}>
          Enter your email and we&apos;ll send you a reset link.
        </p>
      </div>

      <div style={{
        background: 'rgba(255,255,255,0.92)', backdropFilter: 'blur(20px)', WebkitBackdropFilter: 'blur(20px)',
        borderRadius: 24, padding: 24, border: '1px solid rgba(171,173,174,0.18)',
        boxShadow: '0 20px 48px rgba(0,98,140,0.08)', marginBottom: 16,
      }}>
        <form onSubmit={handleSubmit(d => mutation.mutate(d))} noValidate style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div>
            <label style={{ display: 'block', fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: C.outline, marginBottom: 7 }}>
              Email address
            </label>
            <input
              type="email"
              placeholder="you@example.com"
              autoComplete="email"
              style={{
                width: '100%', boxSizing: 'border-box',
                background: errors.email ? '#fff5f5' : C.surfaceLow,
                border: `1px solid ${errors.email ? C.error : 'rgba(171,173,174,0.2)'}`,
                borderRadius: 12, padding: '11px 16px',
                fontFamily: 'Manrope,sans-serif', fontSize: 14, color: C.onSurface, outline: 'none',
                transition: 'all 0.15s',
              }}
              onFocus={e => { if (!errors.email) { e.target.style.background = C.surfaceLowest; e.target.style.boxShadow = `0 0 0 2px rgba(52,181,250,0.35)` } }}
              onBlur={e => { e.target.style.background = errors.email ? '#fff5f5' : C.surfaceLow; e.target.style.boxShadow = 'none' }}
              {...register('email')}
            />
            {errors.email && (
              <p style={{ display: 'flex', alignItems: 'center', gap: 5, marginTop: 6, fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.error }}>
                <AlertCircle size={12} /> {errors.email.message}
              </p>
            )}
          </div>
          <button
            type="submit"
            disabled={mutation.isPending}
            style={{
              width: '100%', padding: '13px', borderRadius: 100, border: 'none',
              cursor: mutation.isPending ? 'not-allowed' : 'pointer',
              background: mutation.isPending ? 'rgba(0,98,140,0.5)' : GRADIENT,
              color: '#fff', fontFamily: 'Manrope,sans-serif', fontSize: 14, fontWeight: 700,
              display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
              boxShadow: '0 4px 16px rgba(0,98,140,0.25)',
            }}
          >
            {mutation.isPending && <Loader2 size={15} style={{ animation: 'spin 1s linear infinite' }} />}
            Send reset link
          </button>
        </form>
      </div>

      <p style={{ textAlign: 'center', fontSize: 13, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant }}>
        <Link to="/login" style={{ color: C.primary, fontWeight: 700, textDecoration: 'none' }}>← Back to log in</Link>
      </p>
    </>
  )
}
