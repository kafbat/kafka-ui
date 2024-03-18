import { StyledForm } from 'components/common/Form/Form.styled';
import { Input } from 'components/common/Input/Input.styled';
import MultiSelect from 'components/common/MultiSelect/MultiSelect.styled';
import styled from 'styled-components';

export const Wrapper = styled.div<{ $open?: boolean }>(
  ({ theme, $open }) => `
  background-color: ${theme.default.backgroundColor};
  position: fixed;
  top: ${theme.layout.navBarHeight};
  bottom: 0;
  width: 37vw;
  right: calc(${$open ? '0px' : theme.layout.rightSidebarWidth} * -1);
  box-shadow: -1px 0px 10px 0px rgba(0, 0, 0, 0.2);
  transition: right 0.3s linear;
  z-index: 200;
  display: flex;
  flex-direction: column;

  h3 {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid ${theme.layout.stuffBorderColor};
    padding: 16px;
  }
`
);

export const Form = styled(StyledForm)`
  margin-top: 16px;
  padding: 0px;

  ${MultiSelect} {
    min-width: 270px;
  }

  ${Input} {
    min-width: 270px;
  }
`;

export const Field = styled.div`
  ${({ theme }) => theme.input.label};
  display: flex;
  justify-content: space-between;

  & ul {
    width: 100%;
  }
`;

export const Label = styled.label`
  line-height: 32px;
`;

export const ControlList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 10px;
`;

export const Footer = styled.div`
  display: flex;
  justify-content: end;
  gap: 8px;
  padding: 16px;
`;
export const Content = styled.div`
  flex: auto;
  padding: 16px;
`;

export const CloseSidebar = styled.div`
  cursor: pointer;
`;
export const Title = styled.span``;
