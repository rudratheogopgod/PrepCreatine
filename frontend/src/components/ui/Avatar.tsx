import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

function cn(...inputs: Parameters<typeof clsx>) {
  return twMerge(clsx(inputs))
}

interface AvatarProps {
  name?: string
  src?: string
  size?: 'xs' | 'sm' | 'md' | 'lg'
  className?: string
}

function getInitials(name?: string) {
  if (!name) return '?'
  return name.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2)
}

const sizeMap = {
  xs: 'h-6 w-6 text-xs',
  sm: 'h-8 w-8 text-xs',
  md: 'h-10 w-10 text-sm',
  lg: 'h-12 w-12 text-base',
}

export default function Avatar({ name, src, size = 'md', className }: AvatarProps) {
  if (src) {
    return (
      <img
        src={src}
        alt={name || 'Avatar'}
        className={cn('rounded-full object-cover bg-gray-200', sizeMap[size], className)}
      />
    )
  }
  return (
    <div
      className={cn(
        'rounded-full bg-sky-500 text-white font-body font-semibold flex items-center justify-center flex-shrink-0',
        sizeMap[size],
        className
      )}
    >
      {getInitials(name)}
    </div>
  )
}
