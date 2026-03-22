import { useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { AnimatePresence, motion } from 'framer-motion'
import { Sidebar, Topbar } from './SidebarTopbar'

// Simple breadcrumb from path
function getBreadcrumb(pathname: string): string {
  const segments = pathname.replace(/^\//, '').split('/')
  const first = segments[0]
  const map: Record<string, string> = {
    home: 'Home',
    learn: 'Learn / AI Tutor',
    roadmap: 'Roadmap',
    syllabus: 'Syllabus',
    test: 'Mock Tests',
    notes: 'My Notes',
    analytics: 'Analytics',
    community: 'Community',
    settings: 'Settings',
    mentor: 'Mentor Dashboard',
    notifications: 'Notifications',
  }
  return map[first] || 'PrepCreatine'
}

export default function AppShell() {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const location = useLocation()
  const breadcrumb = getBreadcrumb(location.pathname)

  return (
    <div className="flex h-screen overflow-hidden bg-gray-50 dark:bg-slate-950">
      {/* Desktop sidebar */}
      <div className="hidden lg:block flex-shrink-0">
        <div className="h-full w-60">
          <Sidebar />
        </div>
      </div>

      {/* Mobile sidebar drawer */}
      <AnimatePresence>
        {sidebarOpen && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="fixed inset-0 z-40 bg-black/40 backdrop-blur-sm lg:hidden"
              onClick={() => setSidebarOpen(false)}
            />
            <motion.div
              initial={{ x: -240 }}
              animate={{ x: 0 }}
              exit={{ x: -240 }}
              transition={{ type: 'spring', stiffness: 300, damping: 30 }}
              className="fixed left-0 top-0 bottom-0 z-50 w-60 lg:hidden"
            >
              <Sidebar onClose={() => setSidebarOpen(false)} />
            </motion.div>
          </>
        )}
      </AnimatePresence>

      {/* Main area */}
      <div className="flex flex-col flex-1 min-w-0 overflow-hidden">
        <Topbar
          onMenuClick={() => setSidebarOpen(true)}
          breadcrumb={breadcrumb}
        />
        <div className="flex-1 overflow-y-auto">
          <Outlet />
        </div>
      </div>
    </div>
  )
}
