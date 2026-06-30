<template>
  <div class="fixed-action-bar">
    <slot />
  </div>
</template>

<style scoped>
.fixed-action-bar {
  flex-shrink: 0;
  padding: 10px 14px;
  padding-bottom: calc(16px + var(--safe-area-bottom));
  background-color: var(--color-bg-white);
  border-top: 1px solid var(--color-border);
  display: flex;
  gap: 12px;
  align-items: center;
  z-index: 100;
  box-shadow: 0 -8px 24px rgba(20, 28, 40, 0.08);
}

.fixed-action-bar :deep(.van-button) {
  flex: 1;
  height: var(--button-capsule-height);
  border-radius: 8px !important;
  font-size: 17px;
  font-weight: 900;
}

.fixed-action-bar :deep(.fixed-actions) {
  position: static;
  width: 100%;
  padding: 0;
  border-top: 0;
  box-shadow: none;
}
</style>
