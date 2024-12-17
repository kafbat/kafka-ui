import { useAppInfo } from 'lib/hooks/api/appConfig';
import React from 'react';
import { ApplicationInfoEnabledFeaturesEnum } from 'generated-sources';
import { useNavigate } from 'react-router-dom';

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

  React.useEffect(() => {
    if (info.data?.raw.url.includes('auth')) {
      navigate('auth');
    }
  }, []);

  const value = React.useMemo(() => {
    const features = info.data?.enabledFeatures || [];
    return {
      hasDynamicConfig: features.includes(
        ApplicationInfoEnabledFeaturesEnum.DYNAMIC_CONFIG
      ),
    };
  }, [info.data]);

  return (
    <GlobalSettingsContext.Provider value={value}>
      {children}
    </GlobalSettingsContext.Provider>
  );
};
