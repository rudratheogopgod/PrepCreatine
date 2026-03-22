import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

function cn(...inputs: Parameters<typeof clsx>) {
  return twMerge(clsx(inputs))
}

interface SkeletonProps {
  className?: string
  width?: string
  height?: string
  style?: React.CSSProperties
}

export default function Skeleton({ className, width, height }: SkeletonProps) {
  return (
    <div
      className={cn(
        'skeleton-shimmer rounded-xl',
        className
      )}
      style={{ width, height }}
    />
  )
}

export function SkeletonCard() {
  return (
    <div className="bg-white dark:bg-slate-800 rounded-2xl p-5 shadow-sm border border-gray-100 dark:border-slate-700 space-y-3">
      <Skeleton className="h-10 w-10 rounded-full" />
      <Skeleton className="h-4 w-3/4" />
      <Skeleton className="h-3 w-full" />
      <Skeleton className="h-3 w-5/6" />
    </div>
  )
}

export function SkeletonText({ lines = 3 }: { lines?: number }) {
  return (
    <div className="space-y-2">
      {Array.from({ length: lines }).map((_, i) => (
        <Skeleton key={i} className="h-3" style={{ width: i === lines - 1 ? '70%' : '100%' } as React.CSSProperties} />
      ))}
    </div>
  )
}
