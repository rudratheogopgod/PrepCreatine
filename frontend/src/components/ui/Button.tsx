import { forwardRef } from 'react'
import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'
import { Loader2 } from 'lucide-react'

function cn(...inputs: Parameters<typeof clsx>) {
  return twMerge(clsx(inputs))
}

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'icon' | 'tertiary'

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: 'sm' | 'md' | 'lg'
  loading?: boolean
  children: React.ReactNode
}

const variants: Record<ButtonVariant, string> = {
  // Cognitive Canvas gradient primary
  primary:
    'btn-gradient-primary text-white rounded-full px-6 py-2.5 text-sm font-manrope font-semibold shadow-card',
  // Ghost border secondary
  secondary:
    'bg-pc-surface-lowest dark:bg-pc-surface-container ghost-border text-pc-on-surface dark:text-white hover:bg-pc-surface-low dark:hover:bg-pc-surface-container rounded-full px-6 py-2.5 text-sm font-manrope font-medium transition-all duration-150',
  // Text-only tertiary
  tertiary:
    'bg-transparent text-pc-primary dark:text-pc-primary-container hover:bg-pc-surface-low dark:hover:bg-pc-surface-container rounded-full px-4 py-2 text-sm font-manrope font-medium transition-all duration-150',
  ghost:
    'bg-transparent hover:bg-pc-surface-low dark:hover:bg-pc-surface-container text-pc-on-surface-variant dark:text-pc-on-surface rounded-xl px-4 py-2 text-sm font-manrope transition-colors duration-150',
  danger:
    'bg-gradient-to-r from-red-600 to-red-500 hover:opacity-90 text-white rounded-full px-5 py-2.5 text-sm font-manrope font-medium transition-all duration-150',
  icon:
    'rounded-xl p-2 hover:bg-pc-surface-low dark:hover:bg-pc-surface-container text-pc-on-surface-variant dark:text-pc-on-surface transition-colors duration-150',
}

const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ variant = 'primary', size, loading = false, className, disabled, children, ...props }, ref) => {
    return (
      <button
        ref={ref}
        disabled={disabled || loading}
        className={cn(
          variants[variant],
          'inline-flex items-center justify-center gap-2 select-none',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-pc-primary-container focus-visible:ring-offset-2',
          (disabled || loading) && 'opacity-50 cursor-not-allowed pointer-events-none',
          size === 'sm' && 'px-4 py-1.5 text-xs',
          size === 'lg' && 'px-8 py-3.5 text-base',
          className
        )}
        {...props}
      >
        {loading && <Loader2 className="h-4 w-4 animate-spin" />}
        {children}
      </button>
    )
  }
)

Button.displayName = 'Button'
export default Button
