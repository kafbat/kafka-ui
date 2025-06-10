import { useLayoutEffect, useRef } from 'react';

function useScrollIntoView<T extends Element>(scroll: boolean) {
  const elementRef = useRef<T>(null);

  useLayoutEffect(() => {
    if (scroll) {
      elementRef.current?.scrollIntoView();
    }
  }, [scroll]);

  return {
    ref: elementRef,
  };
}

export default useScrollIntoView;
