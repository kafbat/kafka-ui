import React, { FC } from "react";
import styled from "styled-components";

const CheckmarkIcon: FC = () => {

  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 64 64"
      width="12"
      height="12"
      aria-labelledby="title"
      aria-describedby="desc"
      role="img"
    >
      <title>Checkmark</title>
      <desc>A line styled icon from Orion Icon Library.</desc>
      <path
        d="M25 48c-1.1 0-2.2-.4-3-1.2l-13-13c-1.6-1.6-1.6-4.2 0-5.8s4.2-1.6 5.8 0L25 38.8 49.2 14.6c1.6-1.6 4.2-1.6 5.8 0s1.6 4.2 0 5.8l-27 27c-.8.8-1.9 1.2-3 1.2z"
      />
    </svg>
  );
};

export default styled(CheckmarkIcon)``;
