import React from 'react';

const ColorPickerIcon: React.FC = () => {
  return (
    <svg
      width="16"
      height="16"
      viewBox="0 0 16 16"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <circle
        cx="8"
        cy="8"
        r="5.5"
        fill="url(#paint0_linear_6232_2411)"
        stroke="white"
      />
      <defs>
        <linearGradient
          id="paint0_linear_6232_2411"
          x1="3"
          y1="8"
          x2="13"
          y2="8"
          gradientUnits="userSpaceOnUse"
        >
          <stop stopColor="#FFA012" />
          <stop offset="0.485" stopColor="#00D5A2" />
          <stop offset="1" stopColor="#127FFF" />
        </linearGradient>
      </defs>
    </svg>
  );
};

export default ColorPickerIcon;
