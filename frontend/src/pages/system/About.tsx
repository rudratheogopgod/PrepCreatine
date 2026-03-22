import PageWrapper from '../../components/layout/PageWrapper'
import Logo from '../../components/layout/Logo'

export default function About() {
  return (
    <PageWrapper maxWidth="max-w-xl" className="text-center py-20">
      <div className="flex justify-center mb-6"><Logo /></div>
      <h1 className="text-3xl font-heading font-bold mb-4 text-gray-900 dark:text-white">About PrepCreatine</h1>
      <p className="text-gray-600 dark:text-slate-400 font-body mb-8 leading-relaxed">
        PrepCreatine is your AI-powered cognitive sanctuary for competitive exam preparation. Designed with modern principles to reduce anxiety and optimize learning.
      </p>
      <div className="p-4 bg-sky-50 dark:bg-sky-900/20 text-sky-700 dark:text-sky-300 rounded-2xl border border-sky-100 dark:border-sky-800 text-sm font-medium">
        Brought to you by Rudra Agrawal
      </div>
    </PageWrapper>
  )
}
