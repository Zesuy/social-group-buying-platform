<template>
  <div class="content-block-editor">
    <div class="content-block-editor__tools">
      <button
        v-for="option in blockOptions"
        :key="option.type"
        type="button"
        :disabled="props.disabled"
        @click="addBlock(option.type)"
      >
        <van-icon :name="option.icon" />
        {{ option.label }}
      </button>
    </div>

    <div v-if="blocks.length > 0" class="content-block-editor__list">
      <article v-for="(block, index) in blocks" :key="index" class="content-block-card">
        <header>
          <strong>{{ index + 1 }}. {{ contentBlockTypeText(block.type) }}</strong>
          <span>
            <button type="button" :disabled="props.disabled || index === 0" @click="moveBlock(index, -1)">上移</button>
            <button type="button" :disabled="props.disabled || index === blocks.length - 1" @click="moveBlock(index, 1)">下移</button>
            <button type="button" :disabled="props.disabled" @click="removeBlock(index)">删除</button>
          </span>
        </header>

        <label v-if="block.type === 'paragraph' || block.type === 'deliveryNote'" class="editor-field">
          <span>{{ block.type === 'deliveryNote' ? '发货和履约说明' : '说明文字' }}</span>
          <textarea
            :value="block.text || ''"
            rows="4"
            :disabled="props.disabled"
            :placeholder="block.type === 'deliveryNote' ? '例如：截单后 48 小时内发货，坏果包赔' : '写清楚推荐理由、口感、产地或购买提醒'"
            @input="updateBlock(index, { text: inputValue($event) })"
          />
        </label>

        <template v-else-if="block.type === 'section'">
          <label class="editor-field">
            <span>小标题</span>
            <input
              :value="block.title || ''"
              :disabled="props.disabled"
              placeholder="例如：为什么推荐"
              @input="updateBlock(index, { title: inputValue($event) })"
            />
          </label>
          <label class="editor-field">
            <span>说明内容</span>
            <textarea
              :value="block.text || ''"
              rows="4"
              :disabled="props.disabled"
              placeholder="补充这部分活动说明"
              @input="updateBlock(index, { text: inputValue($event) })"
            />
          </label>
        </template>

        <template v-else-if="block.type === 'image'">
          <div class="editor-field">
            <span>图片</span>
            <ImageUploader
              :model-value="block.url || ''"
              :disabled="props.disabled"
              :preview-alt="block.caption || '内容图片'"
              demo-kind="cover"
              :show-url-input="false"
              :show-hint="false"
              button-label="上传图片"
              @update:model-value="(value) => updateBlock(index, { url: value })"
            />
          </div>
          <label class="editor-field">
            <span>图片说明</span>
            <input
              :value="block.caption || ''"
              :disabled="props.disabled"
              placeholder="可选，说明图片内容"
              @input="updateBlock(index, { caption: inputValue($event) })"
            />
          </label>
        </template>

        <template v-else-if="block.type === 'list'">
          <label class="editor-field">
            <span>列表标题</span>
            <input
              :value="block.title || ''"
              :disabled="props.disabled"
              placeholder="例如：推荐理由"
              @input="updateBlock(index, { title: inputValue($event) })"
            />
          </label>
          <label class="editor-field">
            <span>列表项</span>
            <textarea
              :value="(block.items || []).join('\n')"
              rows="5"
              :disabled="props.disabled"
              placeholder="每行一个要点"
              @input="updateBlock(index, { items: inputValue($event).split('\n') })"
            />
          </label>
        </template>
      </article>
    </div>

    <div v-else class="content-block-editor__empty">
      添加文字、图片、列表或发货说明，让团购详情页不只是一段简介。
    </div>
  </div>
</template>

<script setup lang="ts">
import ImageUploader from '@/components/ImageUploader.vue'
import { contentBlockTypeText, createContentBlock } from '@/utils'
import type { ContentBlockData } from '@/types'

const props = withDefaults(defineProps<{
  disabled?: boolean
}>(), {
  disabled: false,
})

const blocks = defineModel<ContentBlockData[]>({ default: () => [] })

const blockOptions: Array<{ type: ContentBlockData['type']; label: string; icon: string }> = [
  { type: 'paragraph', label: '文字', icon: 'description' },
  { type: 'section', label: '小标题', icon: 'label-o' },
  { type: 'image', label: '图片', icon: 'photo-o' },
  { type: 'list', label: '要点', icon: 'orders-o' },
  { type: 'deliveryNote', label: '发货', icon: 'logistics' },
]

function addBlock(type: ContentBlockData['type']) {
  if (props.disabled) return
  if (blocks.value.length >= 20) return
  blocks.value = [...blocks.value, createContentBlock(type)]
}

function updateBlock(index: number, patch: Partial<ContentBlockData>) {
  if (props.disabled) return
  blocks.value = blocks.value.map((block, itemIndex) => (
    itemIndex === index ? { ...block, ...patch } : block
  ))
}

function moveBlock(index: number, offset: -1 | 1) {
  if (props.disabled) return
  const targetIndex = index + offset
  if (targetIndex < 0 || targetIndex >= blocks.value.length) return
  const next = [...blocks.value]
  const [current] = next.splice(index, 1)
  next.splice(targetIndex, 0, current)
  blocks.value = next
}

function removeBlock(index: number) {
  if (props.disabled) return
  blocks.value = blocks.value.filter((_, itemIndex) => itemIndex !== index)
}

function inputValue(event: Event): string {
  return (event.target as HTMLInputElement | HTMLTextAreaElement).value
}
</script>

<style scoped>
.content-block-editor {
  display: grid;
  gap: 12px;
}

.content-block-editor__tools {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.content-block-editor__tools button,
.content-block-card header button {
  min-height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 0 10px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #fff;
  color: #374151;
  font: inherit;
  font-size: 12px;
  font-weight: 900;
  cursor: pointer;
}

.content-block-card header button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.content-block-editor__list {
  display: grid;
  gap: 10px;
}

.content-block-card {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
}

.content-block-card header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.content-block-card header strong {
  color: #111827;
  font-size: 14px;
}

.content-block-card header span {
  display: flex;
  gap: 6px;
}

.editor-field {
  display: grid;
  gap: 8px;
}

.editor-field span {
  color: #374151;
  font-size: 13px;
  font-weight: 900;
}

.editor-field input,
.editor-field textarea {
  width: 100%;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 10px 12px;
  background: #fff;
  color: #111827;
  font-size: 14px;
  outline: 0;
}

.editor-field textarea {
  resize: vertical;
  line-height: 1.55;
}

.editor-field input:disabled,
.editor-field textarea:disabled {
  background: #f3f4f6;
  color: #6b7280;
  cursor: not-allowed;
}

.content-block-editor__empty {
  padding: 14px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: #f9fafb;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
}
</style>
