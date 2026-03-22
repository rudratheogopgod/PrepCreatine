import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ArrowUp, ChevronLeft, MessageSquare, CornerDownRight } from 'lucide-react'
import PageWrapper from '../../components/layout/PageWrapper'
import Card from '../../components/ui/Card'
import Badge from '../../components/ui/Badge'
import Button from '../../components/ui/Button'
import Avatar from '../../components/ui/Avatar'
import client from '../../api/client'
import { ENDPOINTS } from '../../api/endpoints'
import { timeAgo } from '../../utils/format'

export default function Thread() {
  const { threadId } = useParams<{ threadId: string }>()

  const { data: thread, isLoading } = useQuery({
    queryKey: ['thread', threadId],
    queryFn: async () => {
      const res = await client.get(ENDPOINTS.THREAD(threadId!))
      return res.data
    },
    placeholderData: {
      id: threadId, title: 'Best resources for Inorganic Chemistry?', body: 'I find it hard to memorize everything. Any tips or mnemonic tricks?', 
      author: 'Aarav M.', upvotes: 124, tags: ['JEE', 'Chemistry'], createdAt: new Date(Date.now()-3600000).toISOString(),
      answers: [
        { id: 'a1', author: 'IIT_Topper', isExpert: true, body: 'Stick to NCERT strictly. Create tables for periodic trends. Don\'t use 10 different reference books.', upvotes: 56, createdAt: new Date(Date.now()-1800000).toISOString() },
        { id: 'a2', author: 'Neha S.', isExpert: false, body: 'I use Anki flashcards. Daily repetition helps a lot.', upvotes: 12, createdAt: new Date(Date.now()-900000).toISOString() }
      ]
    }
  })

  if (isLoading || !thread) return <PageWrapper><div className="animate-spin h-8 w-8 border-b-2 border-sky-500 mx-auto" /></PageWrapper>

  return (
    <PageWrapper maxWidth="max-w-4xl">
      <Link to="/community" className="inline-flex items-center gap-2 text-sm text-gray-500 hover:text-sky-500 font-body mb-6 transition-colors">
        <ChevronLeft size={16} /> Back to Community
      </Link>

      {/* Main Post */}
      <Card padding="lg" className="mb-6">
        <div className="flex gap-4">
          <div className="flex flex-col items-center gap-1 flex-shrink-0 mt-1">
            <button className="p-1 hover:bg-sky-50 hover:text-sky-500 rounded text-gray-400 transition-colors"><ArrowUp size={24} /></button>
            <span className="font-heading font-bold text-gray-700 dark:text-slate-200">{thread.upvotes}</span>
          </div>
          <div className="flex-1">
            <div className="flex flex-wrap gap-2 mb-3">
              {thread.tags.map((t: string) => <Badge key={t} variant="info">{t}</Badge>)}
            </div>
            <h1 className="text-xl font-heading font-bold text-gray-900 dark:text-white mb-3">{thread.title}</h1>
            <p className="text-base font-body text-gray-700 dark:text-slate-300 whitespace-pre-wrap leading-relaxed mb-6">{thread.body}</p>
            
            <div className="flex items-center justify-between border-t border-gray-100 dark:border-slate-800 pt-4">
              <div className="flex items-center gap-2 text-sm text-gray-500">
                <Avatar name={thread.author} size="sm" />
                <span className="font-heading font-medium">{thread.author}</span>
                <span>·</span>
                <span className="font-body text-xs">{timeAgo(thread.createdAt)}</span>
              </div>
              <Button variant="ghost" size="sm" className="flex items-center gap-2"><MessageSquare size={16} /> Reply</Button>
            </div>
          </div>
        </div>
      </Card>

      <h3 className="text-lg font-heading font-semibold text-gray-900 dark:text-white mb-4">
        {thread.answers.length} Answers
      </h3>

      <div className="space-y-4">
        {thread.answers.map((ans: any) => (
          <div key={ans.id} className="flex gap-4 p-5 bg-white dark:bg-slate-800 rounded-2xl border border-gray-100 dark:border-slate-700">
            <div className="flex flex-col items-center gap-1 flex-shrink-0">
              <button className="p-1 hover:bg-sky-50 hover:text-sky-500 rounded text-gray-400"><ArrowUp size={20} /></button>
              <span className="font-heading font-bold text-gray-500 text-sm">{ans.upvotes}</span>
            </div>
            <div className="flex-1">
              <div className="flex items-center gap-2 mb-2">
                <Avatar name={ans.author} size="sm" className="h-6 w-6 text-xs" />
                <span className="font-heading font-semibold text-sm text-gray-800 dark:text-slate-200">{ans.author}</span>
                {ans.isExpert && <Badge variant="sky" className="py-0 border-none text-[10px]">Expert</Badge>}
                <span className="text-xs text-gray-400 ml-auto">{timeAgo(ans.createdAt)}</span>
              </div>
              <p className="text-sm font-body text-gray-700 dark:text-slate-300 leading-relaxed mb-3">{ans.body}</p>
              <button className="text-xs font-medium text-gray-500 hover:text-gray-700 flex items-center gap-1"><CornerDownRight size={14} /> Reply</button>
            </div>
          </div>
        ))}
      </div>
    </PageWrapper>
  )
}
