import { TextEncoder } from 'util';

import React from 'react';
import { screen } from '@testing-library/react';
import { render } from 'lib/testHelpers';
import AvroMetadata, {
  AvroMetadataProps,
} from 'components/Topics/Topic/Messages/MessageContent/AvroMetadata';

const setupWrapper = (props?: Partial<AvroMetadataProps>) => {
  return (
    <table>
      <tbody>
        <AvroMetadata
          deserializeProperties={{
            type: 'AVRO',
            name: 'com.kafbat.MessageType',
            schemaId: 1,
          }}
          {...props}
        />
      </tbody>
    </table>
  );
};

global.TextEncoder = TextEncoder;

describe('AvroMetadata screen', () => {
  beforeEach(() => {
    render(setupWrapper());
  });

  describe('Checking type and schema id', () => {
    it('type in document', () => {
      expect(screen.getByText('MessageType')).toBeInTheDocument();
    });

    it('schema id in document', () => {
      expect(screen.getByText('Schema Id: 1')).toBeInTheDocument();
    });
  });
});
