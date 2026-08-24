import styled, { css } from 'styled-components';
import { MultiLineTag } from 'components/common/Tag/Tag.styled';

export const TagsWrapper = styled.div`
  ${({ theme }) => css`
    display: flex;
    flex-wrap: wrap;
    word-break: break-word;
    white-space: pre-wrap;

    ${MultiLineTag} {
      background-color: ${theme.chips.backgroundColor.normal};
      color: ${theme.chips.color.normal};

      &:hover {
        background-color: ${theme.chips.backgroundColor.hover};
        color: ${theme.chips.color.hover};
      }
    }
  `}
`;
