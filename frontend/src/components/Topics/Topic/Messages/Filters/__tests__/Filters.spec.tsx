import React from 'react';
import Filters, {
  FiltersProps,
} from 'components/Topics/Topic/Messages/Filters/Filters';
import { render, WithRoute } from 'lib/testHelpers';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { clusterTopicPath } from 'lib/paths';
import { useTopicDetails } from 'lib/hooks/api/topics';
import { externalTopicPayload } from 'lib/fixtures/topics';
import { useSerdes } from 'lib/hooks/api/topicMessages';
import { serdesPayload } from 'lib/fixtures/topicMessages';
import {
  MessagesFilterKeys,
  MessagesFilterKeysTypes,
} from 'lib/hooks/useMessagesFilters';
import { PollingMode } from 'generated-sources';
import { ModeOptions } from 'lib/hooks/filterUtils';

const closeIconMock = 'closeIconMock';
const filtersSideBarMock = 'filtersSideBarMock';
const filterMetricsMock = 'filterMetricsMock';

jest.mock('lib/hooks/api/topics', () => ({
  useTopicDetails: jest.fn(),
}));

jest.mock('lib/hooks/api/topicMessages', () => ({
  useSerdes: jest.fn(),
}));

jest.mock('components/common/Icons/CloseIcon', () => () => (
  <div>{closeIconMock}</div>
));

jest.mock(
  'components/Topics/Topic/Messages/Filters/FiltersSideBar',
  () => () => <div>{filtersSideBarMock}</div>
);

jest.mock(
  'components/Topics/Topic/Messages/Filters/FiltersMetrics',
  () => () => <div>{filterMetricsMock}</div>
);

const clusterName = 'cluster-name';
const topicName = 'topic-name';

const renderComponent = (
  props?: Partial<FiltersProps>,
  queryParams?: Partial<Record<MessagesFilterKeysTypes, string>>
) => {
  const urlParams = new URLSearchParams({ ...queryParams });

  return render(
    <WithRoute path={clusterTopicPath()}>
      <Filters isFetching={false} abortFetchData={jest.fn()} {...props} />
    </WithRoute>,
    {
      initialEntries: [
        `${clusterTopicPath(clusterName, topicName)}?${urlParams.toString()}`,
      ],
    }
  );
};

beforeEach(async () => {
  (useTopicDetails as jest.Mock).mockImplementation(() => ({
    data: externalTopicPayload,
  }));
  (useSerdes as jest.Mock).mockImplementation(() => ({
    data: serdesPayload,
  }));
});

describe('Filters component', () => {
  const getModeSelect = () => screen.getAllByRole('listbox')[0];
  const getKeySerdeDropdown = () => screen.getAllByRole('listbox')[1];
  const getValueSerdeDropdown = () => screen.getAllByRole('listbox')[2];

  it('shows refresh button', () => {
    renderComponent();
    expect(screen.getByText('Refresh')).toBeInTheDocument();
  });

  describe('Filter Input default elements', () => {
    const inputValue = 'Hello World!';

    const selectDropdownAndCheckInput = async (
      value: string,
      placeholder: string
    ) => {
      const modeSelect = getModeSelect();
      const option = screen.getAllByRole('option');

      await userEvent.click(modeSelect);

      await userEvent.selectOptions(modeSelect, [value]);

      expect(option[0]).toHaveTextContent(value);
      const timestampInput = screen.getByPlaceholderText(placeholder);
      expect(timestampInput).toHaveValue('');

      await userEvent.type(timestampInput, inputValue);

      expect(timestampInput).toHaveValue(inputValue);
    };

    beforeEach(() => {
      renderComponent();
    });

    it('search input and selectable mode', async () => {
      const searchInput = screen.getByPlaceholderText('Search');
      expect(searchInput).toHaveValue('');
      await userEvent.type(searchInput, inputValue);
      expect(searchInput).toHaveValue(inputValue);
    });

    it('offset input from offset option', async () => {
      await selectDropdownAndCheckInput('From offset', 'Offset');
    });

    it('offset input To offset option', async () => {
      await selectDropdownAndCheckInput('To offset', 'Offset');
    });

    it('timestamp input since time', async () => {
      await selectDropdownAndCheckInput('Since time', 'Select timestamp');
    });

    it('timestamp input since time', async () => {
      await selectDropdownAndCheckInput('To time', 'Select timestamp');
    });
  });

  describe('checks the input values when data comes from the url', () => {
    const renderAndCheckSelectType = (
      mode: PollingMode,
      { timestamp, offset }: { timestamp?: string; offset?: string }
    ) => {
      renderComponent(
        {},
        {
          [MessagesFilterKeys.mode]: mode.toString(),
          [MessagesFilterKeys.timestamp]: timestamp,
          [MessagesFilterKeys.offset]: offset,
        }
      );
      const item = ModeOptions.find((i) => i.value === mode);
      expect(getModeSelect()).toHaveTextContent(item?.label || '');
    };

    describe('modes and the related inputs', () => {
      it('should check the mode input value latest', () => {
        renderAndCheckSelectType(PollingMode.LATEST, {});
      });

      it('should check the mode input value earliest', () => {
        renderAndCheckSelectType(PollingMode.EARLIEST, {});
      });

      it('should check the mode input value tailest', () => {
        renderAndCheckSelectType(PollingMode.TAILING, {});
      });

      it('should check the mode input value from offset', () => {
        const offset = '2';
        renderAndCheckSelectType(PollingMode.FROM_OFFSET, { offset });
        expect(screen.getAllByRole('textbox')[0]).toHaveValue(offset);
      });

      it('should check the mode input value to offset', () => {
        const offset = '2';
        renderAndCheckSelectType(PollingMode.TO_OFFSET, { offset });
        expect(screen.getAllByRole('textbox')[0]).toHaveValue(offset);
      });

      it('should check the mode input value to timestamp', () => {
        const currentDate = new Date(1707940800000);
        renderAndCheckSelectType(PollingMode.TO_TIMESTAMP, {
          timestamp: currentDate.getTime().toString(),
        });
        const formattedDate = currentDate.toLocaleString('en-US', {
          year: 'numeric',
          month: 'short',
          day: 'numeric',
        });

        expect(screen.getByPlaceholderText('Select timestamp')).toHaveValue(
          formattedDate
        );
      });

      it('should check the mode input value from timestamp', () => {
        const currentDate = new Date(1707940800000);
        renderAndCheckSelectType(PollingMode.FROM_TIMESTAMP, {
          timestamp: currentDate.getTime().toString(),
        });
        const formattedDate = currentDate.toLocaleString('en-US', {
          year: 'numeric',
          month: 'short',
          day: 'numeric',
        });

        expect(screen.getByPlaceholderText('Select timestamp')).toHaveValue(
          formattedDate
        );
      });
    });

    it('should check the search value', () => {
      const searchFilter = 'searchFilter';
      renderComponent({}, { [MessagesFilterKeys.stringFilter]: searchFilter });
      expect(screen.getByPlaceholderText('Search')).toHaveValue(searchFilter);
    });

    describe('Serde dropdown', () => {
      beforeEach(async () => {
        (useSerdes as jest.Mock).mockImplementation(() => ({
          data: serdesPayload,
        }));
      });

      it('should check the keySerde', () => {
        if (!serdesPayload.key || !serdesPayload.key[0]) return;

        renderComponent(
          {},
          { [MessagesFilterKeys.keySerde]: serdesPayload.key[0].name }
        );
        expect(getKeySerdeDropdown()).toHaveTextContent(
          serdesPayload.key[0].name || ''
        );
      });

      it('should check the valueSerde', () => {
        if (!serdesPayload.value || !serdesPayload.value[0]) return;

        renderComponent(
          {},
          { [MessagesFilterKeys.valueSerde]: serdesPayload.value[0].name }
        );

        expect(getValueSerdeDropdown()).toHaveTextContent(
          serdesPayload.value[0].name || ''
        );
      });
    });
  });
});
