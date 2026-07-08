<template>
  <div
    class="page-layout"
    :class="{
      'page-layout--with-tabbar': showTabBar,
      'page-layout--with-action': !!$slots.action,
    }"
  >
    <!-- 顶部导航栏（可选） -->
    <NavBar v-if="title" :title="title" :show-back="showBack" @back="handleBack" />

    <!-- 主内容区 -->
    <main
      ref="contentRef"
      class="page-layout__content"
      :class="{
        'page-layout__content--with-nav': !!title,
        'page-layout__content--with-tabbar': showTabBar,
        'page-layout__content--with-action': !!$slots.action,
      }"
    >
      <slot />
    </main>

    <!-- 底部操作栏（可选）— 不做额外包装，由调用方提供 AppFixedActions -->
    <slot name="action" />
  </div>
</template>

<script setup lang="ts">
import { getCurrentInstance, ref } from 'vue'
import type { RouteLocationRaw } from 'vue-router'
import NavBar from './NavBar.vue'
import { useRouteScrollRestoration, useSmartNavigation } from '@/composables'

const props = defineProps<{
  title?: string
  showBack?: boolean
  showTabBar?: boolean
  backFallback?: RouteLocationRaw | string
}>()

const emit = defineEmits<{
  back: []
}>()

const contentRef = ref<HTMLElement | null>(null)
const instance = getCurrentInstance()
const navigation = useSmartNavigation(props.backFallback)

useRouteScrollRestoration(contentRef)

function handleBack() {
  if (instance?.vnode.props && 'onBack' in instance.vnode.props) {
    emit('back')
    return
  }
  navigation.goBack(props.backFallback)
}
</script>

<style scoped>
.page-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  background-color: var(--color-bg);
}

.page-layout--with-tabbar {
  height: calc(100vh - var(--tabbar-height) - var(--safe-area-bottom));
  height: calc(100dvh - var(--tabbar-height) - var(--safe-area-bottom));
}

.page-layout__content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  background-color: var(--color-bg);
}

.page-layout__content--with-action {
  padding-bottom: calc(var(--actionbar-height) + var(--safe-area-bottom));
}
</style>
