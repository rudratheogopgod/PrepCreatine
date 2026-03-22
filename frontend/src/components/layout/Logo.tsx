// src/components/layout/Logo.tsx
export default function Logo({ showWordmark = true, size = 'md' }: { showWordmark?: boolean; size?: 'sm' | 'md' }) {
  const dim = size === 'sm' ? 32 : 36
  return (
    <div className="flex items-center gap-2.5">
      {/* Icon mark */}
      <svg width={dim} height={dim} viewBox="0 0 36 36" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
        <rect width="36" height="36" rx="9" fill="#0EA5E9"/>
        <text x="18" y="25" textAnchor="middle" fill="white" fontFamily="Poppins, sans-serif" fontWeight="700" fontSize="16">PC</text>
      </svg>
      {showWordmark && (
        <span className="font-heading font-bold text-lg leading-none">
          <span className="text-gray-900 dark:text-white">Prep</span>
          <span className="text-sky-500">Creatine</span>
        </span>
      )}
    </div>
  )
}
