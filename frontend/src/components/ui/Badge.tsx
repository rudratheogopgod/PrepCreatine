import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

function cn(...inputs: Parameters<typeof clsx>) {
  return twMerge(clsx(inputs))
}

type BadgeVariant = 'default' | 'success' | 'warning' | 'error' | 'info' | 'ai-insight'

interface BadgeProps {
  children: React.ReactNode
  variant?: BadgeVariant
  className?: string
}

const variants: Record<BadgeVariant, string> = {
  default:
    'bg-pc-surface-container text-pc-on-surface-variant dark:bg-pc-surface-container-high dark:text-pc-on-surface ghost-border',
  info:
    'bg-sky-50 dark:bg-sky-900/20 text-sky-700 dark:text-sky-400 border border-sky-200/40 dark:border-sky-800/40',
  success:
    'bg-emerald-50 dark:bg-emerald-900/20 text-emerald-700 dark:text-emerald-400 border border-emerald-200/40',
  warning:
    'bg-amber-50 dark:bg-amber-900/20 text-amber-700 dark:text-amber-400 border border-amber-200/40',
  error:
    'bg-red-50 dark:bg-red-900/20 text-pc-error dark:text-red-400 border border-red-200/40',
  // Stitch "AI-Insight" Chip — Signature tertiary component
  'ai-insight':
    'chip-ai-insight',
}

export default function Badge({ children, variant = 'default', className }: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-manrope font-semibold',
        variants[variant],
        className
      )}
    >
      {children}
    </span>
  )
}
