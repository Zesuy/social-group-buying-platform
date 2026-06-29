<template>
  <div class="page-layout">
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
  min-height: 100vh;
  background-color: var(--color-bg);
}

.page-layout__content {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

/* NavBar 组件已使用 Vant fixed + placeholder 自动占位，此处无需额外 padding */

.page-layout__content--with-tabbar {
  padding-bottom: 50px;
}

.page-layout__content--with-action {
  padding-bottom: 60px;
}
</style>
