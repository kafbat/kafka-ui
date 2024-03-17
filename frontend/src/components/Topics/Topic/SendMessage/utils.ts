import {
  Partition,
  SerdeDescription,
  TopicSerdeSuggestion,
} from 'generated-sources';
import jsf from 'json-schema-faker';
import Ajv, { DefinedError } from 'ajv/dist/2020';
import addFormats from 'ajv-formats';
import upperFirst from 'lodash/upperFirst';

jsf.option('fillProperties', false);
jsf.option('alwaysFakeOptionals', true);
jsf.option('failOnInvalidFormat', false);

const generateValueFromSchema = (preferred?: SerdeDescription) => {
  if (!preferred?.schema) {
    return undefined;
  }
  const parsedSchema = JSON.parse(preferred.schema);
  const value = jsf.generate(parsedSchema);
  return JSON.stringify(value);
};

export const getPreferredDescription = (serdes: SerdeDescription[]) =>
  serdes.find((s) => s.preferred);

export const getDefaultValues = (serdes: TopicSerdeSuggestion) => {
  const keySerde = getPreferredDescription(serdes.key || []);
  const valueSerde = getPreferredDescription(serdes.value || []);

  return {
    key: generateValueFromSchema(keySerde),
    content: generateValueFromSchema(valueSerde),
    headers: undefined,
    partition: undefined,
    keySerde: keySerde?.name,
    valueSerde: valueSerde?.name,
  };
};

export const getPartitionOptions = (partitions: Partition[]) =>
  partitions.map(({ partition }) => ({
    label: `Partition #${partition}`,
    value: partition,
  }));

export const getSerdeOptions = (items: SerdeDescription[]) => {
  return items.reduce<{ label: string; value: string }[]>((acc, { name }) => {
    if (name) {
      acc.push({ value: name, label: name });
    }
    return acc;
  }, []);
};

export const validateBySchema = (
  value: string,
  schema: string | undefined,
  type: 'key' | 'content'
) => {
  let errors: string[] = [];

  if (!value || !schema) {
    return errors;
  }

  let parsedSchema;
  let parsedValue;

  try {
    parsedSchema = JSON.parse(schema);
  } catch (e) {
    return [`Error in parsing the "${type}" field schema`];
  }
  if (parsedSchema.type === 'string') {
    return [];
  }
  try {
    parsedValue = JSON.parse(value);
  } catch (e) {
    return [`Error in parsing the "${type}" field value`];
  }
  try {
    const ajv = new Ajv();
    addFormats(ajv);
    const validate = ajv.compile(parsedSchema);
    validate(parsedValue);
    if (validate.errors) {
      errors = validate.errors.map(
        ({ schemaPath, message }) =>
          `${schemaPath.replace('#', upperFirst(type))} - ${message}`
      );
    }
  } catch (e) {
    const err = e as DefinedError;
    return [`${upperFirst(type)} ${err.message}`];
  }

  return errors;
};
