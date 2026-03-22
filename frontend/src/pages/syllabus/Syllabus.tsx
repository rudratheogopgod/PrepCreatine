import { useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { ChevronDown, ChevronRight, CheckCircle2, Circle, Search } from 'lucide-react'
import PageWrapper from '../../components/layout/PageWrapper'
import { SkeletonCard } from '../../components/ui/Skeleton'
import ProgressBar from '../../components/ui/ProgressBar'
import client from '../../api/client'
import { ENDPOINTS } from '../../api/endpoints'
import { logger } from '../../utils/logger'

interface Topic {
  id: string
  name: string
  status: 'not_started' | 'in_progress' | 'completed'
  estimatedHours: number
  difficulty: 'low' | 'medium' | 'high'
}

interface Chapter {
  id: string
  name: string
  topics: Topic[]
}

interface Subject {
  id: string
  name: string
  shortName: string
  color: string
  chapters: Chapter[]
}

export default function Syllabus() {
  const [search, setSearch] = useState('')
  const [expandedChapters, setExpandedChapters] = useState<Record<string, boolean>>({})

  const { data: subjects, isLoading } = useQuery<Subject[]>({
    queryKey: ['syllabus'],
    queryFn: async () => {
      const res = await client.get(ENDPOINTS.SYLLABUS)
      return res.data
    },
    staleTime: 5 * 60 * 1000,
    placeholderData: [],
  })

  const updateMutation = useMutation({
    mutationFn: async ({ topicId, status }: { topicId: string; status: string }) => {
      logger.info('[Syllabus] Topic status changed', topicId, status)
      await client.patch(ENDPOINTS.SYLLABUS_TOPIC_STATUS(topicId), { status })
    },
  })

  const toggleChapter = (chapterId: string) => setExpandedChapters((prev) => ({ ...prev, [chapterId]: !prev[chapterId] }))

  const allTopics = subjects?.flatMap(s => s.chapters.flatMap(c => c.topics)) || []
  const completedCount = allTopics.filter(t => t.status === 'completed').length
  const completionPct = allTopics.length ? Math.round((completedCount / allTopics.length) * 100) : 0

  if (isLoading) return (
    <PageWrapper>
      <div className="grid grid-cols-1 gap-4">
        {[1,2,3].map(i => <SkeletonCard key={i} />)}
      </div>
    </PageWrapper>
  )

  return (
    <PageWrapper>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-6 gap-4">
        <div>
          <h1 className="text-2xl font-heading font-bold text-gray-900 dark:text-white">Syllabus Tracker</h1>
          <p className="text-sm text-gray-500 dark:text-slate-400 font-body mt-1">{completedCount} of {allTopics.length} topics completed · {completionPct}%</p>
        </div>
        <ProgressBar value={completionPct} className="w-full sm:w-48" />
      </div>

      {/* Search */}
      <div className="relative mb-6">
        <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
        <input
          type="text"
          placeholder="Search topics…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-full pl-9 pr-4 py-2.5 bg-white dark:bg-slate-800 border border-gray-200 dark:border-slate-600 rounded-xl text-sm font-body text-gray-700 dark:text-slate-200 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-sky-400 transition-all"
        />
      </div>

      {/* Subject sections */}
      {subjects?.map((subject) => (
        <div key={subject.id} className="mb-8">
          <h2 className="text-base font-heading font-semibold text-gray-800 dark:text-white mb-3">{subject.name}</h2>
          <div className="space-y-2">
            {subject.chapters.map((chapter) => {
              const chapterTopics = chapter.topics.filter(t =>
                !search || t.name.toLowerCase().includes(search.toLowerCase())
              )
              if (chapterTopics.length === 0) return null
              const chapterCompleted = chapterTopics.filter(t => t.status === 'completed').length
              const isOpen = expandedChapters[chapter.id]

              return (
                <div key={chapter.id} className="bg-white dark:bg-slate-800 rounded-2xl border border-gray-100 dark:border-slate-700 overflow-hidden">
                  <button
                    onClick={() => toggleChapter(chapter.id)}
                    className="w-full flex items-center justify-between px-5 py-4 hover:bg-gray-50 dark:hover:bg-slate-700/50 transition-colors"
                  >
                    <div className="flex items-center gap-3">
                      {isOpen ? <ChevronDown size={16} className="text-gray-400" /> : <ChevronRight size={16} className="text-gray-400" />}
                      <span className="text-sm font-body font-medium text-gray-700 dark:text-slate-200">{chapter.name}</span>
                    </div>
                    <span className="text-xs font-body text-gray-400 dark:text-slate-500">{chapterCompleted}/{chapterTopics.length}</span>
                  </button>

                  {isOpen && (
                    <div className="border-t border-gray-100 dark:border-slate-700">
                      {chapterTopics.map((topic) => (
                        <div key={topic.id} className="flex items-center gap-3 px-5 py-3 hover:bg-gray-50 dark:hover:bg-slate-700/30 transition-colors">
                          <button
                            onClick={() => updateMutation.mutate({
                              topicId: topic.id,
                              status: topic.status === 'completed' ? 'not_started' : 'completed'
                            })}
                            className="text-gray-300 dark:text-slate-600 hover:text-sky-500 transition-colors"
                            aria-label={topic.status === 'completed' ? 'Mark incomplete' : 'Mark complete'}
                          >
                            {topic.status === 'completed'
                              ? <CheckCircle2 size={18} className="text-green-500" />
                              : <Circle size={18} />}
                          </button>
                          <span className={`text-sm font-body flex-1 ${
                            topic.status === 'completed'
                              ? 'line-through text-gray-400 dark:text-slate-600'
                              : 'text-gray-700 dark:text-slate-200'
                          }`}>
                            {topic.name}
                          </span>
                          <span className={`text-xs font-body px-2 py-0.5 rounded-full ${
                            topic.difficulty === 'high' ? 'bg-red-50 text-red-500' :
                            topic.difficulty === 'medium' ? 'bg-amber-50 text-amber-500' :
                            'bg-green-50 text-green-500'
                          }`}>
                            {topic.difficulty}
                          </span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        </div>
      ))}
    </PageWrapper>
  )
}
