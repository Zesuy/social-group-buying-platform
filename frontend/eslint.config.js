import pluginVue from 'eslint-plugin-vue'
import vueTsEslintConfig from '@vue/eslint-config-typescript'

export default [
  {
    ignores: ['android/', 'dist/', 'node_modules/', '*.local'],
  },
  ...pluginVue.configs['flat/essential'],
  ...vueTsEslintConfig(),
  {
    rules: {
      'vue/multi-word-component-names': 'off',
      '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
    },
  },
]
