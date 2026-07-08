<template>
  <div class="content-blocks-preview">
    <article
      v-for="(block, index) in normalizedBlocks"
      :key="`${block.type}-${index}`"
      class="content-blocks-preview__block"
      :class="`content-blocks-preview__block--${block.type}`"
    >
      <span class="content-blocks-preview__label">{{ contentBlockTypeText(block.type) }}</span>
      <strong v-if="block.title">{{ block.title }}</strong>
      <p v-if="block.text">{{ block.text }}</p>
      <figure v-if="block.type === 'image' && block.url">
        <img :src="block.url" :alt="block.caption || '活动图片'" />
        <figcaption v-if="block.caption">{{ block.caption }}</figcaption>
      </figure>
      <ul v-if="block.items?.length">
        <li v-for="item in block.items" :key="item">{{ item }}</li>
      </ul>
    </article>

    <div v-if="normalizedBlocks.length === 0" class="content-blocks-preview__empty">
      暂无活动正文
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { contentBlockTypeText, normalizeContentBlocks } from '@/utils'
import type { ContentBlockData } from '@/types'

const props = defineProps<{
  blocks?: ContentBlockData[]
}>()

const normalizedBlocks = computed(() => normalizeContentBlocks(props.blocks))
</script>

<style scoped>
.content-blocks-preview {
  display: grid;
  gap: 10px;
}

.content-blocks-preview__block {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
  background: var(--color-bg-surface, #f9fafb);
}

.content-blocks-preview__label {
  color: var(--color-text-hint, #9ca3af);
  font-size: 12px;
  font-weight: 800;
}

.content-blocks-preview__block strong {
  color: var(--color-text-primary, #111827);
  font-size: 14px;
  line-height: 1.45;
}

.content-blocks-preview__block p {
  margin: 0;
  color: var(--color-text-secondary, #4b5563);
  font-size: 14px;
  line-height: 1.65;
}

.content-blocks-preview__block ul {
  display: grid;
  gap: 6px;
  margin: 0;
  padding-left: 18px;
  color: var(--color-text-secondary, #4b5563);
  font-size: 14px;
  line-height: 1.55;
}

.content-blocks-preview__block figure {
  display: grid;
  gap: 6px;
  margin: 0;
}

.content-blocks-preview__block img {
  width: 100%;
  border-radius: 8px;
  object-fit: cover;
}

.content-blocks-preview__block figcaption,
.content-blocks-preview__empty {
  color: var(--color-text-hint, #9ca3af);
  font-size: 12px;
}

.content-blocks-preview__empty {
  padding: 12px;
  border: 1px dashed var(--color-border, #e5e7eb);
  border-radius: 8px;
  text-align: center;
}
</style>
