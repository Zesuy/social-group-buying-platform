<template>
  <PageLayout :title="title" show-back @back="goBack">
    <div class="feature-placeholder">
      <EmptyState :description="description">
        <van-button round plain type="primary" @click="goBack">
          返回
        </van-button>
      </EmptyState>
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import PageLayout from '@/components/PageLayout.vue'
import EmptyState from '@/components/EmptyState.vue'
import { useSmartNavigation } from '@/composables'

const route = useRoute()
const { goBack } = useSmartNavigation('/')

const title = computed(() => String(route.meta.title || '功能占位'))
const description = computed(() => String(route.meta.placeholderText || '该功能将在后续版本开放'))

</script>

<style scoped>
.feature-placeholder {
  padding: 12px 14px;
}

.feature-placeholder :deep(.van-button) {
  min-width: 112px;
  height: var(--button-capsule-height);
  font-weight: 900;
}
</style>
