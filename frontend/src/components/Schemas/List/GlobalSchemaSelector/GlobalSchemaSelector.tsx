import React from 'react';
import {
  Action,
  CompatibilityLevelCompatibilityEnum,
  ResourceType,
} from 'generated-sources';
import useAppParams from 'lib/hooks/useAppParams';
import { ClusterNameRoute } from 'lib/paths';
import { useConfirm } from 'lib/hooks/useConfirm';
import { ActionSelect } from 'components/common/ActionComponent';
import {
  useGetGlobalCompatibilityLayer,
  useUpdateGlobalSchemaCompatibilityLevel,
} from 'lib/hooks/api/schemas';

import * as S from './GlobalSchemaSelector.styled';

function isCompatibilityLevelCompatibilityEnum(
  value: string | number
): value is CompatibilityLevelCompatibilityEnum {
  return value in CompatibilityLevelCompatibilityEnum;
}

const GlobalSchemaSelector: React.FC = () => {
  const { clusterName } = useAppParams<ClusterNameRoute>();
  const { data: currentCompatibilityLevel, isFetching } =
    useGetGlobalCompatibilityLayer(clusterName);
  const { mutateAsync } = useUpdateGlobalSchemaCompatibilityLevel(clusterName);
  const confirm = useConfirm();

  const handleChangeCompatibilityLevel = (level: string | number) => {
    if (!isCompatibilityLevelCompatibilityEnum(level)) return;

    const nextLevel = level;
    confirm(
      <>
        Are you sure you want to update the global compatibility level and set
        it to <b>{nextLevel}</b>? This may affect the compatibility levels of
        the schemas.
      </>,
      async () => {
        await mutateAsync({
          compatibilityLevel: { compatibility: nextLevel },
        });
      }
    );
  };

  if (!currentCompatibilityLevel) return null;

  return (
    <S.Wrapper>
      <div>Global Compatibility Level: </div>
      <ActionSelect
        selectSize="M"
        defaultValue={currentCompatibilityLevel.compatibility}
        minWidth="200px"
        onChange={handleChangeCompatibilityLevel}
        disabled={isFetching}
        options={Object.keys(CompatibilityLevelCompatibilityEnum).map(
          (level) => ({ value: level, label: level })
        )}
        permission={{
          resource: ResourceType.SCHEMA,
          action: Action.MODIFY_GLOBAL_COMPATIBILITY,
        }}
      />
    </S.Wrapper>
  );
};

export default GlobalSchemaSelector;
