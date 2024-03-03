import { RefObject } from 'react';

export type AclFormProps = {
  formRef: RefObject<HTMLFormElement> | null;
};

export enum PrefixType {
  EXACT = 'EXACT',
  PREFIXED = 'PREFIXED',
}

export enum ACLType {
  CUSTOM_ACL = 'CUSTOM_ACL',
  FOR_CONSUMERS = 'FOR_CONSUMERS',
  FOR_PRODUCERS = 'FOR_PRODUCERS',
  FOR_KAFKA_STREAM_APPS = 'FOR_KAFKA_STREAM_APPS',
}
