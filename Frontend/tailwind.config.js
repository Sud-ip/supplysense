/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#12181F',
        paper: '#F6F7F9',
        line: '#E2E5E9',
        brand: {
          DEFAULT: '#0F6E5E',
          dark: '#0B5548',
          soft: '#E4F2EE'
        },
        success: {
          DEFAULT: '#1C8A5B',
          soft: '#E7F5EE'
        },
        warning: {
          DEFAULT: '#B7791F',
          soft: '#FBF0DE'
        },
        danger: {
          DEFAULT: '#C4433D',
          soft: '#FBEBEA'
        }
      },
      fontFamily: {
        display: ['"Space Grotesk"', 'sans-serif'],
        sans: ['"IBM Plex Sans"', 'system-ui', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'monospace']
      },
      borderRadius: {
        DEFAULT: '4px'
      }
    }
  },
  plugins: []
}
