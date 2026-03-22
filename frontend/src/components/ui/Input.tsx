import { forwardRef } from 'react'
import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'
import { AlertCircle } from 'lucide-react'

function cn(...inputs: Parameters<typeof clsx>) {
  return twMerge(clsx(inputs))
}

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  helperText?: string
  leftIcon?: React.ReactNode
}

const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, helperText, leftIcon, className, id, ...props }, ref) => {
    const inputId = id || label?.toLowerCase().replace(/\s+/g, '-')

    return (
      <div className="w-full">
        {label && (
          <label
            htmlFor={inputId}
            className="block text-xs font-manrope font-medium text-pc-on-surface-variant dark:text-pc-outline uppercase tracking-wide mb-2"
          >
            {label}
          </label>
        )}

        <div className="relative">
          {leftIcon && (
            <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-pc-outline dark:text-pc-outline-variant">
              {leftIcon}
            </span>
          )}

          <input
            ref={ref}
            id={inputId}
            className={cn(
              // Clean Slate style — surface-lowest bg, no border
              'w-full bg-pc-surface-lowest dark:bg-pc-surface-container',
              'ghost-border',
              'rounded-xl px-4 py-2.5 text-sm',
              'font-manrope text-pc-on-surface dark:text-white',
              'placeholder:text-pc-outline dark:placeholder:text-pc-outline-variant',
              // Focus — shift bg + glow (no solid border change)
              'focus:outline-none focus:bg-pc-surface-low dark:focus:bg-pc-surface-container-high',
              'focus:ring-2 focus:ring-pc-primary-container/60 transition-all duration-150',
              // Error
              error && 'ring-2 ring-pc-error',
              // Disabled
              props.disabled && 'opacity-50 cursor-not-allowed bg-pc-surface-low dark:bg-pc-surface-container',
              leftIcon && 'pl-10',
              className
            )}
            {...props}
          />
        </div>

        {error && (
          <p className="mt-1.5 flex items-center gap-1 text-xs text-pc-error font-manrope">
            <AlertCircle size={12} />
            {error}
          </p>
        )}

        {helperText && !error && (
          <p className="mt-1.5 text-xs text-pc-outline font-manrope">{helperText}</p>
        )}
      </div>
    )
  }
)

Input.displayName = 'Input'
export default Input
