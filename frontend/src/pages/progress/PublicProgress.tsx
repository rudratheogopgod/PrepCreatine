import { useParams, Link } from 'react-router-dom'
import { Trophy, TrendingUp, Calendar } from 'lucide-react'
import PageWrapper from '../../components/layout/PageWrapper'
import Logo from '../../components/layout/Logo'
import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import ProgressBar from '../../components/ui/ProgressBar'

export default function PublicProgress() {
  useParams()

  return (
    <PageWrapper maxWidth="max-w-3xl" className="py-12">
      <div className="text-center mb-10">
        <div className="flex justify-center mb-6"><Logo /></div>
        <h1 className="text-3xl font-heading font-bold text-gray-900 dark:text-white mb-2">Rudra's Study Progress</h1>
        <p className="text-gray-500 font-body">Preparing for JEE Main • Target: 99%ile</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 mb-8">
        <Card className="text-center py-6">
          <Trophy size={32} className="mx-auto text-amber-500 mb-3" />
          <p className="text-3xl font-heading font-bold text-gray-900 dark:text-white">65%</p>
          <p className="text-xs text-gray-500 uppercase tracking-widest mt-1">Syllabus Done</p>
        </Card>
        <Card className="text-center py-6">
          <TrendingUp size={32} className="mx-auto text-green-500 mb-3" />
          <p className="text-3xl font-heading font-bold text-gray-900 dark:text-white">78%</p>
          <p className="text-xs text-gray-500 uppercase tracking-widest mt-1">Avg Score</p>
        </Card>
        <Card className="text-center py-6">
          <Calendar size={32} className="mx-auto text-sky-500 mb-3" />
          <p className="text-3xl font-heading font-bold text-gray-900 dark:text-white">12</p>
          <p className="text-xs text-gray-500 uppercase tracking-widest mt-1">Day Streak</p>
        </Card>
      </div>

      <Card padding="lg" className="mb-8">
        <h2 className="text-lg font-heading font-semibold mb-6">Subject Breakdown</h2>
        <div className="space-y-6">
          <div>
            <div className="flex justify-between text-sm font-body mb-2"><span>Physics</span><span className="font-bold">45%</span></div>
            <ProgressBar value={45} />
          </div>
          <div>
            <div className="flex justify-between text-sm font-body mb-2"><span>Chemistry</span><span className="font-bold">80%</span></div>
            <ProgressBar value={80} />
          </div>
          <div>
            <div className="flex justify-between text-sm font-body mb-2"><span>Mathematics</span><span className="font-bold">60%</span></div>
            <ProgressBar value={60} />
          </div>
        </div>
      </Card>

      <div className="text-center">
        <p className="text-sm text-gray-500 font-body mb-4">Want to track your own progress?</p>
        <Link to="/signup"><Button>Join PrepCreatine</Button></Link>
      </div>
    </PageWrapper>
  )
}
