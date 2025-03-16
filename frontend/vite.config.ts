import { defineConfig, loadEnv, UserConfigExport } from 'vite';
import react from '@vitejs/plugin-react-swc';
import tsconfigPaths from 'vite-tsconfig-paths';
import { ViteEjsPlugin } from 'vite-plugin-ejs';
import checker from 'vite-plugin-checker';
import { IncomingMessage } from 'http';

export default defineConfig(({ mode }) => {
  process.env = { ...process.env, ...loadEnv(mode, process.cwd()) };
  const isDevMode = mode === 'development';
  const isProxy = process.env.VITE_DEV_PROXY;

  const defaultPlugins = [
    react(),
    tsconfigPaths(),
    ViteEjsPlugin({
      PUBLIC_PATH: !isDevMode ? 'PUBLIC-PATH-VARIABLE' : '',
    }),
  ];

  const prodPlugins = [...defaultPlugins];

  const devPlugins = [
    ...defaultPlugins,
    checker({
      overlay: { initialIsOpen: false },
      typescript: true,
      eslint: { lintCommand: 'eslint --ext .tsx,.ts src/' },
    }),
  ];

  const defaultConfig: UserConfigExport = {
    plugins: isDevMode ? devPlugins : prodPlugins,
    server: {
      port: 3000,
    },
    build: {
      outDir: 'build/vite',
      rollupOptions: {
        output: {
          manualChunks(id: string) {
            if (id.includes('ace-builds') || id.includes('react-ace')) {
              return 'ace';
            }

            // creating a chunk to react routes deps. Reducing the vendor chunk size
            if (
              id.includes('react-router-dom') ||
              id.includes('@remix-run') ||
              id.includes('react-router')
            ) {
              return '@react-router';
            }

            return null;
          },
        },
      },
    },
    experimental: {
      renderBuiltUrl(
        filename: string,
        {
          hostType,
        }: {
          hostId: string;
          hostType: 'js' | 'css' | 'html';
          type: 'asset' | 'public';
        }
      ) {
        if (hostType === 'js') {
          return {
            runtime: `window.__assetsPathBuilder(${JSON.stringify(filename)})`,
          };
        }

        return filename;
      },
    },
    define: {
      'process.env.NODE_ENV': `"${mode}"`,
      'process.env.VITE_TAG': `"${process.env.VITE_TAG}"`,
      'process.env.VITE_COMMIT': `"${process.env.VITE_COMMIT}"`,
    },
  };

  const proxyDevServerConfig = {
    ...defaultConfig.server,
    open: true,
    proxy: {
      '/login': {
        target: isProxy,
        changeOrigin: true,
        secure: false,
        bypass: (req: IncomingMessage) => {
          if (req.method === 'GET') {
            return req.url;
          }
        },
      },
      '/logout': {
        target: isProxy,
        changeOrigin: true,
        secure: false,
      },
      '/api': {
        target: isProxy,
        changeOrigin: true,
        secure: false,
      },
      '/actuator/info': {
        target: isProxy,
        changeOrigin: true,
        secure: false,
      },
    },
  };

  if (isDevMode && isProxy) {
    return {
      ...defaultConfig,
      server: {
        ...proxyDevServerConfig,
      },
    };
  }

  return defaultConfig;
});
