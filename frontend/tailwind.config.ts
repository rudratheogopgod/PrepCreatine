import type { Config } from 'tailwindcss'

const config: Config = {
  darkMode: 'class',
  content: [
    './index.html',
    './src/**/*.{js,ts,jsx,tsx}',
  ],
  theme: {
    extend: {
      colors: {
        // === Cognitive Canvas (Stitch) Primary Palette ===
        'pc-primary': '#00628c',
        'pc-primary-container': '#34b5fa',
        'pc-primary-dim': '#00557b',
        'pc-on-primary': '#e9f4ff',
        'pc-secondary': '#006382',
        'pc-secondary-container': '#93dbff',
        'pc-tertiary': '#584cb5',
        'pc-tertiary-container': '#afa6ff',
        'pc-on-tertiary-container': '#2b1988',
        'pc-surface': '#f5f6f7',
        'pc-surface-low': '#eff1f2',
        'pc-surface-container': '#e6e8ea',
        'pc-surface-container-high': '#e0e3e4',
        'pc-surface-lowest': '#ffffff',
        'pc-on-surface': '#2c2f30',
        'pc-on-surface-variant': '#595c5d',
        'pc-outline': '#757778',
        'pc-outline-variant': '#abadae',
        'pc-error': '#b31b25',
        // === FDD / Legacy ===
        sky: {
          50: '#F0F9FF',
          100: '#E0F2FE',
          200: '#BAE6FD',
          400: '#38BDF8',
          500: '#0EA5E9',
          600: '#0284C7',
          700: '#0369A1',
        },
        gray: {
          50: '#F8FAFC',
          100: '#F1F5F9',
          200: '#E2E8F0',
          400: '#94A3B8',
          500: '#64748B',
          700: '#334155',
          900: '#0F172A',
        },
      },
      fontFamily: {
        // Cognitive Canvas (Stitch)
        jakarta: ['"Plus Jakarta Sans"', 'sans-serif'],
        manrope: ['Manrope', 'sans-serif'],
        // FDD originals (kept for compat)
        heading: ['"Plus Jakarta Sans"', 'Poppins', 'sans-serif'],
        body: ['Manrope', 'Inter', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'monospace'],
      },
      borderRadius: {
        '2xl': '1rem',
        '3xl': '1.5rem',
        '4xl': '2rem',
      },
      boxShadow: {
        'ambient': '0 20px 40px rgba(0, 98, 140, 0.06)',
        'card': '0 2px 8px rgba(0, 98, 140, 0.04)',
        'elevated': '0 8px 24px rgba(0, 98, 140, 0.10)',
      },
      backgroundImage: {
        'gradient-primary': 'linear-gradient(135deg, #00628c 0%, #34b5fa 100%)',
        'gradient-hero': 'linear-gradient(135deg, #f5f6f7 0%, #e8f4fe 50%, #f5f6f7 100%)',
        'gradient-cta': 'linear-gradient(135deg, #00628c 0%, #0ea5e9 100%)',
      },
      animation: {
        float: 'float 3.5s ease-in-out infinite',
      },
      keyframes: {
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-10px)' },
        },
      },
    },
  },
  plugins: [],
}

export default config
