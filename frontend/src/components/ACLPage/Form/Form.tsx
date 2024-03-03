import React, { FC, ReactNode, useRef, useState } from 'react';
import Select from 'components/common/Select/Select';
import Heading from 'components/common/heading/Heading.styled';
import CloseIcon from 'components/common/Icons/CloseIcon';
import { Button } from 'components/common/Button/Button';
import { KafkaAcl } from 'generated-sources';

import { ACLType } from './types';
import { ACLTypeOptions } from './constants';
import * as S from './Form.styled';
import CustomACLForm from './CustomACL/Form';
import ForConsumersForm from './ForConsumers/Form';
import ForProducersForm from './ForProducers/Form';
import ForKafkaStreamAppsForm from './ForKafkaStreamApps/Form';

interface ACLFormProps {
  open: boolean;
  onClose: () => void;
  acl: KafkaAcl | null;
}

const ACLForm: FC<ACLFormProps> = ({ open, onClose, acl }) => {
  const [aclType, setAclType] = useState(ACLType.CUSTOM_ACL);
  const formRef = useRef<HTMLFormElement>(null);

  let content: ReactNode;
  if (aclType === ACLType.CUSTOM_ACL) {
    content = <CustomACLForm formRef={formRef} acl={acl} closeForm={onClose} />;
  } else if (aclType === ACLType.FOR_CONSUMERS) {
    content = <ForConsumersForm formRef={formRef} closeForm={onClose} />;
  } else if (aclType === ACLType.FOR_PRODUCERS) {
    content = <ForProducersForm formRef={formRef} closeForm={onClose} />;
  } else {
    content = <ForKafkaStreamAppsForm formRef={formRef} closeForm={onClose} />;
  }

  return (
    <S.Wrapper $open={open}>
      <Heading level={3}>
        <S.Title>Create ACL</S.Title>
        <S.CloseSidebar onClick={onClose}>
          <CloseIcon />
        </S.CloseSidebar>
      </Heading>
      <S.Content>
        <S.Field>
          <S.Label>Select ACL type</S.Label>
          <Select
            minWidth="270px"
            selectSize="L"
            value={aclType}
            options={ACLTypeOptions}
            onChange={(option) => setAclType(option as ACLType)}
          />
        </S.Field>
        {content}
      </S.Content>

      <S.Footer>
        <Button buttonSize="M" buttonType="primary" onClick={onClose}>
          Cancel
        </Button>
        <Button
          buttonSize="M"
          buttonType="primary"
          onClick={() => formRef.current?.requestSubmit()}
        >
          Submit
        </Button>
      </S.Footer>
    </S.Wrapper>
  );
};

export default React.memo(ACLForm);
