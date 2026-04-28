/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx,ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // Premium Dark Palette
        background: '#0a0a0b',
        surface: {
          DEFAULT: '#111113',
          hover: '#18181b',
          elevated: '#1c1c1f',
        },
        border: {
          DEFAULT: '#27272a',
          subtle: '#1e1e21',
        },
        // Brand - Refined Cyan/Teal
        brand: {
          50: '#f0fdfa',
          100: '#ccfbf1',
          200: '#99f6e4',
          300: '#5eead4',
          400: '#2dd4bf',
          500: '#14b8a6', // Primary accent
          600: '#0d9488',
          700: '#0f766e',
          800: '#115e59',
          900: '#134e4a',
          950: '#042f2e',
        },
        // Secondary - Subtle Violet
        accent: {
          500: '#8b5cf6',
          600: '#7c3aed',
        },
        // Semantic
        success: '#10b981',
        warning: '#f59e0b',
        danger: '#ef4444',
        
        // Text
        muted: '#a1a1aa',
        secondary: '#71717a',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        premium: '0 8px 32px rgba(0,0,0,0.4)',
        'premium-hover': '0 12px 48px rgba(0,0,0,0.5)',
        glow: '0 0 20px rgba(20,184,166,0.15)',
      },
      animation: {
        'fade-in': 'fadeIn 0.4s cubic-bezier(0.16, 1, 0.3, 1)',
        'slide-up': 'slideUp 0.5s cubic-bezier(0.16, 1, 0.3, 1)',
        'scale-in': 'scaleIn 0.3s cubic-bezier(0.16, 1, 0.3, 1)',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%': { opacity: '0', transform: 'translateY(20px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        scaleIn: {
          '0%': { opacity: '0', transform: 'scale(0.95)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
      },
    },
  },
  plugins: [],
};
