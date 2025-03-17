import { fireEvent, screen } from '@testing-library/react';
import MenuColorPicker, {
  MenuColorPickerProps,
} from 'components/Nav/Menu/MenuColorPicker/MenuColorPicker';
import React from 'react';
import { render } from 'lib/testHelpers';
import { ClusterColorKey } from 'theme/theme';

describe('MenuColorPicker component', () => {
  const setupWrapper = (props?: Partial<MenuColorPickerProps>) => (
    <MenuColorPicker setColorKey={jest.fn()} {...props} />
  );

  it('renders all color circles', () => {
    render(setupWrapper());

    const colorKeys: ClusterColorKey[] = [
      'transparent',
      'gray',
      'red',
      'orange',
      'lettuce',
      'green',
      'turquoise',
      'blue',
      'violet',
      'pink',
    ];

    colorKeys.forEach((key) => {
      expect(screen.getByTestId(`color-circle-${key}`)).toBeInTheDocument();
    });
  });

  it('calls setColorKey with the correct color key when a color circle is clicked', () => {
    const setColorKeyMock = jest.fn();
    render(setupWrapper({ setColorKey: setColorKeyMock }));

    const colorCircle = screen.getByTestId('color-circle-red');
    fireEvent.click(colorCircle);

    expect(setColorKeyMock).toHaveBeenCalledWith('red');
  });

  it('renders the Dropdown component', () => {
    render(setupWrapper());
    expect(screen.getByRole('button')).toBeInTheDocument();
  });
});
