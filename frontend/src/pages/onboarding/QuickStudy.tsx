import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { toast } from 'react-hot-toast'
import { Loader2, Sparkles, BookOpen, Link as LinkIcon } from 'lucide-react'
import { motion } from 'framer-motion'
import Logo from '../../components/layout/Logo'
import client from '../../api/client'
import { ENDPOINTS } from '../../api/endpoints'
import { useUserContextStore } from '../../store/userContextStore'
import { logger } from '../../utils/logger'
import { z } from 'zod'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'

const C = {
  surface: '#f5f6f7', surfaceLow: '#eff1f2', surfaceLowest: '#ffffff',
  onSurface: '#2c2f30', onSurfaceVariant: '#595c5d', outline: '#757778', outlineVariant: '#abadae',
  primary: '#00628c', primaryContainer: '#34b5fa',
  tertiary: '#584cb5', error: '#b31b25',
}
const GRADIENT = `linear-gradient(135deg, ${C.primary} 0%, ${C.primaryContainer} 100%)`

const schema = z.object({
  topic: z.string().min(2, 'Please describe what you're studying (min 2 chars).').max(200),
  url: z.union([z.string().url('Please include https:// at the start.'), z.literal('')]).optional(),
})
type FormData = z.infer<typeof schema>

export default function QuickStudy() {
  const navigate = useNavigate()
  const setContext = useUserContextStore(s => s.setContext)
  const { register, handleSubmit, watch, formState: { errors } } = useForm<FormData>({ resolver: zodResolver(schema) })
  const topicValue = watch('topic', '')

  // Strip emoji + control chars on blur
  const sanitize = (v: string) => v.replace(/\p{Emoji}/gu, '').replace(/[\x00-\x1F\x7F]/g, '').trim()

  const mutation = useMutation({
    mutationFn: async (data: FormData) => {
      const payload = { topic: sanitize(data.topic ?? ''), url: data.url?.trim() || undefined }
      logger.info('[Onboarding] Quick study submit', payload)
      const res = await client.post(ENDPOINTS.ONBOARDING_QUICK, payload)
      return res.data
    },
    onSuccess: (_, vars) => {
      setContext({ examType: 'other', weakTopics: [vars.topic ?? ''] })
      logger.info('[Onboarding] Complete (Path C)')
      navigate('/learn', { state: { prefilled: vars.topic } })
    },
    onError: () => toast.error('Something went wrong. Please try again.'),
  })

  return (
    <div style={{
      minHeight: '100vh', background: `linear-gradient(160deg, #e8f3ff 0%, #f0eeff 40%, ${C.surface} 70%)`,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      padding: 24, position: 'relative', overflow: 'hidden',
    }}>
      <div style={{ position: 'absolute', top: -120, left: -120, width: 420, height: 420, borderRadius: '50%', background: 'radial-gradient(circle, rgba(52,181,250,0.22) 0%, transparent 70%)', filter: 'blur(60px)' }} />
      <div style={{ position: 'absolute', bottom: -100, right: -100, width: 360, height: 360, borderRadius: '50%', background: 'radial-gradient(circle, rgba(175,166,255,0.22) 0%, transparent 70%)', filter: 'blur(60px)' }} />

      <motion.div
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        style={{ width: '100%', maxWidth: 520, position: 'relative', zIndex: 1 }}
      >
        {/* Logo */}
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 32 }}><Logo /></div>

        {/* Card */}
        <div style={{ background: 'rgba(255,255,255,0.93)', backdropFilter: 'blur(20px)', WebkitBackdropFilter: 'blur(20px)', borderRadius: 28, padding: 36, border: '1px solid rgba(171,173,174,0.18)', boxShadow: '0 20px 48px rgba(0,98,140,0.08)' }}>
          <div style={{ textAlign: 'center', marginBottom: 28 }}>
            <div style={{ width: 52, height: 52, borderRadius: '50%', background: GRADIENT, display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 16px' }}>
              <Sparkles size={24} color="#fff" />
            </div>
            <h1 style={{ fontSize: 24, fontFamily: '"Plus Jakarta Sans",sans-serif', fontWeight: 800, color: C.onSurface, margin: '0 0 8px' }}>Let's get you studying.</h1>
            <p style={{ fontSize: 14, fontFamily: 'Manrope,sans-serif', color: C.onSurfaceVariant, margin: 0 }}>No long setup. Just tell us what you're studying and start.</p>
          </div>

          <form onSubmit={handleSubmit(d => mutation.mutate(d))} style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
            {/* Topic */}
            <div>
              <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: C.outline, marginBottom: 8 }}>
                <BookOpen size={12} /> What are you studying?
              </label>
              <input
                {...register('topic')}
                placeholder="e.g. Chapter 5 Thermodynamics, World War II, Python basics..."
                onBlur={e => { e.target.value = sanitize(e.target.value) }}
                style={{ width: '100%', boxSizing: 'border-box', background: C.surfaceLow, border: `1px solid ${errors.topic ? C.error : 'rgba(171,173,174,0.2)'}`, borderRadius: 14, padding: '12px 16px', fontFamily: 'Manrope,sans-serif', fontSize: 14, color: C.onSurface, outline: 'none' }}
                onFocus={e => { e.target.style.background = C.surfaceLowest; e.target.style.boxShadow = errors.topic ? `0 0 0 2px rgba(179,27,37,0.25)` : `0 0 0 2px rgba(52,181,250,0.35)` }}
                onBlurCapture={e => { e.target.style.background = C.surfaceLow; e.target.style.boxShadow = 'none' }}
                maxLength={200}
              />
              {errors.topic && (
                <p style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.error, margin: '6px 0 0' }}>{errors.topic.message}</p>
              )}
              {topicValue && (
                <p style={{ fontSize: 11, fontFamily: 'Manrope,sans-serif', color: C.outlineVariant, margin: '5px 0 0', textAlign: 'right' }}>{topicValue.length}/200</p>
              )}
            </div>

            {/* Optional URL */}
            <div>
              <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 11, fontFamily: 'Manrope,sans-serif', fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: C.outline, marginBottom: 8 }}>
                <LinkIcon size={12} /> Notes or URL <span style={{ fontWeight: 400, textTransform: 'none', fontSize: 11, color: C.outlineVariant }}>(optional)</span>
              </label>
              <input
                {...register('url')}
                placeholder="Paste a URL or leave blank"
                style={{ width: '100%', boxSizing: 'border-box', background: C.surfaceLow, border: `1px solid ${errors.url ? C.error : 'rgba(171,173,174,0.2)'}`, borderRadius: 14, padding: '12px 16px', fontFamily: 'Manrope,sans-serif', fontSize: 14, color: C.onSurface, outline: 'none' }}
                onFocus={e => { e.target.style.background = C.surfaceLowest; e.target.style.boxShadow = errors.url ? `0 0 0 2px rgba(179,27,37,0.25)` : `0 0 0 2px rgba(52,181,250,0.35)` }}
                onBlurCapture={e => { e.target.style.background = C.surfaceLow; e.target.style.boxShadow = 'none' }}
                maxLength={2048}
              />
              {errors.url ? (
                <p style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.error, margin: '6px 0 0' }}>Please include https:// at the start.</p>
              ) : (
                <p style={{ fontSize: 12, fontFamily: 'Manrope,sans-serif', color: C.outlineVariant, margin: '6px 0 0' }}>We'll read it and answer questions based on your material.</p>
              )}
            </div>

            <button
              type="submit"
              disabled={mutation.isPending}
              style={{
                width: '100%', padding: '13px', borderRadius: 100, border: 'none', cursor: mutation.isPending ? 'not-allowed' : 'pointer',
                background: mutation.isPending ? 'rgba(0,98,140,0.5)' : GRADIENT, color: '#fff',
                fontFamily: 'Manrope,sans-serif', fontSize: 15, fontWeight: 700,
                display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
                boxShadow: '0 4px 16px rgba(0,98,140,0.25)', marginTop: 8,
              }}
            >
              {mutation.isPending ? <Loader2 size={16} style={{ animation: 'spin 1s linear infinite' }} /> : <Sparkles size={16} />}
              Start studying now →
            </button>
          </form>
        </div>
      </motion.div>
    </div>
  )
}
