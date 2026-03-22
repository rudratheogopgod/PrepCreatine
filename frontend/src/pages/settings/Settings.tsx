import { useState } from 'react'
import { toast } from 'react-hot-toast'
import { User, Bell, Palette, CreditCard, Shield, Check, Zap } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import PageWrapper from '../../components/layout/PageWrapper'
import { useAuthStore } from '../../store/authStore'
import { useThemeStore } from '../../store/themeStore'

const C = {
  surface: '#f5f6f7', surfaceLow: '#eff1f2', surfaceLowest: '#ffffff',
  onSurface: '#2c2f30', onSurfaceVariant: '#595c5d', outline: '#757778', outlineVariant: '#abadae',
  primary: '#00628c', primaryContainer: '#34b5fa',
  tertiary: '#584cb5', tertiaryContainer: '#afa6ff',
}
const GRADIENT = `linear-gradient(135deg, ${C.primary} 0%, ${C.primaryContainer} 100%)`

export default function Settings() {
  const user = useAuthStore(s => s.user)
  const { theme, setTheme } = useThemeStore()
  const [activeTab, setActiveTab] = useState('profile')
  const [fullName, setFullName] = useState(user?.fullName ?? '')

  const handleSaveProfile = (e: React.FormEvent) => {
    e.preventDefault()
    toast.success('Profile updated!')
  }

  const tabs = [
    { id: 'profile', icon: <User size={17} />, label: 'Profile' },
    { id: 'appearance', icon: <Palette size={17} />, label: 'Appearance' },
    { id: 'notifications', icon: <Bell size={17} />, label: 'Notifications' },
    { id: 'subscription', icon: <CreditCard size={17} />, label: 'Subscription' },
    { id: 'security', icon: <Shield size={17} />, label: 'Security' },
  ]

  const Card = ({ children, style }: { children: React.ReactNode; style?: React.CSSProperties }) => (
    <div style={{ background: C.surfaceLowest, borderRadius: 20, padding: 28, boxShadow: '0 2px 12px rgba(0,98,140,0.06)', border: '1px solid rgba(171,173,174,0.15)', ...style }}>
      {children}
    </div>
  )

  const SectionTitle = ({ children }: { children: React.ReactNode }) => (
    <h2 style={{ fontSize: 17, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: '0 0 24px' }}>{children}</h2>
  )

  const Label = ({ children }: { children: React.ReactNode }) => (
    <label style={{ display: 'block', fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: C.outline, marginBottom: 7 }}>
      {children}
    </label>
  )

  const FieldInput = ({ value, onChange, disabled, type = 'text', helper }: { value: string; onChange?: (v: string) => void; disabled?: boolean; type?: string; helper?: string }) => (
    <div>
      <input
        type={type}
        value={value}
        disabled={disabled}
        onChange={e => onChange?.(e.target.value)}
        style={{
          width: '100%', boxSizing: 'border-box',
          background: disabled ? C.surfaceLow : C.surfaceLow,
          border: '1px solid rgba(171,173,174,0.2)', borderRadius: 12,
          padding: '11px 16px', fontFamily: 'Manrope,sans-serif', fontSize: 14, color: disabled ? C.outlineVariant : C.onSurface,
          outline: 'none', cursor: disabled ? 'not-allowed' : 'auto',
        }}
        onFocus={e => { if (!disabled) { e.target.style.background = C.surfaceLowest; e.target.style.boxShadow = `0 0 0 2px rgba(52,181,250,0.35)` } }}
        onBlur={e => { e.target.style.background = C.surfaceLow; e.target.style.boxShadow = 'none' }}
      />
      {helper && <p style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', color: C.outlineVariant, marginTop: 5 }}>{helper}</p>}
    </div>
  )

  const ToggleRow = ({ label, desc, defaultChecked }: { label: string; desc: string; defaultChecked?: boolean }) => {
    const [checked, setChecked] = useState(defaultChecked ?? false)
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '16px 0', borderBottom: '1px solid rgba(171,173,174,0.15)' }}>
        <div>
          <p style={{ fontSize: 14, fontFamily: 'Manrope,sans-serif', fontWeight: 600, color: C.onSurface, margin: 0 }}>{label}</p>
          <p style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, margin: '3px 0 0' }}>{desc}</p>
        </div>
        <button
          onClick={() => setChecked(!checked)}
          style={{
            width: 44, height: 24, borderRadius: 100, border: 'none', cursor: 'pointer',
            background: checked ? GRADIENT : 'rgba(171,173,174,0.35)',
            position: 'relative', transition: 'all 0.2s',
          }}
        >
          <div style={{
            position: 'absolute', top: 3, left: checked ? 22 : 3,
            width: 18, height: 18, borderRadius: '50%', background: '#fff',
            transition: 'left 0.2s', boxShadow: '0 1px 4px rgba(0,0,0,0.15)',
          }} />
        </button>
      </div>
    )
  }

  return (
    <PageWrapper>
      <div style={{ marginBottom: 32 }}>
        <p style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: C.primary, margin: '0 0 6px' }}>SETTINGS</p>
        <h1 style={{ fontSize: 28, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: '0 0 4px' }}>Account Settings</h1>
        <p style={{ fontSize: 14, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, margin: 0 }}>Manage your account preferences and study configuration.</p>
      </div>

      <div style={{ display: 'flex', gap: 28, alignItems: 'flex-start' }}>
        {/* Sidebar nav */}
        <aside style={{ width: 220, flexShrink: 0 }}>
          <div style={{ background: C.surfaceLowest, borderRadius: 20, padding: 12, boxShadow: '0 2px 12px rgba(0,98,140,0.06)', border: '1px solid rgba(171,173,174,0.15)' }}>
            {tabs.map(t => (
              <button
                key={t.id}
                onClick={() => setActiveTab(t.id)}
                style={{
                  width: '100%', display: 'flex', alignItems: 'center', gap: 12,
                  padding: '11px 14px', borderRadius: 14, border: 'none', cursor: 'pointer',
                  background: activeTab === t.id ? 'rgba(0,98,140,0.07)' : 'none',
                  color: activeTab === t.id ? C.primary : C.onSurfaceVariant,
                  fontFamily: 'Manrope,sans-serif', fontSize: 13, fontWeight: 600,
                  textAlign: 'left', transition: 'all 0.15s', marginBottom: 2,
                  borderLeft: activeTab === t.id ? `3px solid ${C.primary}` : '3px solid transparent',
                }}
              >
                {t.icon} {t.label}
              </button>
            ))}
          </div>
        </aside>

        {/* Content */}
        <main style={{ flex: 1, minWidth: 0 }}>
          <AnimatePresence mode="wait">
            <motion.div
              key={activeTab}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.2 }}
            >
              {activeTab === 'profile' && (
                <Card>
                  <SectionTitle>Profile Details</SectionTitle>
                  <form onSubmit={handleSaveProfile} style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
                    <div><Label>Full name</Label><FieldInput value={fullName} onChange={setFullName} /></div>
                    <div><Label>Email address</Label><FieldInput value={user?.email ?? ''} disabled helper="Email cannot be changed" /></div>
                    <div style={{ display: 'flex', justifyContent: 'flex-end', paddingTop: 8 }}>
                      <button
                        type="submit"
                        style={{
                          padding: '11px 24px', borderRadius: 100, border: 'none', cursor: 'pointer',
                          background: GRADIENT, color: '#fff',
                          fontFamily: 'Manrope,sans-serif', fontSize: 14, fontWeight: 700,
                          boxShadow: '0 4px 14px rgba(0,98,140,0.25)',
                        }}
                      >
                        Save Changes
                      </button>
                    </div>
                  </form>
                </Card>
              )}

              {activeTab === 'appearance' && (
                <Card>
                  <SectionTitle>App Theme</SectionTitle>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 14 }}>
                    {(['light', 'dark', 'system'] as const).map(t => (
                      <button
                        key={t}
                        onClick={() => setTheme(t)}
                        style={{
                          padding: '20px 12px', borderRadius: 16, cursor: 'pointer',
                          border: `2px solid ${theme === t ? C.primary : 'rgba(171,173,174,0.25)'}`,
                          background: theme === t ? 'rgba(0,98,140,0.05)' : C.surfaceLowest,
                          fontFamily: 'Manrope,sans-serif', fontSize: 13, fontWeight: 600,
                          color: theme === t ? C.primary : C.onSurfaceVariant,
                          transition: 'all 0.15s', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10,
                        }}
                      >
                        <span style={{ fontSize: 26 }}>{t === 'light' ? '☀️' : t === 'dark' ? '🌙' : '💻'}</span>
                        <span style={{ textTransform: 'capitalize' }}>{t}</span>
                        {theme === t && <div style={{ width: 18, height: 18, borderRadius: '50%', background: GRADIENT, display: 'flex', alignItems: 'center', justifyContent: 'center' }}><Check size={11} color="#fff" /></div>}
                      </button>
                    ))}
                  </div>
                </Card>
              )}

              {activeTab === 'notifications' && (
                <Card>
                  <SectionTitle>Notification Preferences</SectionTitle>
                  <ToggleRow label="Daily Reminders" desc="Get reminded to hit your daily study goal." defaultChecked />
                  <ToggleRow label="Community Replies" desc="When someone replies to your thread." defaultChecked />
                  <ToggleRow label="Streak Alerts" desc="When your streak is at risk of breaking." defaultChecked />
                  <ToggleRow label="Test Score Updates" desc="When your analytics are refreshed." />
                  <ToggleRow label="AI Recommendations" desc="Weekly AI-generated study plan suggestions." defaultChecked />
                </Card>
              )}

              {activeTab === 'subscription' && (
                <Card style={{ textAlign: 'center', padding: '48px 28px' }}>
                  <div style={{ width: 64, height: 64, borderRadius: '50%', background: GRADIENT, display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 20px' }}>
                    <Zap size={28} color="#fff" />
                  </div>
                  <h2 style={{ fontSize: 22, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: '0 0 8px' }}>Free Plan</h2>
                  <p style={{ fontSize: 14, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, margin: '0 auto 28px', maxWidth: 300, lineHeight: 1.6 }}>
                    You are on the free plan. Upgrade to Premium to unlock AI tutoring, unlimited tests, and priority support.
                  </p>
                  <button
                    style={{
                      padding: '13px 36px', borderRadius: 100, border: 'none', cursor: 'pointer',
                      background: GRADIENT, color: '#fff',
                      fontFamily: 'Manrope,sans-serif', fontSize: 15, fontWeight: 700,
                      boxShadow: '0 4px 20px rgba(0,98,140,0.3)',
                    }}
                  >
                    Upgrade to Premium
                  </button>
                  <p style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.outlineVariant, marginTop: 14 }}>7-day free trial · Cancel anytime</p>
                </Card>
              )}

              {activeTab === 'security' && (
                <Card>
                  <SectionTitle>Security & Password</SectionTitle>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                    <button
                      style={{
                        padding: '12px 24px', borderRadius: 100, border: `1.5px solid rgba(171,173,174,0.3)`, cursor: 'pointer',
                        background: C.surfaceLow, color: C.onSurface,
                        fontFamily: 'Manrope,sans-serif', fontSize: 14, fontWeight: 600,
                        width: 'fit-content', transition: 'all 0.15s',
                      }}
                    >
                      Change Password
                    </button>
                    <p style={{ fontSize: 13, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, lineHeight: 1.6 }}>
                      You can also <a href="/forgot-password" style={{ color: C.primary, fontWeight: 600 }}>reset via email</a> if you've forgotten your current password.
                    </p>
                  </div>
                </Card>
              )}
            </motion.div>
          </AnimatePresence>
        </main>
      </div>
    </PageWrapper>
  )
}
