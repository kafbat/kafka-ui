import React from 'react';

import * as S from './MessageContent.styled';

export interface AvroMetadataProps {
  deserializeProperties?: { [key: string]: unknown | undefined };
}

const AvroMetadata: React.FC<AvroMetadataProps> = ({
  deserializeProperties,
}) => {
  if (
    !deserializeProperties ||
    deserializeProperties.type !== 'AVRO' ||
    !deserializeProperties.name ||
    !deserializeProperties.schemaId
  ) {
    return null;
  }

  if (
    typeof deserializeProperties.name !== 'string' ||
    typeof deserializeProperties.schemaId !== 'number'
  ) {
    return null;
  }

  return (
    <S.Metadata>
      <S.MetadataLabel>Value Type</S.MetadataLabel>
      <span>
        <S.MetadataValue>
          {deserializeProperties.name.split('.').pop()}
        </S.MetadataValue>
        <S.MetadataMeta>
          Schema Id: {deserializeProperties.schemaId}
        </S.MetadataMeta>
      </span>
    </S.Metadata>
  );
};

export default AvroMetadata;
