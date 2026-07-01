<template>
  <AppCard>
    <template #header>
      <slot name="header">
        <span>{{ title }}</span>
      </slot>
    </template>
    <template #header-right>
      <slot name="header-right" />
    </template>

    <slot />
  </AppCard>
</template>

<script setup lang="ts">
import AppCard from './AppCard.vue'

defineProps<{
  title?: string
}>()
</script>

<style scoped>
/* Styling is provided by AppCard */
</style>
