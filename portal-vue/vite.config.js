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
      },
      '/mes': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        // 关键修复: /mes 也是前端页面路由的前缀(/mes/area、/mes/org、/mes/device),
        // 如果不 bypass, 浏览器地址栏直接打开 localhost:3000/mes/area 会被代理到网关,
        // 网关因为没带 Authorization 而返回 401 "缺失令牌", 表现就像 token 失效。
        // 浏览器请求 HTML 时(Accept: text/html)应当交给 Vite 提供 index.html(SPA 入口),
        // axios 发出的 XHR/fetch 请求不带 text/html, 仍正常走网关。
        bypass: (req) => {
          if (req.headers.accept && req.headers.accept.includes('text/html')) {
            return '/index.html'
          }
          return undefined
        }
      }
    }
  }
})
