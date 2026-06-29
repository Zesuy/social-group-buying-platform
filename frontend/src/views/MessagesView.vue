<template>
  <PageLayout show-tab-bar>
    <ReminderBanner type="info" text="消息功能将在后续版本上线，敬请期待">
      消息功能将在后续版本上线，敬请期待
    </ReminderBanner>

    <EmptyState description="暂无消息" />
  </PageLayout>
</template>

<script setup lang="ts">
// 消息页 — 只展示空态和提示横幅，不请求任何消息 API
import PageLayout from '@/components/PageLayout.vue'
import EmptyState from '@/components/EmptyState.vue'
import ReminderBanner from '@/components/ReminderBanner.vue'
</script>
