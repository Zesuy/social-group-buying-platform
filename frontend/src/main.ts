import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Vant from 'vant'
import 'vant/lib/index.css'

import App from './App.vue'
import router from './router'
import { useAuthStore } from '@/stores'

async function bootstrap() {
  const app = createApp(App)
  const pinia = createPinia()

  app.use(pinia)
  app.use(Vant)

  // ⚡ 在安装路由前恢复登录态，确保路由守卫首次执行时
  //    isBootstrapped 已为 true，不会把有效 token 的用户重定向到 /login
  const authStore = useAuthStore(pinia)
  await authStore.restoreSession()

  app.use(router)
  app.mount('#app')
}

bootstrap()
