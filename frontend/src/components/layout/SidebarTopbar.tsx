import { useState, useRef, useEffect } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import {
  Home, MessageSquare, Map, BookOpen, ClipboardList,
  FileText, BarChart2, Users, Settings, Bell, Sun, Moon,
  Monitor, LogOut, Search, User, ChevronDown
} from 'lucide-react'
import { AnimatePresence, motion } from 'framer-motion'
import Logo from './Logo'
import Avatar from '../ui/Avatar'
import { useAuthStore } from '../../store/authStore'
import { useThemeStore, type Theme } from '../../store/themeStore'
import { useNotificationStore } from '../../store/notificationStore'

/* ─── Stitch "Cognitive Canvas" tokens ─────────────────── */
const C = {
  surface: '#f5f6f7',
  surfaceLow: '#eff1f2',
  surfaceLowest: '#ffffff',
  onSurface: '#2c2f30',
  onSurfaceVariant: '#595c5d',
  outline: '#757778',
  outlineVariant: '#abadae',
  primary: '#00628c',
  primaryContainer: '#34b5fa',
  tertiary: '#584cb5',
  tertiaryContainer: '#afa6ff',
}

const NAV = [
  { label: 'Home', to: '/home', icon: <Home size={17} /> },
  { label: 'Learn', to: '/learn', icon: <MessageSquare size={17} /> },
  { label: 'Roadmap', to: '/roadmap', icon: <Map size={17} /> },
  { label: 'Syllabus', to: '/syllabus', icon: <BookOpen size={17} /> },
  { label: 'Mock Tests', to: '/test', icon: <ClipboardList size={17} /> },
  { label: 'My Notes', to: '/notes', icon: <FileText size={17} /> },
  { label: 'Analytics', to: '/analytics', icon: <BarChart2 size={17} /> },
  { label: 'Community', to: '/community', icon: <Users size={17} /> },
]

/* ═══════════════════════════════════════════════════════ */
/*  SIDEBAR                                                */
/* ═══════════════════════════════════════════════════════ */
export function Sidebar({ onClose }: { onClose?: () => void }) {
  const user = useAuthStore(s => s.user)
  const logout = useAuthStore(s => s.logout)
  const navigate = useNavigate()

  return (
    <aside
      style={{
        width: 240,
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        background: 'rgba(255,255,255,0.95)',
        backdropFilter: 'blur(20px)',
        WebkitBackdropFilter: 'blur(20px)',
        borderRight: `1px solid rgba(171,173,174,0.15)`,
      }}
    >
      {/* Logo + subtitle */}
      <div style={{ padding: '20px 20px 16px' }}>
        <Logo />
        <p style={{ fontSize: 11, color: C.outline, fontFamily: 'Manrope,sans-serif', marginTop: 4, letterSpacing: '0.05em' }}>
          Student Dashboard
        </p>
      </div>

      {/* Nav */}
      <nav style={{ flex: 1, overflowY: 'auto', padding: '4px 12px' }}>
        {NAV.map(({ label, to, icon }) => (
          <NavLink
            key={to}
            to={to}
            onClick={onClose}
            style={{ textDecoration: 'none' }}
          >
            {({ isActive }) => (
              <div
                style={{
                  position: 'relative',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 10,
                  padding: '9px 12px',
                  borderRadius: 12,
                  marginBottom: 2,
                  fontFamily: 'Manrope,sans-serif',
                  fontSize: 13,
                  fontWeight: isActive ? 600 : 500,
                  color: isActive ? C.primary : C.onSurfaceVariant,
                  background: isActive ? `rgba(0,98,140,0.07)` : 'transparent',
                  cursor: 'pointer',
                  transition: 'all 0.15s',
                }}
                onMouseEnter={e => { if (!isActive) (e.currentTarget as HTMLDivElement).style.background = C.surfaceLow }}
                onMouseLeave={e => { if (!isActive) (e.currentTarget as HTMLDivElement).style.background = 'transparent' }}
              >
                {/* Active gradient left bar */}
                {isActive && (
                  <div style={{
                    position: 'absolute',
                    left: 0,
                    top: '50%',
                    transform: 'translateY(-50%)',
                    width: 3,
                    height: 20,
                    borderRadius: '0 4px 4px 0',
                    background: `linear-gradient(180deg, ${C.primary} 0%, ${C.primaryContainer} 100%)`,
                  }} />
                )}
                <span style={{ color: isActive ? C.primary : C.outlineVariant, display: 'flex' }}>{icon}</span>
                {label}
              </div>
            )}
          </NavLink>
        ))}

        {/* Tonal divider */}
        <div style={{ height: 1, background: C.surfaceLow, margin: '8px 4px' }} />

        <NavLink to="/settings" onClick={onClose} style={{ textDecoration: 'none' }}>
          {({ isActive }) => (
            <div
              style={{
                display: 'flex', alignItems: 'center', gap: 10, padding: '9px 12px',
                borderRadius: 12, fontFamily: 'Manrope,sans-serif', fontSize: 13,
                fontWeight: isActive ? 600 : 500,
                color: isActive ? C.primary : C.onSurfaceVariant,
                background: isActive ? `rgba(0,98,140,0.07)` : 'transparent',
                cursor: 'pointer', transition: 'all 0.15s',
              }}
              onMouseEnter={e => { if (!isActive) (e.currentTarget as HTMLDivElement).style.background = C.surfaceLow }}
              onMouseLeave={e => { if (!isActive) (e.currentTarget as HTMLDivElement).style.background = 'transparent' }}
            >
              <span style={{ color: isActive ? C.primary : C.outlineVariant, display: 'flex' }}><Settings size={17} /></span>
              Settings
            </div>
          )}
        </NavLink>
      </nav>

      {/* User profile footer */}
      <div style={{
        padding: '12px 16px 16px',
        background: C.surfaceLow,
        borderTop: `1px solid rgba(171,173,174,0.12)`,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
          <Avatar name={user?.fullName} size="sm" />
          <div style={{ flex: 1, minWidth: 0 }}>
            <p style={{ fontSize: 12, fontWeight: 700, fontFamily: '"Plus Jakarta Sans",sans-serif', color: C.onSurface, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
              {user?.fullName || 'Student'}
            </p>
            <p style={{ fontSize: 10, color: C.outline, fontFamily: 'Manrope,sans-serif', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
              {user?.email || 'Premium User'}
            </p>
          </div>
          <button
            onClick={() => { logout(); navigate('/') }}
            style={{ padding: '4px 6px', borderRadius: 8, border: 'none', background: 'transparent', color: C.outline, cursor: 'pointer', display: 'flex' }}
            aria-label="Log out"
            title="Log out"
          >
            <LogOut size={14} />
          </button>
        </div>
        <p style={{ fontSize: 10, textAlign: 'center', color: `rgba(117,119,120,0.6)`, fontFamily: 'Manrope,sans-serif' }}>
          Brought to you by Rudra Agrawal
        </p>
      </div>
    </aside>
  )
}

/* ═══════════════════════════════════════════════════════ */
/*  TOPBAR                                                 */
/* ═══════════════════════════════════════════════════════ */
export function Topbar({ onMenuClick, breadcrumb = 'Home' }: {
  onMenuClick?: () => void
  breadcrumb?: string
}) {
  const { theme, setTheme } = useThemeStore()
  const unreadCount = useNotificationStore(s => s.unreadCount)
  const user = useAuthStore(s => s.user)
  const logout = useAuthStore(s => s.logout)
  const navigate = useNavigate()
  const [userMenuOpen, setUserMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const close = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) setUserMenuOpen(false)
    }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [])

  const themeNext: Record<Theme, Theme> = { light: 'dark', dark: 'system', system: 'light' }
  const themeIcon: Record<Theme, React.ReactNode> = {
    light: <Sun size={17} />, dark: <Moon size={17} />, system: <Monitor size={17} />
  }

  return (
    <header style={{
      height: 60,
      display: 'flex',
      alignItems: 'center',
      padding: '0 24px',
      gap: 16,
      background: 'rgba(255,255,255,0.92)',
      backdropFilter: 'blur(20px)',
      WebkitBackdropFilter: 'blur(20px)',
      borderBottom: '1px solid rgba(171,173,174,0.12)',
      flexShrink: 0,
    }}>
      {/* Hamburger (mobile) */}
      {onMenuClick && (
        <button onClick={onMenuClick} style={{ display: 'flex', padding: 6, borderRadius: 8, border: 'none', background: 'transparent', cursor: 'pointer', color: C.onSurfaceVariant }}>
          <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
            <path d="M2 5h16M2 10h16M2 15h16" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
          </svg>
        </button>
      )}

      {/* Search bar — matches Stitch design */}
      <div style={{
        flex: 1,
        maxWidth: 320,
        display: 'flex',
        alignItems: 'center',
        gap: 8,
        background: C.surfaceLow,
        borderRadius: 100,
        padding: '7px 14px',
        border: '1px solid rgba(171,173,174,0.15)',
      }}>
        <Search size={14} color={C.outline} />
        <span style={{ fontSize: 13, fontFamily: 'Manrope,sans-serif', color: `rgba(117,119,120,0.7)` }}>
          Search concepts, tests...
        </span>
      </div>

      <div style={{ flex: 1 }} />

      {/* Right actions */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
        {/* Theme toggle */}
        <button
          onClick={() => setTheme(themeNext[theme])}
          style={{ padding: 8, borderRadius: 10, border: 'none', background: 'transparent', cursor: 'pointer', color: C.onSurfaceVariant, display: 'flex', transition: 'background 0.15s' }}
          aria-label="Toggle theme"
        >
          {themeIcon[theme]}
        </button>

        {/* Bell */}
        <button
          onClick={() => navigate('/notifications')}
          style={{ position: 'relative', padding: 8, borderRadius: 10, border: 'none', background: 'transparent', cursor: 'pointer', color: C.onSurfaceVariant, display: 'flex' }}
          aria-label="Notifications"
        >
          <Bell size={17} />
          {unreadCount > 0 && (
            <span style={{
              position: 'absolute', top: 4, right: 4, width: 14, height: 14, borderRadius: '50%',
              background: `linear-gradient(135deg, ${C.primary}, ${C.primaryContainer})`,
              color: '#fff', fontSize: 9, display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontFamily: 'Manrope,sans-serif', fontWeight: 700,
            }}>
              {unreadCount > 9 ? '9+' : unreadCount}
            </span>
          )}
        </button>

        {/* Avatar + dropdown */}
        <div ref={menuRef} style={{ position: 'relative' }}>
          <button
            onClick={() => setUserMenuOpen(v => !v)}
            style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '4px 6px', borderRadius: 10, border: 'none', background: 'transparent', cursor: 'pointer' }}
            aria-label="User menu"
          >
            <Avatar name={user?.fullName} size="sm" />
            <ChevronDown size={13} color={C.outlineVariant} />
          </button>

          <AnimatePresence>
            {userMenuOpen && (
              <motion.div
                initial={{ opacity: 0, y: 8, scale: 0.96 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: 8, scale: 0.96 }}
                transition={{ duration: 0.12 }}
                style={{
                  position: 'absolute', right: 0, top: '100%', marginTop: 8, width: 210,
                  background: 'rgba(255,255,255,0.96)', backdropFilter: 'blur(20px)',
                  borderRadius: 16, overflow: 'hidden',
                  boxShadow: '0 20px 40px rgba(0,98,140,0.10)',
                  border: '1px solid rgba(171,173,174,0.15)',
                  zIndex: 50,
                }}
              >
                <div style={{ padding: '12px 16px', background: C.surfaceLow }}>
                  <p style={{ fontSize: 13, fontWeight: 700, fontFamily: '"Plus Jakarta Sans",sans-serif', color: C.onSurface }}>{user?.fullName}</p>
                  <p style={{ fontSize: 11, color: C.outline, fontFamily: 'Manrope,sans-serif', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{user?.email}</p>
                </div>
                <button onClick={() => { navigate('/settings'); setUserMenuOpen(false) }}
                  style={{ width: '100%', display: 'flex', alignItems: 'center', gap: 10, padding: '10px 16px', border: 'none', background: 'transparent', cursor: 'pointer', fontFamily: 'Manrope,sans-serif', fontSize: 13, color: C.onSurfaceVariant }}>
                  <User size={14} /> Profile &amp; Settings
                </button>
                <button onClick={() => { logout(); navigate('/') }}
                  style={{ width: '100%', display: 'flex', alignItems: 'center', gap: 10, padding: '10px 16px', border: 'none', background: 'transparent', cursor: 'pointer', fontFamily: 'Manrope,sans-serif', fontSize: 13, color: '#b31b25' }}>
                  <LogOut size={14} /> Log out
                </button>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    </header>
  )
}
