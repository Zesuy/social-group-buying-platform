<template>
  <div class="order-status-steps">
    <div
      v-for="(step, idx) in steps"
      :key="idx"
      :class="['order-status-steps__step', { 'order-status-steps__step--done': step.done }]"
    >
      <div class="order-status-steps__dot" />
      <div class="order-status-steps__label">{{ step.label }}</div>
      <div v-if="idx < steps.length - 1" class="order-status-steps__line" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  status: string
}>()

const statusOrder = ['pendingPay', 'paid', 'shipped', 'completed'] as const

const allSteps = [
  { key: 'pendingPay', label: '已下单' },
  { key: 'paid', label: '已支付' },
  { key: 'shipped', label: '已发货' },
  { key: 'completed', label: '已完成' },
]

const steps = computed(() => {
  if (props.status === 'canceled') {
    return [
      { label: '已下单', done: true },
      { label: '已取消', done: false },
    ]
  }
  const idx = statusOrder.indexOf(props.status as typeof statusOrder[number])
  if (idx === -1) return []

  return allSteps.map((s, i) => ({
    label: s.label,
    done: i <= idx,
  }))
})
</script>

<style scoped>
.order-status-steps {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  text-align: center;
  padding: 14px;
  background: var(--color-bg-card);
  color: #9aa0a6;
  border-radius: var(--radius-card);
  margin-bottom: 12px;
  box-shadow: var(--shadow-card);
  position: relative;
}

.order-status-steps__step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  position: relative;
}

.order-status-steps__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #e0e0e0;
  z-index: 1;
}

.order-status-steps__step--done .order-status-steps__dot {
  background: var(--color-primary);
}

.order-status-steps__step--done .order-status-steps__label {
  color: var(--color-primary);
  font-weight: 900;
}

.order-status-steps__label {
  font-size: var(--font-size-sm);
  line-height: 1.2;
}

.order-status-steps__line {
  position: absolute;
  top: 5px;
  left: calc(50% + 10px);
  right: calc(-50% + 10px);
  height: 2px;
  background: #e0e0e0;
}

.order-status-steps__step--done + .order-status-steps__step--done .order-status-steps__line {
  background: var(--color-primary);
}
</style>
