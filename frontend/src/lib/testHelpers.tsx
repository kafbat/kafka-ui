import React, {
  FC,
  ReactNode,
  PropsWithChildren,
  ReactElement,
  useMemo,
  Suspense,
} from 'react';
import {
  MemoryRouter,
  MemoryRouterProps,
  Route,
  Routes,
} from 'react-router-dom';
import fetchMock from 'fetch-mock';
import { ThemeProvider } from 'styled-components';
import { theme } from 'theme/theme';
import {
  render,
  renderHook,
  RenderOptions,
  waitFor,
} from '@testing-library/react';
import {
  QueryClient,
  QueryClientProvider,
  UseQueryResult,
  UseSuspenseQueryResult,
} from '@tanstack/react-query';
import { ConfirmContextProvider } from 'components/contexts/ConfirmContext';
import ConfirmationModal from 'components/common/ConfirmationModal/ConfirmationModal';
import { GlobalSettingsContext } from 'components/contexts/GlobalSettingsContext';
import { UserInfoRolesAccessContext } from 'components/contexts/UserInfoRolesAccessContext';

import { RolesType, modifyRolesData } from './permissions';

interface CustomRenderOptions extends Omit<RenderOptions, 'wrapper'> {
  initialEntries?: MemoryRouterProps['initialEntries'];
  userInfo?: {
    roles?: RolesType;
    rbacFlag: boolean;
  };
  globalSettings?: {
    hasDynamicConfig: boolean;
  };
}

interface WithRouteProps {
  children: ReactNode;
  path: string;
}

export const expectQueryWorks = async (
  mock: fetchMock.FetchMockStatic,
  result: {
    current:
      | UseQueryResult<unknown, unknown>
      | UseSuspenseQueryResult<unknown, unknown>;
  }
) => {
  await waitFor(() => expect(result.current.isFetched).toBeTruthy());
  expect(mock.calls()).toHaveLength(1);
  expect(result.current.data).toBeDefined();
};

export const WithRoute: FC<WithRouteProps> = ({ children, path }) => {
  return (
    <Routes>
      <Route path={path} element={children} />
    </Routes>
  );
};

export const TestQueryClientProvider: FC<PropsWithChildren<unknown>> = ({
  children,
}) => {
  // use new QueryClient instance for each test run to avoid issues with cache
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

/**
 * @description it will create a UserInfo Provider that will actually
 * disable the rbacFlag , to user if you can pass it as an argument
 * */
const TestUserInfoProvider: FC<
  PropsWithChildren<{ data?: { roles?: RolesType; rbacFlag: boolean } }>
> = ({ children, data }) => {
  const contextValue = useMemo(() => {
    const roles = modifyRolesData(data?.roles);

    return {
      username: 'test',
      rbacFlag: !!(typeof data?.rbacFlag === 'undefined'
        ? false
        : data?.rbacFlag),
      roles,
    };
  }, [data]);

  return (
    <UserInfoRolesAccessContext.Provider value={contextValue}>
      {children}
    </UserInfoRolesAccessContext.Provider>
  );
};

const customRender = (
  ui: ReactElement,
  {
    initialEntries,
    userInfo,
    globalSettings,
    ...renderOptions
  }: CustomRenderOptions = {}
) => {
  // overrides @testing-library/react render.
  const AllTheProviders: FC<PropsWithChildren<unknown>> = ({ children }) => (
    <TestQueryClientProvider>
      <GlobalSettingsContext.Provider
        value={globalSettings || { hasDynamicConfig: false }}
      >
        <ThemeProvider theme={theme}>
          <TestUserInfoProvider data={userInfo}>
            <ConfirmContextProvider>
              <MemoryRouter initialEntries={initialEntries}>
                <Suspense fallback={<div>Loading...</div>}>
                  <div>
                    {children}
                    <ConfirmationModal />
                  </div>
                </Suspense>
              </MemoryRouter>
            </ConfirmContextProvider>
          </TestUserInfoProvider>
        </ThemeProvider>
      </GlobalSettingsContext.Provider>
    </TestQueryClientProvider>
  );
  return render(ui, { wrapper: AllTheProviders, ...renderOptions });
};

const customRenderHook = (
  hook: () =>
    | UseQueryResult<unknown, unknown>
    | UseSuspenseQueryResult<unknown, unknown>
) => {
  const SuspenseWrapper: FC<PropsWithChildren<unknown>> = ({ children }) => {
    return (
      <TestQueryClientProvider>
        <Suspense fallback={<div>Loading...</div>}>{children}</Suspense>
      </TestQueryClientProvider>
    );
  };
  return renderHook(hook, { wrapper: SuspenseWrapper });
};

export { customRender as render, customRenderHook as renderQueryHook };
