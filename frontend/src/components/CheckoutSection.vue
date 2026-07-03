<template>
  <AppCard :clickable="clickable" @click="$emit('click')">
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

withDefaults(defineProps<{
  title?: string
  clickable?: boolean
}>(), {
  clickable: false,
})

defineEmits<{
  click: []
}>()
</script>

<style scoped>
/* Styling is provided by AppCard */
</style>
