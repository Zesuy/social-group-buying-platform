<template>
  <van-nav-bar
    :title="title"
    :left-arrow="showBack"
    @click-left="$emit('back')"
    fixed
    placeholder
    safe-area-inset-top
  />
</template>

<script setup lang="ts">
defineProps<{
  title: string
  showBack?: boolean
}>()

defineEmits<{
  back: []
}>()
</script>
