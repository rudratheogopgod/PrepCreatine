import { useEffect } from 'react'
import { createPortal } from 'react-dom'
import { AnimatePresence, motion } from 'framer-motion'
import { X } from 'lucide-react'
import Button from './Button'

interface ModalProps {
  open: boolean
  onClose: () => void
  title?: string
  maxWidth?: string
  children: React.ReactNode
  footer?: React.ReactNode
}

export default function Modal({ open, onClose, title, maxWidth = 'max-w-md', children, footer }: ModalProps) {
  // Trap focus and handle Escape key
  useEffect(() => {
    if (!open) return
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', handleKeyDown)
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = ''
    }
  }, [open, onClose])

  return createPortal(
    <AnimatePresence>
      {open && (
        <>
          {/* Overlay */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.15 }}
            className="fixed inset-0 z-50 bg-black/50 dark:bg-black/70 backdrop-blur-sm"
            onClick={onClose}
          />
          {/* Panel */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 20 }}
            transition={{ type: 'spring', stiffness: 300, damping: 25 }}
            className={`fixed left-1/2 top-1/2 z-50 w-full mx-4 -translate-x-1/2 -translate-y-1/2 ${maxWidth}`}
            role="dialog"
            aria-modal="true"
          >
            <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-2xl p-6">
              {/* Header */}
              {title && (
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-xl font-heading font-semibold text-gray-800 dark:text-gray-100">{title}</h3>
                  <Button variant="icon" onClick={onClose} aria-label="Close">
                    <X className="h-4 w-4" />
                  </Button>
                </div>
              )}
              {/* Content */}
              <div>{children}</div>
              {/* Footer */}
              {footer && (
                <div className="flex justify-end gap-3 mt-6">{footer}</div>
              )}
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>,
    document.body
  )
}
