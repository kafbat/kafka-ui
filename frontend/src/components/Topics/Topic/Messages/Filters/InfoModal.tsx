import React from 'react';
import * as S from 'components/Topics/Topic/Messages/Filters/Filters.styled';
import { Button } from 'components/common/Button/Button';
import Flexbox from 'components/common/FlexBox/FlexBox';

interface InfoModalProps {
  toggleIsOpen(): void;
}

const InfoModal: React.FC<InfoModalProps> = ({ toggleIsOpen }) => {
  return (
    <S.InfoModal>
      <S.InfoHeading>We use CEL syntax for smart message filters</S.InfoHeading>
      <S.InfoParagraph>
        <b>Variables bound to the context:</b>
      </S.InfoParagraph>
      <ol aria-label="info-list">
        <S.ListItem>key (json if possible)</S.ListItem>
        <S.ListItem>value (json if possible)</S.ListItem>
        <S.ListItem>keyAsText</S.ListItem>
        <S.ListItem>valueAsText</S.ListItem>
        <S.ListItem>header</S.ListItem>
        <S.ListItem>partition</S.ListItem>
        <S.ListItem>timestampMs</S.ListItem>
      </ol>
      <S.InfoParagraph>
        <b>JSON parsing logic:</b>
      </S.InfoParagraph>
      <S.InfoParagraph>
        Key and Value (if parsing to JSON is available) are bound as JSON
        objects, otherwise as nulls.
      </S.InfoParagraph>
      <S.InfoParagraph>
        <b>Filter examples:</b>
      </S.InfoParagraph>
      <ol aria-label="info-list">
        <S.ListItem>
          <code>
            has(record.keyAsText) &&
            record.keyAsText.matches(&quot;.*[Gg]roovy.*&quot;)
          </code>{' '}
          - regex for key as a string
        </S.ListItem>
        <S.ListItem>
          <code>
            has(record.key.name.first) && record.key.name.first ==
            &apos;user1&apos;
          </code>{' '}
          - in case if the value is json
        </S.ListItem>
        <S.ListItem>
          <code>
            record.headers.size() == 1 && !has(record.headers.k1) &&
            record.headers[&apos;k2&apos;] == &apos;v2&apos;
          </code>
        </S.ListItem>
      </ol>
      <Flexbox justifyContent="center" margin="20px 0 0 0">
        <Button
          buttonSize="M"
          buttonType="secondary"
          type="button"
          onClick={toggleIsOpen}
        >
          Ok
        </Button>
      </Flexbox>
    </S.InfoModal>
  );
};

export default InfoModal;
