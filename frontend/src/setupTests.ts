/* eslint-disable no-console */
import 'whatwg-fetch';
import 'jest-styled-components';
import '@testing-library/jest-dom/jest-globals';

// React 18 + RTL: async state updates from react-query, react-select,
// and suspense boundaries trigger act() warnings even in passing tests.
// Filter known-benign patterns so real errors stay visible.
const SUPPRESSED_ERRORS = [
  'not wrapped in act(...)',
  'suspended resource finished loading inside a test',
];

const SUPPRESSED_WARNS = ['The `punycode` module is deprecated'];

const originalError = console.error.bind(console);
const originalWarn = console.warn.bind(console);

beforeAll(() => {
  console.error = (...args: unknown[]) => {
    const msg = typeof args[0] === 'string' ? args[0] : '';
    if (SUPPRESSED_ERRORS.some((pattern) => msg.includes(pattern))) return;
    originalError(...args);
  };

  console.warn = (...args: unknown[]) => {
    const msg = typeof args[0] === 'string' ? args[0] : '';
    if (SUPPRESSED_WARNS.some((pattern) => msg.includes(pattern))) return;
    originalWarn(...args);
  };
});

afterAll(() => {
  console.error = originalError;
  console.warn = originalWarn;
});
