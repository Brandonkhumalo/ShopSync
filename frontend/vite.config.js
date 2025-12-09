import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'https://shopsync-qx6o.onrender.com'
    }
  },
  build: {
    outDir: 'dist',
  }
})

//vercel --prod