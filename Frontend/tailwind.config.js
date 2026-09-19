/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#172033',
        paper: '#F5F7FB',
        line: '#E4E9F2',
        brand: {
          DEFAULT: '#536DFE',
          dark: '#3D52D5',
          soft: '#EEF0FF'
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
        DEFAULT: '12px'
      }
    }
  },
  plugins: []
}
