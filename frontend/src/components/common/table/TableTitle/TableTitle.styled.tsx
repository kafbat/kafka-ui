import React, { PropsWithChildren } from 'react';
import Heading from 'components/common/heading/Heading.styled';
import styled from 'styled-components';

const Heading3 = (props: PropsWithChildren) => <Heading level={3} {...props} />;

export const TableTitle = styled(Heading3)`
  padding: 16px 16px 0;
`;
