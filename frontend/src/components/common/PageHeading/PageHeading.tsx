import React, { PropsWithChildren } from 'react';
import Heading from 'components/common/heading/Heading.styled';
import { buildPageTitle } from 'lib/pageTitles';

import * as S from './PageHeading.styled';

interface PageHeadingProps {
  text: string;
  backTo?: string;
  backText?: string;
  title?: string;
  documentTitle?: string;
}

const PageHeading: React.FC<PropsWithChildren<PageHeadingProps>> = ({
  text,
  backTo,
  backText,
  children,
  title,
  documentTitle,
}) => {
  const isBackButtonVisible = backTo && backText;
  React.useEffect(() => {
    document.title = documentTitle || buildPageTitle(text, title);
  }, [documentTitle, text, title]);

  return (
    <S.Wrapper>
      {title ? <S.Title>{title}</S.Title> : null}
      <S.Content>
        <S.Breadcrumbs>
          {isBackButtonVisible && (
            <S.BackLink to={backTo}>{backText}</S.BackLink>
          )}
          <Heading>{text}</Heading>
        </S.Breadcrumbs>
        <div>{children}</div>
      </S.Content>
    </S.Wrapper>
  );
};

export default PageHeading;
