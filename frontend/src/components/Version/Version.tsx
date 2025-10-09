import React from 'react';
import WarningIcon from 'components/common/Icons/WarningIcon';
import { gitCommitPath } from 'lib/paths';
import { useLatestVersion } from 'lib/hooks/api/latestVersion';
import { formatTimestamp } from 'lib/dateTimeHelpers';
import { useTimezone } from 'lib/hooks/useTimezones';

import * as S from './Version.styled';

const Version: React.FC = () => {
  const { currentTimezone } = useTimezone();
  const { data: latestVersionInfo = {} } = useLatestVersion();
  const { buildTime, commitId, isLatestRelease, version } =
    latestVersionInfo.build;
  const { versionTag } = latestVersionInfo?.latestRelease || '';

  const currentVersion =
    isLatestRelease && version?.match(versionTag)
      ? versionTag
      : formatTimestamp({
          timestamp: buildTime,
          timezone: currentTimezone.value,
        });

  return (
    <S.Wrapper>
      {isLatestRelease === false && (
        <S.OutdatedWarning
          title={`Your app version is outdated. Latest version is ${
            versionTag || 'UNKNOWN'
          }`}
        >
          <WarningIcon />
        </S.OutdatedWarning>
      )}

      {commitId && (
        <div>
          <S.CurrentCommitLink
            title="Current commit"
            target="__blank"
            href={gitCommitPath(commitId)}
          >
            {commitId}
          </S.CurrentCommitLink>
        </div>
      )}
      <S.CurrentVersion>{currentVersion}</S.CurrentVersion>
    </S.Wrapper>
  );
};

export default Version;
