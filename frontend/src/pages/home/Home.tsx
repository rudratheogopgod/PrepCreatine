import { useQuery } from '@tanstack/react-query'
import { MessageSquare, Map, ClipboardList, Gamepad2, FileText, Brain, Flame, Zap } from 'lucide-react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import PageWrapper from '../../components/layout/PageWrapper'
import ProgressBar from '../../components/ui/ProgressBar'
import { SkeletonCard } from '../../components/ui/Skeleton'
import { useAuthStore } from '../../store/authStore'
import { useUserContextStore } from '../../store/userContextStore'
import client from '../../api/client'
import { ENDPOINTS } from '../../api/endpoints'
import { daysUntil, timeAgo } from '../../utils/format'

/* ── Cognitive Canvas tokens ─────────────────────────── */
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
  onTertiaryContainer: '#2b1988',
}

const GRADIENT = `linear-gradient(135deg, ${C.primary} 0%, ${C.primaryContainer} 100%)`

interface DashboardData {
  topicsCompleted: number
  totalTopics: number
  streakDays: number
  studyMinutesToday: number
  recentActivity: { id: string; title: string; type: string; createdAt: string; subject?: string; score?: string; xp?: number }[]
}

const MODE_CARDS = [
  { title: 'Want to learn', desc: 'AI-curated learning paths for your weak spots.', icon: <MessageSquare size={18}/>, to: '/learn', color: '#00628c', bg: 'rgba(0,98,140,0.08)' },
  { title: 'Strategize', desc: 'Analyze performance and plan your roadmap.', icon: <Map size={18}/>, to: '/roadmap', color: '#584cb5', bg: 'rgba(88,76,181,0.08)' },
  { title: 'Exam time', desc: 'Full-length mock tests under pressure.', icon: <ClipboardList size={18}/>, to: '/test', color: '#b45309', bg: 'rgba(180,83,9,0.08)' },
  { title: 'Feeling low', desc: 'Quick gamified revision to boost mood.', icon: <Gamepad2 size={18}/>, to: '/game', color: '#166534', bg: 'rgba(22,101,52,0.08)' },
]

function getGreeting() {
  const h = new Date().getHours()
  return h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening'
}

export default function Home() {
  const user = useAuthStore(s => s.user)
  const { examType, examDate, dailyGoalMins } = useUserContextStore()

  const { data, isLoading } = useQuery<DashboardData>({
    queryKey: ['dashboard'],
    queryFn: async () => {
      const res = await client.get(ENDPOINTS.ME_CONTEXT)
      return res.data
    },
    staleTime: 60_000,
    placeholderData: {
      topicsCompleted: 24,
      totalTopics: 120,
      streakDays: 7,
      studyMinutesToday: 45,
      recentActivity: [
        { id: '1', title: 'Completed: Thermodynamics Concepts', type: 'learn', createdAt: new Date(Date.now() - 7200000).toISOString(), subject: 'Physics', xp: 120 },
        { id: '2', title: 'Mock Test: Organic Chemistry II', type: 'test', createdAt: new Date(Date.now() - 86400000).toISOString(), subject: 'Chemistry', score: '88%' },
        { id: '3', title: 'Updated Roadmap: Math Strategy', type: 'roadmap', createdAt: new Date(Date.now() - 172800000).toISOString(), subject: 'Calculus' },
      ],
    },
  })

  const firstName = user?.fullName?.split(' ')[0] || 'Rudra'
  const greeting = getGreeting()
  const daysLeft = examDate ? daysUntil(examDate) : 47
  const completionPct = data ? Math.round((data.topicsCompleted / data.totalTopics) * 100) : 20
  const studyPct = data ? Math.min(100, Math.round((data.studyMinutesToday / dailyGoalMins) * 100)) : 50

  return (
    <PageWrapper>
      <div style={{ maxWidth: 900, margin: '0 auto' }}>

        {/* ── Greeting ─────────────────────────── */}
        <div style={{ marginBottom: 28 }}>
          <p style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.12em', textTransform: 'uppercase', color: C.primary, marginBottom: 4 }}>
            {greeting}
          </p>
          <h1 style={{ fontSize: 28, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, lineHeight: 1.1, margin: 0 }}>
            Good morning, {firstName} 👋
          </h1>
          <p style={{ fontSize: 13, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, marginTop: 6 }}>
            Your cognitive sanctuary is ready for today&apos;s goals.
          </p>
        </div>

        {/* ── Stat cards container ──────────────── */}
        {isLoading ? (
          <div className="grid grid-cols-3 gap-4 mb-7">
            <SkeletonCard/><SkeletonCard/><SkeletonCard/>
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 16, marginBottom: 28 }}>
            {/* Exam Countdown */}
            <div style={{ background: C.surfaceLowest, borderRadius: 20, padding: '20px 24px', boxShadow: '0 2px 12px rgba(0,98,140,0.06)' }}>
              <p style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 600, letterSpacing: '0.08em', textTransform: 'uppercase', color: C.outline, marginBottom: 10 }}>
                Exam Countdown
              </p>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: 6 }}>
                <span style={{ fontSize: 40, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, lineHeight: 1 }}>
                  {daysLeft}
                </span>
                <span style={{ fontSize: 14, fontFamily: 'Manrope,sans-serif', background: GRADIENT, WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', fontWeight: 600 }}>
                  days to {examType?.toUpperCase() || 'JEE'}
                </span>
              </div>
            </div>

            {/* Syllabus Progress */}
            <div style={{ background: C.surfaceLowest, borderRadius: 20, padding: '20px 24px', boxShadow: '0 2px 12px rgba(0,98,140,0.06)' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 }}>
                <p style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 600, letterSpacing: '0.08em', textTransform: 'uppercase', color: C.outline }}>
                  Syllabus Progress
                </p>
                <span style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, padding: '2px 8px', borderRadius: 100, background: `rgba(0,98,140,0.08)`, color: C.primary }}>
                  {completionPct}% Done
                </span>
              </div>
              <p style={{ fontSize: 20, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 700, color: C.onSurface, marginBottom: 12 }}>
                {data?.topicsCompleted} of {data?.totalTopics} topics
              </p>
              <div style={{ height: 5, background: `rgba(171,173,174,0.3)`, borderRadius: 100, overflow: 'hidden' }}>
                <div style={{ height: '100%', width: `${completionPct}%`, background: GRADIENT, borderRadius: 100, transition: 'width 0.6s ease' }} />
              </div>
            </div>

            {/* Daily Study Goal */}
            <div style={{ background: C.surfaceLowest, borderRadius: 20, padding: '20px 24px', boxShadow: '0 2px 12px rgba(0,98,140,0.06)' }}>
              <p style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 600, letterSpacing: '0.08em', textTransform: 'uppercase', color: C.outline, marginBottom: 10 }}>
                Daily Study Goal
              </p>
              <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                {/* Circular progress */}
                <div style={{ position: 'relative', width: 52, height: 52, flexShrink: 0 }}>
                  <svg viewBox="0 0 52 52" style={{ width: 52, height: 52, transform: 'rotate(-90deg)' }}>
                    <circle cx="26" cy="26" r="22" fill="none" stroke={`rgba(171,173,174,0.25)`} strokeWidth="4" />
                    <circle cx="26" cy="26" r="22" fill="none" stroke={C.primary} strokeWidth="4" strokeLinecap="round"
                      strokeDasharray={`${2 * Math.PI * 22}`}
                      strokeDashoffset={`${2 * Math.PI * 22 * (1 - studyPct / 100)}`}
                      style={{ transition: 'stroke-dashoffset 0.6s ease' }}
                    />
                  </svg>
                  <span style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, color: C.primary }}>
                    {studyPct}%
                  </span>
                </div>
                <div>
                  <p style={{ fontSize: 22, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 700, color: C.onSurface, lineHeight: 1 }}>
                    {data?.studyMinutesToday}/{dailyGoalMins} <span style={{ fontSize: 13, fontWeight: 500, color: C.outline }}>min</span>
                  </p>
                  <p style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, marginTop: 2 }}>
                    Keep going, {firstName}!
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ── Streak banner ──────────────────────── */}
        {(data?.streakDays ?? 0) >= 3 && (
          <motion.div
            initial={{ opacity: 0, x: -8 }}
            animate={{ opacity: 1, x: 0 }}
            style={{
              display: 'flex', alignItems: 'center', gap: 10, padding: '12px 20px',
              borderRadius: 16, marginBottom: 28,
              background: 'rgba(245,158,11,0.08)',
              borderLeft: '3px solid #f59e0b',
            }}
          >
            <Flame size={16} color="#f59e0b" />
            <p style={{ fontSize: 13, fontFamily: 'Manrope,sans-serif', color: '#92400e', margin: 0 }}>
              You&apos;re on a <strong>{data?.streakDays}-day streak!</strong> Consistency is the bridge between goals and accomplishment. Keep it up!
            </p>
          </motion.div>
        )}

        {/* ── Mode cards + Activity side by side ── */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 340px', gap: 24, alignItems: 'start' }}>

          {/* Left: mode cards */}
          <div>
            <p style={{ fontSize: 13, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 700, color: C.onSurface, marginBottom: 14 }}>
              What would you like to do today?
            </p>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              {MODE_CARDS.map((m, i) => (
                <motion.div
                  key={m.to}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: i * 0.07 }}
                >
                  <Link to={m.to} style={{ textDecoration: 'none' }}>
                    <div
                      style={{
                        background: C.surfaceLowest,
                        borderRadius: 16,
                        padding: '18px 20px',
                        border: '1px solid rgba(171,173,174,0.12)',
                        cursor: 'pointer',
                        transition: 'all 0.2s',
                        boxShadow: '0 2px 8px rgba(0,98,140,0.04)',
                      }}
                      onMouseEnter={e => {
                        const el = e.currentTarget as HTMLDivElement
                        el.style.transform = 'translateY(-2px)'
                        el.style.boxShadow = '0 8px 24px rgba(0,98,140,0.10)'
                      }}
                      onMouseLeave={e => {
                        const el = e.currentTarget as HTMLDivElement
                        el.style.transform = 'translateY(0)'
                        el.style.boxShadow = '0 2px 8px rgba(0,98,140,0.04)'
                      }}
                    >
                      <div style={{
                        width: 36, height: 36, borderRadius: 10, marginBottom: 12,
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        background: m.bg, color: m.color,
                      }}>
                        {m.icon}
                      </div>
                      <p style={{ fontSize: 13, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 700, color: C.onSurface, marginBottom: 4 }}>
                        {m.title}
                      </p>
                      <p style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, lineHeight: 1.5 }}>
                        {m.desc}
                      </p>
                    </div>
                  </Link>
                </motion.div>
              ))}
            </div>
          </div>

          {/* Right: Activity + AI Insight */}
          <div>
            {/* Recent Activity */}
            <div style={{ marginBottom: 20 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
                <p style={{ fontSize: 13, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 700, color: C.onSurface }}>
                  Recent Activity
                </p>
                <button style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.primary, background: 'none', border: 'none', cursor: 'pointer', fontWeight: 600 }}>
                  View all
                </button>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                {data?.recentActivity.map((act) => {
                  const iconColor = act.type === 'learn' ? C.primary : act.type === 'test' ? '#b45309' : C.tertiary
                  const iconBg = act.type === 'learn' ? 'rgba(0,98,140,0.08)' : act.type === 'test' ? 'rgba(180,83,9,0.08)' : 'rgba(88,76,181,0.08)'
                  const Icon = act.type === 'learn' ? MessageSquare : act.type === 'test' ? ClipboardList : Map
                  return (
                    <div
                      key={act.id}
                      style={{
                        display: 'flex', alignItems: 'flex-start', gap: 12, padding: '12px 14px',
                        borderRadius: 14, background: C.surfaceLowest,
                        border: '1px solid rgba(171,173,174,0.10)',
                        boxShadow: '0 1px 4px rgba(0,98,140,0.04)',
                      }}
                    >
                      <div style={{ width: 32, height: 32, borderRadius: 8, flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', background: iconBg, color: iconColor }}>
                        <Icon size={14} />
                      </div>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <p style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', fontWeight: 600, color: C.onSurface, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                          {act.title}
                        </p>
                        <p style={{ fontSize: 11, color: C.outline, fontFamily: 'Manrope,sans-serif', marginTop: 1 }}>
                          {act.subject} · {timeAgo(act.createdAt)}
                        </p>
                      </div>
                      {act.xp && (
                        <span style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, color: C.primary, flexShrink: 0, background: `rgba(0,98,140,0.08)`, padding: '2px 6px', borderRadius: 100 }}>+{act.xp}XP</span>
                      )}
                      {act.score && (
                        <span style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, color: '#166534', flexShrink: 0, background: 'rgba(22,101,52,0.08)', padding: '2px 6px', borderRadius: 100 }}>{act.score}</span>
                      )}
                    </div>
                  )
                })}
              </div>
            </div>

            {/* Daily Insight — AI Card (Stitch signature tertiary component) */}
            <div style={{ marginBottom: 14 }}>
              <p style={{ fontSize: 13, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 700, color: C.onSurface, marginBottom: 14 }}>
                Daily Insight
              </p>
              <div style={{
                borderRadius: 20, padding: '20px', overflow: 'hidden', position: 'relative',
                background: `linear-gradient(135deg, ${C.tertiaryContainer} 0%, rgba(175,166,255,0.7) 100%)`,
              }}>
                {/* Decorative blob */}
                <div style={{
                  position: 'absolute', top: -20, right: -20, width: 100, height: 100, borderRadius: '50%',
                  background: 'rgba(255,255,255,0.25)',
                }} />

                {/* AI chip */}
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 12 }}>
                  <Brain size={13} color={C.onTertiaryContainer} />
                  <p style={{ fontSize: 10, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: C.onTertiaryContainer }}>
                    AI Recommendation
                  </p>
                </div>

                <p style={{ fontSize: 15, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 700, color: C.onTertiaryContainer, lineHeight: 1.35, marginBottom: 10 }}>
                  You&apos;re mastering Mechanics faster than 85% of peers.
                </p>
                <p style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: `rgba(43,25,136,0.75)`, lineHeight: 1.5, marginBottom: 16 }}>
                  Based on your recent test results, shifting 20 mins from Physics to Calculus could improve your overall percentile by 3-4 points.
                </p>

                <button
                  onClick={() => {}}
                  style={{
                    display: 'flex', alignItems: 'center', gap: 8, padding: '10px 18px',
                    borderRadius: 100, border: 'none', cursor: 'pointer',
                    background: C.tertiary, color: '#fff',
                    fontFamily: 'Manrope,sans-serif', fontSize: 13, fontWeight: 700,
                    width: '100%', justifyContent: 'center',
                    boxShadow: '0 4px 16px rgba(88,76,181,0.35)',
                  }}
                >
                  <Zap size={14} /> Adjust Roadmap
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </PageWrapper>
  )
}
