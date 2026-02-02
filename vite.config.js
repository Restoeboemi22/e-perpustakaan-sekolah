import { defineConfig } from 'vite'
import { resolve } from 'path'

// Plugin sederhana untuk rewrite URL saat development
const rewritePlugin = () => {
  return {
    name: 'rewrite-rules',
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        if (req.url === '/admin') {
          req.url = '/dashboard.html';
        } else if (req.url === '/admin/books') {
          req.url = '/kelola_buku.html';
        } else if (req.url === '/admin/literacy') {
          req.url = '/kelola_literasi.html';
        }
        next();
      });
    }
  }
}

export default defineConfig({
  plugins: [rewritePlugin()],
  build: {
    rollupOptions: {
      input: {
        literasi: resolve(__dirname, 'kelola_literasi.html'),
        siswa: resolve(__dirname, 'literacy_siswa.html'),
        buku: resolve(__dirname, 'kelola_buku.html'),
        dashboard: resolve(__dirname, 'dashboard.html'),
      },
    },
  },
})
