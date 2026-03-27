import styled from 'styled-components';

interface Props {
  color: 'green' | 'gray' | 'yellow' | 'red' | 'white' | 'blue';
  clickable?: boolean;
}

export const Tag = styled.span.attrs({ role: 'widget' })<Props>`
  border: none;
  border-radius: 16px;
  height: 20px;
  line-height: 20px;
  background-color: ${({ theme, color }) => theme.tag.backgroundColor[color]};
  color: ${({ theme }) => theme.tag.color};
  font-size: 12px;
  display: inline-block;
  padding-left: 0.75em;
  padding-right: 0.75em;
  text-align: center;
  width: max-content;
  margin: 2px 0;
  cursor: ${({ clickable }) => (clickable ? 'pointer' : 'default')};
`;

export const MultiLineTag = styled.div.attrs({ role: 'widget' })<Props>`
  border: none;
  border-radius: 16px;
  height: fit-content;
  line-height: 20px;
  background-color: ${({ theme, color }) => theme.tag.backgroundColor[color]};
  color: ${({ theme }) => theme.tag.color};
  font-size: 12px;
  display: inline-block;
  padding-left: 0.75em;
  padding-right: 0.75em;
  text-align: center;
  width: fit-content;
  margin: 2px 0;
  white-space: pre-wrap;
`;
