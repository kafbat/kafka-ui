import React from 'react';
import GitHubIcon from 'components/common/Icons/GitHubIcon';
import { useLatestVersion } from 'lib/hooks/api/latestVersion';

import * as S from './Footer.styled';

function Footer() {
  const { data: latestVersionInfo = {} } = useLatestVersion();
  const { versionTag } = latestVersionInfo.latestRelease;
  const { commitId } = latestVersionInfo.build;

  return (
    <S.FooterStyledWrapper>
      <S.AppVersionStyled>
        <GitHubIcon />
        <S.AppVersionTextStyled>
          {versionTag} ({commitId})
        </S.AppVersionTextStyled>
      </S.AppVersionStyled>
      <S.InformationTextStyled>
        Access to the system is provided by your system administrator. If you
        have any questions, please contact your system administrator
      </S.InformationTextStyled>
    </S.FooterStyledWrapper>
  );
}

export default Footer;
