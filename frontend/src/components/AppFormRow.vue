<template>
  <div
    :class="[
      'app-form-row',
      {
        'app-form-row--clickable': clickable,
        'app-form-row--disabled': disabled,
      },
    ]"
    @click="clickable ? $emit('click') : undefined"
  >
    <label v-if="$slots.label || label" class="app-form-row__label">
      <slot name="label">{{ label }}</slot>
    </label>
    <div class="app-form-row__value" :class="{ 'app-form-row__value--empty': !value && !$slots.default }">
      <slot>{{ value }}</slot>
    </div>
    <div v-if="$slots.control" class="app-form-row__control">
      <slot name="control" />
    </div>
    <van-icon
      v-if="arrow"
      name="arrow"
      class="app-form-row__arrow"
    />
    <div v-if="error" class="app-form-row__error">
      {{ error }}
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  label?: string
  value?: string
  arrow?: boolean
  clickable?: boolean
  disabled?: boolean
  error?: string
}>()

defineEmits<{
  click: []
}>()
</script>

<style scoped>
.app-form-row {
  display: grid;
  grid-template-columns: 94px 1fr auto;
  gap: 8px;
  align-items: center;
  padding: 13px 14px;
  border-bottom: 1px solid var(--color-border);
  min-height: 52px;
  position: relative;
}

.app-form-row:last-child {
  border-bottom: none;
}

.app-form-row--clickable {
  cursor: pointer;
}

.app-form-row--clickable:active {
  background: var(--color-bg-surface);
}

.app-form-row--disabled {
  opacity: 0.6;
}

.app-form-row__label {
  color: var(--color-text-primary);
  font-weight: 700;
  font-size: var(--font-size-md);
  min-width: 0;
  word-break: break-word;
}

.app-form-row__value {
  color: var(--color-text-secondary);
  font-size: var(--font-size-md);
  min-width: 0;
  word-break: break-word;
}

.app-form-row__value--empty {
  color: var(--color-text-placeholder);
}

.app-form-row__control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.app-form-row__arrow {
  color: var(--color-text-hint);
  font-size: 14px;
}

.app-form-row__error {
  grid-column: 1 / -1;
  color: var(--van-danger-color, #ee0a24);
  font-size: var(--font-size-sm);
  padding: 4px 0 0;
}
</style>
