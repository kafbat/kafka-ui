import { RefObject } from 'react';

export interface AclDetailedFormProps {
  formRef: RefObject<HTMLFormElement> | null;
}

export interface ACLFormProps {
  isOpen: boolean;
}

export enum MatchType {
  EXACT = 'EXACT',
  PREFIXED = 'PREFIXED',
}

export enum ACLType {
  CUSTOM_ACL = 'CUSTOM_ACL',
  FOR_CONSUMERS = 'FOR_CONSUMERS',
  FOR_PRODUCERS = 'FOR_PRODUCERS',
  FOR_KAFKA_STREAM_APPS = 'FOR_KAFKA_STREAM_APPS',
}
