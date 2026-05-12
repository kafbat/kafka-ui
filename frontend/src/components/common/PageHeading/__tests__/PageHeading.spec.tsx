import React from 'react';
import { render } from 'lib/testHelpers';
import PageHeading from 'components/common/PageHeading/PageHeading';

describe('PageHeading', () => {
  afterEach(() => {
    document.title = '';
  });

  it('sets the browser title from heading content by default', () => {
    render(<PageHeading text="Topics" title="local" />);

    expect(document.title).toBe('Topics | local | Kafbat UI');
  });

  it('uses an explicit browser title override when provided', () => {
    render(
      <PageHeading
        text="orders"
        title="local"
        documentTitle="Messages | orders | local | Kafbat UI"
      />
    );

    expect(document.title).toBe('Messages | orders | local | Kafbat UI');
  });
});
