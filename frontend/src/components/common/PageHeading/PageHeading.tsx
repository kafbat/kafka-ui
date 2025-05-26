import React, { PropsWithChildren } from 'react';
import Heading from 'components/common/heading/Heading.styled';
import { ClusterGroupParam } from 'lib/paths';
import useAppParams from 'lib/hooks/useAppParams';

import * as S from './PageHeading.styled';

interface PageHeadingProps {
  text: string;
  backTo?: string;
  backText?: string;
}

const PageHeading: React.FC<PropsWithChildren<PageHeadingProps>> = ({
  text,
  backTo,
  backText,
  children,
}) => {
  const isBackButtonVisible = backTo && backText;
  const { clusterName } = useAppParams<ClusterGroupParam>();

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
