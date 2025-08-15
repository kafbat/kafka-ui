import React, { useState } from 'react';
import Input from 'components/common/Input/Input';
import { useFormContext } from 'react-hook-form';
import SectionHeader from 'widgets/ClusterConfigForm/common/SectionHeader';
import SSLForm from 'widgets/ClusterConfigForm/common/SSLForm';
import Credentials from 'widgets/ClusterConfigForm/common/Credentials';

const SchemaRegistry = () => {
  const { setValue, watch } = useFormContext();
  const schemaRegistry = watch('schemaRegistry');
  const [configOpen, setConfigOpen] = useState(false);
  const toggleConfig = () => {
    setConfigOpen((prevConfigOpen) => !prevConfigOpen);
    setValue(
      'schemaRegistry',
      schemaRegistry
        ? { isActive: false }
        : { isActive: true, url: '', isAuth: false },
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
        title="Schema Registry"
        adding={!configOpen}
        addButtonText="Configure Schema Registry"
        onClick={toggleConfig}
      />
      {configOpen && (
        <>
          <Input
            label="URL *"
            name="schemaRegistry.url"
            type="text"
            placeholder="http://localhost:8081"
            withError
          />
          <Credentials
            prefix="schemaRegistry"
            title="Is Schema Registry secured with auth?"
          />
          <SSLForm prefix="schemaRegistry.keystore" title="Keystore" />
        </>
      )}
    </>
  );
};
export default SchemaRegistry;
