export const standardCompatibilityLevels = [
  'BACKWARD',
  'BACKWARD_TRANSITIVE',
  'FORWARD',
  'FORWARD_TRANSITIVE',
  'FULL',
  'FULL_TRANSITIVE',
  'NONE',
];

export const compatibilityOptions = (current?: string) =>
  [
    ...new Set([...standardCompatibilityLevels, ...(current ? [current] : [])]),
  ].map((level) => ({ value: level, label: level }));
