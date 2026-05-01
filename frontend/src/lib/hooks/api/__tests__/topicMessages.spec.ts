import { act, renderHook, waitFor } from '@testing-library/react';
import { renderQueryHook, TestQueryClientProvider } from 'lib/testHelpers';
import * as hooks from 'lib/hooks/api/topicMessages';
import fetchMock from 'fetch-mock';
import { UseQueryResult, UseSuspenseQueryResult } from '@tanstack/react-query';
import { SerdeUsage } from 'generated-sources';

const clusterName = 'test-cluster';
const topicName = 'test-topic';

const expectQueryWorks = async (
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

jest.mock('lib/errorHandling', () => ({
  ...jest.requireActual('lib/errorHandling'),
  showServerError: jest.fn(),
}));

describe('Topic Messages hooks', () => {
  const createObjectURL = jest.fn(() => 'blob:messages');
  const revokeObjectURL = jest.fn();
  const click = jest
    .spyOn(HTMLAnchorElement.prototype, 'click')
    .mockImplementation();

  beforeEach(() => {
    fetchMock.restore();
    Object.defineProperty(URL, 'createObjectURL', {
      configurable: true,
      value: createObjectURL,
    });
    Object.defineProperty(URL, 'revokeObjectURL', {
      configurable: true,
      value: revokeObjectURL,
    });
    createObjectURL.mockClear();
    createObjectURL.mockReturnValue('blob:messages');
    revokeObjectURL.mockClear();
    click.mockClear();
  });

  it('handles useSerdes', async () => {
    const path = `/api/clusters/${clusterName}/topics/${topicName}/serdes?use=SERIALIZE`;

    const mock = fetchMock.getOnce(path, {});
    const { result } = renderQueryHook(() =>
      hooks.useSerdes({ clusterName, topicName, use: SerdeUsage.SERIALIZE })
    );
    await expectQueryWorks(mock, result);
  });

  it('downloads topic messages zip', async () => {
    const path = `/api/clusters/${clusterName}/topics/${topicName}/messages/download?limit=2&partitions=0%2C1&stringFilter=payload&smartFilterId=abc123&keySerde=String&valueSerde=String`;
    const mock = fetchMock.getOnce(path, {
      body: 'zip-content',
      headers: {
        'content-type': 'application/zip',
        'content-disposition': "attachment; filename*=UTF-8''messages.zip",
      },
    });
    const { result } = renderHook(() => hooks.useDownloadMessagesZip(), {
      wrapper: TestQueryClientProvider,
    });

    await act(() =>
      result.current.mutateAsync({
        clusterName,
        topicName,
        limit: 2,
        partitions: ['0', '1'],
        stringFilter: 'payload',
        smartFilterId: 'abc123',
        keySerde: 'String',
        valueSerde: 'String',
      })
    );

    expect(mock.calls()).toHaveLength(1);
    expect(createObjectURL).toHaveBeenCalledTimes(1);
    expect(click).toHaveBeenCalledTimes(1);
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:messages');
  });

  it('uploads topic message files', async () => {
    const path = `/api/clusters/${clusterName}/topics/${topicName}/messages/upload`;
    const response = {
      dryRun: true,
      filesReceived: 1,
      entriesRead: 1,
      messagesParsed: 1,
      messagesProduced: 0,
      failures: 0,
      files: [],
      previews: [],
      errors: [],
    };
    const mock = fetchMock.postOnce(path, response);
    const file = new File(['hello'], 'message.txt', { type: 'text/plain' });
    const { result } = renderHook(() => hooks.useUploadMessages(), {
      wrapper: TestQueryClientProvider,
    });

    await act(() =>
      result.current.mutateAsync({
        clusterName,
        topicName,
        files: [file],
        parseMode: 'FILE_PER_MESSAGE',
        partitionStrategy: 'ANY',
        keyMode: 'NONE',
        keySerde: 'String',
        valueSerde: 'String',
        includeMetadataHeaders: true,
        dryRun: true,
        messageLimit: '1000',
      })
    );

    const body = mock.lastCall()?.[1]?.body as FormData;

    expect(mock.calls()).toHaveLength(1);
    expect(body.getAll('files')).toHaveLength(1);
    expect(body.get('parseMode')).toBe('FILE_PER_MESSAGE');
    expect(body.get('dryRun')).toBe('true');
  });
});
