import React, { FC, Suspense, useContext, useRef, useState } from 'react';
import Select from 'components/common/Select/Select';
import Heading from 'components/common/heading/Heading.styled';
import CloseIcon from 'components/common/Icons/CloseIcon';
import { Button } from 'components/common/Button/Button';

import { ACLType, AclFormProps } from './types';
import { ACLTypeOptions } from './constants';
import * as S from './Form.styled';
import ACLFormContext from './AclFormContext';
import PageLoader from 'components/common/PageLoader/PageLoader';

interface ACLFormProps {
  isOpen: boolean;
}

const DETAILED_FORM_COMPONENTS: Record<
  keyof typeof ACLType,
  FC<AclFormProps>
> = {
  [ACLType.CUSTOM_ACL]: React.lazy(() => import('./CustomACL/Form')),
  [ACLType.FOR_CONSUMERS]: React.lazy(() => import('./ForConsumers/Form')),
  [ACLType.FOR_PRODUCERS]: React.lazy(() => import('./ForProducers/Form')),
  [ACLType.FOR_KAFKA_STREAM_APPS]: React.lazy(
    () => import('./ForKafkaStreamApps/Form')
  ),
};

const ACLForm: FC<ACLFormProps> = ({ isOpen: open }) => {
  const [aclType, setAclType] = useState(ACLType.CUSTOM_ACL);
  const { onClose } = useContext(ACLFormContext);

  const formRef = useRef<HTMLFormElement>(null);
  const DetailedForm = DETAILED_FORM_COMPONENTS[aclType];

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
        <Suspense fallback={<></>}>
          <DetailedForm formRef={formRef}></DetailedForm>
        </Suspense>
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
