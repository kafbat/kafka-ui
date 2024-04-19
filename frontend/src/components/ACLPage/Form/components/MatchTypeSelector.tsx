import React, { FC, ReactElement, useState } from 'react';
import Radio from 'components/common/Radio/Radio';
import { MatchType } from 'components/ACLPage/Form/types';
import { matchTypeOptions } from 'components/ACLPage/Form/constants';

interface MatchTypeSelectorProps {
  exact: ReactElement;
  prefixed: ReactElement;
  onChange?: (matchType: MatchType) => void;
}

const MatchTypeSelector: FC<MatchTypeSelectorProps> = ({
  exact,
  prefixed,
  onChange,
}) => {
  const [matchType, setMatchType] = useState(MatchType.EXACT);

  const handleChange = (value: MatchType) => {
    setMatchType(value);
    onChange?.(value);
  };

  const content = matchType === MatchType.EXACT ? exact : prefixed;

  return (
    <>
      <Radio
        value={matchType}
        options={matchTypeOptions}
        onChange={(v) => handleChange(v as MatchType)}
      />
      <div>{content}</div>
    </>
  );
};

export default MatchTypeSelector;
