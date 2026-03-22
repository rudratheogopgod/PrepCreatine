import { motion } from 'framer-motion'

interface PageWrapperProps {
  children: React.ReactNode
  className?: string
  maxWidth?: string
}

export default function PageWrapper({ children, className, maxWidth = 'max-w-7xl' }: PageWrapperProps) {
  return (
    <motion.main
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: 8 }}
      transition={{ duration: 0.25, ease: 'easeOut' }}
      className={`${maxWidth} mx-auto px-4 sm:px-6 lg:px-8 py-6 ${className || ''}`}
    >
      {children}
    </motion.main>
  )
}
