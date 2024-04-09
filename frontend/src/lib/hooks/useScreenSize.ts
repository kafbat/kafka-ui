import { useEffect, useState } from 'react';
import { useTheme } from 'styled-components';

export const useScreenSize = () => {
  const {
    breakpoints: { S, M },
  } = useTheme();

  const [screenSize, setScreenSize] = useState({
    isLarge: window.innerWidth > M,
    isMedium: window.innerWidth >= S && window.innerWidth <= M,
  });

  useEffect(() => {
    const checkScreenSize = () => {
      setScreenSize({
        isLarge: window.innerWidth > M,
        isMedium: window.innerWidth >= S && window.innerWidth <= M,
      });
    };

    window.addEventListener('resize', checkScreenSize);
    return () => window.removeEventListener('resize', checkScreenSize);
  }, []);

  return screenSize;
};
