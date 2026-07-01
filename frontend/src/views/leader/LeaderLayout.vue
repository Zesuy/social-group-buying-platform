<template>
  <div class="leader-layout">
    <router-view />
  </div>
</template>

<script setup lang="ts">
// 子视图通过各自的 PageLayout 管理布局和导航栏
</script>

<style scoped>
.leader-layout {
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  background: var(--color-bg);
}
</style>
