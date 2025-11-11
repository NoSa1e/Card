import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
export default defineConfig({
  plugins: [vue()],
  server: { port: 5173, host: true, 
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // 또는 192.168.0.54:8080 같은 백엔드
        changeOrigin: true,
        ws: true,                         // 웹소켓 필요하면
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
})
