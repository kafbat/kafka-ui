import React, {
  type FC,
  type PropsWithChildren,
  Suspense,
  useEffect,
  useMemo,
} from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import NavBar from 'components/NavBar/NavBar';
import * as S from 'components/PageContainer/PageContainer.styled';
import Nav from 'components/Nav/Nav';
import useBoolean from 'lib/hooks/useBoolean';
import { clusterNewConfigPath } from 'lib/paths';
import { GlobalSettingsContext } from 'components/contexts/GlobalSettingsContext';
import { useClusters } from 'lib/hooks/api/clusters';
import { ResourceType } from 'generated-sources';
import { useGetUserInfo } from 'lib/hooks/api/roles';
import { useScreenSize } from 'lib/hooks/useScreenSize';
import PageLoader from 'components/common/PageLoader/PageLoader';

const PageContainer: FC<PropsWithChildren> = ({ children }) => {
  const { isLarge } = useScreenSize();
  const {
    value: isSidebarVisible,
    toggle,
    setFalse: closeSidebar,
  } = useBoolean(isLarge);
  const clusters = useClusters();
  const appInfo = React.useContext(GlobalSettingsContext);
  const location = useLocation();
  const navigate = useNavigate();
  const { data: authInfo } = useGetUserInfo();

  useEffect(() => {
    if (!isLarge) closeSidebar();
  }, [location.key, isLarge]);

  const hasApplicationPermissions = useMemo(() => {
    if (!authInfo?.rbacEnabled) return true;
    return !!authInfo?.userInfo?.permissions.some(
      (permission) => permission.resource === ResourceType.APPLICATIONCONFIG
    );
  }, [authInfo]);

  useEffect(() => {
    if (!appInfo.hasDynamicConfig) return;
    if (clusters?.data?.length !== 0) return;
    if (!hasApplicationPermissions) return;
    navigate(clusterNewConfigPath);
  }, [clusters?.data, appInfo.hasDynamicConfig]);

  return (
    <>
      <NavBar onBurgerClick={toggle} />
      <S.Container $isSidebarVisible={isSidebarVisible}>
        <S.Sidebar aria-label="Sidebar" $visible={isSidebarVisible}>
          <Nav />
        </S.Sidebar>
        <S.Overlay
          $visible={isSidebarVisible}
          onClick={closeSidebar}
          onKeyDown={closeSidebar}
          tabIndex={-1}
          aria-hidden="true"
          aria-label="Overlay"
        />
        <Suspense fallback={<PageLoader fullSize />}>{children}</Suspense>
      </S.Container>
    </>
  );
};

export default PageContainer;
