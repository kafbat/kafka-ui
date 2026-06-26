import React from 'react';

const CopyIcon: React.FC<{ fill?: string }> = ({ fill = 'currentColor' }) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 448 512"
    fill={fill}
    width="14"
    height="14"
  >
    <path d="M384 96V320H192V96H384zM192 32C156.7 32 128 60.65 128 96V320C128 355.3 156.7 384 192 384H384C419.3 384 448 355.3 448 320V96C448 60.65 419.3 32 384 32H192zM64 128C28.65 128 0 156.7 0 192V416C0 451.3 28.65 480 64 480H256C291.3 480 320 451.3 320 416H256V416H64V192H96V128H64z" />
  </svg>
);

export default CopyIcon;
