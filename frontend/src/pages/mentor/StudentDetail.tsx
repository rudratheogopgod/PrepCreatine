import { useParams, Link } from 'react-router-dom'
import { ChevronLeft, MessageSquare } from 'lucide-react'
import PageWrapper from '../../components/layout/PageWrapper'
import Avatar from '../../components/ui/Avatar'
import Button from '../../components/ui/Button'
import Card from '../../components/ui/Card'
import ProgressBar from '../../components/ui/ProgressBar'

export default function StudentDetail() {
  useParams()

  return (
    <PageWrapper maxWidth="max-w-4xl">
      <Link to="/mentor" className="inline-flex items-center gap-2 text-sm text-gray-500 hover:text-sky-500 font-body mb-6 transition-colors">
        <ChevronLeft size={16} /> Back to Dashboard
      </Link>

      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8 bg-white dark:bg-slate-800 p-6 rounded-2xl border border-gray-100 dark:border-slate-700">
        <div className="flex items-center gap-4">
          <Avatar name="Priya Patel" size="lg" />
          <div>
            <h1 className="text-2xl font-heading font-bold text-gray-900 dark:text-white">Priya Patel</h1>
            <p className="text-sm font-body text-gray-500 mt-1">Preparing for JEE Main • Target: 99%ile</p>
          </div>
        </div>
        <Button variant="secondary" className="flex items-center gap-2"><MessageSquare size={16} /> Message Student</Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card padding="lg">
          <h2 className="text-base font-heading font-semibold text-gray-900 dark:text-white mb-4">Current Progress</h2>
          <div className="space-y-4">
            <div>
              <div className="flex justify-between text-sm font-body mb-1">
                <span className="text-gray-600 dark:text-slate-400">Physics</span>
                <span className="font-bold">45%</span>
              </div>
              <ProgressBar value={45} />
            </div>
            <div>
              <div className="flex justify-between text-sm font-body mb-1">
                <span className="text-gray-600 dark:text-slate-400">Chemistry</span>
                <span className="font-bold">60%</span>
              </div>
              <ProgressBar value={60} />
            </div>
            <div>
              <div className="flex justify-between text-sm font-body mb-1">
                <span className="text-gray-600 dark:text-slate-400">Math</span>
                <span className="font-bold pr-1">30%</span>
              </div>
              <ProgressBar value={30} />
            </div>
          </div>
        </Card>

        <Card padding="lg">
          <h2 className="text-base font-heading font-semibold text-gray-900 dark:text-white mb-4">Mentor Interventions</h2>
          <p className="text-sm font-body text-gray-500 mb-4">Add a note or flag a topic for Priya to focus on.</p>
          <textarea className="w-full border border-gray-200 dark:border-slate-600 rounded-xl p-3 text-sm font-body focus:ring-2 focus:ring-sky-500 outline-none bg-transparent" rows={3} placeholder="E.g., You seem to be struggling with Calculus. Let's review it together." />
          <Button className="w-full mt-3">Send to Student</Button>
        </Card>
      </div>
    </PageWrapper>
  )
}
