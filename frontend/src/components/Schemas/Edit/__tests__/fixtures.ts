import { SchemaType, SchemaSubject } from 'generated-sources';

export const schemaVersion1: SchemaSubject = {
  subject: 'schema7_1',
  version: '1',
  id: 2,
  schema:
    '{"$schema":"http://json-schema.org/draft-07/schema#","$id":"http://example.com/myURI.schema.json","title":"TestRecord","type":"object","additionalProperties":false,"properties":{"f1":{"type":"integer"},"f2":{"type":"string"},"schema":{"type":"string"}}}',
  compatibilityLevel: 'FULL',
  schemaType: SchemaType.JSON,
};
export const schemaVersion2: SchemaSubject = {
  subject: 'MySchemaSubject',
  version: '2',
  id: 28,
  schema: '12',
  compatibilityLevel: 'FORWARD_TRANSITIVE',
  schemaType: SchemaType.JSON,
};
export const schemaVersionWithNonAsciiChars: SchemaSubject = {
  subject: 'test/test',
  version: '1',
  id: 29,
  schema: '13',
  compatibilityLevel: 'FORWARD_TRANSITIVE',
  schemaType: SchemaType.JSON,
};

export { schemaVersion1 as schemaVersion };
