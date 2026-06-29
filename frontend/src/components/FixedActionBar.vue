<template>
  <div class="fixed-action-bar">
    <slot />
  </div>
</template>

<style scoped>
.fixed-action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 8px 16px;
  padding-bottom: calc(8px + env(safe-area-inset-bottom, 0px));
  background-color: var(--color-bg-white);
  border-top: 1px solid var(--color-border);
  display: flex;
  gap: 12px;
  align-items: center;
  z-index: 100;
}

.fixed-action-bar :deep(.van-button) {
  flex: 1;
}
</style>
