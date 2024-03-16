import React from 'react';
import styled from 'styled-components';

const ProductHuntIcon: React.FC<{ className?: string }> = ({ className }) => (
  <svg
    width="24"
    height="25"
    className={className}
    viewBox="0 0 24 25"
    xmlns="http://www.w3.org/2000/svg"
  >
    <path d="M10.1998 12.5H13.6C14.0774 12.5 14.5352 12.3104 14.8728 11.9728C15.2104 11.6352 15.4 11.1774 15.4 10.7C15.4 10.2226 15.2104 9.76477 14.8728 9.42721C14.5352 9.08964 14.0774 8.9 13.6 8.9H10.1998V12.5Z" />
    <path
      fillRule="evenodd"
      clipRule="evenodd"
      d="M12 23.2422C18.0753 23.2422 23 18.3175 23 12.2422C23 6.16689 18.0753 1.24219 12 1.24219C5.9247 1.24219 1 6.16689 1 12.2422C1 18.3175 5.9247 23.2422 12 23.2422ZM7.7998 6.5H13.6C14.7139 6.5 15.7822 6.9425 16.5699 7.73015C17.3575 8.5178 17.8 9.58609 17.8 10.7C17.8 11.8139 17.3575 12.8822 16.5699 13.6698C15.7822 14.4575 14.7139 14.9 13.6 14.9H10.1998V18.5H7.7998V6.5Z"
    />
  </svg>
);

export default styled(ProductHuntIcon)``;
