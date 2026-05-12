import React from 'react';
import Header from 'components/Connect/Header/Header';
import ClusterContext, {
  initialValue,
} from 'components/contexts/ClusterContext';
import { render } from 'lib/testHelpers';
import { screen } from '@testing-library/react';
import { connects } from 'lib/fixtures/kafkaConnect';
import { useConnects } from 'lib/hooks/api/kafkaConnect';

jest.mock('lib/hooks/api/kafkaConnect', () => ({
  useConnects: jest.fn(),
}));

describe('Kafka Connect header', () => {
  beforeEach(() => {
    (useConnects as jest.Mock).mockImplementation(() => ({
      data: connects,
    }));
  });

  async function renderComponent({ isReadOnly }: { isReadOnly: boolean }) {
    render(
      <ClusterContext.Provider value={{ ...initialValue, isReadOnly }}>
        <Header />
      </ClusterContext.Provider>
    );
  }

  it('render create connector button', () => {
    renderComponent({ isReadOnly: false });

    const btn = screen.getByRole('button', { name: 'Create Connector' });

    expect(btn).toBeInTheDocument();
    expect(btn).toBeEnabled();
  });

  describe('when no connects', () => {
    it('create connector button is disabled', () => {
      renderComponent({ isReadOnly: false });

      const btn = screen.getByRole('button', { name: 'Create Connector' });

      expect(btn).toBeInTheDocument();
      expect(btn).toBeEnabled();
    });
  });

  describe('when cluster is readonly', () => {
    it('doesnt render create connector button', () => {
      (useConnects as jest.Mock).mockImplementation(() => ({
        data: [],
      }));
      renderComponent({ isReadOnly: true });

      const btn = screen.queryByRole('button', { name: 'Create Connector' });

      expect(btn).not.toBeInTheDocument();
    });
  });
});
