<template>
  <div
    class="address-card"
    :class="{
      'address-card--selected': selected && selectable,
      'address-card--selectable': selectable,
    }"
    @click="handleClick"
  >
    <div class="address-card__header">
      <span class="address-card__name">{{ address.receiverName }}</span>
      <span class="address-card__phone">{{ address.receiverPhone }}</span>
      <van-tag v-if="address.isDefault" plain type="primary" size="medium" class="address-card__tag">
        默认
      </van-tag>
    </div>
    <p class="address-card__detail">{{ address.fullAddress }}</p>
    <div v-if="!selectable" class="address-card__actions">
      <van-button
        size="small"
        plain
        type="default"
        icon="edit"
        @click.stop="$emit('edit', address.id)"
      >
        编辑
      </van-button>
      <van-button
        size="small"
        plain
        type="danger"
        icon="delete"
        @click.stop="$emit('delete', address.id)"
      >
        删除
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { AddressData } from '@/types'

const props = withDefaults(defineProps<{
  address: AddressData
  selected?: boolean
  selectable?: boolean
}>(), {
  selected: false,
  selectable: false,
})

const emit = defineEmits<{
  click: [address: AddressData]
  edit: [id: number]
  delete: [id: number]
}>()

function handleClick() {
  emit('click', props.address)
}
</script>

<style scoped>
.address-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  padding: var(--spacing-md) var(--spacing-lg);
  margin-bottom: var(--spacing-sm);
  border: 2px solid transparent;
  transition: border-color 0.2s;
}

.address-card--selectable {
  cursor: pointer;
}

.address-card--selected {
  border-color: var(--color-primary);
}

.address-card__header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-xs);
}

.address-card__name {
  font-size: var(--font-size-lg);
  font-weight: 500;
  color: var(--color-text-primary);
}

.address-card__phone {
  font-size: var(--font-size-md);
  color: var(--color-text-secondary);
}

.address-card__tag {
  margin-left: auto;
}

.address-card__detail {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  line-height: 1.5;
  margin-bottom: 0;
}

.address-card__actions {
  display: flex;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-sm);
  padding-top: var(--spacing-sm);
  border-top: 1px solid var(--color-border-light);
  justify-content: flex-end;
}
</style>
