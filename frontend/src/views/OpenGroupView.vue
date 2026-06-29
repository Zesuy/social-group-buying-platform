<template>
  <PageLayout show-tab-bar>
    <ReminderBanner type="info">
      选择开团类型后即可发布团购活动
    </ReminderBanner>

    <div class="group-types-grid">
      <div
        v-for="type in groupTypes"
        :key="type.key"
        :class="[
          'group-type-card',
          { 'group-type-card--disabled': !type.available },
        ]"
        @click="onCardClick(type)"
      >
        <van-icon
          :name="type.icon"
          class="group-type-icon"
          :class="{ 'group-type-icon--disabled': !type.available }"
        />
        <div class="group-type-label">{{ type.label }}</div>
        <div class="group-type-subtitle">{{ type.subtitle }}</div>
        <van-tag v-if="!type.available" plain class="group-type-tag">
          即将开放
        </van-tag>
      </div>
    </div>

    <van-action-sheet
      v-model:show="showActionSheet"
      :actions="actionSheetActions"
      cancel-text="取消"
      @select="onActionSelect"
      close-on-click-action
    />
  </PageLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores'
import PageLayout from '@/components/PageLayout.vue'
import ReminderBanner from '@/components/ReminderBanner.vue'

interface GroupType {
  key: string
  label: string
  icon: string
  subtitle: string
  available: boolean
}

const router = useRouter()
const authStore = useAuthStore()
const { isLoggedIn, isLeader } = storeToRefs(authStore)

const showActionSheet = ref(false)
const actionSheetActions = [
  {
    name: '发布团购',
    callback: () => {
      showToast('发布功能将在后续 batch 开放')
    },
  },
  {
    name: '管理团购',
    callback: () => {
      router.push('/leader/group-buys')
    },
  },
]

const groupTypes: GroupType[] = [
  {
    key: 'normal',
    label: '普通团购',
    icon: 'shop-o',
    subtitle: '发布普通商品团购',
    available: true,
  },
  {
    key: 'presale',
    label: '预售团购',
    icon: 'clock-o',
    subtitle: '预售商品，后续开放',
    available: false,
  },
  {
    key: 'coupon',
    label: '卡券团购',
    icon: 'coupon-o',
    subtitle: '卡券商品，后续开放',
    available: false,
  },
  {
    key: 'signup',
    label: '报名团购',
    icon: 'notes-o',
    subtitle: '报名活动，后续开放',
    available: false,
  },
]

function onCardClick(type: GroupType) {
  if (!type.available) {
    showToast('后续开放')
    return
  }

  if (!isLoggedIn.value) {
    showToast('请先登录')
    router.push('/login?redirect=/open-group')
    return
  }

  if (!isLeader.value) {
    router.push('/store/create?redirect=/open-group')
    return
  }

  showActionSheet.value = true
}

function onActionSelect(action: (typeof actionSheetActions)[number]) {
  if (action.callback) {
    action.callback()
  }
}
</script>

<style scoped>
.group-types-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  padding: 16px;
}

.group-type-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 8px;
  padding: 24px 16px;
  position: relative;
  cursor: pointer;
}

.group-type-card--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.group-type-icon {
  font-size: 40px;
  margin-bottom: 12px;
  color: var(--color-primary);
}

.group-type-icon--disabled {
  color: var(--color-text-hint, #999);
}

.group-type-label {
  font-size: 16px;
  font-weight: 600;
}

.group-type-subtitle {
  font-size: 12px;
  color: var(--color-text-hint);
  margin-top: 4px;
}

.group-type-tag {
  margin-top: 8px;
}
</style>
