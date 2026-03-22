import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

function cn(...inputs: Parameters<typeof clsx>) {
  return twMerge(clsx(inputs))
}

type IconCircleSize = 'sm' | 'md' | 'lg'
type IconCircleVariant = 'primary' | 'neutral'

const sizeMap: Record<IconCircleSize, { circle: string; icon: string }> = {
  sm: { circle: 'h-8 w-8', icon: '16' },
  md: { circle: 'h-10 w-10', icon: '20' },
  lg: { circle: 'h-14 w-14', icon: '28' },
}

interface IconCircleProps {
  size?: IconCircleSize
  variant?: IconCircleVariant
  className?: string
  children: React.ReactNode
}

export default function IconCircle({ size = 'md', variant = 'primary', className, children }: IconCircleProps) {
  return (
    <div
      className={cn(
        'rounded-full flex items-center justify-center flex-shrink-0',
        sizeMap[size].circle,
        variant === 'primary'
          ? 'bg-sky-50 dark:bg-sky-900/30'
          : 'bg-gray-100 dark:bg-slate-700',
        className
      )}
    >
      <span className={variant === 'primary' ? 'text-sky-500' : 'text-gray-500 dark:text-slate-400'}>
        {children}
      </span>
    </div>
  )
}
