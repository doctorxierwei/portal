import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      // 开发时代理到网关（按服务名前缀路由转发）
      '/portal-auth': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/users': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/blogs': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/files': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/gateway': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
