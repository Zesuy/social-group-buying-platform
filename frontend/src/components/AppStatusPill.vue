<template>
  <span
    :class="[
      'app-status-pill',
      `app-status-pill--${variant}`,
      {
        'app-status-pill--dot': dot,
        'app-status-pill--sm': size === 'sm',
      },
    ]"
  >
    <span v-if="dot" class="app-status-pill__dot" />
    <slot>{{ label }}</slot>
  </span>
</template>

<script setup lang="ts">
defineProps<{
  label?: string
  variant?: 'green' | 'orange' | 'gray' | 'red'
  dot?: boolean
  size?: 'sm' | 'md'
}>()
</script>

<style scoped>
.app-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: var(--font-size-sm);
  font-weight: 700;
  line-height: 1.2;
  white-space: nowrap;
  flex-shrink: 0;
}

/* ── 变体 ── */
.app-status-pill--green {
  background: #eafaf1;
  color: var(--color-primary);
}

.app-status-pill--orange {
  background: #fff2e8;
  color: #f36b2a;
}

.app-status-pill--gray {
  background: #f2f4f5;
  color: var(--color-text-hint);
}

.app-status-pill--red {
  background: #fef0ef;
  color: var(--van-danger-color, #ee0a24);
}

/* ── 点模式 ── */
.app-status-pill--dot {
  background: transparent;
  padding: 0;
  font-weight: 400;
  color: var(--color-text-secondary);
  gap: 6px;
}

.app-status-pill__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.app-status-pill--green .app-status-pill__dot {
  background: var(--color-primary);
}

.app-status-pill--orange .app-status-pill__dot {
  background: #f36b2a;
}

.app-status-pill--gray .app-status-pill__dot {
  background: #c4c9d0;
}

.app-status-pill--red .app-status-pill__dot {
  background: var(--van-danger-color, #ee0a24);
}

/* ── 小号 ── */
.app-status-pill--sm {
  font-size: 11px;
  padding: 2px 8px;
}
</style>
