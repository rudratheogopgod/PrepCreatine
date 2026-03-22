import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Users, TrendingUp, AlertCircle } from 'lucide-react'
import PageWrapper from '../../components/layout/PageWrapper'
import Card from '../../components/ui/Card'
import Avatar from '../../components/ui/Avatar'

export default function MentorDashboard() {
  const { data: students, isLoading } = useQuery({
    queryKey: ['mentor', 'students'],
    queryFn: async () => [
      { id: '1', name: 'Rohan Sharma', lastActive: '2 hours ago', status: 'on_track', score: 85 },
      { id: '2', name: 'Priya Patel', lastActive: '1 day ago', status: 'needs_help', score: 62 },
    ]
  })

  return (
    <PageWrapper maxWidth="max-w-5xl">
      <div className="mb-8 border-b border-gray-100 dark:border-slate-800 pb-4">
        <h1 className="text-2xl font-heading font-bold text-gray-900 dark:text-white">Mentor Dashboard</h1>
        <p className="text-sm font-body text-gray-500 mt-1">Monitor your students' progress and intervene when necessary.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
        <Card className="flex items-center gap-4 p-4">
          <div className="p-3 bg-sky-50 dark:bg-sky-900/20 text-sky-500 rounded-xl"><Users size={24} /></div>
          <div>
            <p className="text-2xl font-bold font-heading text-gray-900 dark:text-white">12</p>
            <p className="text-xs text-gray-500 font-body">Total Students</p>
          </div>
        </Card>
        <Card className="flex items-center gap-4 p-4">
          <div className="p-3 bg-green-50 dark:bg-green-900/20 text-green-500 rounded-xl"><TrendingUp size={24} /></div>
          <div>
            <p className="text-2xl font-bold font-heading text-gray-900 dark:text-white">78%</p>
            <p className="text-xs text-gray-500 font-body">Avg Class Score</p>
          </div>
        </Card>
        <Card className="flex items-center gap-4 p-4">
          <div className="p-3 bg-amber-50 dark:bg-amber-900/20 text-amber-500 rounded-xl"><AlertCircle size={24} /></div>
          <div>
            <p className="text-2xl font-bold font-heading text-gray-900 dark:text-white">3</p>
            <p className="text-xs text-gray-500 font-body">Need Attention</p>
          </div>
        </Card>
      </div>

      <h2 className="text-lg font-heading font-semibold text-gray-900 dark:text-white mb-4">My Students</h2>
      
      {isLoading ? (
        <div className="animate-pulse space-y-4">
          {[1,2,3].map(i => <div key={i} className="h-16 bg-gray-100 dark:bg-slate-800 rounded-xl" />)}
        </div>
      ) : (
        <div className="bg-white dark:bg-slate-800 rounded-2xl border border-gray-100 dark:border-slate-700 overflow-hidden">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-gray-50 dark:bg-slate-900/50 border-b border-gray-100 dark:border-slate-700 text-xs font-heading text-gray-500 dark:text-slate-400 uppercase tracking-wider">
                <th className="p-4 font-semibold">Student</th>
                <th className="p-4 font-semibold">Avg Score</th>
                <th className="p-4 font-semibold">Status</th>
                <th className="p-4 font-semibold">Last Active</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 dark:divide-slate-700">
              {students?.map(s => (
                <tr key={s.id} className="hover:bg-gray-50 dark:hover:bg-slate-700/50 transition-colors">
                  <td className="p-4">
                    <Link to={`/mentor/student/${s.id}`} className="flex items-center gap-3 group">
                      <Avatar name={s.name} size="sm" />
                      <span className="font-body font-medium text-gray-900 dark:text-slate-200 group-hover:text-sky-500 transition-colors">{s.name}</span>
                    </Link>
                  </td>
                  <td className="p-4 font-body text-sm font-semibold text-gray-700 dark:text-slate-300">{s.score}%</td>
                  <td className="p-4">
                    <span className={`px-2.5 py-1 rounded-full text-xs font-body font-medium ${
                      s.status === 'on_track' ? 'bg-green-50 text-green-600 dark:bg-green-900/20 dark:text-green-400' : 'bg-amber-50 text-amber-600 dark:bg-amber-900/20 dark:text-amber-400'
                    }`}>
                      {s.status.replace('_', ' ')}
                    </span>
                  </td>
                  <td className="p-4 text-sm text-gray-500 font-body">{s.lastActive}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </PageWrapper>
  )
}
