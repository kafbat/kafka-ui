import React from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { ThemeModeProvider } from 'components/contexts/ThemeModeContext';
import App from 'components/App';
import 'lib/constants';
import 'theme/index.scss';

const container =
  document.getElementById('root') || document.createElement('div');
const root = createRoot(container);

root.render(
  <BrowserRouter basename={window.basePath || '/'}>
    <ThemeModeProvider>
      <App />
    </ThemeModeProvider>
  </BrowserRouter>
);
