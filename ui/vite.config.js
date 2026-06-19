import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { imagetools } from 'vite-imagetools'
import { fileURLToPath, URL } from 'node:url'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const theme = mode === 'classic' ? 'classic' : 'liquid'

  return {
    plugins: [vue(), imagetools()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
        '@theme': fileURLToPath(new URL(`./src/themes/${theme}`, import.meta.url)),
      },
    },
    build: {
      outDir: `dist/${theme}`,
      emptyOutDir: true,
    },
    define: {
      __ASKBOX_THEME__: JSON.stringify(theme),
    },
    server: {
      host: '0.0.0.0',
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  }
})
