import { defineConfig } from 'vite'
import { resolve } from 'path'

export default defineConfig({
  build: {
    rollupOptions: {
      input: {
        literasi: resolve(__dirname, 'kelola_literasi.html'),
        siswa: resolve(__dirname, 'literacy_siswa.html'),
      },
    },
  },
})
