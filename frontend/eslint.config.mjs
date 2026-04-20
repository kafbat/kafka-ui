// @ts-check
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { FlatCompat } from '@eslint/eslintrc';
import js from '@eslint/js';
import globals from 'globals';
import reactHooks from 'eslint-plugin-react-hooks';
import tseslint from 'typescript-eslint';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const compat = new FlatCompat({
  baseDirectory: __dirname,
  recommendedConfig: js.configs.recommended,
  allConfig: js.configs.all,
});

// Rules removed from @typescript-eslint v8 (formatting/stylistic moved to
// Prettier / ESLint Stylistic, plus a couple of base-language rules).
// airbnb-typescript 18 still references them, so we turn them off here
// and delegate to either Prettier or the base ESLint rule of the same name.
const typescriptEslintV8RemovedRules = {
  '@typescript-eslint/brace-style': 'off',
  '@typescript-eslint/comma-dangle': 'off',
  '@typescript-eslint/comma-spacing': 'off',
  '@typescript-eslint/func-call-spacing': 'off',
  '@typescript-eslint/indent': 'off',
  '@typescript-eslint/keyword-spacing': 'off',
  '@typescript-eslint/lines-between-class-members': 'off',
  '@typescript-eslint/no-extra-parens': 'off',
  '@typescript-eslint/no-extra-semi': 'off',
  '@typescript-eslint/object-curly-spacing': 'off',
  '@typescript-eslint/quotes': 'off',
  '@typescript-eslint/semi': 'off',
  '@typescript-eslint/space-before-blocks': 'off',
  '@typescript-eslint/space-before-function-paren': 'off',
  '@typescript-eslint/space-infix-ops': 'off',
  '@typescript-eslint/no-loss-of-precision': 'off',
  '@typescript-eslint/no-throw-literal': 'off',
};

export default tseslint.config(
  {
    ignores: [
      'src/generated-sources/**',
      'node_modules/**',
      'dist/**',
      'build/**',
      'coverage/**',
    ],
  },
  ...compat.extends(
    'airbnb',
    'airbnb-typescript',
    'plugin:@typescript-eslint/recommended',
    'plugin:jest-dom/recommended',
    'plugin:prettier/recommended',
    'eslint:recommended',
    'plugin:react/recommended',
    'prettier'
  ),
  {
    plugins: {
      'react-hooks': reactHooks,
    },
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.es2018,
        ...globals.jest,
        Atomics: 'readonly',
        SharedArrayBuffer: 'readonly',
      },
      parserOptions: {
        ecmaFeatures: { jsx: true },
        ecmaVersion: 2018,
        sourceType: 'module',
        projectService: true,
        tsconfigRootDir: __dirname,
      },
    },
    rules: {
      ...typescriptEslintV8RemovedRules,
      'react/no-unused-prop-types': 'off',
      'react/require-default-props': 'off',
      'prettier/prettier': 'warn',
      '@typescript-eslint/explicit-module-boundary-types': 'off',
      'jsx-a11y/label-has-associated-control': 'off',
      'import/prefer-default-export': 'off',
      '@typescript-eslint/no-explicit-any': 'error',
      'react-hooks/rules-of-hooks': 'error',
      'react-hooks/exhaustive-deps': 'off',
      'jsx-a11y/control-has-associated-label': 'off',
      'import/no-extraneous-dependencies': [
        'error',
        { devDependencies: true },
      ],
      'import/no-cycle': 'error',
      'import/order': [
        'error',
        {
          groups: ['builtin', 'external', 'parent', 'sibling', 'index'],
          'newlines-between': 'always',
        },
      ],
      'import/no-relative-parent-imports': 'error',
      'no-debugger': 'warn',
      'react/jsx-props-no-spreading': 'off',
      'no-param-reassign': [
        'error',
        {
          props: true,
          ignorePropertyModificationsFor: ['state', 'acc', 'accumulator'],
        },
      ],
      'react/function-component-definition': [
        2,
        {
          namedComponents: ['arrow-function', 'function-declaration'],
          unnamedComponents: 'arrow-function',
        },
      ],
      'react/jsx-no-constructed-context-values': 'off',
      'react/display-name': 'off',
    },
  },
  {
    files: ['**/*.tsx'],
    rules: {
      'react/prop-types': 'off',
    },
  },
  {
    files: ['**/*.spec.tsx'],
    rules: {
      'react/jsx-props-no-spreading': 'off',
    },
  }
);
