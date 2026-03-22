import { useQuery } from '@tanstack/react-query'
import {
  Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement,
  BarElement, Title, Tooltip, Legend, Filler
} from 'chart.js'
import { Line, Bar } from 'react-chartjs-2'
import { Target, TrendingUp, AlertTriangle, Activity, Flame, ArrowUp, ArrowDown } from 'lucide-react'
import { motion } from 'framer-motion'
import PageWrapper from '../../components/layout/PageWrapper'
import client from '../../api/client'
import { ENDPOINTS } from '../../api/endpoints'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, Title, Tooltip, Legend, Filler)

const C = {
  surface: '#f5f6f7', surfaceLow: '#eff1f2', surfaceLowest: '#ffffff',
  onSurface: '#2c2f30', onSurfaceVariant: '#595c5d', outline: '#757778', outlineVariant: '#abadae',
  primary: '#00628c', primaryContainer: '#34b5fa',
  tertiary: '#584cb5', tertiaryContainer: '#afa6ff',
}
const GRADIENT = `linear-gradient(135deg, ${C.primary} 0%, ${C.primaryContainer} 100%)`

// ──────────────────────────────────────────────
// GitHub-style Activity Heatmap (53 × 7 SVG)
// ──────────────────────────────────────────────
function ActivityHeatmap({ data }: { data: { date: string; minutes: number }[] }) {
  const today = new Date()
  const WEEKS = 26
  const CELL = 13
  const GAP = 3
  const SIZE = CELL + GAP

  // Build 26w × 7d grid (most recent on right)
  const grid: { date: string; minutes: number }[][] = []
  const startDate = new Date(today)
  startDate.setDate(today.getDate() - WEEKS * 7 + 1)

  for (let w = 0; w < WEEKS; w++) {
    const week: { date: string; minutes: number }[] = []
    for (let d = 0; d < 7; d++) {
      const dt = new Date(startDate)
      dt.setDate(startDate.getDate() + w * 7 + d)
      const iso = dt.toISOString().split('T')[0]
      const entry = data.find(x => x.date === iso)
      week.push({ date: iso, minutes: entry?.minutes ?? 0 })
    }
    grid.push(week)
  }

  const max = Math.max(...data.map(d => d.minutes), 1)
  const color = (min: number) => {
    if (min === 0) return 'rgba(171,173,174,0.15)'
    const pct = min / max
    if (pct < 0.25) return 'rgba(0,98,140,0.25)'
    if (pct < 0.5) return 'rgba(0,98,140,0.5)'
    if (pct < 0.75) return 'rgba(0,98,140,0.75)'
    return C.primary
  }

  const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']
  const days = ['','M','','W','','F','']

  return (
    <div style={{ overflowX: 'auto', paddingBottom: 4 }}>
      <svg width={WEEKS * SIZE + 28} height={7 * SIZE + 28} style={{ display: 'block' }}>
        {/* Day labels */}
        {days.map((d, i) => d && (
          <text key={i} x={0} y={24 + i * SIZE} fontSize={10} fill={C.outlineVariant} fontFamily="Manrope,sans-serif">{d}</text>
        ))}
        {/* Month labels */}
        {grid.map((week, wi) => {
          const firstDay = new Date(week[0].date)
          if (firstDay.getDate() <= 7) {
            return <text key={wi} x={28 + wi * SIZE} y={10} fontSize={10} fill={C.outlineVariant} fontFamily="Manrope,sans-serif">{months[firstDay.getMonth()]}</text>
          }
          return null
        })}
        {/* Cells */}
        {grid.map((week, wi) =>
          week.map((cell, di) => (
            <rect
              key={`${wi}-${di}`}
              x={28 + wi * SIZE}
              y={16 + di * SIZE}
              width={CELL} height={CELL}
              rx={3} ry={3}
              fill={color(cell.minutes)}
              style={{ cursor: 'pointer' }}
            >
              <title>{cell.date}: {cell.minutes > 0 ? `${cell.minutes} min studied` : 'No activity'}</title>
            </rect>
          ))
        )}
      </svg>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 6 }}>
        <span style={{ fontSize: 10, fontFamily: 'Manrope,sans-serif', color: C.outlineVariant }}>Less</span>
        {[0, 0.25, 0.5, 0.75, 1].map((pct, i) => (
          <div key={i} style={{ width: 11, height: 11, borderRadius: 2, background: color(pct * max) }} />
        ))}
        <span style={{ fontSize: 10, fontFamily: 'Manrope,sans-serif', color: C.outlineVariant }}>More</span>
      </div>
    </div>
  )
}

// ──────────────────────────────────────
// SVG Circular Readiness Gauge
// ──────────────────────────────────────
function ReadinessGauge({ score }: { score: number }) {
  const R = 72, STROKE = 10
  const CIRC = 2 * Math.PI * R
  const pct = Math.min(Math.max(score, 0), 100)
  const offset = CIRC - (pct / 100) * CIRC
  const color = pct >= 70 ? '#1a7a4a' : pct >= 40 ? '#e07b00' : '#b31b25'

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12 }}>
      <svg width={172} height={172} style={{ overflow: 'visible' }}>
        {/* Track */}
        <circle cx={86} cy={86} r={R} fill="none" stroke="rgba(171,173,174,0.2)" strokeWidth={STROKE} />
        {/* Progress */}
        <motion.circle
          cx={86} cy={86} r={R} fill="none"
          stroke={color} strokeWidth={STROKE}
          strokeLinecap="round"
          strokeDasharray={CIRC}
          initial={{ strokeDashoffset: CIRC }}
          animate={{ strokeDashoffset: offset }}
          transition={{ duration: 1.2, ease: 'easeOut' }}
          style={{ transformOrigin: '86px 86px', transform: 'rotate(-90deg)' }}
        />
        <text x={86} y={82} textAnchor="middle" fontSize={30} fontWeight={900} fill={C.onSurface} fontFamily='"Plus Jakarta Sans",sans-serif'>{pct}%</text>
        <text x={86} y={102} textAnchor="middle" fontSize={12} fill={C.onSurfaceVariant} fontFamily='Manrope,sans-serif'>Readiness</text>
      </svg>
      <p style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, textAlign: 'center', maxWidth: 200, lineHeight: 1.6, margin: 0 }}>
        Based on completion rate, test scores, and days remaining.
      </p>
    </div>
  )
}

function StatCard({ icon, label, value, trend }: { icon: React.ReactNode; label: string; value: string | number; trend?: number }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      style={{ background: C.surfaceLowest, borderRadius: 20, padding: '20px 24px', boxShadow: '0 2px 12px rgba(0,98,140,0.06)', border: '1px solid rgba(171,173,174,0.15)', display: 'flex', alignItems: 'center', gap: 16 }}
    >
      <div style={{ width: 46, height: 46, borderRadius: 14, background: GRADIENT, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
        {icon}
      </div>
      <div style={{ flex: 1 }}>
        <p style={{ fontSize: 22, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: 0 }}>{value}</p>
        <p style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, margin: 0 }}>{label}</p>
      </div>
      {trend !== undefined && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 3, fontSize: 12, fontFamily: 'Manrope,sans-serif', color: trend >= 0 ? '#1a7a4a' : '#b31b25', fontWeight: 600 }}>
          {trend >= 0 ? <ArrowUp size={13} /> : <ArrowDown size={13} />}
          {Math.abs(trend)}%
        </div>
      )}
    </motion.div>
  )
}

function ChartCard({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div style={{ background: C.surfaceLowest, borderRadius: 20, padding: 24, boxShadow: '0 2px 12px rgba(0,98,140,0.06)', border: '1px solid rgba(171,173,174,0.15)' }}>
      <h3 style={{ fontSize: 14, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 700, color: C.onSurface, margin: '0 0 20px' }}>{title}</h3>
      {children}
    </div>
  )
}

const PLACEHOLDER_HEATMAP = Array.from({ length: 60 }, (_, i) => {
  const d = new Date(); d.setDate(d.getDate() - i)
  return { date: d.toISOString().split('T')[0], minutes: Math.random() > 0.4 ? Math.floor(Math.random() * 120 + 20) : 0 }
})

export default function Analytics() {
  const { data, isLoading } = useQuery({
    queryKey: ['analytics'],
    queryFn: async () => { const res = await client.get(ENDPOINTS.ANALYTICS); return res.data },
    placeholderData: {
      stats: { topicsCompleted: 34, testsTaken: 12, avgScore: 78, currentStreak: 7 },
      heatmap: PLACEHOLDER_HEATMAP,
      readiness: 67,
      weakness: [{ topic: 'Thermodynamics', subject: 'Physics', score: 45 }, { topic: 'Optics', subject: 'Physics', score: 55 }, { topic: 'Integration', subject: 'Math', score: 62 }, { topic: 'Organic Reactions', subject: 'Chemistry', score: 65 }, { topic: 'Electrostatics', subject: 'Physics', score: 68 }],
      strengths: [{ topic: 'Kinematics', subject: 'Physics', score: 92 }, { topic: 'Algebra', subject: 'Math', score: 88 }, { topic: 'Atomic Structure', subject: 'Chemistry', score: 85 }, { topic: 'Waves', subject: 'Physics', score: 82 }, { topic: 'Probability', subject: 'Math', score: 80 }],
      progressChart: { labels: ['W1','W2','W3','W4','W5','W6'], scores: [50,55,52,65,71,78] },
      activityChart: { labels: ['Mon','Tue','Wed','Thu','Fri','Sat','Sun'], minutes: [45,60,90,30,120,150,45] },
    }
  })

  const chartOptions = {
    responsive: true, maintainAspectRatio: false,
    plugins: { legend: { display: false }, tooltip: { backgroundColor: '#fff', titleColor: C.onSurface, bodyColor: C.onSurfaceVariant, borderColor: 'rgba(171,173,174,0.25)', borderWidth: 1, padding: 12, cornerRadius: 12 } },
    scales: {
      x: { grid: { display: false }, ticks: { color: C.outlineVariant, font: { family: 'Manrope', size: 11 } } },
      y: { grid: { color: 'rgba(171,173,174,0.15)' }, ticks: { color: C.outlineVariant, font: { family: 'Manrope', size: 11 } } },
    }
  }

  const stats = [
    { icon: <Target size={22} color="#fff" />, label: 'Topics Completed', value: data?.stats?.topicsCompleted ?? 0, trend: 12 },
    { icon: <Activity size={22} color="#fff" />, label: 'Tests Taken', value: data?.stats?.testsTaken ?? 0, trend: 5 },
    { icon: <TrendingUp size={22} color="#fff" />, label: 'Avg Score', value: `${data?.stats?.avgScore ?? 0}%`, trend: 8 },
    { icon: <Flame size={22} color="#fff" />, label: 'Day Streak', value: `${data?.stats?.currentStreak ?? 0}d`, trend: undefined },
  ]

  const subjectColors: Record<string, string> = { Physics: 'rgba(0,98,140,0.1)', Chemistry: 'rgba(88,76,181,0.1)', Math: 'rgba(224,123,0,0.1)' }
  const subjectText: Record<string, string> = { Physics: C.primary, Chemistry: C.tertiary, Math: '#e07b00' }

  return (
    <PageWrapper>
      <div style={{ marginBottom: 32 }}>
        <p style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: C.primary, margin: '0 0 6px' }}>ANALYTICS</p>
        <h1 style={{ fontSize: 28, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: '0 0 4px' }}>Progress Overview</h1>
        <p style={{ fontSize: 14, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, margin: 0 }}>Data-driven insights to optimise your learning path.</p>
      </div>

      {/* ROW 1: Stat cards */}
      {isLoading ? (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(180px,1fr))', gap: 16, marginBottom: 24 }}>
          {[1,2,3,4].map(i => <div key={i} style={{ height: 90, background: C.surfaceLow, borderRadius: 20 }} />)}
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(180px,1fr))', gap: 16, marginBottom: 24 }}>
          {stats.map((s, i) => <StatCard key={i} {...s} />)}
        </div>
      )}

      {/* ROW 2: Heatmap */}
      <ChartCard title="Activity — Last 26 Weeks">
        <ActivityHeatmap data={data?.heatmap ?? []} />
      </ChartCard>

      <div style={{ height: 20 }} />

      {/* ROW 3: Score trend + Activity bar */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20, marginBottom: 20 }}>
        <ChartCard title="Performance Trajectory">
          <div style={{ height: 200 }}>
            {data && <Line data={{ labels: data.progressChart.labels, datasets: [{ label: 'Score %', data: data.progressChart.scores, borderColor: C.primary, backgroundColor: 'rgba(0,98,140,0.08)', fill: true, tension: 0.45, pointBackgroundColor: C.primaryContainer, borderWidth: 2.5 }] }} options={{ ...chartOptions, scales: { ...chartOptions.scales, y: { ...chartOptions.scales.y, max: 100, min: 0 } } }} />}
          </div>
        </ChartCard>
        <ChartCard title="Activity — Last 7 Days (minutes)">
          <div style={{ height: 200 }}>
            {data && <Bar data={{ labels: data.activityChart.labels, datasets: [{ label: 'Minutes', data: data.activityChart.minutes, backgroundColor: C.primaryContainer, borderRadius: 8 }] }} options={chartOptions} />}
          </div>
        </ChartCard>
      </div>

      {/* ROW 4: Weaknesses + Strengths */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20, marginBottom: 20 }}>
        {/* Weaknesses */}
        <div style={{ background: C.surfaceLowest, borderRadius: 20, padding: 24, boxShadow: '0 2px 12px rgba(0,98,140,0.06)', border: '1px solid rgba(171,173,174,0.15)' }}>
          <h3 style={{ fontSize: 14, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 700, color: C.onSurface, margin: '0 0 20px', display: 'flex', alignItems: 'center', gap: 8 }}>
            <AlertTriangle size={16} color="#e07b00" /> Areas to Improve
          </h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            {data?.weakness?.map((w: { topic: string; subject: string; score: number }, i: number) => (
              <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <div style={{ flex: 1 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 5 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <span style={{ fontSize: 13, fontFamily: 'Manrope,sans-serif', fontWeight: 600, color: C.onSurface }}>{w.topic}</span>
                      <span style={{ fontSize: 10, background: subjectColors[w.subject] ?? 'rgba(171,173,174,0.15)', color: subjectText[w.subject] ?? C.outline, padding: '2px 7px', borderRadius: 100, fontWeight: 700, fontFamily: 'Manrope,sans-serif' }}>{w.subject}</span>
                    </div>
                    <span style={{ fontSize: 12, fontWeight: 700, color: '#e07b00', fontFamily: 'Manrope,sans-serif' }}>{w.score}%</span>
                  </div>
                  <div style={{ height: 6, borderRadius: 100, background: 'rgba(171,173,174,0.2)', overflow: 'hidden' }}>
                    <motion.div initial={{ width: 0 }} animate={{ width: `${w.score}%` }} transition={{ delay: 0.3 + i * 0.1, duration: 0.6 }} style={{ height: '100%', borderRadius: 100, background: 'linear-gradient(90deg, #e07b00, #f97316)' }} />
                  </div>
                </div>
                <a href="/learn" style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, color: C.primary, textDecoration: 'none', flexShrink: 0 }}>Study →</a>
              </div>
            ))}
          </div>
        </div>

        {/* Strengths */}
        <div style={{ background: C.surfaceLowest, borderRadius: 20, padding: 24, boxShadow: '0 2px 12px rgba(0,98,140,0.06)', border: '1px solid rgba(171,173,174,0.15)' }}>
          <h3 style={{ fontSize: 14, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 700, color: C.onSurface, margin: '0 0 20px', display: 'flex', alignItems: 'center', gap: 8 }}>
            <TrendingUp size={16} color="#1a7a4a" /> Your Strengths
          </h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            {data?.strengths?.map((s: { topic: string; subject: string; score: number }, i: number) => (
              <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <div style={{ flex: 1 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 5 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <span style={{ fontSize: 13, fontFamily: 'Manrope,sans-serif', fontWeight: 600, color: C.onSurface }}>{s.topic}</span>
                      <span style={{ fontSize: 10, background: subjectColors[s.subject] ?? 'rgba(171,173,174,0.15)', color: subjectText[s.subject] ?? C.outline, padding: '2px 7px', borderRadius: 100, fontWeight: 700, fontFamily: 'Manrope,sans-serif' }}>{s.subject}</span>
                    </div>
                    <span style={{ fontSize: 12, fontWeight: 700, color: '#1a7a4a', fontFamily: 'Manrope,sans-serif' }}>{s.score}%</span>
                  </div>
                  <div style={{ height: 6, borderRadius: 100, background: 'rgba(171,173,174,0.2)', overflow: 'hidden' }}>
                    <motion.div initial={{ width: 0 }} animate={{ width: `${s.score}%` }} transition={{ delay: 0.3 + i * 0.1, duration: 0.6 }} style={{ height: '100%', borderRadius: 100, background: 'linear-gradient(90deg, #1a7a4a, #34d374)' }} />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* ROW 5: Readiness gauge */}
      <div style={{ background: C.surfaceLowest, borderRadius: 20, padding: 28, boxShadow: '0 2px 12px rgba(0,98,140,0.06)', border: '1px solid rgba(171,173,174,0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 48 }}>
        <ReadinessGauge score={data?.readiness ?? 67} />
        <div>
          <h3 style={{ fontSize: 18, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: '0 0 10px' }}>Exam Readiness Score</h3>
          <p style={{ fontSize: 14, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, maxWidth: 380, lineHeight: 1.7, margin: '0 0 16px' }}>
            Your readiness is calculated from your syllabus completion (50%), average test score (35%), and days remaining (15%).
          </p>
          <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
            {[{ label: 'Completion', pct: 45, color: C.primary }, { label: 'Test Score', pct: 78, color: '#1a7a4a' }, { label: 'Time Factor', pct: 60, color: '#e07b00' }].map(({ label, pct, color }) => (
              <div key={label} style={{ background: C.surfaceLow, borderRadius: 12, padding: '10px 16px', textAlign: 'center' }}>
                <p style={{ fontSize: 18, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color, margin: 0 }}>{pct}%</p>
                <p style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', color: C.outlineVariant, margin: 0 }}>{label}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </PageWrapper>
  )
}
