import { useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, BookOpen, MessageSquare, Network, Loader2, Sparkles, FileText, Copy, Check } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import { lazy, Suspense } from 'react'
import PageWrapper from '../../components/layout/PageWrapper'
import Avatar from '../../components/ui/Avatar'
import client from '../../api/client'
import { ENDPOINTS } from '../../api/endpoints'
import { useAuthStore } from '../../store/authStore'
import { sanitizeHtml } from '../../utils/sanitize'
import { logger } from '../../utils/logger'

// Lazy-load React Flow for performance
const ReactFlowWrapper = lazy(() => import('./ReactFlowWrapper'))

const C = {
  surfaceLow: '#eff1f2', surfaceLowest: '#ffffff',
  onSurface: '#2c2f30', onSurfaceVariant: '#595c5d', outline: '#757778', outlineVariant: '#abadae',
  primary: '#00628c', primaryContainer: '#34b5fa',
  tertiary: '#584cb5',
}
const GRADIENT = `linear-gradient(135deg, ${C.primary} 0%, ${C.primaryContainer} 100%)`

interface SourceData {
  title: string; type: 'pdf' | 'text' | 'url'; createdAt: string; subject?: string;
  topicCount?: number; status: 'ready' | 'processing' | 'failed';
  studyGuide?: { summary: string; concepts: string[]; questions: string[] };
}

function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false)
  const copy = () => { navigator.clipboard.writeText(text); setCopied(true); setTimeout(() => setCopied(false), 2000) }
  return (
    <button onClick={copy} style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 4 }}>
      {copied ? <Check size={14} color={C.primary} /> : <Copy size={14} color={C.outlineVariant} />}
    </button>
  )
}

export default function SourceViewer() {
  const { sourceId } = useParams<{ sourceId: string }>()
  const user = useAuthStore(s => s.user)
  const [activeTab, setActiveTab] = useState<'guide' | 'chat' | 'map'>('guide')
  const [chatInput, setChatInput] = useState('')
  const [messages, setMessages] = useState([{
    id: '0', role: 'assistant' as const,
    content: "I'm answering based only on this source. What would you like to know?",
  }])
  const [isStreaming, setIsStreaming] = useState(false)

  const { data: source, isLoading } = useQuery<SourceData>({
    queryKey: ['source', sourceId],
    queryFn: async () => { const res = await client.get(`${ENDPOINTS.SOURCES}/${sourceId}`); return res.data },
    placeholderData: {
      title: 'Thermodynamics Chapter Summary', type: 'pdf', createdAt: new Date().toISOString(),
      subject: 'Physics', topicCount: 12, status: 'ready',
      studyGuide: {
        summary: `This chapter covers the fundamental laws of thermodynamics, including the zeroth, first, second, and third laws. 
Key topics include heat transfer mechanisms, work done in thermodynamic processes, entropy, and the Carnot cycle. 
The chapter emphasizes practical applications in heat engines and refrigerators relevant to the JEE syllabus.`,
        concepts: ['First Law of Thermodynamics', 'Entropy', 'Carnot Cycle', 'Heat Engines', 'Isothermal Process', 'Adiabatic Process', 'Internal Energy', 'Specific Heat Capacity'],
        questions: [
          'A gas undergoes an isothermal process. If the volume doubles, what happens to the pressure?',
          'Define entropy and explain the second law of thermodynamics.',
          'Calculate the efficiency of a Carnot engine operating between 300K and 600K.',
          'What is the significance of the zeroth law of thermodynamics?',
        ],
      },
    },
  })

  const sendMessage = async () => {
    if (!chatInput.trim() || isStreaming) return
    const userMsg = { id: Date.now().toString(), role: 'user' as const, content: chatInput.trim() }
    setMessages(prev => [...prev, userMsg])
    setChatInput('')
    setIsStreaming(true)
    logger.info('[Notes] Chat started for source', sourceId)
    const assistantId = (Date.now() + 1).toString()
    setMessages(prev => [...prev, { id: assistantId, role: 'assistant' as const, content: '' }])
    try {
      const res = await client.post(ENDPOINTS.CHAT, { message: userMsg.content, sourceId, useNotes: true })
      setMessages(prev => prev.map(m => m.id === assistantId ? { ...m, content: res.data.message } : m))
    } catch {
      setMessages(prev => prev.map(m => m.id === assistantId ? { ...m, content: "I couldn't connect. Please try again." } : m))
    } finally { setIsStreaming(false) }
  }

  const tabs = [
    { id: 'guide', icon: <BookOpen size={15} />, label: 'Study Guide' },
    { id: 'chat', icon: <MessageSquare size={15} />, label: 'Chat' },
    { id: 'map', icon: <Network size={15} />, label: 'Mind Map' },
  ]

  return (
    <PageWrapper>
      {/* Back + Header */}
      <div style={{ marginBottom: 28 }}>
        <Link to="/notes" style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: 13, fontFamily: 'Manrope,sans-serif', fontWeight: 600, color: C.primary, textDecoration: 'none', marginBottom: 16 }}>
          <ArrowLeft size={15} /> Back to My Notes
        </Link>
        {isLoading ? (
          <div style={{ height: 32, width: 280, background: C.surfaceLow, borderRadius: 8, animation: 'pulse 1.5s ease-in-out infinite' }} />
        ) : (
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
              <h1 style={{ fontSize: 24, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: 0 }}>{source?.title}</h1>
              <span style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, background: 'rgba(0,98,140,0.08)', color: C.primary, padding: '3px 10px', borderRadius: 100 }}>
                {source?.type?.toUpperCase()}
              </span>
              {source?.subject && (
                <span style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 600, background: 'rgba(88,76,181,0.08)', color: C.tertiary, padding: '3px 10px', borderRadius: 100 }}>
                  {source.subject}
                </span>
              )}
            </div>
            <p style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.outlineVariant, margin: '6px 0 0' }}>
              {source?.topicCount} key topics extracted · Imported {new Date(source?.createdAt ?? '').toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}
            </p>
          </div>
        )}
      </div>

      {/* Tab bar */}
      <div style={{ display: 'flex', background: C.surfaceLow, borderRadius: 14, padding: 4, gap: 2, marginBottom: 24, width: 'fit-content' }}>
        {tabs.map(t => (
          <button key={t.id} onClick={() => setActiveTab(t.id as typeof activeTab)} style={{
            display: 'flex', alignItems: 'center', gap: 7, padding: '9px 18px', borderRadius: 11, border: 'none', cursor: 'pointer',
            background: activeTab === t.id ? C.surfaceLowest : 'none',
            fontFamily: 'Manrope,sans-serif', fontSize: 13, fontWeight: 600,
            color: activeTab === t.id ? C.onSurface : C.outlineVariant,
            boxShadow: activeTab === t.id ? '0 1px 4px rgba(0,0,0,0.06)' : 'none',
            transition: 'all 0.15s',
          }}>{t.icon} {t.label}</button>
        ))}
      </div>

      <AnimatePresence mode="wait">
        <motion.div key={activeTab} initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -8 }} transition={{ duration: 0.2 }}>

          {/* ── STUDY GUIDE ── */}
          {activeTab === 'guide' && (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 320px', gap: 20 }}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
                {/* Summary */}
                <div style={{ background: C.surfaceLowest, borderRadius: 20, padding: 24, boxShadow: '0 2px 12px rgba(0,98,140,0.06)', border: '1px solid rgba(171,173,174,0.15)' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 16 }}>
                    <div style={{ width: 28, height: 28, borderRadius: 8, background: GRADIENT, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                      <Sparkles size={14} color="#fff" />
                    </div>
                    <h2 style={{ fontSize: 15, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: 0 }}>AI Summary</h2>
                  </div>
                  {isLoading ? (
                    [1,2,3].map(i => <div key={i} style={{ height: 16, background: C.surfaceLow, borderRadius: 6, marginBottom: 8, width: `${70 + i * 10}%`, animation: 'pulse 1.5s ease-in-out infinite' }} />)
                  ) : (
                    <p style={{ fontSize: 14, fontFamily: 'Manrope,sans-serif', color: C.onSurface, lineHeight: 1.8, margin: 0, whiteSpace: 'pre-line' }}>
                      {source?.studyGuide?.summary}
                    </p>
                  )}
                </div>

                {/* Likely exam questions */}
                <div style={{ background: C.surfaceLowest, borderRadius: 20, padding: 24, boxShadow: '0 2px 12px rgba(0,98,140,0.06)', border: '1px solid rgba(171,173,174,0.15)' }}>
                  <h2 style={{ fontSize: 15, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: '0 0 16px' }}>Likely Exam Questions</h2>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                    {source?.studyGuide?.questions.map((q, i) => (
                      <div key={i} style={{ background: C.surfaceLow, borderRadius: 12, padding: '12px 16px', display: 'flex', alignItems: 'flex-start', gap: 12 }}>
                        <span style={{ fontSize: 12, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.primary, minWidth: 20 }}>Q{i+1}</span>
                        <p style={{ fontSize: 13, fontFamily: 'Manrope,sans-serif', color: C.onSurface, margin: 0, lineHeight: 1.6, flex: 1 }}>{q}</p>
                        <CopyButton text={q} />
                      </div>
                    ))}
                  </div>
                </div>
              </div>

              {/* Key concepts sidebar */}
              <div style={{ background: C.surfaceLowest, borderRadius: 20, padding: 24, boxShadow: '0 2px 12px rgba(0,98,140,0.06)', border: '1px solid rgba(171,173,174,0.15)', height: 'fit-content' }}>
                <h2 style={{ fontSize: 15, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: '0 0 16px' }}>Key Concepts</h2>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                  {source?.studyGuide?.concepts.map((c, i) => (
                    <span key={i} style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', fontWeight: 600, background: 'rgba(0,98,140,0.06)', color: C.primary, padding: '5px 12px', borderRadius: 100 }}>
                      {c}
                    </span>
                  ))}
                </div>
                <div style={{ marginTop: 24, padding: '14px 16px', background: 'rgba(88,76,181,0.06)', borderRadius: 14 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8 }}>
                    <div style={{ width: 18, height: 18, borderRadius: '50%', background: GRADIENT, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                      <Sparkles size={9} color="#fff" />
                    </div>
                    <span style={{ fontSize: 10, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase', color: C.tertiary }}>TIP</span>
                  </div>
                  <p style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.onSurface, lineHeight: 1.6, margin: 0 }}>
                    Switch to the <strong>Chat</strong> tab to ask specific questions about this source.
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* ── CHAT ── */}
          {activeTab === 'chat' && (
            <div style={{ background: C.surfaceLowest, borderRadius: 20, padding: 0, boxShadow: '0 2px 12px rgba(0,98,140,0.06)', border: '1px solid rgba(171,173,174,0.15)', overflow: 'hidden', display: 'flex', flexDirection: 'column', height: 520 }}>
              {/* System banner */}
              <div style={{ padding: '12px 20px', background: 'rgba(0,98,140,0.04)', borderBottom: '1px solid rgba(171,173,174,0.15)', display: 'flex', alignItems: 'center', gap: 8 }}>
                <FileText size={14} color={C.primary} />
                <span style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', fontWeight: 600, color: C.primary }}>
                  Answering based only on: <strong>{source?.title}</strong>
                </span>
              </div>
              {/* Messages */}
              <div style={{ flex: 1, overflowY: 'auto', padding: '16px 20px', display: 'flex', flexDirection: 'column', gap: 14 }}>
                {messages.map(msg => (
                  <div key={msg.id} style={{ display: 'flex', gap: 10, flexDirection: msg.role === 'user' ? 'row-reverse' : 'row' }}>
                    {msg.role === 'assistant' ? (
                      <div style={{ width: 28, height: 28, borderRadius: '50%', background: GRADIENT, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                        <Sparkles size={13} color="#fff" />
                      </div>
                    ) : <Avatar name={user?.fullName} size="sm" />}
                    <div style={{
                      maxWidth: '72%', padding: '10px 14px', borderRadius: 16, fontFamily: 'Manrope,sans-serif', fontSize: 13, lineHeight: 1.65,
                      ...(msg.role === 'user'
                        ? { background: GRADIENT, color: '#fff', borderTopRightRadius: 4 }
                        : { background: C.surfaceLow, color: C.onSurface, borderTopLeftRadius: 4 }
                      ),
                    }}>
                      {msg.role === 'assistant' ? <span dangerouslySetInnerHTML={{ __html: sanitizeHtml(msg.content || '▊') }} /> : msg.content}
                    </div>
                  </div>
                ))}
              </div>
              {/* Input */}
              <div style={{ padding: '12px 16px', borderTop: '1px solid rgba(171,173,174,0.15)', display: 'flex', gap: 10 }}>
                <input
                  value={chatInput} onChange={e => setChatInput(e.target.value)}
                  onKeyDown={e => { if (e.key === 'Enter') sendMessage() }}
                  placeholder={`Ask anything about "${source?.title}"…`}
                  style={{ flex: 1, background: C.surfaceLow, border: '1px solid rgba(171,173,174,0.2)', borderRadius: 12, padding: '10px 14px', fontFamily: 'Manrope,sans-serif', fontSize: 13, color: C.onSurface, outline: 'none' }}
                />
                <button onClick={sendMessage} disabled={isStreaming || !chatInput.trim()} style={{ width: 40, height: 40, borderRadius: 11, border: 'none', cursor: 'pointer', background: chatInput.trim() ? GRADIENT : 'rgba(171,173,174,0.3)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  {isStreaming ? <Loader2 size={15} color="#fff" style={{ animation: 'spin 1s linear infinite' }} /> : <MessageSquare size={16} color={chatInput.trim() ? '#fff' : C.outlineVariant} />}
                </button>
              </div>
            </div>
          )}

          {/* ── MIND MAP ── */}
          {activeTab === 'map' && (
            <div style={{ background: C.surfaceLowest, borderRadius: 20, boxShadow: '0 2px 12px rgba(0,98,140,0.06)', border: '1px solid rgba(171,173,174,0.15)', overflow: 'hidden', height: 520 }}>
              <Suspense fallback={
                <div style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', flexDirection: 'column', gap: 12 }}>
                  <Loader2 size={24} color={C.primary} style={{ animation: 'spin 1s linear infinite' }} />
                  <p style={{ fontSize: 13, fontFamily: 'Manrope,sans-serif', color: C.outlineVariant }}>Loading mind map…</p>
                </div>
              }>
                <ReactFlowWrapper sourceId={sourceId ?? ''} />
              </Suspense>
            </div>
          )}

        </motion.div>
      </AnimatePresence>
    </PageWrapper>
  )
}
