import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation } from '@tanstack/react-query'
import { toast } from 'react-hot-toast'
import { Clock, ChevronLeft, ChevronRight, Flag } from 'lucide-react'
import Button from '../../components/ui/Button'
import client from '../../api/client'
import { ENDPOINTS } from '../../api/endpoints'
import { formatTimer } from '../../utils/format'
import { sanitizeHtml } from '../../utils/sanitize'
import { logger } from '../../utils/logger'

interface Question {
  id: string
  text: string
  options: { id: string; text: string }[]
  type: 'mcq' | 'numerical'
}

interface TestSession {
  id: string
  name: string
  durationMins: number
  expiresAt: string
  questions: Question[]
}

export default function ExamSession() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  
  const [currentQ, setCurrentQ] = useState(0)
  const [answers, setAnswers] = useState<Record<string, string>>({})
  const [flags, setFlags] = useState<Record<string, boolean>>({})
  const [timeLeft, setTimeLeft] = useState<number | null>(null)

  const { data: session, isLoading } = useQuery<TestSession>({
    queryKey: ['test', id],
    queryFn: async () => {
      const res = await client.get(ENDPOINTS.TEST_SESSION(id!))
      return res.data
    },
    refetchOnWindowFocus: false,
    placeholderData: {
      id: id || '1',
      name: 'Mock Test #1',
      durationMins: 60,
      expiresAt: new Date(Date.now() + 60 * 60000).toISOString(),
      questions: [
        { id: 'q1', type: 'mcq', text: '<p>What is the integral of e^x?</p>', options: [{id:'a', text:'e^x'}, {id:'b', text:'xe^x'}, {id:'c', text:'e^x + c'}, {id:'d', text:'1'}] },
        { id: 'q2', type: 'numerical', text: '<p>Calculate the value of 5!</p>', options: [] }
      ]
    }
  })

  // Timer logic
  useEffect(() => {
    if (!session?.expiresAt) return
    const expiry = new Date(session.expiresAt).getTime()
    
    const tick = () => {
      const remaining = Math.max(0, Math.floor((expiry - Date.now()) / 1000))
      setTimeLeft(remaining)
      if (remaining <= 0) {
        toast('Time is up! Submitting test automatically.', { icon: '⏰' })
        submitMutation.mutate()
      }
    }
    
    tick()
    const int = setInterval(tick, 1000)
    return () => clearInterval(int)
  }, [session?.expiresAt])

  // Before unload warning
  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      e.preventDefault()
      e.returnValue = ''
    }
    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [])

  const submitMutation = useMutation({
    mutationFn: async () => {
      logger.info('[Exam] Submitting test', { testId: id })
      await client.post(ENDPOINTS.TEST_SUBMIT(id!), { answers })
    },
    onSuccess: () => {
      navigate(`/test/results/${id}`, { replace: true })
    },
    onError: () => toast.error('Failed to submit test. Retrying will happen automatically if network is down.'),
  })

  const handleAnswer = (val: string) => {
    if (!session) return
    const qid = session.questions[currentQ].id
    setAnswers(prev => ({ ...prev, [qid]: val }))
  }

  const toggleFlag = () => {
    if (!session) return
    const qid = session.questions[currentQ].id
    setFlags(prev => ({ ...prev, [qid]: !prev[qid] }))
  }

  if (isLoading || !session) return (
    <div className="h-screen w-screen flex items-center justify-center bg-gray-50 dark:bg-slate-950">
      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-sky-500" />
    </div>
  )

  const q = session.questions[currentQ]

  return (
    <div className="h-screen w-screen flex flex-col bg-gray-50 dark:bg-slate-950 overflow-hidden">
      {/* Topbar */}
      <header className="h-14 bg-white dark:bg-slate-900 border-b border-gray-100 dark:border-slate-800 flex items-center justify-between px-4 sm:px-6 flex-shrink-0">
        <div className="flex items-center gap-3">
          <div className="hidden sm:flex items-center gap-1.5 px-3 py-1.5 bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 rounded-lg text-xs font-body font-semibold">
            <span className="w-2 h-2 rounded-full bg-red-500 animate-pulse" />
            LIVE EXAM
          </div>
          <h1 className="text-sm font-heading font-semibold text-gray-800 dark:text-white truncate max-w-[200px] sm:max-w-xs">{session.name}</h1>
        </div>
        
        <div className="flex items-center gap-4 border border-gray-200 dark:border-slate-700 rounded-xl px-4 py-1.5 bg-gray-50 dark:bg-slate-800">
          <Clock size={16} className={timeLeft && timeLeft < 300 ? 'text-red-500 animate-pulse' : 'text-gray-500 dark:text-slate-400'} />
          <span className={`text-xl font-mono font-bold tracking-tight ${timeLeft && timeLeft < 300 ? 'text-red-500' : 'text-gray-900 dark:text-white'}`}>
            {timeLeft !== null ? formatTimer(timeLeft) : '--:--:--'}
          </span>
        </div>

        <Button variant="danger" size="sm" onClick={() => {
          if (confirm('Are you sure you want to finish the test manually?')) {
            submitMutation.mutate()
          }
        }} loading={submitMutation.isPending}>
          Submit
        </Button>
      </header>

      <div className="flex flex-1 overflow-hidden">
        {/* Main Q Area */}
        <main className="flex-1 overflow-y-auto p-4 sm:p-8 flex flex-col bg-white dark:bg-slate-900 mx-4 my-4 rounded-3xl border border-gray-100 dark:border-slate-800 shadow-sm">
          <div className="flex items-center justify-between mb-6 pb-4 border-b border-gray-100 dark:border-slate-800">
            <h2 className="text-lg font-heading font-semibold text-gray-800 dark:text-slate-200">Question {currentQ + 1} of {session.questions.length}</h2>
            <button onClick={toggleFlag} className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm font-body transition-colors ${flags[q.id] ? 'bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-400' : 'bg-gray-100 text-gray-600 dark:bg-slate-800 dark:text-slate-400 hover:bg-gray-200'}`}>
              <Flag size={14} className={flags[q.id] ? 'fill-amber-500' : ''} /> {flags[q.id] ? 'Flagged' : 'Flag for review'}
            </button>
          </div>
          
          {/* Question Text */}
          <div className="prose dark:prose-invert max-w-none mb-8 font-body text-gray-800 dark:text-slate-200 text-base leading-relaxed" dangerouslySetInnerHTML={{ __html: sanitizeHtml(q.text) }} />
          
          {/* Options */}
          {q.type === 'mcq' ? (
            <div className="space-y-3 mt-auto">
              {q.options.map((opt, i) => {
                const isSelected = answers[q.id] === opt.id
                return (
                  <button
                    key={opt.id}
                    onClick={() => handleAnswer(opt.id)}
                    className={`w-full text-left flex items-center gap-4 p-4 rounded-xl border-2 transition-all ${
                      isSelected 
                        ? 'border-sky-500 bg-sky-50 dark:bg-sky-900/20 text-sky-700 dark:text-sky-300 shadow-sm' 
                        : 'border-gray-200 dark:border-slate-700 hover:border-sky-300 dark:hover:border-sky-700 bg-transparent text-gray-700 dark:text-slate-300'
                    }`}
                  >
                    <div className={`w-6 h-6 rounded-full flex items-center justify-center border text-xs font-bold ${
                      isSelected ? 'bg-sky-500 border-sky-500 text-white' : 'border-gray-300 dark:border-slate-500 text-gray-500 dark:text-slate-400'
                    }`}>
                      {String.fromCharCode(65 + i)}
                    </div>
                    <span className="font-body text-sm" dangerouslySetInnerHTML={{ __html: sanitizeHtml(opt.text) }} />
                  </button>
                )
              })}
            </div>
          ) : (
            <div className="mt-auto">
              <label className="block text-sm font-medium text-gray-700 dark:text-slate-300 font-body mb-2">Your Answer:</label>
              <input 
                type="number" 
                step="any"
                className="w-full max-w-sm px-4 py-3 border-2 border-gray-200 dark:border-slate-700 rounded-xl focus:border-sky-500 focus:ring-1 focus:ring-sky-500 bg-transparent text-gray-900 dark:text-white font-body"
                value={answers[q.id] || ''}
                onChange={(e) => handleAnswer(e.target.value)}
                placeholder="Enter numerical value"
              />
            </div>
          )}

          {/* Navigation */}
          <div className="flex justify-between mt-8 pt-6 border-t border-gray-100 dark:border-slate-800">
            <Button variant="secondary" onClick={() => setCurrentQ(q => Math.max(0, q - 1))} disabled={currentQ === 0}>
              <ChevronLeft size={16} /> Previous
            </Button>
            {currentQ < session.questions.length - 1 ? (
              <Button onClick={() => setCurrentQ(q => q + 1)}>
                Save & Next <ChevronRight size={16} />
              </Button>
            ) : (
              <Button variant="danger" onClick={() => {
                if (confirm('You have reached the end. Submit test?')) submitMutation.mutate()
              }} loading={submitMutation.isPending}>
                Complete Test
              </Button>
            )}
          </div>
        </main>

        {/* Right Sidebar - Progress grid */}
        <aside className="w-64 bg-white dark:bg-slate-900 border-l border-gray-100 dark:border-slate-800 p-4 overflow-y-auto hidden md:block">
          <h3 className="text-xs font-heading font-semibold text-gray-500 dark:text-slate-400 uppercase tracking-wider mb-4">Question Palette</h3>
          <div className="grid grid-cols-4 gap-2">
            {session.questions.map((sq, i) => {
              const isAns = !!answers[sq.id]
              const isFlag = flags[sq.id]
              const isCurr = currentQ === i
              
              let bg = 'bg-gray-100 dark:bg-slate-800 text-gray-600 dark:text-slate-400 border-transparent border-2'
              if (isCurr) bg = 'bg-white dark:bg-slate-900 border-sky-500 text-sky-600 dark:text-sky-400 border-2'
              else if (isFlag && isAns) bg = 'bg-amber-500 text-white shadow-sm border-transparent border-2'
              else if (isFlag) bg = 'bg-amber-100 text-amber-700 dark:bg-amber-900/50 dark:text-amber-400 border-transparent border-2'
              else if (isAns) bg = 'bg-green-500 text-white shadow-sm border-transparent border-2'

              return (
                <button
                  key={sq.id}
                  onClick={() => setCurrentQ(i)}
                  className={`w-10 h-10 rounded-xl flex items-center justify-center text-xs font-bold font-heading transition-transform hover:scale-105 ${bg}`}
                >
                  {i + 1}
                </button>
              )
            })}
          </div>

          <div className="mt-8 space-y-3">
            <div className="flex items-center gap-2 text-xs font-body text-gray-600 dark:text-slate-300">
              <div className="w-4 h-4 rounded-md bg-green-500" /> Answered
            </div>
            <div className="flex items-center gap-2 text-xs font-body text-gray-600 dark:text-slate-300">
              <div className="w-4 h-4 rounded-md bg-amber-500" /> Flagged & Answered
            </div>
            <div className="flex items-center gap-2 text-xs font-body text-gray-600 dark:text-slate-300">
              <div className="w-4 h-4 rounded-md bg-amber-100 dark:bg-amber-900" /> Flagged (No answer)
            </div>
            <div className="flex items-center gap-2 text-xs font-body text-gray-600 dark:text-slate-300">
              <div className="w-4 h-4 rounded-md bg-gray-100 dark:bg-slate-800" /> Unanswered
            </div>
          </div>
        </aside>
      </div>
    </div>
  )
}
