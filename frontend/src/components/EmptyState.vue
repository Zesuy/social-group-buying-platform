<template>
  <div class="empty-state">
    <van-empty :image="image" :description="description">
      <template v-if="$slots.default">
        <slot />
      </template>
    </van-empty>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  description?: string
  image?: string
}>(), {
  description: '暂无内容',
  image: 'default',
})
</script>

<style scoped>
.empty-state {
  padding: 40px 16px;
}
</style>
