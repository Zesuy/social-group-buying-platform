<template>
  <div
    class="page-layout"
    :class="{
      'page-layout--with-tabbar': showTabBar,
      'page-layout--with-action': !!$slots.action,
    }"
  >
    <!-- 顶部导航栏（可选） -->
    <NavBar v-if="title" :title="title" :show-back="showBack" @back="$emit('back')" />

    <!-- 主内容区 -->
    <main
      class="page-layout__content"
      :class="{
        'page-layout__content--with-nav': !!title,
        'page-layout__content--with-tabbar': showTabBar,
        'page-layout__content--with-action': !!$slots.action,
      }"
    >
      <slot />
    </main>

    <!-- 底部操作栏（可选） -->
    <FixedActionBar v-if="$slots.action">
      <slot name="action" />
    </FixedActionBar>
  </div>
</template>

<script setup lang="ts">
import NavBar from './NavBar.vue'
import FixedActionBar from './FixedActionBar.vue'

defineProps<{
  title?: string
  showBack?: boolean
  showTabBar?: boolean
}>()

defineEmits<{
  back: []
}>()
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

/* NavBar 作为 flex 子项在正常文档流中；内容区域 flex:1 填充剩余高度 */

.page-layout__content--with-tabbar {
  scroll-padding-bottom: 14px;
}

.page-layout__content--with-action {
  scroll-padding-bottom: 14px;
}
</style>
