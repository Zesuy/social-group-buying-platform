import type { CapacitorConfig } from '@capacitor/cli'

const config: CapacitorConfig = {
  appId: 'com.zesuy.groupshop',
  appName: '邻鲜团',
  webDir: 'dist',
  server: {
    url: 'https://shop.zesuy.top',
    cleartext: true,
  },
}

export default config
