import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { CheckCircle, AlertCircle, Loader2 } from 'lucide-react'
import { motion } from 'framer-motion'
import Logo from '../../components/layout/Logo'
import client from '../../api/client'
import { ENDPOINTS } from '../../api/endpoints'

const C = { surface: '#f5f6f7', onSurface: '#2c2f30', onSurfaceVariant: '#595c5d', primary: '#00628c', primaryContainer: '#34b5fa' }
const GRADIENT = `linear-gradient(135deg, ${C.primary} 0%, ${C.primaryContainer} 100%)`

export default function VerifyEmail() {
  const { token } = useParams<{ token: string }>()
  const navigate = useNavigate()
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading')

  useEffect(() => {
    if (!token) { setStatus('error'); return }
    client.post(ENDPOINTS.AUTH_VERIFY_EMAIL, { token })
      .then(() => { setStatus('success'); setTimeout(() => navigate('/onboarding'), 1500) })
      .catch(() => setStatus('error'))
  }, [token, navigate])

  return (
    <div style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: `linear-gradient(160deg, #e8f3ff 0%, #f0eeff 40%, ${C.surface} 70%)`,
      padding: 24, position: 'relative', overflow: 'hidden',
    }}>
      <div style={{ position: 'absolute', top: -100, left: -100, width: 380, height: 380, borderRadius: '50%', background: 'radial-gradient(circle, rgba(52,181,250,0.22) 0%, transparent 70%)', filter: 'blur(60px)' }} />
      <div style={{ position: 'absolute', bottom: -80, right: -80, width: 320, height: 320, borderRadius: '50%', background: 'radial-gradient(circle, rgba(175,166,255,0.22) 0%, transparent 70%)', filter: 'blur(60px)' }} />

      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4 }}
        style={{ textAlign: 'center', maxWidth: 380, position: 'relative', zIndex: 1 }}
      >
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 28 }}><Logo /></div>

        <div style={{
          background: 'rgba(255,255,255,0.92)', backdropFilter: 'blur(20px)', WebkitBackdropFilter: 'blur(20px)',
          borderRadius: 24, padding: '40px 32px', border: '1px solid rgba(171,173,174,0.18)',
          boxShadow: '0 20px 48px rgba(0,98,140,0.08)',
        }}>
          {status === 'loading' && (
            <>
              <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 16 }}>
                <Loader2 size={40} color={C.primary} style={{ animation: 'spin 1s linear infinite' }} />
              </div>
              <p style={{ fontSize: 15, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant }}>Verifying your email...</p>
            </>
          )}
          {status === 'success' && (
            <>
              <div style={{ width: 64, height: 64, borderRadius: '50%', background: 'rgba(22,101,52,0.08)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 16px' }}>
                <CheckCircle size={32} color="#166534" />
              </div>
              <h2 style={{ fontSize: 22, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: '0 0 8px' }}>Email verified!</h2>
              <p style={{ fontSize: 14, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant }}>Setting up your profile...</p>
            </>
          )}
          {status === 'error' && (
            <>
              <div style={{ width: 64, height: 64, borderRadius: '50%', background: 'rgba(179,27,37,0.08)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 16px' }}>
                <AlertCircle size={32} color="#b31b25" />
              </div>
              <h2 style={{ fontSize: 22, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: '0 0 8px' }}>This link has expired.</h2>
              <p style={{ fontSize: 14, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, marginBottom: 20, lineHeight: 1.6 }}>
                Please request a new verification email from your settings.
              </p>
              <button
                onClick={() => navigate('/home')}
                style={{
                  display: 'inline-flex', padding: '12px 24px', borderRadius: 100, border: 'none', cursor: 'pointer',
                  background: GRADIENT, color: '#fff', fontFamily: 'Manrope,sans-serif', fontSize: 14, fontWeight: 700,
                  boxShadow: '0 4px 16px rgba(0,98,140,0.25)',
                }}
              >
                Go to PrepCreatine
              </button>
            </>
          )}
        </div>
      </motion.div>
    </div>
  )
}
