import React from 'react';
import { render } from 'lib/testHelpers';
import { screen } from '@testing-library/react';
import * as S from 'components/Nav/Menu/styled';
import { theme } from 'theme/theme';
import { ServerStatus } from 'generated-sources';

describe('Cluster Styled Components', () => {
  const getMenuItem = () => screen.getByRole('menuitem');
  describe('MenuItem Component', () => {
    it('should check the rendering and correct Styling when it is open', () => {
      render(<S.MenuItem $variant="primary" $isActive />);
      expect(getMenuItem()).toHaveStyle(
        `color:${theme.menu.primary.color.active}`
      );
    });
    it('should check the rendering and correct Styling when it is Not open', () => {
      render(<S.MenuItem $variant="primary" $isActive={false} />);
      expect(getMenuItem()).toHaveStyle(
        `color:${theme.menu.primary.color.normal}`
      );
    });
  });

  describe('StatusIcon Component', () => {
    const getStatusCircle = () => screen.getByRole('status-circle');
    it('should check the rendering and correct Styling when it is online', () => {
      render(
        <svg>
          <S.StatusIcon status={ServerStatus.ONLINE} />
        </svg>
      );

      expect(getStatusCircle()).toHaveStyle(
        `fill:${theme.menu.primary.statusIconColor.online}`
      );
    });

    it('should check the rendering and correct Styling when it is offline', () => {
      render(
        <svg>
          <S.StatusIcon status={ServerStatus.OFFLINE} />
        </svg>
      );
      expect(getStatusCircle()).toHaveStyle(
        `fill:${theme.menu.primary.statusIconColor.offline}`
      );
    });

    it('should check the rendering and correct Styling when it is Initializing', () => {
      render(
        <svg>
          <S.StatusIcon status={ServerStatus.INITIALIZING} />
        </svg>
      );
      expect(getStatusCircle()).toHaveStyle(
        `fill:${theme.menu.primary.statusIconColor.initializing}`
      );
    });
  });
});
