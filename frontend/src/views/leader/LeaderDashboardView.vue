<template>
  <PageLayout title="团长仪表盘">
    <div class="dashboard-stats">
      <div class="stat-card">
        <div class="stat-card__value">&yen;0.00</div>
        <div class="stat-card__label">累计收入（余额）</div>
      </div>
      <div class="stat-card">
        <div class="stat-card__value">0</div>
        <div class="stat-card__label">我的团员</div>
      </div>
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import PageLayout from '@/components/PageLayout.vue'
</script>

<style scoped>
.dashboard-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 14px;
}

.stat-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  padding: 20px 16px;
  box-shadow: var(--shadow-card);
  text-align: center;
}

.stat-card__value {
  font-size: 24px;
  font-weight: 900;
  color: var(--color-text-primary);
  margin-bottom: 6px;
}

.stat-card__label {
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
}
</style>
