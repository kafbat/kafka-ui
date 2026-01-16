import {
  Action,
  ResourceType,
  TopicMessage,
  TopicMessageTimestampTypeEnum,
} from 'generated-sources';
import { PreviewFilter } from 'components/Topics/Topic/Messages/Message';
import { RolesType } from 'lib/permissions';

export const mockMessageContentText = 'messageContentText';

export const mockRoles: Map<string, Map<ResourceType, RolesType>> = new Map([
  [
    'test-cluster',
    new Map([
      [
        ResourceType.TOPIC,
        [
          {
            resource: ResourceType.TOPIC,
            actions: [Action.MESSAGES_PRODUCE, Action.VIEW],
            value: '.*',
            clusters: ['test-cluster'],
          },
        ],
      ],
    ]),
  ],
]);

export const mockNoRoles: Map<string, Map<ResourceType, RolesType>> = new Map([
  ['test-cluster', new Map([])],
]);

export const mockMessageKey = '{"payload":{"subreddit":"learnprogramming"}}';

export const mockMessageValue =
  '{"payload":{"author":"DwaywelayTOP","archived":false,"name":"t3_11jshwd","id":"11jshwd"}}';
export const mockMessage: TopicMessage = {
  timestamp: new Date(),
  timestampType: TopicMessageTimestampTypeEnum.CREATE_TIME,
  offset: 0,
  key: 'test-key',
  partition: 6,
  value: '{"data": "test"}',
  headers: { header: 'test' },
  keyDeserializeProperties: undefined,
  valueDeserializeProperties: undefined,
};

export const mockMessageWithSchema: TopicMessage = {
  ...mockMessage,
  valueDeserializeProperties: { schemaId: 1, type: 'AVRO' },
  keyDeserializeProperties: { schemaId: 2, type: 'AVRO' },
};

export const mockKeyFilters: PreviewFilter = {
  field: 'sub',
  path: '$.payload.subreddit',
};

export const mockContentFilters: PreviewFilter = {
  field: 'author',
  path: '$.payload.author',
};
