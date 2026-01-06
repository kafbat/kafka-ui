import React, { Suspense, useContext } from 'react';
import { Routes, Route, Navigate, useMatch } from 'react-router-dom';
import {
  accessErrorPage,
  clusterPath,
  errorPage,
  getNonExactPath,
  clusterNewConfigPath,
} from 'lib/paths';
import PageLoader from 'components/common/PageLoader/PageLoader';
import { ThemeProvider } from 'styled-components';
import { theme, darkTheme } from 'theme/theme';
import {
  MutationCache,
  QueryCache,
  QueryClient,
  QueryClientProvider,
} from '@tanstack/react-query';
import { showServerError } from 'lib/errorHandling';
import { Toaster } from 'react-hot-toast';
import GlobalCSS from 'components/globalCss';
import * as S from 'components/App.styled';
import { ThemeModeContext } from 'components/contexts/ThemeModeContext';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';

import ConfirmationModal from './common/ConfirmationModal/ConfirmationModal';
import { ConfirmContextProvider } from './contexts/ConfirmContext';
import { GlobalSettingsProvider } from './contexts/GlobalSettingsContext';
import { UserInfoRolesAccessProvider } from './contexts/UserInfoRolesAccessContext';
import PageContainer from './PageContainer/PageContainer';

const AuthPage = React.lazy(() => import('components/AuthPage/AuthPage'));
const Dashboard = React.lazy(() => import('components/Dashboard/Dashboard'));
const ClusterPage = React.lazy(
  () => import('components/ClusterPage/ClusterPage')
);
const ClusterConfigForm = React.lazy(() => import('widgets/ClusterConfigForm'));
const ErrorPage = React.lazy(() => import('components/ErrorPage/ErrorPage'));

const queryCache = new QueryCache({
  onError: (error) => {
    showServerError(error as unknown as Response);
  },
});

const mutationCache = new MutationCache({
  onError: (error) => {
    showServerError(error as unknown as Response);
  },
});

const queryClient = new QueryClient({
  queryCache,
  mutationCache,
  defaultOptions: {
    queries: {
      networkMode: 'offlineFirst',
    },
    mutations: {
      onError(error) {
        showServerError(error as unknown as Response);
      },
    },
  },
});
const App: React.FC = () => {
  const { isDarkMode } = useContext(ThemeModeContext);
  const isAuthRoute = useMatch('/login');

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={isDarkMode ? darkTheme : theme}>
        {isAuthRoute ? (
          <AuthPage />
        ) : (
          <GlobalSettingsProvider>
            <Suspense fallback={<PageLoader fullSize />}>
              <UserInfoRolesAccessProvider>
                <ConfirmContextProvider>
                  <GlobalCSS />
                  <S.Layout>
                    <PageContainer>
                      <Routes>
                        {['/', '/ui', '/ui/clusters'].map((path) => (
                          <Route
                            key="Home" // optional: avoid full re-renders on route changes
                            path={path}
                            element={<Dashboard />}
                          />
                        ))}
                        <Route
                          path={getNonExactPath(clusterNewConfigPath)}
                          element={<ClusterConfigForm />}
                        />
                        <Route
                          path={getNonExactPath(clusterPath())}
                          element={<ClusterPage />}
                        />
                        <Route
                          path={accessErrorPage}
                          element={
                            <ErrorPage status={403} text="Access is Denied" />
                          }
                        />
                        <Route path={errorPage} element={<ErrorPage />} />
                        <Route
                          path="*"
                          element={<Navigate to={errorPage} replace />}
                        />
                      </Routes>
                    </PageContainer>
                    <Toaster position="bottom-right" />
                  </S.Layout>
                  <ConfirmationModal />
                </ConfirmContextProvider>
              </UserInfoRolesAccessProvider>
            </Suspense>
          </GlobalSettingsProvider>
        )}
      </ThemeProvider>
      <ReactQueryDevtools initialIsOpen={false} buttonPosition="bottom-right" />
    </QueryClientProvider>
  );
};

export default App;
