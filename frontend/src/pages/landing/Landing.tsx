import { useState } from 'react'
import { Link } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import { useMutation } from '@tanstack/react-query'
import { toast } from 'react-hot-toast'
import { Sparkles, ArrowRight, Play, Brain, BarChart2, Map, ClipboardList, FileText, X, Mail, Loader2 } from 'lucide-react'
import Logo from '../../components/layout/Logo'
import client from '../../api/client'
import { ENDPOINTS } from '../../api/endpoints'

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

const PILL_EXAMS = [
  { id: 'jee', label: 'JEE', tagline: 'Crack JEE Main & Advanced with AI-powered prep.' },
  { id: 'neet', label: 'NEET', tagline: 'Ace NEET UG with personalised study roadmaps.' },
  { id: 'cuet', label: 'CUET', tagline: 'Dominate CUET with targeted topic mastery.' },
  { id: 'gate', label: 'GATE', tagline: 'Score big in GATE with concept-first learning.' },
]

const NAV_LINKS = ['Science', 'Benefits', 'Reviews', 'FAQ']

const FEATURES = [
  { icon: <Brain size={20}/>, title: 'AI Tutor', desc: 'Ask anything. Get step-by-step answers anytime.', color: C.primary },
  { icon: <Map size={20}/>, title: 'Smart Roadmap', desc: 'Dynamic study schedule adapts to your progress.', color: C.tertiary },
  { icon: <ClipboardList size={20}/>, title: 'Mock Tests', desc: 'Real exam interface. Instant detailed analytics.', color: '#b45309' },
  { icon: <BarChart2 size={20}/>, title: 'Analytics', desc: 'Know exactly where you lose marks and why.', color: '#166534' },
  { icon: <FileText size={20}/>, title: 'Smart Notes', desc: 'Upload PDFs. AI generates flashcards automatically.', color: '#006382' },
]

export default function Landing() {
  const [selectedExam, setSelectedExam] = useState(PILL_EXAMS[0])
  const [waitlistOpen, setWaitlistOpen] = useState(false)
  const [waitlistEmail, setWaitlistEmail] = useState('')

  const waitlistMutation = useMutation({
    mutationFn: async () => { await client.post(ENDPOINTS.WAITLIST, { email: waitlistEmail.trim() }) },
    onSuccess: () => { toast.success('You\'re on the list! We\'ll notify you.'); setWaitlistOpen(false); setWaitlistEmail('') },
    onError: () => toast.error('Something went wrong. Please try again.'),
  })

  return (
    <div style={{ minHeight: '100vh', background: C.surface, fontFamily: 'Manrope,sans-serif' }}>

      {/* ═══ NAVBAR ════════════════════════════════════════ */}
      <nav style={{
        position: 'sticky', top: 0, zIndex: 50, height: 60,
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '0 40px',
        background: 'rgba(255,255,255,0.90)',
        backdropFilter: 'blur(20px)',
        WebkitBackdropFilter: 'blur(20px)',
        borderBottom: '1px solid rgba(171,173,174,0.12)',
      }}>
        <Link to="/" style={{ textDecoration: 'none' }}><Logo /></Link>

        <div style={{ display: 'flex', alignItems: 'center', gap: 32 }}>
          {NAV_LINKS.map((link, i) => (
            <a key={link} href={`#${link.toLowerCase()}`} style={{
              textDecoration: 'none', fontSize: 13, fontFamily: 'Manrope,sans-serif', fontWeight: 500,
              color: i === 0 ? C.primary : C.onSurfaceVariant,
              borderBottom: i === 0 ? `2px solid ${C.primary}` : '2px solid transparent',
              paddingBottom: 2, transition: 'color 0.15s',
            }}>
              {link.toUpperCase()}
            </a>
          ))}
        </div>

        <Link to="/signup" style={{ textDecoration: 'none' }}>
          <button style={{
            padding: '9px 22px', borderRadius: 100, border: 'none', cursor: 'pointer',
            background: GRADIENT, color: '#fff',
            fontFamily: 'Manrope,sans-serif', fontSize: 13, fontWeight: 700,
            boxShadow: '0 4px 16px rgba(0,98,140,0.25)',
            transition: 'opacity 0.15s, transform 0.15s',
          }}
            onMouseEnter={e => { (e.currentTarget as HTMLButtonElement).style.opacity = '0.9'; (e.currentTarget as HTMLButtonElement).style.transform = 'translateY(-1px)' }}
            onMouseLeave={e => { (e.currentTarget as HTMLButtonElement).style.opacity = '1'; (e.currentTarget as HTMLButtonElement).style.transform = 'translateY(0)' }}
          >
            Get Started
          </button>
        </Link>
      </nav>

      {/* ═══ HERO ══════════════════════════════════════════ */}
      <section style={{
        position: 'relative', minHeight: '91vh', display: 'flex', alignItems: 'center',
        overflow: 'hidden',
        background: `linear-gradient(160deg, #e8f3ff 0%, #f0eeff 40%, ${C.surface} 70%)`,
      }}>
        {/* Background blobs */}
        <div style={{
          position: 'absolute', top: -100, left: -100, width: 500, height: 500,
          borderRadius: '50%', opacity: 0.3,
          background: 'radial-gradient(circle, rgba(52,181,250,0.4) 0%, transparent 70%)',
          filter: 'blur(60px)',
        }} />
        <div style={{
          position: 'absolute', bottom: -80, right: -80, width: 400, height: 400,
          borderRadius: '50%', opacity: 0.25,
          background: 'radial-gradient(circle, rgba(175,166,255,0.5) 0%, transparent 70%)',
          filter: 'blur(60px)',
        }} />

        <div style={{
          maxWidth: 1200, margin: '0 auto', padding: '80px 48px',
          display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 80, alignItems: 'center',
          position: 'relative', zIndex: 1, width: '100%',
        }}>
          {/* Left */}
          <motion.div initial={{ opacity: 0, y: 24 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.55 }}>
            {/* AI eyebrow chip */}
            <div style={{
              display: 'inline-flex', alignItems: 'center', gap: 6,
              padding: '6px 14px', borderRadius: 100, marginBottom: 28,
              background: `rgba(175,166,255,0.2)`,
              border: `1px solid rgba(175,166,255,0.4)`,
              color: C.onTertiaryContainer,
              fontFamily: 'Manrope,sans-serif', fontSize: 11, fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase',
            }}>
              <Sparkles size={11} color={C.tertiary} />
              AI-Powered Study Companion
            </div>

            {/* Headline */}
            <h1 style={{ margin: 0, marginBottom: 16, lineHeight: 1.05, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800 }}>
              <span style={{ fontSize: 56, background: GRADIENT, WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
                Creatine
              </span>
              <br />
              <span style={{ fontSize: 52, color: C.onSurface }}> for your</span>
              <br />
              <span style={{ fontSize: 52, color: C.onSurface }}>exam prep</span>
            </h1>

            {/* Subtagline — changes with pill */}
            <p style={{ fontSize: 15, color: C.onSurfaceVariant, lineHeight: 1.6, marginBottom: 28, maxWidth: 420 }}>
              {selectedExam.tagline}
            </p>

            {/* Exam pills */}
            <p style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.12em', textTransform: 'uppercase', color: C.outline, marginBottom: 12 }}>
              Target your goal
            </p>
            <div style={{ display: 'flex', gap: 8, marginBottom: 36, flexWrap: 'wrap' }}>
              {PILL_EXAMS.map(exam => (
                <button
                  key={exam.id}
                  onClick={() => setSelectedExam(exam)}
                  style={{
                    padding: '7px 18px', borderRadius: 100, border: 'none', cursor: 'pointer',
                    fontFamily: 'Manrope,sans-serif', fontSize: 13, fontWeight: 600,
                    background: selectedExam.id === exam.id ? GRADIENT : C.surfaceLowest,
                    color: selectedExam.id === exam.id ? '#fff' : C.onSurfaceVariant,
                    boxShadow: selectedExam.id === exam.id ? '0 4px 12px rgba(0,98,140,0.25)' : '0 1px 4px rgba(0,0,0,0.06)',
                    transition: 'all 0.15s',
                    outline: selectedExam.id === exam.id ? 'none' : `1px solid rgba(171,173,174,0.25)`,
                  }}
                >
                  {exam.label}
                </button>
              ))}
            </div>

            {/* CTAs */}
            <div style={{ display: 'flex', gap: 12 }}>
              <Link to="/signup" style={{ textDecoration: 'none' }}>
                <button
                  style={{
                    display: 'flex', alignItems: 'center', gap: 8,
                    padding: '13px 28px', borderRadius: 100, border: 'none', cursor: 'pointer',
                    background: GRADIENT, color: '#fff',
                    fontFamily: 'Manrope,sans-serif', fontSize: 14, fontWeight: 700,
                    boxShadow: '0 6px 20px rgba(0,98,140,0.30)',
                    transition: 'all 0.2s',
                  }}
                  onMouseEnter={e => { const el = e.currentTarget; el.style.transform = 'translateY(-2px)'; el.style.boxShadow = '0 10px 28px rgba(0,98,140,0.35)' }}
                  onMouseLeave={e => { const el = e.currentTarget; el.style.transform = 'translateY(0)'; el.style.boxShadow = '0 6px 20px rgba(0,98,140,0.30)' }}
                >
                  Start Free Journey <ArrowRight size={16} />
                </button>
              </Link>
              <button
                onClick={() => setWaitlistOpen(true)}
                style={{
                  display: 'flex', alignItems: 'center', gap: 8,
                  padding: '13px 24px', borderRadius: 100, cursor: 'pointer',
                  background: 'transparent', color: C.onSurfaceVariant,
                  fontFamily: 'Manrope,sans-serif', fontSize: 14, fontWeight: 600,
                  border: `1px solid rgba(171,173,174,0.3)`, transition: 'all 0.15s',
                }}
                onMouseEnter={e => { const el = e.currentTarget; el.style.background = C.surfaceLowest; el.style.color = C.onSurface }}
                onMouseLeave={e => { const el = e.currentTarget; el.style.background = 'transparent'; el.style.color = C.onSurfaceVariant }}
              >
                <Play size={14} style={{ fill: 'currentColor' }} /> Coming Soon — Join Waitlist
              </button>
            </div>
          </motion.div>

          {/* Right — Floating glassmorphism mockup */}
          <motion.div
            initial={{ opacity: 0, y: 40 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, delay: 0.2 }}
          >
            <div style={{
              position: 'relative',
              animation: 'float 5s ease-in-out infinite',
            }}>
              <div style={{
                background: 'rgba(255,255,255,0.88)',
                backdropFilter: 'blur(24px)',
                WebkitBackdropFilter: 'blur(24px)',
                borderRadius: 24, padding: 24,
                boxShadow: '0 32px 64px rgba(0,98,140,0.10), 0 0 0 1px rgba(255,255,255,0.7)',
                border: '1px solid rgba(171,173,174,0.15)',
              }}>
                {/* Window chrome */}
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 20 }}>
                  <div style={{ width: 11, height: 11, borderRadius: '50%', background: '#ff606e' }} />
                  <div style={{ width: 11, height: 11, borderRadius: '50%', background: '#ffbb38' }} />
                  <div style={{ width: 11, height: 11, borderRadius: '50%', background: '#2ac74d' }} />
                  <span style={{ marginLeft: 12, fontSize: 11, fontFamily: 'Manrope,sans-serif', color: C.outline }}>
                    PrepCreatine Dashboard
                  </span>
                </div>

                {/* Profile header area */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                  <div style={{ flex: 1 }}>
                    <div style={{ height: 10, background: C.surfaceLow, borderRadius: 6, width: '50%', marginBottom: 8 }} />
                    <div style={{ height: 14, background: C.surfaceLowest, borderRadius: 6, width: '75%', border: `1px solid rgba(171,173,174,0.15)` }} />
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <div style={{ width: 8, height: 8, borderRadius: '50%', background: C.primaryContainer }} />
                    <div style={{ height: 10, background: GRADIENT, borderRadius: 6, width: 80 }} />
                  </div>
                </div>

                {/* Mini cards */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginBottom: 14 }}>
                  {[
                    { label: 'Physics', color: C.primary },
                    { label: 'Roadmap', color: C.tertiary },
                  ].map(mc => (
                    <div key={mc.label} style={{
                      background: C.surfaceLow, borderRadius: 14, padding: '14px 14px',
                      border: `1px solid rgba(171,173,174,0.10)`,
                    }}>
                      <div style={{ width: 24, height: 24, borderRadius: 8, background: `${mc.color}18`, marginBottom: 8, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <div style={{ width: 10, height: 10, borderRadius: '50%', background: mc.color }} />
                      </div>
                      <div style={{ height: 9, background: C.surfaceLowest, borderRadius: 5, width: '70%', marginBottom: 6, border: `1px solid rgba(171,173,174,0.12)` }} />
                      <div style={{ height: 6, background: mc.color, borderRadius: 4, width: '45%', opacity: 0.7 }} />
                    </div>
                  ))}
                </div>

                <div style={{ height: 1, background: C.surfaceLow, marginBottom: 14 }} />

                {/* Progress bar */}
                <div style={{ marginBottom: 14 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                    <div style={{ height: 9, background: C.surfaceLow, borderRadius: 5, width: '40%' }} />
                    <div style={{ height: 9, background: GRADIENT, borderRadius: 5, width: '15%', opacity: 0.9 }} />
                  </div>
                  <div style={{ height: 6, background: C.surfaceLow, borderRadius: 100, overflow: 'hidden' }}>
                    <div style={{ height: '100%', width: '24%', background: GRADIENT, borderRadius: 100 }} />
                  </div>
                </div>

                {/* AI Insight mini card */}
                <div style={{
                  background: 'rgba(175,166,255,0.18)',
                  borderRadius: 14, padding: '14px',
                  border: '1px solid rgba(175,166,255,0.35)',
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 5, marginBottom: 8 }}>
                    <Brain size={11} color={C.onTertiaryContainer} />
                    <span style={{ fontSize: 9, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase', color: C.onTertiaryContainer }}>AI Suggestion</span>
                  </div>
                  <div style={{ height: 8, background: 'rgba(175,166,255,0.4)', borderRadius: 4, width: '90%', marginBottom: 5 }} />
                  <div style={{ height: 8, background: 'rgba(175,166,255,0.3)', borderRadius: 4, width: '75%', marginBottom: 5 }} />
                  <div style={{ height: 8, background: 'rgba(175,166,255,0.25)', borderRadius: 4, width: '58%' }} />
                </div>
              </div>
            </div>
          </motion.div>
        </div>
      </section>

      {/* ═══ TRUST BAR ═════════════════════════════════════ */}
      <div style={{
        background: C.surfaceLowest, padding: '16px 48px',
        display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 32,
        borderBottom: `1px solid rgba(171,173,174,0.10)`,
      }}>
        <span style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.outline, fontWeight: 500 }}>Trusted by students preparing for</span>
        {['JEE', 'NEET', 'CUET', 'GATE', 'CAT', 'UPSC'].map(e => (
          <span key={e} style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', fontWeight: 700, color: C.primary }}>{e}</span>
        ))}
      </div>

      {/* ═══ FEATURES ══════════════════════════════════════ */}
      <section id="benefits" style={{ padding: '96px 48px', background: C.surfaceLow }}>
        <div style={{ maxWidth: 1100, margin: '0 auto' }}>
          <div style={{ textAlign: 'center', marginBottom: 56 }}>
            <p style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.15em', textTransform: 'uppercase', color: C.primary, marginBottom: 12 }}>
              Features
            </p>
            <h2 style={{ fontSize: 40, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: 0, lineHeight: 1.1 }}>
              Everything you need,<br />nothing you don&apos;t
            </h2>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: 16 }}>
            {FEATURES.map((f, i) => (
              <motion.div
                key={f.title}
                initial={{ opacity: 0, y: 16 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.08, duration: 0.4 }}
              >
                <div
                  style={{
                    background: C.surfaceLowest, borderRadius: 20, padding: '24px 20px',
                    border: '1px solid rgba(171,173,174,0.10)',
                    boxShadow: '0 2px 8px rgba(0,98,140,0.04)',
                    transition: 'all 0.2s', height: '100%',
                  }}
                  onMouseEnter={e => { const el = e.currentTarget as HTMLDivElement; el.style.transform = 'translateY(-4px)'; el.style.boxShadow = '0 12px 28px rgba(0,98,140,0.10)' }}
                  onMouseLeave={e => { const el = e.currentTarget as HTMLDivElement; el.style.transform = 'translateY(0)'; el.style.boxShadow = '0 2px 8px rgba(0,98,140,0.04)' }}
                >
                  <div style={{
                    width: 44, height: 44, borderRadius: 12, marginBottom: 16,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    background: `${f.color}12`, color: f.color,
                  }}>
                    {f.icon}
                  </div>
                  <h3 style={{ fontSize: 13, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 700, color: C.onSurface, marginBottom: 8, margin: '0 0 8px' }}>
                    {f.title}
                  </h3>
                  <p style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, lineHeight: 1.55, margin: 0 }}>
                    {f.desc}
                  </p>
                </div>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* ═══ FINAL CTA ═════════════════════════════════════ */}
      <section style={{ padding: '80px 48px', background: C.surface }}>
        <div style={{ maxWidth: 700, margin: '0 auto', textAlign: 'center' }}>
          <div style={{
            borderRadius: 32, padding: '64px 48px', position: 'relative', overflow: 'hidden',
            background: GRADIENT,
            boxShadow: '0 32px 64px rgba(0,98,140,0.22)',
          }}>
            <div style={{ position: 'absolute', top: -40, right: -40, width: 200, height: 200, borderRadius: '50%', background: 'rgba(255,255,255,0.08)' }} />
            <h2 style={{ fontSize: 36, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: '#fff', margin: '0 0 16px', lineHeight: 1.1 }}>
              Start studying smarter today
            </h2>
            <p style={{ fontSize: 15, fontFamily: 'Manrope,sans-serif', color: 'rgba(255,255,255,0.8)', marginBottom: 32 }}>
              Free to use. No credit card. Works for any exam.
            </p>
            <Link to="/signup" style={{ textDecoration: 'none' }}>
              <button style={{
                display: 'inline-flex', alignItems: 'center', gap: 10,
                padding: '14px 32px', borderRadius: 100, border: 'none', cursor: 'pointer',
                background: 'white', color: C.primary,
                fontFamily: 'Manrope,sans-serif', fontSize: 15, fontWeight: 700,
                boxShadow: '0 4px 20px rgba(0,0,0,0.15)',
                transition: 'all 0.2s',
              }}
                onMouseEnter={e => { const el = e.currentTarget; el.style.transform = 'scale(1.03)'; el.style.boxShadow = '0 8px 28px rgba(0,0,0,0.2)' }}
                onMouseLeave={e => { const el = e.currentTarget; el.style.transform = 'scale(1)'; el.style.boxShadow = '0 4px 20px rgba(0,0,0,0.15)' }}
              >
                Create your free account <ArrowRight size={16} />
              </button>
            </Link>
            <p style={{ marginTop: 20, fontSize: 12, fontFamily: 'Manrope,sans-serif', color: 'rgba(255,255,255,0.55)' }}>
              Brought to you by Rudra Agrawal
            </p>
          </div>
        </div>
      </section>

      {/* ═══ FOOTER ════════════════════════════════════════ */}
      <footer style={{ background: '#0c0f10', padding: '48px 48px 32px' }}>
        <div style={{ maxWidth: 1100, margin: '0 auto', display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 32, marginBottom: 32 }}>
          <div>
            <Logo />
            <p style={{ fontSize: 13, fontFamily: 'Manrope,sans-serif', color: '#6a6c6d', marginTop: 12, maxWidth: 220, lineHeight: 1.6 }}>
              The Digital Sanctuary for Cognitive Growth.
            </p>
            <p style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', color: '#4a4c4d', marginTop: 8 }}>
              By Rudra Agrawal
            </p>
          </div>
          {[
            { title: 'Product', links: ['Features', 'Pricing', 'Changelog'] },
            { title: 'Exams', links: ['JEE', 'NEET', 'CAT', 'GATE', 'UPSC'] },
            { title: 'Company', links: ['About', 'Privacy', 'Terms', 'Contact'] },
          ].map(col => (
            <div key={col.title}>
              <h4 style={{ fontSize: 12, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 700, color: '#fff', marginBottom: 16, marginTop: 0 }}>{col.title}</h4>
              {col.links.map(link => (
                <p key={link} style={{ fontSize: 13, fontFamily: 'Manrope,sans-serif', color: '#6a6c6d', marginBottom: 10, cursor: 'pointer', transition: 'color 0.15s' }}
                  onMouseEnter={e => (e.currentTarget.style.color = '#aaa')}
                  onMouseLeave={e => (e.currentTarget.style.color = '#6a6c6d')}
                >
                  {link}
                </p>
              ))}
            </div>
          ))}
        </div>
        <div style={{ borderTop: '1px solid rgba(255,255,255,0.06)', paddingTop: 20, display: 'flex', justifyContent: 'space-between', fontSize: 12, fontFamily: 'Manrope,sans-serif', color: '#4a4c4d' }}>
          <span>© 2025 PrepCreatine. All rights reserved.</span>
          <span>PrepCreatine — Creatine for your exam prep</span>
        </div>
      </footer>

      {/* ── Waitlist Modal ── */}
      <AnimatePresence>
        {waitlistOpen && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(6px)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100, padding: 24 }}
            onClick={() => setWaitlistOpen(false)}
          >
            <motion.div initial={{ opacity: 0, y: 20, scale: 0.97 }} animate={{ opacity: 1, y: 0, scale: 1 }} exit={{ opacity: 0, y: 12, scale: 0.97 }}
              onClick={e => e.stopPropagation()}
              style={{ background: 'rgba(255,255,255,0.97)', backdropFilter: 'blur(24px)', borderRadius: 28, padding: 36, width: '100%', maxWidth: 420, boxShadow: '0 24px 60px rgba(0,0,0,0.18)', textAlign: 'center' }}
            >
              <button onClick={() => setWaitlistOpen(false)} style={{ position: 'absolute', top: 16, right: 16, background: '#eff1f2', border: 'none', borderRadius: 10, width: 32, height: 32, display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
                <X size={16} color="#595c5d" />
              </button>
              <div style={{ width: 52, height: 52, borderRadius: '50%', background: 'linear-gradient(135deg, #00628c 0%, #34b5fa 100%)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 18px' }}>
                <Sparkles size={24} color="#fff" />
              </div>
              <h2 style={{ fontSize: 22, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: '#2c2f30', margin: '0 0 8px' }}>Early Access is Coming</h2>
              <p style={{ fontSize: 14, fontFamily: 'Manrope,sans-serif', color: '#595c5d', lineHeight: 1.6, margin: '0 0 24px' }}>
                We're onboarding students in batches. Drop your email and we'll let you in first.
              </p>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                <div style={{ position: 'relative' }}>
                  <Mail size={16} style={{ position: 'absolute', left: 14, top: '50%', transform: 'translateY(-50%)', color: '#abadae' }} />
                  <input
                    type="email" placeholder="your@email.com" value={waitlistEmail}
                    onChange={e => setWaitlistEmail(e.target.value)}
                    onKeyDown={e => { if (e.key === 'Enter') waitlistMutation.mutate() }}
                    disabled={waitlistMutation.isPending}
                    style={{ width: '100%', boxSizing: 'border-box', paddingLeft: 40, background: '#eff1f2', border: '1px solid rgba(171,173,174,0.2)', borderRadius: 14, padding: '13px 16px 13px 40px', fontFamily: 'Manrope,sans-serif', fontSize: 14, color: '#2c2f30', outline: 'none' }}
                    onFocus={e => { e.target.style.background = '#fff'; e.target.style.boxShadow = '0 0 0 2px rgba(52,181,250,0.35)' }}
                    onBlur={e => { e.target.style.background = '#eff1f2'; e.target.style.boxShadow = 'none' }}
                  />
                </div>
                <button
                  onClick={() => waitlistMutation.mutate()}
                  disabled={waitlistMutation.isPending || !waitlistEmail.includes('@')}
                  style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8, width: '100%', padding: '13px', borderRadius: 100, border: 'none', cursor: 'pointer', background: 'linear-gradient(135deg, #00628c 0%, #34b5fa 100%)', color: '#fff', fontFamily: 'Manrope,sans-serif', fontSize: 14, fontWeight: 700, boxShadow: '0 4px 16px rgba(0,98,140,0.25)', opacity: (!waitlistEmail.includes('@')) ? 0.6 : 1 }}
                >
                  {waitlistMutation.isPending && <Loader2 size={15} style={{ animation: 'spin 1s linear infinite' }} />}
                  Notify Me When It's Ready
                </button>
              </div>
              <p style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: '#abadae', marginTop: 14 }}>No spam. Unsubscribe anytime.</p>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}
