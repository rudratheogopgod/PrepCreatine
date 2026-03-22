import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AnimatePresence, motion } from 'framer-motion'
import { useMutation } from '@tanstack/react-query'
import { toast } from 'react-hot-toast'
import { ChevronLeft, ChevronRight, Loader2, Zap } from 'lucide-react'
import Logo from '../../components/layout/Logo'
import client from '../../api/client'
import { ENDPOINTS } from '../../api/endpoints'
import { useUserContextStore } from '../../store/userContextStore'
import { logger } from '../../utils/logger'

const C = {
  surface: '#f5f6f7', surfaceLow: '#eff1f2', surfaceLowest: '#ffffff',
  onSurface: '#2c2f30', onSurfaceVariant: '#595c5d', outline: '#757778', outlineVariant: '#abadae',
  primary: '#00628c', primaryContainer: '#34b5fa',
  tertiary: '#584cb5', tertiaryContainer: '#afa6ff', onTertiaryContainer: '#2b1988',
}
const GRADIENT = `linear-gradient(135deg, ${C.primary} 0%, ${C.primaryContainer} 100%)`

const EXAMS = [
  { id: 'jee', label: 'JEE Main & Advanced', icon: '⚗️', desc: 'Engineering entrance' },
  { id: 'neet', label: 'NEET UG', icon: '🩺', desc: 'Medical entrance' },
  { id: 'cuet', label: 'CUET', icon: '🎓', desc: 'University entrance' },
  { id: 'gate', label: 'GATE', icon: '🔧', desc: 'Postgraduate engineering' },
  { id: 'cat', label: 'CAT', icon: '📊', desc: 'MBA entrance' },
  { id: 'upsc', label: 'UPSC CSE', icon: '🏛️', desc: 'Civil services' },
  { id: 'ssc', label: 'SSC CGL', icon: '📋', desc: 'Staff selection' },
  { id: 'state', label: 'State PETs', icon: '🗺️', desc: 'State-level entrance' },
  { id: 'other', label: 'Other / Quick study', icon: '📚', desc: 'Any other topic' },
]
const SUBJECTS_BY_EXAM: Record<string, string[]> = {
  jee: ['Physics', 'Chemistry', 'Mathematics'],
  neet: ['Physics', 'Chemistry', 'Biology'],
  gate: ['Engineering Mathematics', 'General Aptitude', 'Core Subject'],
  cat: ['Quantitative Aptitude', 'Verbal Ability', 'Logical Reasoning', 'Data Interpretation'],
  cuet: ['English', 'Domain Subjects', 'General Test'],
  upsc: ['History', 'Geography', 'Polity', 'Economics', 'Science & Tech', 'Current Affairs', 'CSAT'],
  ssc: ['Quantitative Aptitude', 'English', 'General Intelligence', 'General Awareness'],
  state: ['Mathematics', 'Science', 'English', 'General Knowledge'],
  other: ['My custom topic'],
}

const SLIDE = {
  enter: (d: number) => ({ x: d > 0 ? 80 : -80, opacity: 0 }),
  center: { x: 0, opacity: 1 },
  exit: (d: number) => ({ x: d > 0 ? -80 : 80, opacity: 0 }),
}

// PillBtn removed — was unused (Path C now uses QuickStudy page)

export default function Onboarding() {
  const navigate = useNavigate()
  const setContext = useUserContextStore(s => s.setContext)
  const [step, setStep] = useState(0)
  const [dir, setDir] = useState(1)
  const [examType, setExamType] = useState('')
  const [examDate, setExamDate] = useState('')
  const [subjects, setSubjects] = useState<string[]>([])
  const [dailyGoal, setDailyGoal] = useState(90)
  const [mentorCode, setMentorCode] = useState('')
  const [theme, setTheme] = useState<'light' | 'dark' | 'system'>('system')

  const totalSteps = examType === 'other' ? 2 : 4

  const completeMutation = useMutation({
    mutationFn: async () => {
      logger.info('[Onboarding] Complete', { examType })
      await client.post(ENDPOINTS.ME_CONTEXT, { examType, examDate, subjects, dailyGoalMins: dailyGoal, mentorCode, theme })
    },
    onSuccess: () => { setContext({ examType, examDate, dailyGoalMins: dailyGoal, theme }); navigate('/home') },
    onError: () => toast.error('Something went wrong. Please try again.'),
  })

  const next = () => {
    // Path C: redirect to dedicated QuickStudy page
    if (step === 0 && examType === 'other') { navigate('/onboarding/quick'); return }
    setDir(1); setStep(s => s + 1)
  }
  const prev = () => { setDir(-1); setStep(s => Math.max(0, s - 1)) }

  const h2Style: React.CSSProperties = { fontSize: 22, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: '0 0 6px' }
  const subStyle: React.CSSProperties = { fontSize: 13, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, margin: '0 0 20px' }

  const PrimaryBtn = ({ onClick, disabled, loading, children }: { onClick?: () => void; disabled?: boolean; loading?: boolean; children: React.ReactNode }) => (
    <button
      onClick={onClick}
      disabled={disabled || loading}
      style={{
        width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
        padding: '13px', borderRadius: 100, border: 'none',
        cursor: (disabled || loading) ? 'not-allowed' : 'pointer',
        background: disabled ? 'rgba(171,173,174,0.4)' : loading ? 'rgba(0,98,140,0.5)' : GRADIENT,
        color: '#fff', fontFamily: 'Manrope,sans-serif', fontSize: 14, fontWeight: 700,
        boxShadow: disabled ? 'none' : '0 4px 16px rgba(0,98,140,0.25)',
        transition: 'all 0.15s',
      }}
    >
      {loading && <Loader2 size={15} style={{ animation: 'spin 1s linear infinite' }} />}
      {children}
    </button>
  )

  const steps = [
    // Step 0: Exam
    <div key="exam">
      <h2 style={h2Style}>What are you preparing for?</h2>
      <p style={subStyle}>This helps us personalise everything for you.</p>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginBottom: 20 }}>
        {EXAMS.map(ex => (
          <button
            key={ex.id}
            onClick={() => {
            setExamType(ex.id)
            // FDD: auto-advance after 400ms once selection is confirmed
            // For 'other' this triggers a navigate in next()
            setTimeout(() => { if (ex.id !== 'other') { setDir(1); setStep(s => s + 1) } else { navigate('/onboarding/quick') } }, 400)
          }}
            style={{
              display: 'flex', alignItems: 'center', gap: 12, padding: '12px 16px', borderRadius: 16,
              border: `2px solid ${examType === ex.id ? C.primary : 'rgba(171,173,174,0.25)'}`,
              background: examType === ex.id ? 'rgba(0,98,140,0.05)' : C.surfaceLowest,
              cursor: 'pointer', textAlign: 'left', transition: 'all 0.15s',
              boxShadow: examType === ex.id ? '0 0 0 3px rgba(52,181,250,0.2)' : 'none',
            }}
          >
            <span style={{ fontSize: 22 }}>{ex.icon}</span>
            <div>
              <p style={{ fontSize: 13, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 700, color: examType === ex.id ? C.primary : C.onSurface, margin: 0 }}>{ex.label}</p>
              <p style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', color: C.outlineVariant, margin: 0 }}>{ex.desc}</p>
            </div>
          </button>
        ))}
      </div>
      <PrimaryBtn disabled={!examType} onClick={next}>Continue <ChevronRight size={16} /></PrimaryBtn>
    </div>,

    // Step 1: Date
    <div key="date">
      <h2 style={h2Style}>When is your exam?</h2>
      <p style={subStyle}>Optional — helps us build your countdown and study plan.</p>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 10, marginBottom: 20 }}>
        {[
          { label: '< 1 month', value: '30d' }, { label: '1–3 months', value: '90d' },
          { label: '3–6 months', value: '180d' }, { label: '6–12 months', value: '365d' },
          { label: '12+ months', value: '400d' }, { label: "I don't know", value: '' },
        ].map(opt => (
          <button
            key={opt.label}
            onClick={() => setExamDate(opt.value)}
            style={{
              padding: '11px 8px', borderRadius: 12, cursor: 'pointer', textAlign: 'center',
              border: `2px solid ${examDate === opt.value ? C.primary : 'rgba(171,173,174,0.25)'}`,
              background: examDate === opt.value ? 'rgba(0,98,140,0.05)' : C.surfaceLowest,
              fontSize: 13, fontFamily: 'Manrope,sans-serif', fontWeight: 600,
              color: examDate === opt.value ? C.primary : C.onSurfaceVariant,
              transition: 'all 0.15s',
            }}
          >
            {opt.label}
          </button>
        ))}
      </div>
      <PrimaryBtn onClick={next}>Continue <ChevronRight size={16} /></PrimaryBtn>
      <button onClick={next} style={{ width: '100%', padding: '10px', background: 'none', border: 'none', cursor: 'pointer', fontSize: 13, fontFamily: 'Manrope,sans-serif', color: C.outlineVariant, marginTop: 8 }}>Skip for now</button>
    </div>,

    // Step 2: Subjects
    <div key="subjects">
      <h2 style={h2Style}>Which subjects do you want to track?</h2>
      <p style={subStyle}>Select all that apply. You can change this anytime.</p>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 24 }}>
        {(SUBJECTS_BY_EXAM[examType] || []).map(sub => (
          <button
            key={sub}
            onClick={() => setSubjects(prev => prev.includes(sub) ? prev.filter(s => s !== sub) : [...prev, sub])}
            style={{
              padding: '8px 18px', borderRadius: 100, cursor: 'pointer',
              border: `2px solid ${subjects.includes(sub) ? C.primary : 'rgba(171,173,174,0.25)'}`,
              background: subjects.includes(sub) ? 'rgba(0,98,140,0.07)' : C.surfaceLowest,
              fontSize: 13, fontFamily: 'Manrope,sans-serif', fontWeight: 600,
              color: subjects.includes(sub) ? C.primary : C.onSurfaceVariant,
              transition: 'all 0.15s',
            }}
          >
            {sub}
          </button>
        ))}
      </div>
      <PrimaryBtn onClick={next}>Continue <ChevronRight size={16} /></PrimaryBtn>
    </div>,

    // Step 3: Setup
    <div key="setup">
      <h2 style={h2Style}>Almost there! Quick setup</h2>
      <p style={subStyle}>Personalise your experience. You can change these later.</p>

      {/* Daily goal slider */}
      <div style={{ marginBottom: 24 }}>
        <p style={{ fontSize: 13, fontFamily: 'Manrope,sans-serif', fontWeight: 600, color: C.onSurface, marginBottom: 10 }}>
          Daily study goal: <span style={{ background: GRADIENT, WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', fontWeight: 800 }}>{dailyGoal} minutes</span>
        </p>
        <input type="range" min={15} max={300} step={15} value={dailyGoal} onChange={e => setDailyGoal(Number(e.target.value))}
          style={{ width: '100%', accentColor: C.primary }} />
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, fontFamily: 'Manrope,sans-serif', color: C.outlineVariant, marginTop: 4 }}>
          <span>15 min</span><span>5 hours</span>
        </div>
      </div>

      {/* Mentor code */}
      <div style={{ marginBottom: 20 }}>
        <p style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: C.outline, marginBottom: 7 }}>
          Mentor code <span style={{ fontWeight: 400, textTransform: 'none', letterSpacing: 0 }}>(optional)</span>
        </p>
        <input
          type="text" placeholder="Enter mentor code" maxLength={50}
          value={mentorCode}
          onChange={e => setMentorCode(e.target.value.replace(/[^a-zA-Z0-9]/g, '').toUpperCase())}
          style={{
            width: '100%', boxSizing: 'border-box', background: C.surfaceLow, border: '1px solid rgba(171,173,174,0.2)',
            borderRadius: 12, padding: '11px 16px', fontFamily: 'Manrope,sans-serif', fontSize: 14, color: C.onSurface, outline: 'none',
          }}
          onFocus={e => { e.target.style.background = C.surfaceLowest; e.target.style.boxShadow = `0 0 0 2px rgba(52,181,250,0.35)` }}
          onBlur={e => { e.target.style.background = C.surfaceLow; e.target.style.boxShadow = 'none' }}
        />
      </div>

      {/* Theme */}
      <div style={{ marginBottom: 24 }}>
        <p style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: C.outline, marginBottom: 10 }}>App theme</p>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 10 }}>
          {(['light', 'dark', 'system'] as const).map(t => (
            <button
              key={t}
              onClick={() => setTheme(t)}
              style={{
                padding: '12px 8px', borderRadius: 12, cursor: 'pointer', textAlign: 'center',
                border: `2px solid ${theme === t ? C.primary : 'rgba(171,173,174,0.25)'}`,
                background: theme === t ? 'rgba(0,98,140,0.05)' : C.surfaceLowest,
                fontSize: 13, fontFamily: 'Manrope,sans-serif', fontWeight: 600,
                color: theme === t ? C.primary : C.onSurfaceVariant,
                transition: 'all 0.15s',
              }}
            >
              {t === 'light' ? '☀️' : t === 'dark' ? '🌙' : '💻'}<br /><span style={{ fontSize: 12, textTransform: 'capitalize' }}>{t}</span>
            </button>
          ))}
        </div>
      </div>

      <PrimaryBtn onClick={() => completeMutation.mutate()} loading={completeMutation.isPending}>
        <Zap size={16} /> Start studying!
      </PrimaryBtn>
    </div>,
  ]

  // Path C: redirect to QuickStudy when 'other' is confirmed
  const visibleSteps = steps

  return (
    <div style={{
      minHeight: '100vh', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
      background: `linear-gradient(160deg, #e8f3ff 0%, #f0eeff 40%, ${C.surface} 70%)`,
      padding: '40px 16px', position: 'relative', overflow: 'hidden',
    }}>
      <div style={{ position: 'absolute', top: -100, left: -100, width: 400, height: 400, borderRadius: '50%', background: 'radial-gradient(circle, rgba(52,181,250,0.2) 0%, transparent 70%)', filter: 'blur(60px)', pointerEvents: 'none' }} />
      <div style={{ position: 'absolute', bottom: -100, right: -100, width: 340, height: 340, borderRadius: '50%', background: 'radial-gradient(circle, rgba(175,166,255,0.2) 0%, transparent 70%)', filter: 'blur(60px)', pointerEvents: 'none' }} />

      <div style={{ position: 'relative', zIndex: 1, width: '100%', maxWidth: 560 }}>
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 24 }}><Logo /></div>

        {/* Step dots */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6, marginBottom: 24 }}>
          {Array.from({ length: totalSteps }).map((_, i) => (
            <div key={i} style={{
              height: 6, borderRadius: 100, transition: 'all 0.3s',
              width: i === step ? 28 : 6,
              background: i === step ? C.primary : i < step ? C.primaryContainer : 'rgba(171,173,174,0.35)',
            }} />
          ))}
        </div>

        {/* Card */}
        <div style={{
          background: 'rgba(255,255,255,0.92)', backdropFilter: 'blur(20px)', WebkitBackdropFilter: 'blur(20px)',
          borderRadius: 24, padding: '28px 32px', border: '1px solid rgba(171,173,174,0.18)',
          boxShadow: '0 20px 48px rgba(0,98,140,0.08)', overflow: 'hidden',
        }}>
          {step > 0 && (
            <button
              onClick={prev}
              style={{ display: 'flex', alignItems: 'center', gap: 4, padding: '4px 0', background: 'none', border: 'none', cursor: 'pointer', fontSize: 13, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, marginBottom: 16 }}
            >
              <ChevronLeft size={15} /> Back
            </button>
          )}

          <AnimatePresence mode="wait" custom={dir}>
            <motion.div
              key={step}
              custom={dir}
              variants={SLIDE}
              initial="enter"
              animate="center"
              exit="exit"
              transition={{ type: 'spring', stiffness: 320, damping: 30, duration: 0.2 }}
            >
              {visibleSteps[step]}
            </motion.div>
          </AnimatePresence>
        </div>
      </div>
    </div>
  )
}
