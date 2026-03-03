import { useAppInfo } from 'lib/hooks/api/appConfig';
import React from 'react';
import { ApplicationInfoEnabledFeaturesEnum } from 'generated-sources';
import { useNavigate, useLocation } from 'react-router-dom';

interface GlobalSettingsContextProps {
  hasDynamicConfig: boolean;
}

export const GlobalSettingsContext =
  React.createContext<GlobalSettingsContextProps>({
    hasDynamicConfig: false,
  });

export const GlobalSettingsProvider: React.FC<
  React.PropsWithChildren<unknown>
> = ({ children }) => {
  const info = useAppInfo();
  const navigate = useNavigate();
  const location = useLocation();
  const [value, setValue] = React.useState<GlobalSettingsContextProps>({
    hasDynamicConfig: false,
  });

  React.useEffect(() => {
    if (info.data?.redirect && !info.isFetching) {
      const currentPath = location.pathname + location.search;
      const returnTo =
        currentPath && currentPath !== '/' && currentPath !== '/login'
          ? `?returnTo=${encodeURIComponent(currentPath)}`
          : '';
      navigate(`/login${returnTo}`);
      return;
    }

    const features = info?.data?.response?.enabledFeatures;

    if (features) {
      setValue({
        hasDynamicConfig: features.includes(
          ApplicationInfoEnabledFeaturesEnum.DYNAMIC_CONFIG
        ),
      });
    }
  }, [info.data]);

  return (
    <GlobalSettingsContext.Provider value={value}>
      {children}
    </GlobalSettingsContext.Provider>
  );
};