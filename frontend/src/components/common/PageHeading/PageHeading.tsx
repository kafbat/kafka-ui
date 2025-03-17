import React, { PropsWithChildren } from 'react';
import Heading from 'components/common/heading/Heading.styled';

import * as S from './PageHeading.styled';

interface PageHeadingProps {
  text: string;
  clusterName?: string;
  backTo?: string;
  backText?: string;
}

const PageHeading: React.FC<PropsWithChildren<PageHeadingProps>> = ({
  text,
  clusterName,
  backTo,
  backText,
  children,
}) => {
  const isBackButtonVisible = backTo && backText;

  return (
    <S.Wrapper>
      <S.Breadcrumbs>
        <Heading>
          {clusterName && (
            <>
              <S.ClusterTitle>{clusterName}</S.ClusterTitle>
              <S.Slash>/</S.Slash>
            </>
          )}
          {isBackButtonVisible && (
            <>
              <S.BackLink to={backTo}>{backText}</S.BackLink>
              <S.Slash>/</S.Slash>
            </>
          )}
          {text}
        </Heading>
      </S.Breadcrumbs>
      <div>{children}</div>
    </S.Wrapper>
  );
};

export default PageHeading;
