<template>
  <PageLayout show-tab-bar>
    <div class="page-card">
      <h2>我的</h2>
      <p class="placeholder-text">个人中心将在后续 batch 实现</p>
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import PageLayout from '@/components/PageLayout.vue'
</script>

<style scoped>
.placeholder-text {
  margin-top: 12px;
  color: var(--color-text-hint);
  font-size: var(--font-size-md);
}
</style>
