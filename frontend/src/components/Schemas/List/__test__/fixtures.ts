import {
  schemaVersion1,
  schemaVersion2,
  schemaVersionWithNonAsciiChars,
} from 'components/Schemas/Edit/__tests__/fixtures';

const schemas = [
  schemaVersion1,
  schemaVersion2,
  schemaVersionWithNonAsciiChars,
];

export const schemasPayload = {
  pageCount: 1,
  schemas,
};

export const schemasEmptyPayload = {
  pageCount: 1,
  schemas: [],
};
