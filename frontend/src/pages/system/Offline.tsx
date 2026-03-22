import { WifiOff, RefreshCw, BookOpen, Map, FileText, Zap } from 'lucide-react'
import { motion } from 'framer-motion'

const C = { surface: '#f5f6f7', surfaceLow: '#eff1f2', surfaceLowest: '#ffffff', onSurface: '#2c2f30', onSurfaceVariant: '#595c5d', outlineVariant: '#abadae', primary: '#00628c', primaryContainer: '#34b5fa' }
const GRADIENT = `linear-gradient(135deg, ${C.primary} 0%, ${C.primaryContainer} 100%)`

const offlineFeatures = [
  { icon: <BookOpen size={16} />, label: 'Syllabus tracker', desc: 'Mark topics done while offline — syncs when reconnected' },
  { icon: <Map size={16} />, label: 'Last saved roadmap', desc: 'Your most recent roadmap is cached locally' },
  { icon: <FileText size={16} />, label: 'Saved notes & sources', desc: 'View previously loaded study materials' },
  { icon: <Zap size={16} />, label: 'Practice questions', desc: 'Flashcards and saved questions remain accessible' },
]

export default function Offline() {
  return (
    <div style={{
      minHeight: '100vh', background: `linear-gradient(160deg, #e8f3ff 0%, #f0eeff 40%, ${C.surface} 70%)`,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      padding: 24, position: 'relative', overflow: 'hidden',
    }}>
      <div style={{ position: 'absolute', top: -120, left: -120, width: 420, height: 420, borderRadius: '50%', background: 'radial-gradient(circle, rgba(52,181,250,0.18) 0%, transparent 70%)', filter: 'blur(60px)' }} />

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        style={{ width: '100%', maxWidth: 480, position: 'relative', zIndex: 1 }}
      >
        <div style={{ background: 'rgba(255,255,255,0.93)', backdropFilter: 'blur(20px)', WebkitBackdropFilter: 'blur(20px)', borderRadius: 28, padding: 36, border: '1px solid rgba(171,173,174,0.18)', boxShadow: '0 20px 48px rgba(0,98,140,0.08)', textAlign: 'center' }}>
          <div style={{ width: 64, height: 64, borderRadius: '50%', background: C.surfaceLow, display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 20px' }}>
            <WifiOff size={28} color={C.outlineVariant} />
          </div>
          <h1 style={{ fontSize: 24, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: '0 0 10px' }}>You're offline</h1>
          <p style={{ fontSize: 14, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, margin: '0 0 28px', lineHeight: 1.6 }}>
            Don't worry — here's what still works while you're offline:
          </p>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 10, textAlign: 'left', marginBottom: 28 }}>
            {offlineFeatures.map((f, i) => (
              <div key={i} style={{ display: 'flex', alignItems: 'flex-start', gap: 14, background: C.surfaceLow, borderRadius: 14, padding: '12px 16px' }}>
                <div style={{ width: 34, height: 34, borderRadius: 10, background: 'rgba(0,98,140,0.08)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, color: C.primary }}>
                  {f.icon}
                </div>
                <div>
                  <p style={{ fontSize: 13, fontFamily: 'Manrope,sans-serif', fontWeight: 700, color: C.onSurface, margin: '0 0 2px' }}>{f.label}</p>
                  <p style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, margin: 0, lineHeight: 1.5 }}>{f.desc}</p>
                </div>
              </div>
            ))}
          </div>

          <button
            onClick={() => window.location.reload()}
            style={{
              display: 'inline-flex', alignItems: 'center', gap: 8, padding: '12px 28px',
              borderRadius: 100, border: 'none', cursor: 'pointer',
              background: GRADIENT, color: '#fff',
              fontFamily: 'Manrope,sans-serif', fontSize: 14, fontWeight: 700,
              boxShadow: '0 4px 16px rgba(0,98,140,0.25)',
            }}
          >
            <RefreshCw size={16} /> Retry connection
          </button>
        </div>
      </motion.div>
    </div>
  )
}
