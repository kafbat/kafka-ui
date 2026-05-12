import React from 'react';
import Spinner from 'components/common/Spinner/Spinner';

import * as S from './PageLoader.styled';
import { PageLoaderProps } from './PageLoader.styled';

const PageLoader: React.FC<PageLoaderProps> = ({ fullSize, offsetY = 154 }) => (
  <S.Wrapper fullSize={fullSize} offsetY={offsetY}>
    <Spinner />
  </S.Wrapper>
);

export default PageLoader;
