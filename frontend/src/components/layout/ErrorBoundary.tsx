import { Component } from 'react'
import type { ErrorInfo, ReactNode } from 'react'
import { AlertTriangle } from 'lucide-react'
import Button from '../ui/Button'

interface Props { children: ReactNode }
interface State { hasError: boolean; error?: Error }

export default class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('[ErrorBoundary]', error, info)
  }

  render() {
    if (!this.state.hasError) return this.props.children

    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-slate-950 px-4">
        <div className="text-center max-w-md">
          <div className="w-16 h-16 rounded-full bg-red-50 dark:bg-red-900/30 flex items-center justify-center mx-auto mb-4">
            <AlertTriangle className="h-8 w-8 text-red-500" />
          </div>
          <h2 className="text-xl font-heading font-semibold text-gray-900 dark:text-white mb-2">
            Something went wrong
          </h2>
          <p className="text-sm text-gray-500 dark:text-slate-400 mb-6 font-body">
            We encountered an unexpected error. Please refresh the page and try again.
          </p>
          <Button onClick={() => window.location.reload()}>Refresh page</Button>
        </div>
      </div>
    )
  }
}
