<template>
  <div
    :class="[
      'app-card',
      {
        'app-card--clickable': clickable,
        'app-card--flush': flush,
        'app-card--no-pad': !pad,
      },
    ]"
    @click="clickable ? $emit('click') : undefined"
  >
    <div v-if="$slots.header || title" class="app-card__header">
      <slot name="header">
        <h3 v-if="title" class="app-card__title">{{ title }}</h3>
      </slot>
      <slot name="header-right" />
    </div>
    <div class="app-card__body">
      <slot />
    </div>
    <div v-if="$slots.footer" class="app-card__footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  title?: string
  clickable?: boolean
  flush?: boolean
  pad?: boolean
}>(), {
  pad: true,
})

defineEmits<{
  click: []
}>()
</script>

<style scoped>
.app-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  overflow: hidden;
  box-shadow: var(--shadow-card);
}

.app-card + .app-card {
  margin-top: 12px;
}

.app-card--clickable {
  cursor: pointer;
}

.app-card--clickable:active {
  opacity: 0.85;
}

.app-card--flush {
  border-radius: 0;
  box-shadow: none;
}

.app-card--no-pad .app-card__body {
  padding: 0;
}

.app-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px;
  border-bottom: 1px solid var(--color-border);
  gap: 12px;
}

.app-card__title {
  margin: 0;
  font-size: 18px;
  font-weight: 900;
  color: var(--color-text-primary);
  line-height: 1.35;
}

.app-card__body {
  padding: 14px;
}

.app-card__footer {
  padding: 0 14px 14px;
  border-top: 1px solid var(--color-border);
  margin-top: 0;
  padding-top: 12px;
}
</style>
