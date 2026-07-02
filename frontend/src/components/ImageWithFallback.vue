<template>
  <div class="image-with-fallback" :style="containerStyle" @click="$emit('click')">
    <img
      v-if="imgSrc"
      :src="imgSrc"
      :alt="alt"
      :style="imgStyle"
      class="image-with-fallback__img"
      @error="onError"
      @load="onLoad"
    />
    <div
      v-else
      :class="['image-with-fallback__placeholder', { 'image-with-fallback__placeholder--named': placeholderText }]"
    >
      <span v-if="placeholderText" class="image-with-fallback__placeholder-text">
        {{ placeholderText }}
      </span>
      <van-icon v-else name="photo" :size="iconSize" color="var(--color-text-placeholder)" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { resolveDisplayImageUrl } from '@/utils/demo-images'

const props = withDefaults(defineProps<{
  src?: string | null
  fallbackSrc?: string
  alt?: string
  width?: string
  height?: string
  radius?: string
  fit?: 'cover' | 'contain' | 'fill'
  demoKind?: 'product' | 'cover' | 'avatar' | 'store'
}>(), {
  alt: '',
  fit: 'cover',
  width: '100%',
  height: '100%',
  demoKind: 'product',
})

defineEmits<{ click: [] }>()

const hasError = ref(false)
const loaded = ref(false)

const imgSrc = computed(() => {
  if (hasError.value) return resolveDisplayImageUrl(props.fallbackSrc, props.alt, props.demoKind)
  return resolveDisplayImageUrl(props.src, props.alt, props.demoKind)
})

const placeholderText = computed(() => props.alt.trim())

watch(() => props.src, () => {
  hasError.value = false
  loaded.value = false
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
  opacity: loaded.value ? 1 : 0,
  transition: 'opacity 0.3s ease-in',
}))

const iconSize = computed(() => {
  const w = parseInt(props.width)
  return isNaN(w) ? '32px' : `${Math.min(w, 48)}px`
})

function onError() {
  hasError.value = true
}

function onLoad() {
  loaded.value = true
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

.image-with-fallback__placeholder--named {
  padding: 8px;
  background:
    linear-gradient(145deg, rgba(255, 210, 115, 0.92), rgba(85, 170, 93, 0.92)),
    var(--color-border-light);
  color: #fff;
  font-size: 13px;
  font-weight: 900;
  line-height: 1.25;
  text-align: center;
}

.image-with-fallback__placeholder-text {
  display: -webkit-box;
  max-width: 100%;
  overflow: hidden;
  text-shadow: 0 1px 5px rgba(0, 0, 0, 0.18);
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
</style>
