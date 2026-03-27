import React from 'react';
import { render, WithRoute } from 'lib/testHelpers';
import { screen } from '@testing-library/dom';
import userEvent from '@testing-library/user-event';
import { clusterACLPath } from 'lib/paths';
import ACList from 'components/ACLPage/List/List';
import { useAcls, useCreateCustomAcl, useDeleteAcl } from 'lib/hooks/api/acl';
import { aclPayload } from 'lib/fixtures/acls';

jest.mock('lib/hooks/api/acl', () => ({
  useAcls: jest.fn(),
  useDeleteAcl: jest.fn(),
  useCreateCustomAcl: jest.fn(),
}));

describe('ACLList Component', () => {
  const clusterName = 'local';
  const renderComponent = () =>
    render(
      <WithRoute path={clusterACLPath()}>
        <ACList />
      </WithRoute>,
      {
        initialEntries: [clusterACLPath(clusterName)],
      }
    );

  describe('ACLList', () => {
    describe('when the acls are loaded', () => {
      beforeEach(() => {
        (useAcls as jest.Mock).mockImplementation(() => ({
          data: aclPayload,
          isSuccess: true,
        }));
        (useDeleteAcl as jest.Mock).mockImplementation(() => ({
          deleteResource: jest.fn(),
        }));
        (useCreateCustomAcl as jest.Mock).mockImplementation(() => ({
          createResource: jest.fn(),
        }));
      });

      it('renders ACLList with records', async () => {
        renderComponent();
        expect(screen.getByRole('table')).toBeInTheDocument();
        expect(screen.getAllByRole('row').length).toEqual(4);
      });

      it('shows delete icon on hover', async () => {
        const { container } = renderComponent();
        const [trElement] = screen.getAllByRole('row');
        await userEvent.hover(trElement);
        const deleteElement = container.querySelector('svg');
        expect(deleteElement).not.toHaveStyle({
          fill: 'transparent',
        });
      });

      it('header has button for create ACL', () => {
        renderComponent();
        const button = screen.getByText('Create ACL');
        expect(button).toBeInTheDocument();
      });

      it('form not in the document', async () => {
        renderComponent();
        const form = screen.queryByTestId('aclForm');
        expect(form).not.toBeInTheDocument();
      });

      describe('after acl button click', () => {
        it('form is in the document', async () => {
          renderComponent();
          const button = screen.getByText('Create ACL');
          await userEvent.click(button);
          const form = screen.queryByTestId('aclForm');
          expect(form).toBeInTheDocument();
        });
      });
    });

    describe('when it has no acls', () => {
      beforeEach(() => {
        (useAcls as jest.Mock).mockImplementation(() => ({
          data: [],
          isSuccess: true,
        }));
        (useDeleteAcl as jest.Mock).mockImplementation(() => ({
          deleteResource: jest.fn(),
        }));
      });

      it('renders empty ACLList with message', async () => {
        renderComponent();
        expect(screen.getByRole('table')).toBeInTheDocument();
        expect(
          screen.getByRole('row', { name: 'No ACL items found' })
        ).toBeInTheDocument();
      });
    });
  });
});
