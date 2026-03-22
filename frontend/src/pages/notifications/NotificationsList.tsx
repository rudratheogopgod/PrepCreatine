import { Bell, CheckCircle2 } from 'lucide-react'
import { motion } from 'framer-motion'
import PageWrapper from '../../components/layout/PageWrapper'
import { useNotificationStore } from '../../store/notificationStore'
import { timeAgo } from '../../utils/format'

const C = {
  surface: '#f5f6f7', surfaceLow: '#eff1f2', surfaceLowest: '#ffffff',
  onSurface: '#2c2f30', onSurfaceVariant: '#595c5d', outline: '#757778', outlineVariant: '#abadae',
  primary: '#00628c', primaryContainer: '#34b5fa',
}
const GRADIENT = `linear-gradient(135deg, ${C.primary} 0%, ${C.primaryContainer} 100%)`

export default function NotificationsList() {
  const { notifications, markAllRead } = useNotificationStore()

  return (
    <PageWrapper>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 32, flexWrap: 'wrap', gap: 12 }}>
        <div>
          <p style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: C.primary, margin: '0 0 6px' }}>INBOX</p>
          <h1 style={{ fontSize: 28, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: 0 }}>Notifications</h1>
        </div>
        {notifications.some(n => !n.isRead) && (
          <button
            onClick={markAllRead}
            style={{ fontSize: 13, fontFamily: 'Manrope,sans-serif', fontWeight: 700, color: C.primary, background: 'rgba(0,98,140,0.08)', border: 'none', borderRadius: 100, padding: '8px 18px', cursor: 'pointer' }}
          >
            Mark all as read
          </button>
        )}
      </div>

      {notifications.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '64px 24px', background: C.surfaceLowest, borderRadius: 24, border: '2px dashed rgba(171,173,174,0.25)' }}>
          <div style={{ width: 64, height: 64, borderRadius: '50%', background: C.surfaceLow, display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 16px' }}>
            <Bell size={28} color={C.outlineVariant} />
          </div>
          <h3 style={{ fontSize: 18, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: '0 0 8px' }}>All caught up!</h3>
          <p style={{ fontSize: 14, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, margin: 0 }}>You have no new notifications right now.</p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {notifications.map((n, i) => (
            <motion.div
              key={n.id}
              initial={{ opacity: 0, x: -8 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: i * 0.05 }}
              style={{
                display: 'flex', alignItems: 'flex-start', gap: 16,
                background: n.isRead ? C.surfaceLow : C.surfaceLowest,
                borderRadius: 18, padding: '16px 20px',
                border: `1px solid ${n.isRead ? 'transparent' : 'rgba(0,98,140,0.1)'}`,
                boxShadow: n.isRead ? 'none' : '0 2px 12px rgba(0,98,140,0.06)',
                opacity: n.isRead ? 0.7 : 1,
                transition: 'all 0.15s',
              }}
            >
              {/* Unread dot */}
              <div style={{ marginTop: 5, flexShrink: 0 }}>
                {n.isRead ? (
                  <CheckCircle2 size={14} color={C.outlineVariant} />
                ) : (
                  <div style={{ width: 8, height: 8, borderRadius: '50%', background: GRADIENT, marginTop: 3 }} />
                )}
              </div>

              <div style={{ flex: 1 }}>
                <p style={{ fontSize: 14, fontFamily: 'Manrope,sans-serif', fontWeight: n.isRead ? 400 : 600, color: C.onSurface, margin: '0 0 4px', lineHeight: 1.5 }}>
                  {n.title}
                </p>
                <p style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.outlineVariant, margin: 0 }}>
                  {timeAgo(n.createdAt)}
                </p>
              </div>
            </motion.div>
          ))}
        </div>
      )}
    </PageWrapper>
  )
}
