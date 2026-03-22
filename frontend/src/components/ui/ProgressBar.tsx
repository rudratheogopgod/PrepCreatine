import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

function cn(...inputs: Parameters<typeof clsx>) {
  return twMerge(clsx(inputs))
}

interface ProgressBarProps {
  value: number // 0-100
  className?: string
  color?: string
}

export default function ProgressBar({ value, className, color }: ProgressBarProps) {
  return (
    <div className={cn('h-2 bg-gray-100 dark:bg-slate-700 rounded-full overflow-hidden', className)}>
      <div
        className={cn('h-full rounded-full transition-all duration-700 ease-out', color || 'bg-sky-500')}
        style={{ width: `${Math.min(100, Math.max(0, value))}%` }}
      />
    </div>
  )
}
