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
        } else if (req.url === '/admin/loans') {
          req.url = '/kelola_peminjaman.html';
        } else if (req.url === '/admin/students') {
          req.url = '/kelola_siswa.html';
        } else if (req.url === '/admin/stats') {
          req.url = '/statistik_siswa.html';
        } else if (req.url === '/admin/reports') {
          req.url = '/laporan.html';
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
        peminjaman: resolve(__dirname, 'kelola_peminjaman.html'),
        kelola_siswa: resolve(__dirname, 'kelola_siswa.html'),
        statistik: resolve(__dirname, 'statistik_siswa.html'),
        laporan: resolve(__dirname, 'laporan.html'),
      },
    },
  },
})
