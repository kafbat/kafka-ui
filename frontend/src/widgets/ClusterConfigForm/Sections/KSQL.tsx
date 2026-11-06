import React, { useState } from 'react';
import Input from 'components/common/Input/Input';
import { useFormContext } from 'react-hook-form';
import SectionHeader from 'widgets/ClusterConfigForm/common/SectionHeader';
import SSLForm from 'widgets/ClusterConfigForm/common/SSLForm';
import Credentials from 'widgets/ClusterConfigForm/common/Credentials';

const KSQL = () => {
  const { setValue } = useFormContext();
  const [configOpen, setConfigOpen] = useState(false);
  const toggleConfig = () => {
    // Branch on the direction of the toggle, not on whether `ksql` is already
    // set. `ksql` stays truthy after a remove (it holds `{ isActive: false }`),
    // so keying off it left the section inactive when it was reopened.
    const willOpen = !configOpen;
    setConfigOpen(willOpen);
    setValue(
      'ksql',
      willOpen
        ? { isActive: true, url: '', isAuth: false }
        : { isActive: false },
      {
        shouldValidate: true,
        shouldDirty: true,
        shouldTouch: true,
      }
    );
  };
  return (
    <>
      <SectionHeader
        title="KSQL DB"
        adding={!configOpen}
        addButtonText="Configure KSQL DB"
        onClick={toggleConfig}
      />
      {configOpen && (
        <>
          <Input
            label="URL *"
            name="ksql.url"
            type="text"
            placeholder="http://localhost:8088"
            withError
          />
          <Credentials prefix="ksql" title="Is KSQL DB secured with auth?" />
          <SSLForm prefix="ksql.keystore" title="KSQL DB Keystore" />
        </>
      )}
    </>
  );
};
export default KSQL;
