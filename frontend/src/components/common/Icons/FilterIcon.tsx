import React from 'react';
import styled from 'styled-components';

const FilterIcon: React.FC<{ isOpen: boolean }> = () => {
  return (
    <svg
      width="12"
      height="12"
      viewBox="0 0 12 12"
      fill="currentColor"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path
        fillRule="evenodd"
        clipRule="evenodd"
        d="M11.1434 0.0107422C11.473 0.0585966 11.7612 0.268807 11.9051 0.576172C12.049 0.883518 12.0267 1.23964 11.8524 1.52344L11.7684 1.64062L6.99987 7.3623V11C6.99987 11.5522 6.55209 11.9999 5.99987 12C5.44758 12 4.99987 11.5523 4.99987 11V7.3623L0.231311 1.64062C-0.0170576 1.34258 -0.0699041 0.927525 0.0945918 0.576172C0.259192 0.224847 0.611886 0 0.999865 0H10.9999L11.1434 0.0107422ZM5.99987 5.43652L8.86412 2H3.13561L5.99987 5.43652Z"
      />
    </svg>
  );
};

export default styled(FilterIcon)``;
