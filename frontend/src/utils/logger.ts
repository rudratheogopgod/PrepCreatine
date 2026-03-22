const isDev = import.meta.env.MODE === 'development'

const timestamp = () => new Date().toISOString().split('T')[1].slice(0, 8)

export const logger = {
  debug: (...args: unknown[]) => {
    if (isDev) console.debug(`%c[${timestamp()}] DEBUG`, 'color:#64748b;font-weight:bold', ...args)
  },
  info: (...args: unknown[]) => {
    if (isDev) console.info(`%c[${timestamp()}] INFO`, 'color:#0EA5E9;font-weight:bold', ...args)
  },
  warn: (...args: unknown[]) => {
    console.warn(`%c[${timestamp()}] WARN`, 'color:#f59e0b;font-weight:bold', ...args)
  },
  error: (...args: unknown[]) => {
    console.error(`%c[${timestamp()}] ERROR`, 'color:#ef4444;font-weight:bold', ...args)
  },
}
