import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { UploadCloud, FileText, Search, Plus, Loader2, Sparkles, Folder, X, Link as LinkIcon } from 'lucide-react'
import { toast } from 'react-hot-toast'
import { motion, AnimatePresence } from 'framer-motion'
import PageWrapper from '../../components/layout/PageWrapper'
import client from '../../api/client'
import { ENDPOINTS } from '../../api/endpoints'
import { timeAgo } from '../../utils/format'
import { logger } from '../../utils/logger'

const C = {
  surface: '#f5f6f7', surfaceLow: '#eff1f2', surfaceLowest: '#ffffff',
  onSurface: '#2c2f30', onSurfaceVariant: '#595c5d', outline: '#757778', outlineVariant: '#abadae',
  primary: '#00628c', primaryContainer: '#34b5fa',
  tertiary: '#584cb5', tertiaryContainer: '#afa6ff',
}
const GRADIENT = `linear-gradient(135deg, ${C.primary} 0%, ${C.primaryContainer} 100%)`

interface Source {
  id: string; title: string; type: 'pdf' | 'text';
  createdAt: string; wordCount?: number; subject?: string;
}

export default function Notes() {
  const queryClient = useQueryClient()
  const [search, setSearch] = useState('')
  const [uploadOpen, setUploadOpen] = useState(false)
  const [uploadType, setUploadType] = useState<'url' | 'pdf' | 'text'>('url')
  const [textInput, setTextInput] = useState('')
  const [titleInput, setTitleInput] = useState('')
  const [urlInput, setUrlInput] = useState('')
  const [urlError, setUrlError] = useState('')
  const [file, setFile] = useState<File | null>(null)

  const { data: sources, isLoading } = useQuery<Source[]>({
    queryKey: ['sources'],
    queryFn: async () => { const res = await client.get(ENDPOINTS.SOURCES); return res.data },
    placeholderData: [
      { id: '1', title: 'Thermodynamics Chapter Summary', type: 'pdf', createdAt: new Date(Date.now() - 86400000).toISOString(), subject: 'Physics' },
      { id: '2', title: 'Calculus Formulas', type: 'text', createdAt: new Date(Date.now() - 172800000).toISOString(), subject: 'Math', wordCount: 450 }
    ]
  })

  const uploadMutation = useMutation({
    mutationFn: async () => {
      logger.info('[Notes] Source added', { type: uploadType })
      if (uploadType === 'url') {
        await client.post(`${ENDPOINTS.SOURCES}/import-url`, { url: urlInput.trim(), title: titleInput || urlInput })
      } else {
        const formData = new FormData()
        formData.append('title', titleInput)
        formData.append('type', uploadType)
        if (uploadType === 'pdf' && file) formData.append('file', file)
        if (uploadType === 'text') formData.append('content', textInput)
        await client.post(ENDPOINTS.SOURCES, formData, { headers: { 'Content-Type': 'multipart/form-data' } })
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sources'] })
      setUploadOpen(false); setTitleInput(''); setTextInput(''); setFile(null); setUrlInput(''); setUrlError('')
      toast.success('Source added! Processing will complete in a moment.')
    },
    onError: () => toast.error('Failed to upload source. Please try again.')
  })

  const handleUrlBlur = () => {
    if (!urlInput) { setUrlError(''); return }
    try { new URL(urlInput); setUrlError('') } catch { setUrlError('Please include https:// at the start.') }
  }

  const isSubmitDisabled = uploadMutation.isPending ||
    (uploadType === 'url' && (!urlInput || !!urlError)) ||
    (uploadType === 'pdf' && !file) ||
    (uploadType === 'text' && textInput.length < 50)

  const filtered = sources?.filter(s => search ? s.title.toLowerCase().includes(search.toLowerCase()) : true)

  const typeColors: Record<'pdf' | 'text', { bg: string; color: string }> = {
    pdf: { bg: 'rgba(179,27,37,0.08)', color: '#b31b25' },
    text: { bg: 'rgba(0,98,140,0.08)', color: C.primary },
  }

  return (
    <PageWrapper>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 32, gap: 16, flexWrap: 'wrap' }}>
        <div>
          <p style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: C.primary, margin: '0 0 6px' }}>NOTES</p>
          <h1 style={{ fontSize: 28, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: '0 0 4px' }}>My Notes</h1>
          <p style={{ fontSize: 14, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, margin: 0 }}>Upload PDFs or text for AI-generated summaries and flashcards.</p>
        </div>
        <button
          onClick={() => setUploadOpen(true)}
          style={{
            display: 'flex', alignItems: 'center', gap: 8, padding: '11px 22px',
            borderRadius: 100, border: 'none', cursor: 'pointer',
            background: GRADIENT, color: '#fff',
            fontFamily: 'Manrope,sans-serif', fontSize: 14, fontWeight: 700,
            boxShadow: '0 4px 14px rgba(0,98,140,0.25)', flexShrink: 0,
          }}
        >
          <Plus size={18} /> Add Source
        </button>
      </div>

      {/* Search */}
      <div style={{ position: 'relative', marginBottom: 24 }}>
        <Search size={16} style={{ position: 'absolute', left: 14, top: '50%', transform: 'translateY(-50%)', color: C.outlineVariant }} />
        <input
          type="text"
          placeholder="Search your notes…"
          value={search}
          onChange={e => setSearch(e.target.value)}
          style={{
            width: '100%', boxSizing: 'border-box',
            background: C.surfaceLowest, border: '1px solid rgba(171,173,174,0.2)',
            borderRadius: 14, padding: '11px 16px 11px 42px',
            fontFamily: 'Manrope,sans-serif', fontSize: 14, color: C.onSurface, outline: 'none',
          }}
          onFocus={e => { e.target.style.boxShadow = `0 0 0 2px rgba(52,181,250,0.35)` }}
          onBlur={e => { e.target.style.boxShadow = 'none' }}
        />
      </div>

      {/* Content */}
      {isLoading ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: '48px 0' }}>
          <Loader2 size={28} color={C.primary} style={{ animation: 'spin 1s linear infinite' }} />
        </div>
      ) : filtered?.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '64px 24px', background: C.surfaceLowest, borderRadius: 24, border: '2px dashed rgba(171,173,174,0.3)' }}>
          <div style={{ width: 64, height: 64, borderRadius: '50%', background: C.surfaceLow, display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 16px' }}>
            <Folder size={28} color={C.outlineVariant} />
          </div>
          <h3 style={{ fontSize: 18, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: '0 0 8px' }}>No notes found</h3>
          <p style={{ fontSize: 14, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, margin: '0 0 24px' }}>Upload PDFs or paste text to create your knowledge base.</p>
          <button onClick={() => setUploadOpen(true)} style={{ padding: '11px 24px', borderRadius: 100, border: `1.5px solid rgba(171,173,174,0.3)`, background: 'none', cursor: 'pointer', fontFamily: 'Manrope,sans-serif', fontSize: 14, fontWeight: 600, color: C.onSurface }}>Upload your first note</button>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill,minmax(260px,1fr))', gap: 16 }}>
          {filtered?.map((source, i) => (
            <motion.div key={source.id} initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.05 }}>
              <Link to={`/notes/${source.id}`} style={{ textDecoration: 'none' }}>
                <div
                  style={{ background: C.surfaceLowest, borderRadius: 20, padding: 20, border: '1px solid rgba(171,173,174,0.15)', boxShadow: '0 2px 10px rgba(0,98,140,0.05)', cursor: 'pointer', height: '100%', transition: 'all 0.15s', display: 'flex', flexDirection: 'column' }}
                  onMouseEnter={e => { const el = e.currentTarget as HTMLElement; el.style.boxShadow = '0 8px 24px rgba(0,98,140,0.12)'; el.style.transform = 'translateY(-2px)' }}
                  onMouseLeave={e => { const el = e.currentTarget as HTMLElement; el.style.boxShadow = '0 2px 10px rgba(0,98,140,0.05)'; el.style.transform = 'none' }}
                >
                  <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 12 }}>
                    <div style={{ width: 42, height: 42, borderRadius: 12, background: typeColors[source.type].bg, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                      <FileText size={20} color={typeColors[source.type].color} />
                    </div>
                    <span style={{ fontSize: 10, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase', background: C.surfaceLow, color: C.outline, padding: '3px 8px', borderRadius: 6 }}>
                      {source.type}
                    </span>
                  </div>
                  <h3 style={{ fontSize: 14, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 700, color: C.onSurface, margin: 0, lineHeight: 1.4 }}>{source.title}</h3>
                  <div style={{ marginTop: 'auto', paddingTop: 14, display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', color: C.outlineVariant }}>{timeAgo(source.createdAt)}</span>
                    {source.subject && (
                      <span style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 600, color: C.primary, background: 'rgba(0,98,140,0.08)', padding: '2px 9px', borderRadius: 100 }}>
                        {source.subject}
                      </span>
                    )}
                  </div>
                </div>
              </Link>
            </motion.div>
          ))}
        </div>
      )}

      {/* Upload Modal */}
      <AnimatePresence>
        {uploadOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            style={{
              position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.45)', backdropFilter: 'blur(4px)',
              display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50, padding: 16,
            }}
            onClick={() => setUploadOpen(false)}
          >
            <motion.div
              initial={{ opacity: 0, y: 24 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: 16 }}
              transition={{ type: 'spring', stiffness: 400, damping: 35 }}
              onClick={e => e.stopPropagation()}
              style={{ background: C.surfaceLowest, borderRadius: 24, padding: 28, width: '100%', maxWidth: 480, boxShadow: '0 28px 60px rgba(0,0,0,0.18)' }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 24 }}>
                <h2 style={{ fontSize: 18, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: 0 }}>Add Source Material</h2>
                <button onClick={() => setUploadOpen(false)} style={{ background: C.surfaceLow, border: 'none', borderRadius: 10, width: 32, height: 32, display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
                  <X size={16} color={C.onSurfaceVariant} />
                </button>
              </div>

              {/* Type tabs — 3 tabs per FDD: URL, PDF, Text */}
              <div style={{ display: 'flex', background: C.surfaceLow, borderRadius: 12, padding: 4, marginBottom: 20 }}>
                {(['url', 'pdf', 'text'] as const).map(t => (
                  <button key={t} onClick={() => setUploadType(t)} style={{
                    flex: 1, padding: '9px', borderRadius: 10, border: 'none', cursor: 'pointer',
                    background: uploadType === t ? C.surfaceLowest : 'none',
                    fontFamily: 'Manrope,sans-serif', fontSize: 13, fontWeight: 600,
                    color: uploadType === t ? C.primary : C.onSurfaceVariant,
                    boxShadow: uploadType === t ? '0 1px 4px rgba(0,0,0,0.08)' : 'none',
                    transition: 'all 0.15s',
                  }}>
                    {t === 'url' ? 'Paste URL' : t === 'pdf' ? 'Upload PDF' : 'Paste Text'}
                  </button>
                ))}
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                {/* Title */}
                <div>
                  <label style={{ display: 'block', fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: C.outline, marginBottom: 7 }}>Title</label>
                  <input type="text" placeholder="E.g. Cell Biology Chapter 3" value={titleInput} onChange={e => setTitleInput(e.target.value)}
                    style={{ width: '100%', boxSizing: 'border-box', background: C.surfaceLow, border: '1px solid rgba(171,173,174,0.2)', borderRadius: 12, padding: '11px 16px', fontFamily: 'Manrope,sans-serif', fontSize: 14, color: C.onSurface, outline: 'none' }}
                    onFocus={e => { e.target.style.background = C.surfaceLowest; e.target.style.boxShadow = `0 0 0 2px rgba(52,181,250,0.35)` }}
                    onBlur={e => { e.target.style.background = C.surfaceLow; e.target.style.boxShadow = 'none' }}
                  />
                </div>

                {uploadType === 'url' ? (
                  <div>
                    <label style={{ display: 'block', fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: C.outline, marginBottom: 7 }}>Website or Article URL</label>
                    <div style={{ position: 'relative' }}>
                      <LinkIcon size={15} style={{ position: 'absolute', left: 14, top: '50%', transform: 'translateY(-50%)', color: C.outlineVariant }} />
                      <input
                        type="url" placeholder="https://example.com/article" value={urlInput} maxLength={2048}
                        onChange={e => setUrlInput(e.target.value)}
                        onBlur={handleUrlBlur}
                        style={{ width: '100%', boxSizing: 'border-box', paddingLeft: 40, background: C.surfaceLow, border: `1px solid ${urlError ? '#b31b25' : 'rgba(171,173,174,0.2)'}`, borderRadius: 12, padding: '11px 16px 11px 40px', fontFamily: 'Manrope,sans-serif', fontSize: 14, color: C.onSurface, outline: 'none' }}
                        onFocus={e => { e.target.style.background = C.surfaceLowest; e.target.style.boxShadow = `0 0 0 2px rgba(52,181,250,0.35)` }}
                        onBlurCapture={e => { e.target.style.background = C.surfaceLow; e.target.style.boxShadow = 'none' }}
                      />
                    </div>
                    {urlError && <p style={{ fontSize: 12, color: '#b31b25', fontFamily: 'Manrope,sans-serif', margin: '5px 0 0' }}>{urlError}</p>}
                    <p style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.outlineVariant, margin: '6px 0 0' }}>We'll read the page and let you ask questions based on it.</p>
                  </div>
                ) : uploadType === 'pdf' ? (
                  <div>
                    <label style={{ display: 'block', fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: C.outline, marginBottom: 7 }}>PDF File</label>
                    <div style={{ border: '2px dashed rgba(171,173,174,0.3)', borderRadius: 16, padding: '32px 24px', textAlign: 'center', cursor: 'pointer', transition: 'all 0.15s' }}>
                      <input type="file" accept=".pdf" style={{ display: 'none' }} id="file-upload" onChange={e => setFile(e.target.files?.[0] || null)} />
                      <label htmlFor="file-upload" style={{ cursor: 'pointer', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10 }}>
                        <div style={{ width: 48, height: 48, borderRadius: '50%', background: 'rgba(0,98,140,0.08)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                          <UploadCloud size={24} color={C.primary} />
                        </div>
                        <span style={{ fontSize: 14, fontFamily: 'Manrope,sans-serif', fontWeight: 600, color: file ? C.primary : C.onSurface }}>
                          {file ? file.name : 'Click to browse or drag & drop'}
                        </span>
                        <span style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.outlineVariant }}>Max 20MB per PDF</span>
                      </label>
                    </div>
                  </div>
                ) : (
                  <div>
                    <label style={{ display: 'block', fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: C.outline, marginBottom: 7 }}>Text Content</label>
                    <textarea rows={5} value={textInput} onChange={e => setTextInput(e.target.value)} placeholder="Paste your notes here... (min 50 characters)"
                      style={{ width: '100%', boxSizing: 'border-box', background: C.surfaceLow, border: '1px solid rgba(171,173,174,0.2)', borderRadius: 12, padding: '11px 16px', fontFamily: 'Manrope,sans-serif', fontSize: 14, color: C.onSurface, outline: 'none', resize: 'none' }}
                      onFocus={e => { e.target.style.background = C.surfaceLowest; e.target.style.boxShadow = `0 0 0 2px rgba(52,181,250,0.35)` }}
                      onBlurCapture={e => { e.target.style.background = C.surfaceLow; e.target.style.boxShadow = 'none' }}
                    />
                    <p style={{ fontSize: 11, textAlign: 'right', fontFamily: 'Manrope,sans-serif', color: C.outlineVariant, margin: '4px 0 0' }}>{textInput.length}/50000 {textInput.length < 50 && textInput.length > 0 && '— need 50+ chars'}</p>
                  </div>
                )}

                {/* AI info chip */}
                <div style={{ background: 'rgba(88,76,181,0.06)', borderRadius: 14, padding: '12px 16px', display: 'flex', gap: 12, alignItems: 'flex-start' }}>
                  <Sparkles size={18} color={C.tertiary} style={{ flexShrink: 0, marginTop: 1 }} />
                  <p style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.onSurface, lineHeight: 1.6, margin: 0 }}>
                    Our AI will automatically generate <strong>summaries</strong>, <strong>flashcards</strong>, and <strong>Q&A</strong> from this source.
                  </p>
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 4 }}>
                  <button onClick={() => setUploadOpen(false)} style={{ padding: '11px 20px', borderRadius: 100, border: '1.5px solid rgba(171,173,174,0.3)', background: 'none', cursor: 'pointer', fontFamily: 'Manrope,sans-serif', fontSize: 14, fontWeight: 600, color: C.onSurfaceVariant }}>Cancel</button>
                  <button
                    onClick={() => uploadMutation.mutate()}
                    disabled={isSubmitDisabled}
                    style={{
                      display: 'flex', alignItems: 'center', gap: 8,
                      padding: '11px 22px', borderRadius: 100, border: 'none', cursor: isSubmitDisabled ? 'not-allowed' : 'pointer',
                      background: GRADIENT, color: '#fff',
                      fontFamily: 'Manrope,sans-serif', fontSize: 14, fontWeight: 700,
                      boxShadow: '0 4px 14px rgba(0,98,140,0.25)', opacity: isSubmitDisabled ? 0.55 : 1,
                    }}
                  >
                    {uploadMutation.isPending && <Loader2 size={15} style={{ animation: 'spin 1s linear infinite' }} />}
                    {uploadType === 'url' ? 'Import URL' : 'Upload & Process'}
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
