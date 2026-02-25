import React, { FC } from 'react';
import BytesFormatted from 'components/common/BytesFormatted/BytesFormatted';
import useAppParams from 'lib/hooks/useAppParams';
import { clusterSchemaPath, RouteParamsClusterTopic } from 'lib/paths';
import {
  SchemaRegistryDeserializeProperties,
  TopicMessage,
} from 'generated-sources';

import * as S from './Serde.styled';

function isSchemaRegistryDeserializeProperties(
  serde?: string,
  properties?: TopicMessage['valueDeserializeProperties']
): properties is SchemaRegistryDeserializeProperties {
  if (properties && serde === 'SchemaRegistry') {
    return true;
  }
  return false;
}

function getSubject(
  topicName: string,
  properties: SchemaRegistryDeserializeProperties
): string | undefined {
  const subjects = properties.subjects ?? [];
  const subjectWithTopicName = subjects.find((subject) =>
    subject.includes(topicName)
  );

  return subjectWithTopicName ?? subjects[0];
}

const useGetLinkToSchema = (
  serde?: string,
  properties?: TopicMessage['valueDeserializeProperties']
) => {
  const { clusterName, topicName } = useAppParams<RouteParamsClusterTopic>();
  let linkToSchema: string | undefined;

  if (isSchemaRegistryDeserializeProperties(serde, properties)) {
    const subject = getSubject(topicName, properties);
    if (subject) {
      linkToSchema = clusterSchemaPath(clusterName, subject);
    }
  }

  return linkToSchema;
};

const Serde: FC<{
  title: string;
  serde?: string;
  size?: number;
  properties: TopicMessage['valueDeserializeProperties'];
}> = ({ title, serde, size, properties }) => {
  const schemaLink = useGetLinkToSchema(serde, properties);

  return (
    <S.Metadata>
      <S.MetadataLabel>{title}</S.MetadataLabel>
      <span>
        {schemaLink ? (
          <S.SchemaLink to={schemaLink}>{serde}</S.SchemaLink>
        ) : (
          <S.MetadataValue>{serde}</S.MetadataValue>
        )}

        <S.MetadataMeta>
          Size: <BytesFormatted value={size} />
        </S.MetadataMeta>
      </span>
    </S.Metadata>
  );
};

export default Serde;
