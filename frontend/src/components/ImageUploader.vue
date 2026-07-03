<template>
  <div class="image-uploader">
    <ImageWithFallback
      class="image-uploader__preview"
      :src="modelValue"
      :alt="previewAlt"
      :demo-kind="demoKind"
      width="76px"
      height="76px"
      radius="8px"
    />
    <div class="image-uploader__body">
      <input
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
          选择图片
        </AppButton>
        <span class="image-uploader__hint">jpg / png / webp，5MB 内</span>
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
}>(), {
  modelValue: '',
  placeholder: '可选，输入图片 URL',
  previewAlt: '图片预览',
  demoKind: 'cover',
  disabled: false,
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
  grid-template-columns: 76px 1fr;
  gap: 10px;
  align-items: center;
  width: 100%;
  min-width: 0;
}

.image-uploader__preview {
  background: var(--color-bg-surface);
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
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
