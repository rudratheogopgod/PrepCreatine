import { useState, useRef, useEffect, lazy, Suspense } from 'react'
import { Send, BookOpen, ExternalLink, Sparkles, Network, Plus, Loader2, ChevronLeft, ChevronRight } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import { useQuery } from '@tanstack/react-query'
import Avatar from '../../components/ui/Avatar'
import { useAuthStore } from '../../store/authStore'
import client from '../../api/client'
import { ENDPOINTS } from '../../api/endpoints'
import { sanitizeHtml } from '../../utils/sanitize'
import { logger } from '../../utils/logger'

// Lazy load React Flow concept map to avoid bundle bloat
const ReactFlowWrapper = lazy(() => import('../notes/ReactFlowWrapper'))

const C = {
  surface: '#f5f6f7', surfaceLow: '#eff1f2', surfaceLowest: '#ffffff',
  onSurface: '#2c2f30', onSurfaceVariant: '#595c5d', outline: '#757778', outlineVariant: '#abadae',
  primary: '#00628c', primaryContainer: '#34b5fa',
  tertiary: '#584cb5',
}
const GRADIENT = `linear-gradient(135deg, ${C.primary} 0%, ${C.primaryContainer} 100%)`

interface Message {
  id: string; role: 'user' | 'assistant'; content: string;
  youtubeVideos?: { title: string; url: string }[]
}
interface ChatHistory { id: string; title: string; createdAt: string; preview: string }

export default function Learn() {
  const user = useAuthStore(s => s.user)
  const [messages, setMessages] = useState<Message[]>([{
    id: '0', role: 'assistant',
    content: "Hi! I'm your PrepCreatine AI tutor. Ask me anything about your syllabus — from Newton's Laws to organic chemistry. What would you like to understand today?",
  }])
  const [input, setInput] = useState('')
  const [isStreaming, setIsStreaming] = useState(false)
  const [useNotes, setUseNotes] = useState(false)
  const [activePanel, setActivePanel] = useState<'map' | null>(null)
  const [historyOpen, setHistoryOpen] = useState(true)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const { data: chatHistory } = useQuery<ChatHistory[]>({
    queryKey: ['chat-history'],
    queryFn: async () => { const res = await client.get(`${ENDPOINTS.CHAT}/history`); return res.data },
    placeholderData: [
      { id: 'h1', title: 'Thermodynamics revision', createdAt: new Date(Date.now() - 86400000).toISOString(), preview: 'Explain the first law...' },
      { id: 'h2', title: 'Organic Chemistry mechanisms', createdAt: new Date(Date.now() - 172800000).toISOString(), preview: 'What is SN2 reaction?' },
      { id: 'h3', title: 'Integration techniques', createdAt: new Date(Date.now() - 259200000).toISOString(), preview: 'How to solve definite...' },
    ]
  })

  const sendMessage = async () => {
    if (!input.trim() || isStreaming) return
    const userMsg: Message = { id: Date.now().toString(), role: 'user', content: input.trim() }
    setMessages(prev => [...prev, userMsg])
    setInput('')
    setIsStreaming(true)
    logger.info('[Chat] Stream started', { useNotes })
    const assistantId = (Date.now() + 1).toString()
    setMessages(prev => [...prev, { id: assistantId, role: 'assistant', content: '' }])

    try {
      const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}${ENDPOINTS.CHAT_STREAM}`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        credentials: 'include', body: JSON.stringify({ message: userMsg.content, useNotes }),
      })
      if (!response.body) throw new Error('No response body')
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let accumulated = ''
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        accumulated += decoder.decode(value, { stream: true })
        setMessages(prev => prev.map(m => m.id === assistantId ? { ...m, content: accumulated } : m))
      }
      logger.info('[Chat] Stream ended')
    } catch {
      try {
        const res = await client.post(ENDPOINTS.CHAT, { message: userMsg.content, useNotes })
        setMessages(prev => prev.map(m => m.id === assistantId ? { ...m, content: res.data.message, youtubeVideos: res.data.youtubeVideos } : m))
      } catch {
        setMessages(prev => prev.map(m => m.id === assistantId ? { ...m, content: "I'm having trouble connecting right now. Please try again in a moment." } : m))
        logger.error('[Chat] Stream error')
      }
    } finally { setIsStreaming(false) }
  }

  return (
    <div style={{ display: 'flex', height: 'calc(100vh - 64px)', overflow: 'hidden', background: C.surface }}>

      {/* ── Chat History Sidebar ── */}
      <div style={{
        width: historyOpen ? 240 : 0, minWidth: historyOpen ? 240 : 0,
        overflow: 'hidden', transition: 'all 0.25s ease',
        background: C.surfaceLowest, borderRight: '1px solid rgba(171,173,174,0.18)',
        display: 'flex', flexDirection: 'column',
      }}>
        <div style={{ padding: '16px 16px 12px', borderBottom: '1px solid rgba(171,173,174,0.12)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 }}>
            <p style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: C.outline, margin: 0 }}>History</p>
            <button onClick={() => setHistoryOpen(false)} style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 2 }}>
              <ChevronLeft size={16} color={C.outlineVariant} />
            </button>
          </div>
          <button onClick={() => setMessages([{ id: Date.now().toString(), role: 'assistant', content: "Hi! Starting a new conversation. What would you like to learn?" }])}
            style={{ display: 'flex', alignItems: 'center', gap: 7, width: '100%', padding: '9px 12px', borderRadius: 10, border: 'none', cursor: 'pointer', background: GRADIENT, color: '#fff', fontFamily: 'Manrope,sans-serif', fontSize: 12, fontWeight: 600 }}
          >
            <Plus size={13} /> New Conversation
          </button>
        </div>
        <div style={{ flex: 1, overflowY: 'auto', padding: '10px 10px' }}>
          {chatHistory?.map(h => (
            <div key={h.id} style={{ padding: '10px 12px', borderRadius: 10, cursor: 'pointer', marginBottom: 4, transition: 'all 0.1s' }}
              onMouseEnter={e => (e.currentTarget as HTMLElement).style.background = C.surfaceLow}
              onMouseLeave={e => (e.currentTarget as HTMLElement).style.background = 'none'}
            >
              <p style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', fontWeight: 600, color: C.onSurface, margin: '0 0 3px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{h.title}</p>
              <p style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', color: C.outlineVariant, margin: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{h.preview}</p>
            </div>
          ))}
        </div>
      </div>

      {/* ── Main Chat Area ── */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden', minWidth: 0 }}>
        {/* Header */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '14px 24px', borderBottom: '1px solid rgba(171,173,174,0.18)', background: C.surfaceLowest }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            {!historyOpen && (
              <button onClick={() => setHistoryOpen(true)} style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '2px 6px 2px 0' }}>
                <ChevronRight size={16} color={C.outlineVariant} />
              </button>
            )}
            <div style={{ width: 32, height: 32, borderRadius: '50%', background: GRADIENT, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Sparkles size={15} color="#fff" />
            </div>
            <div>
              <h1 style={{ fontSize: 15, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: 0 }}>AI Tutor</h1>
              <p style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', color: C.outlineVariant, margin: 0 }}>Powered by Gemini</p>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            {/* Use notes toggle */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }} onClick={() => setUseNotes(!useNotes)}>
              <span style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', fontWeight: 600, color: C.onSurfaceVariant, display: 'flex', alignItems: 'center', gap: 5 }}>
                <BookOpen size={13} /> My notes
              </span>
              <div style={{ width: 36, height: 20, borderRadius: 100, background: useNotes ? GRADIENT : 'rgba(171,173,174,0.3)', position: 'relative', transition: 'all 0.2s', flexShrink: 0 }}>
                <div style={{ position: 'absolute', top: 2, left: useNotes ? 18 : 2, width: 16, height: 16, borderRadius: '50%', background: '#fff', transition: 'left 0.2s', boxShadow: '0 1px 3px rgba(0,0,0,0.15)' }} />
              </div>
            </div>
            {/* Concept map toggle */}
            <button
              onClick={() => setActivePanel(p => p === 'map' ? null : 'map')}
              style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '7px 14px', borderRadius: 10, border: 'none', cursor: 'pointer', fontFamily: 'Manrope,sans-serif', fontSize: 12, fontWeight: 600, background: activePanel === 'map' ? 'rgba(0,98,140,0.1)' : C.surfaceLow, color: activePanel === 'map' ? C.primary : C.onSurfaceVariant, transition: 'all 0.15s' }}
            >
              <Network size={14} /> Concept Map
            </button>
          </div>
        </div>

        {/* Chat + Concept Map split */}
        <div style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>
          {/* Messages */}
          <div style={{ flex: 1, overflowY: 'auto', padding: '20px 24px', display: 'flex', flexDirection: 'column', gap: 16, minWidth: 0 }}>
            <AnimatePresence initial={false}>
              {messages.map(msg => (
                <motion.div key={msg.id} initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}
                  style={{ display: 'flex', gap: 12, flexDirection: msg.role === 'user' ? 'row-reverse' : 'row' }}
                >
                  {msg.role === 'assistant' ? (
                    <div style={{ width: 30, height: 30, borderRadius: '50%', background: GRADIENT, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                      <Sparkles size={14} color="#fff" />
                    </div>
                  ) : (
                    <Avatar name={user?.fullName} size="sm" />
                  )}
                  <div style={{
                    maxWidth: '70%', padding: '12px 16px', borderRadius: 18,
                    fontFamily: 'Manrope,sans-serif', fontSize: 14, lineHeight: 1.65,
                    ...(msg.role === 'user'
                      ? { background: GRADIENT, color: '#fff', borderTopRightRadius: 4, boxShadow: '0 4px 12px rgba(0,98,140,0.22)' }
                      : { background: C.surfaceLowest, color: C.onSurface, borderTopLeftRadius: 4, border: '1px solid rgba(171,173,174,0.15)', boxShadow: '0 2px 8px rgba(0,98,140,0.05)' }
                    ),
                  }}>
                    {msg.role === 'assistant'
                      ? <span dangerouslySetInnerHTML={{ __html: sanitizeHtml(msg.content || '▊') }} />
                      : msg.content
                    }
                    {msg.youtubeVideos?.map(v => (
                      <a key={v.url} href={v.url} target="_blank" rel="noopener noreferrer"
                        style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 10, fontSize: 12, color: C.primary, background: 'rgba(0,98,140,0.06)', borderRadius: 10, padding: '8px 12px', textDecoration: 'none' }}
                      >
                        <ExternalLink size={12} /><span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{v.title}</span>
                      </a>
                    ))}
                  </div>
                </motion.div>
              ))}
            </AnimatePresence>
            {isStreaming && (
              <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end' }}>
                <div style={{ width: 30, height: 30, borderRadius: '50%', background: GRADIENT, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                  <Sparkles size={14} color="#fff" />
                </div>
                <div style={{ background: C.surfaceLowest, borderRadius: 18, borderTopLeftRadius: 4, padding: '12px 16px', border: '1px solid rgba(171,173,174,0.15)' }}>
                  <div style={{ display: 'flex', gap: 5 }}>
                    {[0, 1, 2].map(i => (
                      <div key={i} style={{ width: 7, height: 7, borderRadius: '50%', background: C.outlineVariant, animation: `pulse 1.2s ease-in-out ${i * 0.2}s infinite` }} />
                    ))}
                  </div>
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* ── Concept Map Panel ── */}
          {activePanel === 'map' && (
            <div style={{ width: 380, borderLeft: '1px solid rgba(171,173,174,0.18)', background: C.surfaceLowest, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
              <div style={{ padding: '12px 16px', borderBottom: '1px solid rgba(171,173,174,0.12)', display: 'flex', alignItems: 'center', gap: 8 }}>
                <Network size={15} color={C.primary} />
                <p style={{ fontSize: 13, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 700, color: C.onSurface, margin: 0 }}>Concept Map</p>
              </div>
              <div style={{ flex: 1, overflow: 'hidden' }}>
                <Suspense fallback={
                  <div style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', flexDirection: 'column', gap: 10 }}>
                    <Loader2 size={22} color={C.primary} style={{ animation: 'spin 1s linear infinite' }} />
                    <p style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.outlineVariant }}>Loading concept map…</p>
                  </div>
                }>
                  <ReactFlowWrapper sourceId="current-session" />
                </Suspense>
              </div>
            </div>
          )}
        </div>

        {/* Input bar */}
        <div style={{ padding: '14px 24px', borderTop: '1px solid rgba(171,173,174,0.18)', background: C.surfaceLowest }}>
          <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end' }}>
            <div style={{ flex: 1, background: C.surface, borderRadius: 16, padding: '12px 16px', border: '1px solid rgba(171,173,174,0.2)', transition: 'all 0.15s' }}>
              <textarea
                value={input}
                onChange={e => setInput(e.target.value)}
                onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage() } }}
                placeholder="Ask anything about your syllabus…"
                rows={1}
                disabled={isStreaming}
                style={{ width: '100%', background: 'none', border: 'none', outline: 'none', resize: 'none', fontFamily: 'Manrope,sans-serif', fontSize: 14, color: C.onSurface }}
              />
            </div>
            <button
              onClick={sendMessage}
              disabled={isStreaming || !input.trim()}
              aria-label="Send message"
              style={{
                width: 44, height: 44, borderRadius: 13, border: 'none', cursor: (isStreaming || !input.trim()) ? 'not-allowed' : 'pointer',
                background: (isStreaming || !input.trim()) ? 'rgba(171,173,174,0.3)' : GRADIENT,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                boxShadow: (!isStreaming && input.trim()) ? '0 4px 12px rgba(0,98,140,0.25)' : 'none',
                transition: 'all 0.15s',
              }}
            >
              <Send size={17} color={(!isStreaming && input.trim()) ? '#fff' : C.outlineVariant} />
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
