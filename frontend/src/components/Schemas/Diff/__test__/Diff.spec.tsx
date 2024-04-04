import React from 'react';
import Diff from 'components/Schemas/Diff/Diff';
import { render, WithRoute } from 'lib/testHelpers';
import { screen } from '@testing-library/react';
import { clusterSchemaComparePath } from 'lib/paths';
import userEvent from '@testing-library/user-event';
import { useGetSchemasVersions } from 'lib/hooks/api/schemas';

import { versions } from './fixtures';

const defaultClusterName = 'defaultClusterName';
const defaultSubject = 'defaultSubject';
const defaultPathName = clusterSchemaComparePath(
  defaultClusterName,
  defaultSubject
);

jest.mock('lib/hooks/api/schemas', () => ({
  useGetSchemasVersions: jest.fn(),
}));

describe('Diff', () => {
  const setupComponent = (
    searchQuery: { rightVersion?: string; leftVersion?: string } = {}
  ) => {
    let pathname = defaultPathName;
    const searchParams = new URLSearchParams(pathname);
    if (searchQuery.rightVersion) {
      searchParams.set('rightVersion', searchQuery.rightVersion);
    }
    if (searchQuery.leftVersion) {
      searchParams.set('leftVersion', searchQuery.leftVersion);
    }

    pathname = `${pathname}?${searchParams.toString()}`;

    return render(
      <WithRoute path={clusterSchemaComparePath()}>
        <Diff />
      </WithRoute>,
      {
        initialEntries: [pathname],
      }
    );
  };

  describe('Container', () => {
    it('renders view', () => {
      (useGetSchemasVersions as jest.Mock).mockImplementation(() => ({
        data: versions,
        isFetching: false,
        isError: false,
      }));
      setupComponent();
      // TODO make sure this case it correct
      expect(screen.getAllByText('Version 3').length).toEqual(2);
    });
  });

  describe('when page with schema versions is loading', () => {
    beforeAll(() => {
      (useGetSchemasVersions as jest.Mock).mockImplementation(() => ({
        data: undefined,
        isFetching: true,
        isError: false,
      }));
      setupComponent();
    });
    it('renders PageLoader', () => {
      expect(screen.getByRole('progressbar')).toBeInTheDocument();
    });
  });

  describe('when schema versions are loaded and no specified versions in path', () => {
    beforeEach(() => {
      (useGetSchemasVersions as jest.Mock).mockImplementation(() => ({
        data: versions,
        isFetching: false,
        isError: false,
      }));
      setupComponent();
    });

    it('renders all options', () => {
      expect(screen.getAllByRole('option').length).toEqual(2);
    });
    it('renders left select with empty value', () => {
      const select = screen.getAllByRole('listbox')[0];
      expect(select).toBeInTheDocument();
      expect(select).toHaveTextContent(versions[0].version);
    });

    it('renders right select with empty value', () => {
      const select = screen.getAllByRole('listbox')[1];
      expect(select).toBeInTheDocument();
      expect(select).toHaveTextContent(versions[0].version);
    });
  });

  describe('when schema versions are loaded and two versions in path', () => {
    beforeEach(() => {
      (useGetSchemasVersions as jest.Mock).mockImplementation(() => ({
        data: versions,
        isFetching: false,
        isError: false,
      }));
      setupComponent({ leftVersion: '1', rightVersion: '2' });
    });

    it('renders left select with version 1', () => {
      const select = screen.getAllByRole('listbox')[0];
      expect(select).toBeInTheDocument();
      expect(select).toHaveTextContent('1');
    });

    it('renders right select with version 2', () => {
      const select = screen.getAllByRole('listbox')[1];
      expect(select).toBeInTheDocument();
      expect(select).toHaveTextContent('2');
    });
  });

  describe('when schema versions are loaded and only one versions in path', () => {
    beforeEach(() => {
      (useGetSchemasVersions as jest.Mock).mockImplementation(() => ({
        data: versions,
        isFetching: false,
        isError: false,
      }));
      setupComponent({
        leftVersion: '1',
      });
    });

    it('renders left select with version 1', () => {
      const select = screen.getAllByRole('listbox')[0];
      expect(select).toBeInTheDocument();
      expect(select).toHaveTextContent('1');
    });

    it('renders right select with empty value', () => {
      const select = screen.getAllByRole('listbox')[1];
      expect(select).toBeInTheDocument();
      expect(select).toHaveTextContent(versions[0].version);
    });
  });

  describe('Back button', () => {
    beforeEach(() => {
      (useGetSchemasVersions as jest.Mock).mockImplementation(() => ({
        data: versions,
        isFetching: false,
        isError: false,
      }));
      setupComponent();
    });

    it('back button is appear', () => {
      const backButton = screen.getAllByRole('button', { name: 'Back' });
      expect(backButton[0]).toBeInTheDocument();
    });

    it('click on back button', () => {
      const backButton = screen.getAllByRole('button', { name: 'Back' });
      userEvent.click(backButton[0]);
      expect(screen.queryByRole('Back')).not.toBeInTheDocument();
    });
  });
});
