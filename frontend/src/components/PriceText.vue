<template>
  <span
    class="price-text"
    :class="[size ? `price-text--${size}` : '']"
    :style="color ? { color } : {}"
  >
    <slot name="prefix">{{ prefix }}</slot>
    {{ displayAmount }}
    <slot name="suffix">{{ suffix }}</slot>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatAmount } from '@/utils/format'

const props = withDefaults(defineProps<{
  amount: number | null | undefined
  size?: 'sm' | 'md' | 'lg' | 'xl'
  color?: string
  prefix?: string
  suffix?: string
}>(), {
  size: 'md',
  prefix: '',
  suffix: '',
})

const displayAmount = computed(() => formatAmount(props.amount))
</script>

<style scoped>
.price-text {
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0;
}

.price-text--sm { font-size: var(--font-size-sm); }
.price-text--md { font-size: var(--font-size-md); }
.price-text--lg { font-size: 17px; }
.price-text--xl { font-size: var(--font-size-xxl); }
</style>
