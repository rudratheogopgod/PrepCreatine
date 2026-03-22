import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { toast } from 'react-hot-toast'
import { Clock, PlayCircle, Settings2, ClipboardList } from 'lucide-react'
import PageWrapper from '../../components/layout/PageWrapper'
import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import client from '../../api/client'
import { ENDPOINTS } from '../../api/endpoints'
import { logger } from '../../utils/logger'
import { SkeletonCard } from '../../components/ui/Skeleton'

interface TestConfigOption {
  durationMins: number
  totalQuestions: number
  difficulty: 'adaptive' | 'easy' | 'medium' | 'hard'
  mode: 'full_syllabus' | 'custom'
  subjects?: string[]
}

export default function TestConfig() {
  const navigate = useNavigate()
  const [config, setConfig] = useState<TestConfigOption>({
    durationMins: 60,
    totalQuestions: 30,
    difficulty: 'adaptive',
    mode: 'full_syllabus',
  })

  const { data: pastTests, isLoading } = useQuery({
    queryKey: ['tests', 'past'],
    queryFn: async () => {
      const res = await client.get(ENDPOINTS.TESTS)
      return res.data
    },
    placeholderData: [
      { id: 'test_123', name: 'Full Mock Test 1', score: 85, total: 120, createdAt: new Date().toISOString() }
    ],
  })

  const startMutation = useMutation({
    mutationFn: async (cfg: TestConfigOption) => {
      logger.info('[Test] Starting new test', cfg)
      const res = await client.post(ENDPOINTS.TEST_START, cfg)
      return res.data
    },
    onSuccess: (data) => {
      // Open exam session full screen
      navigate(`/test/session/${data.id}`)
    },
    onError: () => toast.error('Failed to start test. Please try again.'),
  })

  return (
    <PageWrapper maxWidth="max-w-4xl">
      <div className="mb-8">
        <h1 className="text-2xl font-heading font-bold text-gray-900 dark:text-white">Mock Tests</h1>
        <p className="text-sm font-body text-gray-500 dark:text-slate-400 mt-1">Configure a new mock test or review past results.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        <div>
          <h2 className="text-lg font-heading font-semibold text-gray-800 dark:text-white mb-4 flex items-center gap-2">
            <Settings2 size={18} /> Configure New Test
          </h2>
          <Card className="space-y-6">
            <div>
              <p className="text-sm font-body font-medium text-gray-700 dark:text-slate-300 mb-2 border-b border-gray-100 dark:border-slate-700 pb-2">Mode</p>
              <div className="flex gap-2">
                {(['full_syllabus', 'custom'] as const).map(m => (
                  <button key={m}
                    onClick={() => setConfig(c => ({ ...c, mode: m }))}
                    className={`flex-1 py-2 rounded-xl text-sm font-body transition-colors border ${config.mode === m ? 'border-sky-500 bg-sky-50 text-sky-600 dark:bg-sky-900/30 dark:text-sky-400' : 'border-gray-200 text-gray-600 dark:border-slate-700 dark:text-slate-400 hover:bg-gray-50 max-w-[50%]'}`}>
                    {m === 'full_syllabus' ? 'Full Syllabus' : 'Custom Topics'}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <p className="text-sm font-body font-medium text-gray-700 dark:text-slate-300 mb-2 border-b border-gray-100 dark:border-slate-700 pb-2">Difficulty</p>
              <div className="grid grid-cols-4 gap-2">
                {(['adaptive', 'easy', 'medium', 'hard'] as const).map(d => (
                  <button key={d}
                    onClick={() => setConfig(c => ({ ...c, difficulty: d }))}
                    className={`py-2 rounded-xl text-xs font-body transition-colors border capitalize ${config.difficulty === d ? 'border-sky-500 bg-sky-50 text-sky-600 dark:bg-sky-900/30 dark:text-sky-400' : 'border-gray-200 text-gray-600 dark:border-slate-700 dark:text-slate-400 hover:bg-gray-50'}`}>
                    {d}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <div className="flex justify-between items-end mb-2 border-b border-gray-100 dark:border-slate-700 pb-2">
                <p className="text-sm font-body font-medium text-gray-700 dark:text-slate-300">Duration & Questions</p>
                <p className="text-sm font-body text-sky-600 dark:text-sky-400 font-semibold">{config.durationMins} min / {config.totalQuestions} Qs</p>
              </div>
              <input type="range" min={15} max={180} step={15} value={config.durationMins}
                onChange={(e) => {
                  const val = Number(e.target.value)
                  setConfig(c => ({ ...c, durationMins: val, totalQuestions: Math.max(10, Math.floor(val * 0.8)) }))
                }}
                className="w-full accent-sky-500 mb-2" />
            </div>

            <Button className="w-full text-base py-3" onClick={() => startMutation.mutate(config)} loading={startMutation.isPending}>
              <PlayCircle size={20} /> Start Mock Test
            </Button>
            <p className="text-xs text-center text-gray-400 font-body">Ensure you have a stable internet connection.</p>
          </Card>
        </div>

        <div>
           <h2 className="text-lg font-heading font-semibold text-gray-800 dark:text-white mb-4 flex items-center gap-2">
            <Clock size={18} /> Past Tests
          </h2>
          <div className="space-y-3">
            {isLoading ? (
              [1,2,3].map(i => <SkeletonCard key={i} />)
            ) : pastTests?.length === 0 ? (
              <div className="text-center py-10 bg-white dark:bg-slate-800 border-2 border-dashed border-gray-200 dark:border-slate-700 rounded-2xl">
                <ClipboardList size={32} className="mx-auto text-gray-300 dark:text-slate-600 mb-2" />
                <p className="text-sm font-body text-gray-500 dark:text-slate-400">No past tests found.</p>
              </div>
            ) : (
              pastTests?.map((t: any) => (
                <Card key={t.id} hover className="flex items-center justify-between" onClick={() => navigate(`/test/results/${t.id}`)}>
                  <div>
                    <h3 className="text-sm font-heading font-semibold text-gray-800 dark:text-slate-200">{t.name}</h3>
                    <p className="text-xs font-body text-gray-500 dark:text-slate-400 mt-1">{new Date(t.createdAt).toLocaleDateString()}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-sm font-body font-bold text-sky-600 dark:text-sky-400">{t.score} / {t.total}</p>
                    <p className="text-[10px] font-body text-gray-400 uppercase tracking-widest mt-1">Score</p>
                  </div>
                </Card>
              ))
            )}
          </div>
        </div>
      </div>
    </PageWrapper>
  )
}
