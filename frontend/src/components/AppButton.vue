<template>
  <button
    :class="[
      'app-button',
      `app-button--${variant}`,
      {
        'app-button--block': block,
        'app-button--loading': loading,
        'app-button--disabled': disabled,
        'app-button--pill': pill,
      },
    ]"
    :disabled="disabled || loading"
    :type="htmlType"
    @click="$emit('click', $event)"
  >
    <van-loading
      v-if="loading"
      class="app-button__loading"
      size="18px"
      color="currentColor"
    />
    <van-icon v-if="icon && !loading" :name="icon" class="app-button__icon" />
    <span v-if="$slots.default || label" class="app-button__text">
      <slot>{{ label }}</slot>
    </span>
  </button>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  label?: string
  variant?: 'primary' | 'ghost' | 'danger' | 'plain' | 'success'
  block?: boolean
  loading?: boolean
  disabled?: boolean
  icon?: string
  htmlType?: 'button' | 'submit' | 'reset'
  pill?: boolean
}>(), {
  variant: 'primary',
})

defineEmits<{
  click: [event: MouseEvent]
}>()
</script>

<style scoped>
.app-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 44px;
  padding: 0 20px;
  border: 1px solid transparent;
  border-radius: 9px;
  font-size: var(--font-size-md);
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
  font-family: inherit;
  line-height: 1.2;
}

.app-button:active:not(:disabled) {
  transform: scale(0.96);
}

.app-button--block {
  display: flex;
  width: 100%;
}

.app-button--disabled,
.app-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.app-button--disabled:active,
.app-button:disabled:active {
  transform: none;
}

/* ── 变体 ── */
.app-button--primary {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

.app-button--ghost {
  background: #fff;
  color: var(--color-text-secondary);
  border-color: var(--color-border);
}

.app-button--danger {
  background: #fff;
  color: var(--van-danger-color, #ee0a24);
  border-color: #ffd3cc;
}

.app-button--plain {
  background: transparent;
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.app-button--success {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

/* ── 胶囊 ── */
.app-button--pill {
  border-radius: var(--radius-pill);
  height: var(--button-capsule-height);
  padding: 0 24px;
  font-size: var(--font-size-lg);
  font-weight: 600;
}

.app-button__loading {
  flex-shrink: 0;
}

.app-button__icon {
  flex-shrink: 0;
  font-size: 18px;
}

.app-button__text {
  min-width: 0;
}
</style>
