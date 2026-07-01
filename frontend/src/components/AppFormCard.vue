<template>
  <div class="app-form-card">
    <div v-if="$slots.title || title" class="app-form-card__title">
      <slot name="title">
        <h3 class="app-form-card__heading">{{ title }}</h3>
      </slot>
    </div>
    <div class="app-form-card__body">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title?: string
}>()
</script>

<style scoped>
.app-form-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  overflow: hidden;
  box-shadow: var(--shadow-card);
}

.app-form-card + .app-form-card,
.app-form-card + .app-card,
.app-card + .app-form-card {
  margin-top: 12px;
}

.app-form-card__title {
  padding: 14px;
  border-bottom: 1px solid var(--color-border);
}

.app-form-card__heading {
  margin: 0;
  font-size: 18px;
  font-weight: 900;
  color: var(--color-text-primary);
}

.app-form-card__body {
  padding: 0;
}
</style>
