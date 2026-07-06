import type { CapacitorConfig } from '@capacitor/cli'

const config: CapacitorConfig = {
  appId: 'com.zesuy.groupshop',
  appName: '社群团购',
  webDir: 'dist',
  server: {
    cleartext: true,
  },
}

export default config
