import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { CheckCircle2, XCircle, ArrowRight, BarChart3, AlertTriangle } from 'lucide-react'
import PageWrapper from '../../components/layout/PageWrapper'
import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import client from '../../api/client'
import { ENDPOINTS } from '../../api/endpoints'
import { sanitizeHtml } from '../../utils/sanitize'
import { formatScore } from '../../utils/format'

export default function TestResults() {
  const { id } = useParams<{ id: string }>()

  const { data: result, isLoading } = useQuery({
    queryKey: ['test-result', id],
    queryFn: async () => {
      const res = await client.get(ENDPOINTS.TEST_RESULTS(id!))
      return res.data
    },
    placeholderData: {
      id, score: 90, total: 120, timeTakenMins: 45,
      insights: [
        { subject: 'Physics', text: 'You struggled with Thermodynamics (0/4 correct). AI recommends a targeted revision session.' },
        { subject: 'Math', text: 'Excellent speed in Calculus! Maintained 100% accuracy in under 1 min per question.' }
      ],
      questions: [
        { id: 'q1', text: 'Integration of e^x?', userCorrect: true, userAnswer: 'e^x+c', correctAnswer: 'e^x+c', explanation: 'Standard integral formula.' },
        { id: 'q2', text: 'Value of 5!', userCorrect: false, userAnswer: '100', correctAnswer: '120', explanation: '5 * 4 * 3 * 2 * 1 = 120.' }
      ]
    }
  })

  if (isLoading || !result) return <PageWrapper><div className="animate-spin h-8 w-8 border-b-2 border-sky-500 mx-auto" /></PageWrapper>

  const pct = Math.round((result.score / result.total) * 100)

  return (
    <PageWrapper maxWidth="max-w-4xl">
      <div className="bg-gradient-to-br from-sky-500 to-indigo-600 rounded-3xl p-8 sm:p-12 mb-8 text-white text-center shadow-lg">
        <h1 className="text-3xl sm:text-4xl font-heading font-bold mb-2">Test Complete!</h1>
        <p className="font-body text-sky-100 mb-8 opacity-90">Here is how you performed on this mock test.</p>
        
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 max-w-2xl mx-auto">
          <div className="bg-white/10 backdrop-blur-md rounded-2xl p-4 border border-white/20">
            <p className="text-xs uppercase tracking-widest text-sky-200 font-body mb-1">Score</p>
            <p className="text-3xl font-heading font-bold">{formatScore(result.score, result.total)}</p>
          </div>
          <div className="bg-white/10 backdrop-blur-md rounded-2xl p-4 border border-white/20">
            <p className="text-xs uppercase tracking-widest text-sky-200 font-body mb-1">Accuracy</p>
            <p className="text-3xl font-heading font-bold">{pct}%</p>
          </div>
          <div className="bg-white/10 backdrop-blur-md rounded-2xl p-4 border border-white/20">
            <p className="text-xs uppercase tracking-widest text-sky-200 font-body mb-1">Time Taken</p>
            <p className="text-3xl font-heading font-bold">{result.timeTakenMins} <span className="text-base font-normal">m</span></p>
          </div>
        </div>
      </div>

      <div className="mb-8">
        <h2 className="text-xl font-heading font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
          <BarChart3 className="text-sky-500" /> AI Insights
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {result.insights.map((ins: any, i: number) => (
            <Card key={i} className="flex gap-4 items-start">
              <AlertTriangle className="text-amber-500 flex-shrink-0 mt-1" size={20} />
              <div>
                <p className="text-sm font-heading font-semibold text-gray-800 dark:text-slate-200">{ins.subject}</p>
                <p className="text-sm font-body text-gray-600 dark:text-slate-400 mt-1 leading-relaxed">{ins.text}</p>
              </div>
            </Card>
          ))}
        </div>
        <div className="mt-4 flex justify-end">
          <Link to="/roadmap"><Button>Update my Roadmap <ArrowRight size={16} /></Button></Link>
        </div>
      </div>

      <div>
        <h2 className="text-xl font-heading font-semibold text-gray-900 dark:text-white mb-4">Detailed Solutions</h2>
        <div className="space-y-4">
          {result.questions.map((q: any, i: number) => (
            <div key={q.id} className="bg-white dark:bg-slate-800 rounded-2xl border border-gray-100 dark:border-slate-700 overflow-hidden">
              <div className="p-4 sm:p-6 border-b border-gray-100 dark:border-slate-700 flex gap-4">
                <div className="mt-1 flex-shrink-0">
                  {q.userCorrect ? <CheckCircle2 className="text-green-500" size={24} /> : <XCircle className="text-red-500" size={24} />}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-heading font-semibold text-gray-500 mb-1">Q{i + 1}</p>
                  <div className="text-base font-body text-gray-800 dark:text-slate-200 prose dark:prose-invert max-w-none mb-4" dangerouslySetInnerHTML={{ __html: sanitizeHtml(q.text) }} />
                  
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm font-body bg-gray-50 dark:bg-slate-900/50 p-4 rounded-xl">
                    <div>
                      <span className="text-gray-500 block mb-1">Your Answer:</span>
                      <span className={q.userCorrect ? "text-green-600 dark:text-green-400 font-semibold" : "text-red-600 dark:text-red-400 font-semibold line-through"}>{q.userAnswer || 'Not Answered'}</span>
                    </div>
                    <div>
                      <span className="text-gray-500 block mb-1">Correct Answer:</span>
                      <span className="text-green-600 dark:text-green-400 font-semibold">{q.correctAnswer}</span>
                    </div>
                  </div>
                </div>
              </div>
              <div className="p-4 sm:p-6 bg-sky-50 dark:bg-sky-900/10">
                <p className="text-xs font-heading font-bold text-sky-600 dark:text-sky-400 uppercase tracking-widest mb-2">Explanation</p>
                <div className="text-sm font-body text-gray-700 dark:text-slate-300 prose dark:prose-invert max-w-none" dangerouslySetInnerHTML={{ __html: sanitizeHtml(q.explanation) }} />
              </div>
            </div>
          ))}
        </div>
      </div>
    </PageWrapper>
  )
}
