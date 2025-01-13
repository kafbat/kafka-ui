import styled from 'styled-components';

export const OAuthSignInStyled = styled.div`
  display: flex;
  flex-direction: column;
  gap: 8px;
`;

export const ErrorMessage = styled.div`
  display: flex;
  column-gap: 2px;
  align-items: center;
  justify-content: center;
  font-weight: 400;
  font-size: 14px;
  line-height: 20px;
  margin-bottom: 8px;
`;

export const ErrorMessageText = styled.span`
  ${({ theme }) => theme.auth_page.signIn.errorMessage};
  font-weight: 400;
  font-size: 14px;
  line-height: 20px;
`;
