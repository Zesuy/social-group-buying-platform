<template>
  <div
    class="image-uploader"
    :class="[
      `image-uploader--${variant}`,
      { 'image-uploader--with-url': showUrlInput },
    ]"
  >
    <ImageWithFallback
      class="image-uploader__preview"
      :src="modelValue"
      :alt="variant === 'tile' ? '' : previewAlt"
      :demo-kind="demoKind"
      :width="variant === 'tile' ? '100%' : '72px'"
      :height="variant === 'tile' ? '100%' : '72px'"
      :radius="variant === 'tile' ? '10px' : '8px'"
    />
    <button
      v-if="variant === 'tile'"
      type="button"
      class="image-uploader__tile-action"
      :aria-label="uploading ? '图片上传中' : `${tileLabel}图片`"
      :disabled="disabled || uploading"
      @click="openFilePicker"
    >
      <van-icon name="photo-o" />
    </button>
    <div v-else class="image-uploader__body">
      <input
        v-if="showUrlInput"
        class="image-uploader__input"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled || uploading"
        @input="onUrlInput"
      />
      <div class="image-uploader__actions">
        <AppButton
          variant="ghost"
          icon="photo"
          :loading="uploading"
          :disabled="disabled"
          @click="openFilePicker"
        >
          {{ buttonLabel }}
        </AppButton>
        <span v-if="showHint" class="image-uploader__hint">jpg / png / webp，5MB 内</span>
      </div>
    </div>
    <input
      ref="fileInputRef"
      class="image-uploader__file"
      type="file"
      accept="image/jpeg,image/png,image/webp"
      :disabled="disabled || uploading"
      @change="onFileChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { showToast } from 'vant'
import AppButton from './AppButton.vue'
import ImageWithFallback from './ImageWithFallback.vue'
import { uploadImage } from '@/api/uploads'
import type { ImageUploadData } from '@/types'

const props = withDefaults(defineProps<{
  modelValue?: string
  placeholder?: string
  previewAlt?: string
  demoKind?: 'product' | 'cover' | 'avatar' | 'store'
  disabled?: boolean
  showUrlInput?: boolean
  showHint?: boolean
  variant?: 'row' | 'tile'
  buttonLabel?: string
  tileLabel?: string
}>(), {
  modelValue: '',
  placeholder: '可选，输入图片 URL',
  previewAlt: '图片预览',
  demoKind: 'cover',
  disabled: false,
  showUrlInput: true,
  showHint: true,
  variant: 'row',
  buttonLabel: '选择图片',
  tileLabel: '更换',
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  uploaded: [data: ImageUploadData]
}>()

const fileInputRef = ref<HTMLInputElement | null>(null)
const uploading = ref(false)

function onUrlInput(event: Event) {
  emit('update:modelValue', (event.target as HTMLInputElement).value)
}

function openFilePicker() {
  if (props.disabled || uploading.value) return
  fileInputRef.value?.click()
}

async function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  uploading.value = true
  try {
    const data = await uploadImage(file)
    emit('update:modelValue', data.url)
    emit('uploaded', data)
    showToast('图片已上传')
  } catch (err) {
    const apiErr = err as { message?: string }
    showToast(apiErr.message || '图片上传失败')
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.image-uploader {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 10px;
  align-items: center;
  width: 100%;
  min-width: 0;
}

.image-uploader--tile {
  position: relative;
  display: block;
  aspect-ratio: 1;
  min-height: 0;
  overflow: hidden;
  border-radius: 10px;
  background: var(--color-bg-surface);
}

.image-uploader__preview {
  background: var(--color-bg-surface);
}

.image-uploader--tile .image-uploader__preview {
  width: 100%;
  height: 100%;
}

.image-uploader__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.image-uploader__input {
  width: 100%;
  min-width: 0;
  height: 42px;
  padding: 0 10px;
  border: 1px solid var(--color-border-light);
  border-radius: 8px;
  outline: none;
  background: #f7f8fa;
  color: var(--color-text-primary);
  font-size: var(--font-size-md);
}

.image-uploader__input:focus {
  border-color: var(--color-primary);
}

.image-uploader__input:disabled {
  opacity: 0.55;
}

.image-uploader__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.image-uploader__tile-action {
  position: absolute;
  left: 6px;
  bottom: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  min-height: 30px;
  min-width: 30px;
  max-width: 30px;
  max-height: 30px;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.82);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  color: var(--color-text-primary);
  font-family: inherit;
  font-size: 16px;
  font-weight: 700;
  line-height: 1;
  appearance: none;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.12);
}

.image-uploader__tile-action:disabled {
  opacity: 0.6;
}

.image-uploader__hint {
  min-width: 0;
  color: var(--color-text-hint);
  font-size: var(--font-size-sm);
  line-height: 1.4;
}

.image-uploader__file {
  display: none;
}

@media (max-width: 360px) {
  .image-uploader {
    grid-template-columns: 64px 1fr;
  }

  .image-uploader__actions {
    flex-wrap: wrap;
  }
}
</style>
