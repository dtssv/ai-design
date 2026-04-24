import react from '@vitejs/plugin-react'
import path from 'path'
import { defineConfig } from 'vite'

export default defineConfig({
    plugins: [react()],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src'),
        },
    },
    server: {
        port: 3001,
        proxy: {
            '/api/admin': {
                target: 'http://localhost:8081',
                changeOrigin: true,
            },
        },
    },
})