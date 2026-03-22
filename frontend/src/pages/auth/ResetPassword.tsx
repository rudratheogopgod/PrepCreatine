import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useNavigate, useParams } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { toast } from 'react-hot-toast'
import { motion } from 'framer-motion'
import { AlertCircle, Loader2, Lock } from 'lucide-react'
import Logo from '../../components/layout/Logo'
import client from '../../api/client'
import { ENDPOINTS } from '../../api/endpoints'

const C = {
  surface: '#f5f6f7', surfaceLow: '#eff1f2', surfaceLowest: '#ffffff',
  onSurface: '#2c2f30', onSurfaceVariant: '#595c5d', outline: '#757778', outlineVariant: '#abadae',
  primary: '#00628c', primaryContainer: '#34b5fa', error: '#b31b25',
}
const GRADIENT = `linear-gradient(135deg, ${C.primary} 0%, ${C.primaryContainer} 100%)`

const schema = z.object({
  password: z.string().min(8, 'Password must be at least 8 characters.').max(128),
  confirmPassword: z.string(),
}).refine(d => d.password === d.confirmPassword, { message: 'Passwords do not match.', path: ['confirmPassword'] })
type FormData = z.infer<typeof schema>

export default function ResetPassword() {
  const { token } = useParams<{ token: string }>()
  const navigate = useNavigate()

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({ resolver: zodResolver(schema) })

  const mutation = useMutation({
    mutationFn: async (data: FormData) => {
      await client.post(ENDPOINTS.AUTH_RESET_PASSWORD, { token, password: data.password })
    },
    onSuccess: () => { toast.success('Password updated! Please log in.'); navigate('/login') },
    onError: () => toast.error('Reset link expired or invalid. Please request a new one.'),
  })

  return (
    <div style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: `linear-gradient(160deg, #e8f3ff 0%, #f0eeff 40%, ${C.surface} 70%)`,
      padding: '40px 16px', position: 'relative', overflow: 'hidden',
    }}>
      <div style={{ position: 'absolute', top: -100, left: -100, width: 380, height: 380, borderRadius: '50%', background: 'radial-gradient(circle, rgba(52,181,250,0.22) 0%, transparent 70%)', filter: 'blur(60px)' }} />
      <div style={{ position: 'absolute', bottom: -80, right: -80, width: 320, height: 320, borderRadius: '50%', background: 'radial-gradient(circle, rgba(175,166,255,0.22) 0%, transparent 70%)', filter: 'blur(60px)' }} />

      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4 }}
        style={{ width: '100%', maxWidth: 400, position: 'relative', zIndex: 1 }}
      >
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 28 }}><Logo /></div>

        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <div style={{ width: 52, height: 52, borderRadius: '50%', background: 'rgba(0,98,140,0.08)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 14px' }}>
            <Lock size={22} color={C.primary} />
          </div>
          <h1 style={{ fontSize: 24, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: '0 0 6px' }}>Set a new password</h1>
          <p style={{ fontSize: 14, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, margin: 0 }}>Make it strong and memorable.</p>
        </div>

        <div style={{
          background: 'rgba(255,255,255,0.92)', backdropFilter: 'blur(20px)', WebkitBackdropFilter: 'blur(20px)',
          borderRadius: 24, padding: '24px 28px', border: '1px solid rgba(171,173,174,0.18)',
          boxShadow: '0 20px 48px rgba(0,98,140,0.08)',
        }}>
          <form onSubmit={handleSubmit(d => mutation.mutate(d))} noValidate style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {([
              { id: 'reset-pw', label: 'New password', field: 'password', placeholder: '8+ characters', err: errors.password },
              { id: 'reset-confirm', label: 'Confirm new password', field: 'confirmPassword', placeholder: 'Same as above', err: errors.confirmPassword },
            ] as const).map(f => (
              <div key={f.id}>
                <label htmlFor={f.id} style={{ display: 'block', fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: C.outline, marginBottom: 7 }}>{f.label}</label>
                <input
                  id={f.id}
                  type="password"
                  placeholder={f.placeholder}
                  autoComplete="new-password"
                  style={{
                    width: '100%', boxSizing: 'border-box',
                    background: f.err ? '#fff5f5' : C.surfaceLow,
                    border: `1px solid ${f.err ? C.error : 'rgba(171,173,174,0.2)'}`,
                    borderRadius: 12, padding: '11px 16px',
                    fontFamily: 'Manrope,sans-serif', fontSize: 14, color: C.onSurface, outline: 'none',
                  }}
                  onFocus={e => { if (!f.err) { e.target.style.background = C.surfaceLowest; e.target.style.boxShadow = `0 0 0 2px rgba(52,181,250,0.35)` } }}
                  onBlur={e => { e.target.style.background = f.err ? '#fff5f5' : C.surfaceLow; e.target.style.boxShadow = 'none' }}
                  {...register(f.field as 'password' | 'confirmPassword')}
                />
                {f.err && (
                  <p style={{ display: 'flex', alignItems: 'center', gap: 5, marginTop: 6, fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.error }}>
                    <AlertCircle size={12} /> {f.err.message}
                  </p>
                )}
              </div>
            ))}

            <button
              type="submit"
              disabled={mutation.isPending}
              style={{
                marginTop: 4, width: '100%', padding: '13px', borderRadius: 100, border: 'none',
                cursor: mutation.isPending ? 'not-allowed' : 'pointer',
                background: mutation.isPending ? 'rgba(0,98,140,0.5)' : GRADIENT,
                color: '#fff', fontFamily: 'Manrope,sans-serif', fontSize: 14, fontWeight: 700,
                display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
                boxShadow: '0 4px 16px rgba(0,98,140,0.25)',
              }}
            >
              {mutation.isPending && <Loader2 size={15} style={{ animation: 'spin 1s linear infinite' }} />}
              Update password
            </button>
          </form>
        </div>
      </motion.div>
    </div>
  )
}
