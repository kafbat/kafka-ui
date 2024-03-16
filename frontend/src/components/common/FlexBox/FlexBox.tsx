import React, { CSSProperties, ReactNode } from 'react';
import styled from 'styled-components';

interface FlexboxProps {
  flexDirection?: CSSProperties['flexDirection'];
  alignItems?: CSSProperties['alignItems'];
  alignSelf?: CSSProperties['alignSelf'];
  justifyContent?: CSSProperties['justifyContent'];
  justifyItems?: CSSProperties['justifyItems'];
  gap?: CSSProperties['gap'];
  margin?: CSSProperties['margin'];
  padding?: CSSProperties['padding'];
  color?: CSSProperties['color'];
  flexGrow?: CSSProperties['flexGrow'];
  flexWrap?: CSSProperties['flexWrap'];
  width?: CSSProperties['width'];
  children: ReactNode;
}

const FlexboxContainer = styled.div<FlexboxProps>`
  display: flex;
  flex-direction: ${(props) => props.flexDirection || 'row'};
  align-items: ${(props) => props.alignItems};
  align-self: ${(props) => props.alignSelf};
  justify-content: ${(props) => props.justifyContent};
  justify-items: ${(props) => props.justifyItems};
  gap: ${(props) => props.gap};
  margin: ${(props) => props.margin};
  padding: ${(props) => props.padding};
  flex-grow: ${(props) => props.flexGrow};
  width: ${(props) => props.width};
  color ${(props) => props.color};
`;

const Flexbox: React.FC<FlexboxProps> = ({ children, ...rest }) => {
  return <FlexboxContainer {...rest}>{children}</FlexboxContainer>;
};

export default Flexbox;
