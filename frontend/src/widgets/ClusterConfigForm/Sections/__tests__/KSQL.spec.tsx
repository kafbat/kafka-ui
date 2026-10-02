import React from 'react';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { FormProvider, useForm } from 'react-hook-form';
import { render } from 'lib/testHelpers';
import KSQL from 'widgets/ClusterConfigForm/Sections/KSQL';
import { transformFormDataToPayload } from 'widgets/ClusterConfigForm/utils/transformFormDataToPayload';
import { ClusterConfigFormValues } from 'widgets/ClusterConfigForm/types';

// SSLForm -> Fileupload -> appConfig -> api pulls in `generated-sources`, which
// only exists after `pnpm gen:sources`. None of it is exercised here, so stub it
// and keep this test runnable without the codegen step.
jest.mock('widgets/ClusterConfigForm/common/SSLForm', () => () => null);

const KsqlHarness = ({ onPayload }: { onPayload: (p: unknown) => void }) => {
  const methods = useForm<ClusterConfigFormValues>({
    defaultValues: { ksql: undefined },
  });

  return (
    <FormProvider {...methods}>
      <KSQL />
      <button
        type="button"
        onClick={() =>
          onPayload(
            transformFormDataToPayload({
              ...methods.getValues(),
              name: 'test-cluster',
              readOnly: false,
              bootstrapServers: [{ host: 'localhost', port: '9092' }],
            })
          )
        }
      >
        submit
      </button>
    </FormProvider>
  );
};

describe('KSQL section', () => {
  it('marks the config as active so it is persisted on submit', async () => {
    const user = userEvent.setup();
    const onPayload = jest.fn();
    render(<KsqlHarness onPayload={onPayload} />);

    await user.click(
      screen.getByRole('button', { name: /configure ksql db/i })
    );

    expect(await screen.findByLabelText(/url/i)).toBeInTheDocument();

    await user.type(screen.getByLabelText(/url/i), 'http://ksql:8088');
    await user.click(screen.getByRole('button', { name: 'submit' }));

    // transformFormDataToPayload only emits `ksqldbServer` when
    // `ksql.isActive` is true, so a false flag silently discards the config.
    expect(onPayload).toHaveBeenCalledWith(
      expect.objectContaining({ ksqldbServer: 'http://ksql:8088' })
    );
  });

  it('clears the config when the section is toggled off', async () => {
    const user = userEvent.setup();
    const onPayload = jest.fn();
    render(<KsqlHarness onPayload={onPayload} />);

    await user.click(
      screen.getByRole('button', { name: /configure ksql db/i })
    );
    await screen.findByLabelText(/url/i);
    // Once open, the same control becomes "Remove from config".
    await user.click(
      screen.getByRole('button', { name: /remove from config/i })
    );
    await user.click(screen.getByRole('button', { name: 'submit' }));

    expect(onPayload).toHaveBeenCalledWith(
      expect.not.objectContaining({ ksqldbServer: expect.anything() })
    );
  });
});
