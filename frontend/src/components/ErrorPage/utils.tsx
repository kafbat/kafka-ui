import UnexpectedErrorIcon from 'components/common/Icons/UnexpectedErrorIcon';
import AccessErrorIcon from 'components/common/Icons/AccessErrorIcon';
import NotFoundErrorIcon from 'components/common/Icons/NotFoundErrorIcon';

export const getErrorInfoByCode = (
  code: number | undefined,
  resourceName?: string
) => {
  switch (code) {
    case 404:
      return {
        title: 'Resource not found',
        icon: <NotFoundErrorIcon />,
        text: `Information about the ${resourceName || 'resource'} cannot be found.`,
      };
    case 403:
      return {
        title: 'Access Denied',
        icon: <AccessErrorIcon />,
        text: 'You do not have permission to view this page.',
      };
    case 500:
      return {
        title: 'Unexpected error',
        icon: <UnexpectedErrorIcon />,
        text: 'An unexpected error occurred. Please try again.',
      };
    default:
      return {
        title: 'Unexpected error',
        icon: <UnexpectedErrorIcon />,
        text: 'An unexpected error occurred. Please try again.',
      };
  }
};
