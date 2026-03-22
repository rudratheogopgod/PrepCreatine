import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'

const C = { surface: '#f5f6f7', onSurface: '#2c2f30', onSurfaceVariant: '#595c5d', primary: '#00628c', primaryContainer: '#34b5fa' }
const GRADIENT = `linear-gradient(135deg, ${C.primary} 0%, ${C.primaryContainer} 100%)`

const STUDY_TIPS = [
  "Teach what you just learned to someone else — it's the fastest way to find gaps.",
  "Study in 25-minute Pomodoro sessions with 5-minute breaks to stay sharp.",
  "Write key formulas by hand — it encodes them better than reading.",
  "Review your notes within 24 hours to move facts to long-term memory.",
  "Practice old exam papers under timed conditions — simulate the real thing.",
  "Sleep converts your day's learning into memory. Don't skip it.",
  "Connect new concepts to things you already know — create mental hooks.",
  "Make mistakes on purpose in practice to understand where your logic breaks.",
  "Your brain consolidates memories during rest. Naps can boost recall 20%.",
  "One focused hour beats four distracted hours. Turn off your phone.",
]

const tip = STUDY_TIPS[Math.floor(Math.random() * STUDY_TIPS.length)]

export default function NotFound() {
  return (
    <div style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: `linear-gradient(160deg, #e8f3ff 0%, #f0eeff 40%, ${C.surface} 70%)`,
      padding: 24, position: 'relative', overflow: 'hidden',
    }}>
      <div style={{ position: 'absolute', top: -100, left: -100, width: 380, height: 380, borderRadius: '50%', background: 'radial-gradient(circle, rgba(52,181,250,0.2) 0%, transparent 70%)', filter: 'blur(70px)' }} />
      <div style={{ position: 'absolute', bottom: -100, right: -100, width: 320, height: 320, borderRadius: '50%', background: 'radial-gradient(circle, rgba(175,166,255,0.2) 0%, transparent 70%)', filter: 'blur(70px)' }} />

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        style={{ textAlign: 'center', maxWidth: 440, position: 'relative', zIndex: 1 }}
      >
        <div style={{ background: 'rgba(255,255,255,0.9)', backdropFilter: 'blur(20px)', WebkitBackdropFilter: 'blur(20px)', borderRadius: 28, padding: '48px 36px', border: '1px solid rgba(171,173,174,0.18)', boxShadow: '0 24px 60px rgba(0,98,140,0.08)' }}>
          <div style={{ fontSize: 88, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 900, background: GRADIENT, WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', lineHeight: 1, marginBottom: 16 }}>
            404
          </div>
          <h1 style={{ fontSize: 22, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: '0 0 10px' }}>
            Page missing in action
          </h1>
          <p style={{ fontSize: 14, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, lineHeight: 1.6, margin: '0 0 24px' }}>
            The page you&apos;re looking for doesn&apos;t exist or has been moved.
          </p>

          {/* Study tip */}
          <div style={{ background: 'rgba(0,98,140,0.05)', borderRadius: 16, padding: '14px 18px', marginBottom: 24, textAlign: 'left', borderLeft: `3px solid ${C.primaryContainer}` }}>
            <p style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: C.primary, margin: '0 0 6px' }}>Study tip while you&apos;re here</p>
            <p style={{ fontSize: 13, fontFamily: 'Manrope,sans-serif', color: C.onSurface, lineHeight: 1.6, margin: 0 }}>"{tip}"</p>
          </div>

          <Link to="/home" style={{ textDecoration: 'none' }}>
            <button style={{
              display: 'inline-flex', padding: '12px 32px', borderRadius: 100, border: 'none', cursor: 'pointer',
              background: GRADIENT, color: '#fff', fontFamily: 'Manrope,sans-serif', fontSize: 14, fontWeight: 700,
              boxShadow: '0 4px 16px rgba(0,98,140,0.25)',
            }}>
              Go Home
            </button>
          </Link>
        </div>
      </motion.div>
    </div>
  )
}
