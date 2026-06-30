<template>
  <div class="leader-layout">
    <div class="leader-header">
      <van-nav-bar :title="route.meta.title as string" left-arrow @click-left="goBack" />
    </div>
    <div class="leader-content">
      <router-view />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

function goBack() {
  router.back()
}
</script>

<style scoped>
.leader-layout {
  min-height: 100vh;
  min-height: 100dvh;
  background: var(--color-bg);
}

.leader-header :deep(.van-nav-bar) {
  box-shadow: var(--shadow-nav);
}

.leader-content {
  padding: 12px 14px;
}
</style>
