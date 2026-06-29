<template>
  <div class="image-with-fallback" :style="containerStyle" @click="$emit('click')">
    <img
      v-if="imgSrc"
      :src="imgSrc"
      :alt="alt"
      :style="imgStyle"
      class="image-with-fallback__img"
      @error="onError"
    />
    <div v-else class="image-with-fallback__placeholder">
      <van-icon name="photo" :size="iconSize" color="var(--color-text-placeholder)" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

const props = withDefaults(defineProps<{
  src?: string | null
  fallbackSrc?: string
  alt?: string
  width?: string
  height?: string
  radius?: string
  fit?: 'cover' | 'contain' | 'fill'
}>(), {
  alt: '',
  fit: 'cover',
  width: '100%',
  height: '100%',
})

defineEmits<{ click: [] }>()

const hasError = ref(false)

const imgSrc = computed(() => {
  if (hasError.value) return props.fallbackSrc || null
  return props.src || null
})

const containerStyle = computed(() => ({
  width: props.width,
  height: props.height,
  borderRadius: props.radius || 'var(--radius-sm)',
  overflow: 'hidden',
  backgroundColor: 'var(--color-border-light)',
}))

const imgStyle = computed(() => ({
  width: '100%',
  height: '100%',
  objectFit: props.fit,
}))

const iconSize = computed(() => {
  const w = parseInt(props.width)
  return isNaN(w) ? '32px' : `${Math.min(w, 48)}px`
})

function onError() {
  hasError.value = true
}
</script>

<style scoped>
.image-with-fallback {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.image-with-fallback__img {
  display: block;
}

.image-with-fallback__placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}
</style>
