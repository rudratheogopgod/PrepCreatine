import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

function cn(...inputs: Parameters<typeof clsx>) {
  return twMerge(clsx(inputs))
}

interface CardProps {
  children: React.ReactNode
  className?: string
  /**
   * hover — adds scale + elevated shadow + border highlight on hover
   * floating — uses ambient glow shadow (for high-priority elements)
   */
  variant?: 'default' | 'floating'
  hover?: boolean
  padding?: 'sm' | 'md' | 'lg'
  onClick?: () => void
}

export default function Card({
  children,
  className,
  variant = 'default',
  hover = false,
  padding = 'md',
  onClick,
}: CardProps) {
  const paddings = { sm: 'p-4', md: 'p-5', lg: 'p-6' }

  return (
    <div
      onClick={onClick}
      className={cn(
        // Base — tonal lift: white on #eff1f2 or surface-low backgrounds
        'bg-pc-surface-lowest dark:bg-pc-surface-container rounded-2xl',
        paddings[padding],
        // No hard borders — ghost border only at ~15% opacity
        'ghost-border',
        // Variant
        variant === 'floating' && 'shadow-ambient',
        variant === 'default' && 'shadow-card',
        // Hover — elevated shadow + subtle left-border highlight
        hover && [
          'cursor-pointer transition-all duration-200',
          'hover:shadow-elevated hover:border-pc-primary-container/30 dark:hover:border-pc-primary-container/20',
          'hover:-translate-y-0.5',
        ],
        onClick && 'cursor-pointer',
        className
      )}
    >
      {children}
    </div>
  )
}
