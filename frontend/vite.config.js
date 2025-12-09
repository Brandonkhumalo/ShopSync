import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  base: './',  // relative paths for Netlify
  server: {
    proxy: {
      '/api': {
        target: 'https://shopsync-qx6o.onrender.com',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'build', // so Netlify can use frontend/build
    emptyOutDir: true
  }
})
