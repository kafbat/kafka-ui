import React from 'react';
import QuestionIcon from 'components/common/Icons/QuestionIcon';
import useBoolean from 'lib/hooks/useBoolean';
import * as S from 'components/Topics/Topic/Messages/Filters/Filters.styled';

import InfoModal from './InfoModal';

const QuestionInfo: React.FC = () => {
  const { toggle, value: isOpen } = useBoolean();
  return (
    <>
      <S.QuestionIconContainer type="button" aria-label="info" onClick={toggle}>
        <QuestionIcon />
      </S.QuestionIconContainer>
      {isOpen && <InfoModal toggleIsOpen={toggle} />}
    </>
  );
};

export default QuestionInfo;
