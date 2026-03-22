import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { MessageSquare, ArrowUp, Search, Plus, Loader2, Users, RefreshCw, ExternalLink, X, ChevronDown } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import { toast } from 'react-hot-toast'
import PageWrapper from '../../components/layout/PageWrapper'
import Avatar from '../../components/ui/Avatar'
import client from '../../api/client'
import { ENDPOINTS } from '../../api/endpoints'
import { timeAgo } from '../../utils/format'
import { sanitizeHtml } from '../../utils/sanitize'
import { logger } from '../../utils/logger'

const C = {
  surface: '#f5f6f7', surfaceLow: '#eff1f2', surfaceLowest: '#ffffff',
  onSurface: '#2c2f30', onSurfaceVariant: '#595c5d', outline: '#757778', outlineVariant: '#abadae',
  primary: '#00628c', primaryContainer: '#34b5fa',
  tertiary: '#584cb5',
}
const GRADIENT = `linear-gradient(135deg, ${C.primary} 0%, ${C.primaryContainer} 100%)`

interface Thread {
  id: string; title: string; body: string; author: string;
  upvotes: number; replies: number; tags: string[]; createdAt: string;
}
interface RedditPost { title: string; url: string; upvotes: number }

const SUBJECTS = ['Physics', 'Chemistry', 'Mathematics', 'Biology', 'English', 'History', 'Geography', 'General']
const tagColors: Record<string, { bg: string; color: string }> = {
  JEE: { bg: 'rgba(0,98,140,0.08)', color: '#00628c' },
  NEET: { bg: 'rgba(22,101,52,0.08)', color: '#166534' },
  Chemistry: { bg: 'rgba(88,76,181,0.08)', color: '#584cb5' },
  Mathematics: { bg: 'rgba(224,123,0,0.08)', color: '#e07b00' },
  General: { bg: 'rgba(171,173,174,0.2)', color: '#757778' },
  'Mental Health': { bg: 'rgba(179,27,37,0.08)', color: '#b31b25' },
}
const defaultTag = { bg: 'rgba(171,173,174,0.15)', color: '#757778' }

export default function Community() {
  const queryClient = useQueryClient()
  const [filter, setFilter] = useState<'trending' | 'recent' | 'my_threads'>('trending')
  const [search, setSearch] = useState('')
  const [askOpen, setAskOpen] = useState(false)

  // Ask form state
  const [qTitle, setQTitle] = useState('')
  const [qBody, setQBody] = useState('')
  const [qSubject, setQSubject] = useState('')
  const [qTopic, setQTopic] = useState('')

  const { data: threads, isLoading } = useQuery({
    queryKey: ['threads', filter],
    queryFn: async () => { const res = await client.get(ENDPOINTS.THREADS, { params: { filter } }); return res.data },
    placeholderData: [
      { id: '1', title: 'Best resources for Inorganic Chemistry?', body: 'I find it hard to memorize everything. Any tips for quick revision?', author: 'Aarav M.', upvotes: 124, replies: 32, tags: ['JEE', 'Chemistry'], createdAt: new Date(Date.now() - 3600000).toISOString() },
      { id: '2', title: 'How to handle exam anxiety?', body: 'My mock scores are dropping because I panic under pressure.', author: 'Priya K.', upvotes: 89, replies: 15, tags: ['General', 'Mental Health'], createdAt: new Date(Date.now() - 86400000).toISOString() },
      { id: '3', title: 'Best approach for Calculus limits?', body: 'Looking for structured approach to solving complex limit problems.', author: 'Rohan S.', upvotes: 67, replies: 9, tags: ['JEE', 'Mathematics'], createdAt: new Date(Date.now() - 172800000).toISOString() },
    ]
  })

  const { data: redditPosts, isLoading: redditLoading, refetch: refetchReddit } = useQuery<RedditPost[]>({
    queryKey: ['reddit-pulse'],
    queryFn: async () => { const res = await client.get(`${ENDPOINTS.THREADS}/reddit-pulse`); return res.data },
    staleTime: 5 * 60 * 1000,
    placeholderData: [
      { title: 'Which mock test series is best for JEE 2025?', url: 'https://reddit.com/r/JEEPreparation', upvotes: 842 },
      { title: 'Sharing my NEET study schedule that got me 680+', url: 'https://reddit.com/r/NEETpreparation', upvotes: 1204 },
      { title: 'Physical chemistry vs organic — which to focus first?', url: 'https://reddit.com/r/JEEPreparation', upvotes: 456 },
    ]
  })

  const askMutation = useMutation({
    mutationFn: async () => {
      const sanitizedTitle = sanitizeHtml(qTitle.trim())
      const sanitizedBody = sanitizeHtml(qBody.trim())
      logger.info('[Community] Post created', { subject: qSubject })
      const res = await client.post(ENDPOINTS.THREADS, { title: sanitizedTitle, body: sanitizedBody, subject: qSubject, topic: qTopic })
      return res.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['threads'] })
      setAskOpen(false); setQTitle(''); setQBody(''); setQSubject(''); setQTopic('')
      toast.success('Question posted!')
    },
    onError: () => toast.error('Failed to post. Please try again.'),
  })

  const filterLabels = { trending: 'Trending', recent: 'Recent', my_threads: 'My Threads' }
  const filtered = threads?.filter((t: Thread) => search ? t.title.toLowerCase().includes(search.toLowerCase()) : true)

  return (
    <PageWrapper>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 28, gap: 16, flexWrap: 'wrap' }}>
        <div>
          <p style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: C.primary, margin: '0 0 6px' }}>COMMUNITY</p>
          <h1 style={{ fontSize: 28, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: '0 0 4px' }}>Discussions</h1>
          <p style={{ fontSize: 14, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, margin: 0 }}>Connect, discuss, and clear doubts with peers.</p>
        </div>
        <button
          onClick={() => setAskOpen(true)}
          style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '11px 22px', borderRadius: 100, border: 'none', cursor: 'pointer', background: GRADIENT, color: '#fff', fontFamily: 'Manrope,sans-serif', fontSize: 14, fontWeight: 700, boxShadow: '0 4px 14px rgba(0,98,140,0.25)', flexShrink: 0 }}
        >
          <Plus size={18} /> Ask a Question
        </button>
      </div>

      {/* Reddit Pulse Widget */}
      <div style={{ background: 'rgba(255,86,0,0.04)', border: '1px solid rgba(255,86,0,0.2)', borderRadius: 20, padding: 20, marginBottom: 24 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            {/* Reddit SVG logo */}
            <svg width={22} height={22} viewBox="0 0 20 20" fill="none">
              <circle cx={10} cy={10} r={10} fill="#FF4500"/>
              <path d="M16.67 10a1.46 1.46 0 00-2.47-1 7.12 7.12 0 00-3.85-1.23l.65-3.1 2.13.45a1 1 0 101 .94 1 1 0 00-.93-.94l-2.38-.5a.25.25 0 00-.29.19l-.73 3.43a7.14 7.14 0 00-3.83 1.23 1.46 1.46 0 10-1.6 2.39 2.87 2.87 0 000 .43c0 2.18 2.54 3.95 5.67 3.95s5.67-1.77 5.67-3.95a2.87 2.87 0 000-.43 1.46 1.46 0 00.86-1.36zM7.5 11a1 1 0 111 1 1 1 0 01-1-1zm5.6 2.71a3.58 3.58 0 01-3.1.8 3.58 3.58 0 01-3.1-.8.25.25 0 01.35-.35 3.08 3.08 0 002.75.59 3.08 3.08 0 002.75-.59.25.25 0 01.35.35zm-.1-1.71a1 1 0 111-1 1 1 0 01-1 1z" fill="white"/>
            </svg>
            <div>
              <p style={{ fontSize: 13, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 700, color: C.onSurface, margin: 0 }}>Reddit Pulse</p>
              <p style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', color: C.outlineVariant, margin: 0 }}>What the JEE/NEET community is discussing right now</p>
            </div>
          </div>
          <button onClick={() => refetchReddit()} style={{ display: 'flex', alignItems: 'center', gap: 5, padding: '6px 14px', borderRadius: 100, border: '1px solid rgba(255,86,0,0.25)', background: 'none', cursor: 'pointer', fontFamily: 'Manrope,sans-serif', fontSize: 12, fontWeight: 600, color: '#FF4500' }}>
            <RefreshCw size={12} /> Refresh
          </button>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(200px,1fr))', gap: 10 }}>
          {redditLoading ? (
            [1,2,3].map(i => <div key={i} style={{ height: 60, background: 'rgba(255,86,0,0.06)', borderRadius: 12, animation: 'pulse 1.5s ease-in-out infinite' }} />)
          ) : redditPosts?.map((p: RedditPost, i: number) => (
            <a key={i} href={p.url} target="_blank" rel="noopener noreferrer" style={{ textDecoration: 'none' }}>
              <div style={{ background: 'rgba(255,86,0,0.05)', borderRadius: 12, padding: '12px 14px', display: 'flex', flexDirection: 'column', gap: 8, transition: 'all 0.15s', cursor: 'pointer' }}
                onMouseEnter={e => (e.currentTarget as HTMLElement).style.background = 'rgba(255,86,0,0.1)'}
                onMouseLeave={e => (e.currentTarget as HTMLElement).style.background = 'rgba(255,86,0,0.05)'}
              >
                <p style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', fontWeight: 600, color: C.onSurface, margin: 0, lineHeight: 1.4, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>{p.title}</p>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', color: '#FF4500', fontWeight: 700 }}>▲ {p.upvotes.toLocaleString()}</span>
                  <ExternalLink size={11} color={C.outlineVariant} />
                </div>
              </div>
            </a>
          ))}
        </div>
      </div>

      {/* Search + Filter */}
      <div style={{ display: 'flex', gap: 12, marginBottom: 20, flexWrap: 'wrap' }}>
        <div style={{ position: 'relative', flex: 1, minWidth: 200 }}>
          <Search size={16} style={{ position: 'absolute', left: 14, top: '50%', transform: 'translateY(-50%)', color: C.outlineVariant }} />
          <input type="text" placeholder="Search discussions…" value={search} onChange={e => setSearch(e.target.value)}
            style={{ width: '100%', boxSizing: 'border-box', background: C.surfaceLowest, border: '1px solid rgba(171,173,174,0.2)', borderRadius: 14, padding: '11px 16px 11px 42px', fontFamily: 'Manrope,sans-serif', fontSize: 14, color: C.onSurface, outline: 'none' }}
            onFocus={e => { e.target.style.boxShadow = `0 0 0 2px rgba(52,181,250,0.35)` }}
            onBlur={e => { e.target.style.boxShadow = 'none' }}
          />
        </div>
        <div style={{ display: 'flex', background: C.surfaceLow, borderRadius: 12, padding: 4, gap: 2 }}>
          {(['trending', 'recent', 'my_threads'] as const).map(f => (
            <button key={f} onClick={() => setFilter(f)} style={{ padding: '8px 14px', borderRadius: 10, border: 'none', cursor: 'pointer', background: filter === f ? C.surfaceLowest : 'none', fontFamily: 'Manrope,sans-serif', fontSize: 12, fontWeight: 600, color: filter === f ? C.onSurface : C.outlineVariant, transition: 'all 0.15s', boxShadow: filter === f ? '0 1px 4px rgba(0,0,0,0.06)' : 'none' }}>
              {filterLabels[f]}
            </button>
          ))}
        </div>
      </div>

      {/* Thread list */}
      {isLoading ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {[1,2,3].map(i => <div key={i} style={{ height: 100, background: C.surfaceLow, borderRadius: 20, animation: 'pulse 1.5s ease-in-out infinite' }} />)}
        </div>
      ) : filtered?.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '64px 24px', background: C.surfaceLowest, borderRadius: 24, border: '2px dashed rgba(171,173,174,0.3)' }}>
          <Users size={32} color={C.outlineVariant} style={{ margin: '0 auto 12px' }} />
          <h3 style={{ fontSize: 18, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: '0 0 8px' }}>No threads found</h3>
          <p style={{ fontSize: 14, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, margin: 0 }}>Be the first to ask a question!</p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {filtered?.map((thread: Thread, i: number) => (
            <motion.div key={thread.id} initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.04 }}>
              <Link to={`/community/${thread.id}`} style={{ textDecoration: 'none' }}>
                <div style={{ background: C.surfaceLowest, borderRadius: 20, padding: '18px 22px', border: '1px solid rgba(171,173,174,0.15)', boxShadow: '0 2px 8px rgba(0,98,140,0.04)', transition: 'all 0.15s', cursor: 'pointer' }}
                  onMouseEnter={e => { const el = e.currentTarget as HTMLElement; el.style.boxShadow = '0 8px 24px rgba(0,98,140,0.10)'; el.style.transform = 'translateY(-1px)' }}
                  onMouseLeave={e => { const el = e.currentTarget as HTMLElement; el.style.boxShadow = '0 2px 8px rgba(0,98,140,0.04)'; el.style.transform = 'none' }}
                >
                  <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 12, marginBottom: 10 }}>
                    <div style={{ flex: 1 }}>
                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginBottom: 8 }}>
                        {thread.tags.map(tag => {
                          const tc = tagColors[tag] ?? defaultTag
                          return <span key={tag} style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, background: tc.bg, color: tc.color, padding: '2px 9px', borderRadius: 100 }}>{tag}</span>
                        })}
                      </div>
                      <h3 style={{ fontSize: 15, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 700, color: C.onSurface, margin: '0 0 6px', lineHeight: 1.4 }}>{thread.title}</h3>
                      <p style={{ fontSize: 13, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, margin: 0, lineHeight: 1.5, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>{thread.body}</p>
                    </div>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
                      <Avatar name={thread.author} size="xs" />
                      <span style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant }}>{thread.author}</span>
                    </div>
                    <span style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.outlineVariant }}>{timeAgo(thread.createdAt)}</span>
                    <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 14 }}>
                      <span style={{ display: 'flex', alignItems: 'center', gap: 4, fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant }}><ArrowUp size={13} /> {thread.upvotes}</span>
                      <span style={{ display: 'flex', alignItems: 'center', gap: 4, fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant }}><MessageSquare size={13} /> {thread.replies}</span>
                    </div>
                  </div>
                </div>
              </Link>
            </motion.div>
          ))}
        </div>
      )}

      {/* ── Ask a Question Modal ── */}
      <AnimatePresence>
        {askOpen && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.45)', backdropFilter: 'blur(4px)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50, padding: 16 }}
            onClick={() => setAskOpen(false)}
          >
            <motion.div initial={{ opacity: 0, y: 24 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 16 }}
              onClick={e => e.stopPropagation()}
              style={{ background: C.surfaceLowest, borderRadius: 24, padding: 28, width: '100%', maxWidth: 540, boxShadow: '0 28px 60px rgba(0,0,0,0.18)' }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 22 }}>
                <h2 style={{ fontSize: 18, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: 0 }}>Ask the Community</h2>
                <button onClick={() => setAskOpen(false)} style={{ background: C.surfaceLow, border: 'none', borderRadius: 10, width: 32, height: 32, display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
                  <X size={16} color={C.onSurfaceVariant} />
                </button>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                {/* Title */}
                <div>
                  <label style={{ display: 'block', fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: C.outline, marginBottom: 7 }}>Question Title *</label>
                  <input value={qTitle} onChange={e => setQTitle(e.target.value)} maxLength={200}
                    placeholder="e.g. How to approach organic mechanism questions?"
                    style={{ width: '100%', boxSizing: 'border-box', background: C.surfaceLow, border: '1px solid rgba(171,173,174,0.2)', borderRadius: 12, padding: '11px 16px', fontFamily: 'Manrope,sans-serif', fontSize: 14, color: C.onSurface, outline: 'none' }}
                    onFocus={e => { e.target.style.background = C.surfaceLowest; e.target.style.boxShadow = `0 0 0 2px rgba(52,181,250,0.35)` }}
                    onBlur={e => { e.target.style.background = C.surfaceLow; e.target.style.boxShadow = 'none' }}
                  />
                  {qTitle.length > 0 && qTitle.length < 10 && (
                    <p style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, margin: '5px 0 0' }}>Add a bit more detail to get better answers.</p>
                  )}
                </div>

                {/* Body */}
                <div>
                  <label style={{ display: 'block', fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: C.outline, marginBottom: 7 }}>Details <span style={{ fontWeight: 400, textTransform: 'none' }}>(optional)</span></label>
                  <textarea value={qBody} onChange={e => setQBody(e.target.value)} rows={4} maxLength={2000}
                    placeholder="Describe your question in more detail..."
                    style={{ width: '100%', boxSizing: 'border-box', background: C.surfaceLow, border: '1px solid rgba(171,173,174,0.2)', borderRadius: 12, padding: '11px 16px', fontFamily: 'Manrope,sans-serif', fontSize: 14, color: C.onSurface, outline: 'none', resize: 'none' }}
                    onFocus={e => { e.target.style.background = C.surfaceLowest; e.target.style.boxShadow = `0 0 0 2px rgba(52,181,250,0.35)` }}
                    onBlur={e => { e.target.style.background = C.surfaceLow; e.target.style.boxShadow = 'none' }}
                  />
                  <p style={{ fontSize: 11, textAlign: 'right', fontFamily: 'Manrope,sans-serif', color: C.outlineVariant, margin: '4px 0 0' }}>{qBody.length}/2000</p>
                </div>

                {/* Subject + Topic row */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                  <div>
                    <label style={{ display: 'block', fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: C.outline, marginBottom: 7 }}>Subject</label>
                    <div style={{ position: 'relative' }}>
                      <select value={qSubject} onChange={e => setQSubject(e.target.value)}
                        style={{ width: '100%', boxSizing: 'border-box', background: C.surfaceLow, border: '1px solid rgba(171,173,174,0.2)', borderRadius: 12, padding: '11px 36px 11px 14px', fontFamily: 'Manrope,sans-serif', fontSize: 14, color: qSubject ? C.onSurface : C.outlineVariant, outline: 'none', appearance: 'none' }}
                      >
                        <option value="">Pick subject</option>
                        {SUBJECTS.map(s => <option key={s} value={s}>{s}</option>)}
                      </select>
                      <ChevronDown size={14} style={{ position: 'absolute', right: 12, top: '50%', transform: 'translateY(-50%)', color: C.outlineVariant, pointerEvents: 'none' }} />
                    </div>
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: C.outline, marginBottom: 7 }}>Topic</label>
                    <input value={qTopic} onChange={e => setQTopic(e.target.value)} maxLength={100}
                      placeholder="e.g. Thermodynamics"
                      style={{ width: '100%', boxSizing: 'border-box', background: C.surfaceLow, border: '1px solid rgba(171,173,174,0.2)', borderRadius: 12, padding: '11px 16px', fontFamily: 'Manrope,sans-serif', fontSize: 14, color: C.onSurface, outline: 'none' }}
                      onFocus={e => { e.target.style.background = C.surfaceLowest; e.target.style.boxShadow = `0 0 0 2px rgba(52,181,250,0.35)` }}
                      onBlur={e => { e.target.style.background = C.surfaceLow; e.target.style.boxShadow = 'none' }}
                    />
                  </div>
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 4 }}>
                  <button onClick={() => setAskOpen(false)} style={{ padding: '11px 20px', borderRadius: 100, border: '1.5px solid rgba(171,173,174,0.3)', background: 'none', cursor: 'pointer', fontFamily: 'Manrope,sans-serif', fontSize: 14, fontWeight: 600, color: C.onSurfaceVariant }}>Cancel</button>
                  <button
                    onClick={() => askMutation.mutate()}
                    disabled={askMutation.isPending || qTitle.trim().length < 10}
                    style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '11px 22px', borderRadius: 100, border: 'none', cursor: (askMutation.isPending || qTitle.trim().length < 10) ? 'not-allowed' : 'pointer', background: GRADIENT, color: '#fff', fontFamily: 'Manrope,sans-serif', fontSize: 14, fontWeight: 700, boxShadow: '0 4px 14px rgba(0,98,140,0.25)', opacity: (askMutation.isPending || qTitle.trim().length < 10) ? 0.55 : 1 }}
                  >
                    {askMutation.isPending && <Loader2 size={15} style={{ animation: 'spin 1s linear infinite' }} />}
                    Post Question
                  </button>
                </div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </PageWrapper>
  )
}
