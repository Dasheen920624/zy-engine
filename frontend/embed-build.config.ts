import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

/**
 * Embed 子工程独立打包配置
 * 输出: dist-embed/embed.js
 *
 * 使用方式:
 *   HIS 系统通过 iframe 引入 embed.html
 *   或直接 <script src="embed.js"> 引入
 */
export default defineConfig({
  plugins: [react()],
  root: '.',
  build: {
    outDir: 'dist-embed',
    emptyOutDir: true,
    rollupOptions: {
      input: path.resolve(__dirname, 'embed.html'),
      output: {
        entryFileNames: 'embed.js',
        chunkFileNames: 'chunks/[name]-[hash].js',
        assetFileNames: 'assets/[name]-[hash][extname]',
      },
    },
    // 独立打包，不提取公共依赖
    cssCodeSplit: false,
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
});
