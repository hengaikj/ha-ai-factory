import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '')
  return {
    plugins: [vue()],
    server: {
      proxy: {
        '/auth': { target: env.HA_BACKEND_URL || 'http://localhost:8080', changeOrigin: true, secure: true },
        '/oauth2': { target: env.HA_BACKEND_URL || 'http://localhost:8080', changeOrigin: true, secure: true },
        '/login': { target: env.HA_BACKEND_URL || 'http://localhost:8080', changeOrigin: true, secure: true },
        '/projects': { target: env.HA_BACKEND_URL || 'http://localhost:8080', changeOrigin: true, secure: true },
      },
    },
  }
})
