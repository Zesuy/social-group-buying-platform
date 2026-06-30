<template>
  <PageLayout show-tab-bar>
    <div class="app-topbar">
      请选择开团类型
    </div>

    <div class="notice-strip">
      <span>打开提醒，官方活动早知道</span>
      <button type="button" @click="onNoticeClick">设置</button>
    </div>

    <div class="open-group-hero">
      <div class="open-group-hero__mark">
        <van-icon name="friends-o" />
      </div>
      <h1>请选择开团类型</h1>
      <div class="open-group-hero__actions">
        <button type="button" @click="onGuideClick('tutorial')">
          开团教程
          <van-icon name="arrow" />
        </button>
        <button type="button" @click="onGuideClick('newcomer')">
          新人攻略
          <van-icon name="arrow" />
        </button>
      </div>
    </div>

    <div class="group-types-list">
      <div
        v-for="type in groupTypes"
        :key="type.key"
        :class="[
          'group-type-item',
          { 'group-type-item--disabled': !type.available },
        ]"
        @click="onCardClick(type)"
      >
        <div class="group-type-icon-wrap" :class="`group-type-icon-wrap--${type.key}`">
          <van-icon :name="type.icon" class="group-type-icon" />
        </div>
        <div class="group-type-copy">
          <h2>{{ type.label }}</h2>
          <p>{{ type.subtitle }}</p>
        </div>
        <span v-if="!type.available" class="group-type-tag">即将开放</span>
        <van-icon name="arrow" class="group-type-arrow" />
      </div>
    </div>

    <p class="open-group-rule" @click="onGuideClick('rule')">
      《邻鲜团禁发商品及信息管理规范》
    </p>

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
import { isFeatureDisabled } from '@/utils/non-mvp'

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
      if (isFeatureDisabled('groupBuyPublish')) {
        showToast('发布功能将在后续 batch 开放')
      }
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
    if (type.key === 'coupon' && isFeatureDisabled('coupon')) {
      showToast('卡券团购将在后续开放')
      return
    }
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

function onNoticeClick() {
  if (isFeatureDisabled('wechatPush')) {
    showToast('公众号提醒将在后续开放')
  }
}

function onGuideClick(_type: string) {
  showToast('内容将在后续开放')
}

function onActionSelect(action: (typeof actionSheetActions)[number]) {
  if (action.callback) {
    action.callback()
  }
}
</script>

<style scoped>
.notice-strip button,
.open-group-hero__actions button {
  border: 1px solid #aeeccd;
  background: #fff;
  color: var(--color-primary);
  border-radius: 999px;
  padding: 7px 14px;
  font-weight: 800;
  min-height: 36px;
}

.open-group-hero {
  padding: 18px 14px 16px;
  text-align: center;
}

.open-group-hero__mark {
  width: 96px;
  height: 96px;
  margin: 0 auto 8px;
  border-radius: 30px;
  background: #e8fff2;
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 58px;
}

.open-group-hero h1 {
  margin: 0 0 14px;
  font-size: 24px;
  font-weight: 900;
}

.open-group-hero__actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.group-types-list {
  padding: 0 14px;
}

.group-type-item {
  min-height: 76px;
  background: #fff;
  border-radius: 12px;
  padding: 13px;
  margin-bottom: 10px;
  display: flex;
  gap: 12px;
  align-items: center;
  box-shadow: var(--shadow-card);
}

.group-type-item--disabled {
  opacity: 0.62;
}

.group-type-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--color-primary);
  background: var(--color-primary-light);
  font-size: 28px;
}

.group-type-icon-wrap--presale {
  color: #ff9d32;
  background: #fff5df;
}

.group-type-icon-wrap--coupon {
  color: #ff6a2e;
  background: #fff2e8;
}

.group-type-icon-wrap--signup {
  color: #22a7f0;
  background: #edf5ff;
}

.group-type-copy {
  flex: 1;
  min-width: 0;
}

.group-type-copy h2 {
  margin: 0;
  font-size: 19px;
  font-weight: 900;
}

.group-type-copy p {
  margin: 4px 0 0;
  color: var(--color-text-hint);
  font-size: 13px;
}

.group-type-tag {
  color: #f36b2a;
  background: #fff5df;
  border: 1px solid #ffc49b;
  border-radius: 999px;
  padding: 3px 8px;
  font-size: 11px;
  font-weight: 900;
  white-space: nowrap;
}

.group-type-arrow {
  color: var(--color-text-hint);
  flex-shrink: 0;
}

.open-group-rule {
  margin: 14px 14px 24px;
  text-align: center;
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 800;
}
</style>
