<template>
  <PageLayout show-tab-bar>
    <div class="app-topbar">
      开团
    </div>

    <div class="open-group-page">
      <section class="open-group-hero" aria-labelledby="open-group-title">
        <div class="open-group-hero__copy">
          <span class="open-group-hero__eyebrow">团长经营入口</span>
          <h1 id="open-group-title">把微信群里的需求，整理成一次团购</h1>
          <p>选择商品、填写履约说明，发布后团员可直接下单。</p>
        </div>
        <div class="open-group-hero__badge" aria-hidden="true">
          <van-icon name="shop-collect-o" />
        </div>
      </section>

      <section class="open-group-status">
        <div>
          <span>{{ statusTitle }}</span>
          <p>{{ statusDescription }}</p>
        </div>
        <button type="button" @click="onPrimaryStatusClick">
          {{ statusActionText }}
        </button>
      </section>

      <section class="open-group-section">
        <div class="open-group-section__head">
          <div>
            <h2>选择开团方式</h2>
            <p>当前优先支持商品开团，适合生鲜、特产和社区复购商品。</p>
          </div>
        </div>

        <div class="group-types-list">
          <button
            v-for="type in groupTypes"
            :key="type.key"
            type="button"
            class="group-type-card"
            :class="[
              `group-type-card--${type.key}`,
              { 'group-type-card--disabled': !type.available },
            ]"
            @click="onCardClick(type)"
          >
            <span class="group-type-card__icon">
              <van-icon :name="type.icon" />
            </span>
            <span class="group-type-card__copy">
              <span class="group-type-card__title">
                {{ type.label }}
                <small v-if="!type.available">即将开放</small>
              </span>
              <span class="group-type-card__subtitle">{{ type.subtitle }}</span>
            </span>
            <van-icon name="arrow" class="group-type-card__arrow" />
          </button>
        </div>
      </section>

      <section class="open-group-checklist">
        <div class="open-group-section__head">
          <div>
            <h2>开团前准备</h2>
            <p>让团员下单前能看懂商品、价格和履约承诺。</p>
          </div>
        </div>
        <div class="open-group-checklist__grid">
          <div v-for="item in checklist" :key="item.title" class="open-group-check">
            <van-icon :name="item.icon" />
            <div>
              <b>{{ item.title }}</b>
              <span>{{ item.desc }}</span>
            </div>
          </div>
        </div>
      </section>

      <button type="button" class="open-group-rule" @click="onGuideClick('rule')">
        《邻鲜团禁发商品及信息管理规范》
      </button>
    </div>

    <template #action>
      <div class="open-group-bottom">
        <button type="button" class="open-group-bottom__primary" @click="onCardClick(groupTypes[0])">
          {{ bottomActionText }}
        </button>
      </div>
    </template>

  </PageLayout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
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

const groupTypes: GroupType[] = [
  {
    key: 'normal',
    label: '普通团购',
    icon: 'shop-o',
    subtitle: '发布商品、设置团购价和履约说明',
    available: true,
  },
  {
    key: 'presale',
    label: '预售团购',
    icon: 'clock-o',
    subtitle: '适合预约收单，后续开放',
    available: false,
  },
  {
    key: 'coupon',
    label: '卡券团购',
    icon: 'coupon-o',
    subtitle: '适合服务券和权益券，后续开放',
    available: false,
  },
  {
    key: 'signup',
    label: '报名团购',
    icon: 'notes-o',
    subtitle: '适合活动报名，后续开放',
    available: false,
  },
]

const checklist = [
  { icon: 'goods-collect-o', title: '商品资料', desc: '名称、图片、价格、库存' },
  { icon: 'logistics', title: '履约说明', desc: '发货时间、配送或自提方式' },
  { icon: 'friends-o', title: '社群说明', desc: '团长承诺和群内通知口径' },
  { icon: 'description-o', title: '订单准备', desc: '收货地址与下单规则清晰' },
]

const statusTitle = computed(() => {
  if (!isLoggedIn.value) return '登录后开始开团'
  if (!isLeader.value) return '先创建店铺成为团长'
  return '你的团长身份已就绪'
})

const statusDescription = computed(() => {
  if (!isLoggedIn.value) return '用模拟登录进入后，可创建店铺并发布团购。'
  if (!isLeader.value) return '店铺会展示在团购详情和团长主页，是团员信任你的第一步。'
  return '可以发布新团购，也可以继续管理已经发布的活动。'
})

const statusActionText = computed(() => {
  if (!isLoggedIn.value) return '去登录'
  if (!isLeader.value) return '创建店铺'
  return '管理团购'
})

const bottomActionText = computed(() => {
  if (!isLoggedIn.value) return '登录后开团'
  if (!isLeader.value) return '创建店铺后开团'
  return '立即发布'
})

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

  goToPublish()
}

function onPrimaryStatusClick() {
  if (!isLoggedIn.value) {
    router.push('/login?redirect=/open-group')
    return
  }
  if (!isLeader.value) {
    router.push('/store/create?redirect=/open-group')
    return
  }
  router.push('/leader/group-buys')
}

function onGuideClick(_type: string) {
  showToast('内容将在后续开放')
}

function goToPublish() {
  if (isFeatureDisabled('groupBuyPublish')) {
    showToast('发布功能将在后续 batch 开放')
    return
  }
  router.push('/leader/group-buys/new')
}
</script>

<style scoped>
.open-group-page {
  min-height: calc(100vh - var(--tabbar-height));
  background: var(--color-bg);
  padding: 12px 14px calc(var(--tabbar-height) + 88px + var(--safe-area-bottom));
}

.open-group-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 76px;
  gap: 14px;
  align-items: center;
  border: 1px solid rgba(16, 196, 104, 0.14);
  border-radius: var(--radius-lg);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-card);
  padding: 18px 16px;
}

.open-group-hero__eyebrow {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  border-radius: var(--radius-pill);
  background: var(--color-primary-light);
  color: var(--color-primary-dark);
  padding: 0 9px;
  font-size: var(--font-size-xs);
  font-weight: 900;
  line-height: 1;
}

.open-group-hero h1 {
  margin: 10px 0 8px;
  color: var(--color-text-primary);
  font-size: 23px;
  font-weight: 900;
  line-height: 1.28;
}

.open-group-hero p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-md);
  line-height: 1.55;
}

.open-group-hero__badge {
  width: 76px;
  height: 76px;
  border-radius: 24px;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 42px;
  box-shadow: 0 10px 24px rgba(16, 196, 104, 0.22);
}

.open-group-status,
.open-group-section,
.open-group-checklist {
  margin-top: 12px;
  border-radius: var(--radius-card);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-card);
}

.open-group-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
}

.open-group-status span {
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
  font-weight: 900;
  line-height: 1.3;
}

.open-group-status p {
  margin: 4px 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.45;
}

.open-group-status button,
.open-group-bottom__primary {
  border: 1px solid var(--color-primary);
  background: var(--color-primary);
  color: #fff;
  border-radius: var(--radius-pill);
  font-weight: 900;
  white-space: nowrap;
  cursor: pointer;
}

.open-group-status button {
  min-height: 40px;
  padding: 0 14px;
  flex-shrink: 0;
  font-size: var(--font-size-sm);
}

.open-group-section,
.open-group-checklist {
  padding: 14px;
}

.open-group-section__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.open-group-section__head h2 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: var(--font-size-xl);
  font-weight: 900;
  line-height: 1.3;
}

.open-group-section__head p {
  margin: 4px 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.45;
}

.group-types-list {
  display: grid;
  gap: 10px;
}

.group-type-card {
  width: 100%;
  min-height: 82px;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  background: var(--color-bg-card);
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, opacity 0.18s ease, border-color 0.18s ease;
}

.group-type-card:active {
  transform: scale(0.98);
}

.group-type-card--normal {
  border-color: rgba(16, 196, 104, 0.32);
  background: linear-gradient(180deg, rgba(232, 255, 242, 0.72), #fff);
}

.group-type-card--disabled {
  opacity: 0.68;
}

.group-type-card__icon {
  width: 48px;
  height: 48px;
  border-radius: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--color-primary);
  background: var(--color-primary-light);
  font-size: 26px;
}

.group-type-card--presale .group-type-card__icon {
  color: #ff9d32;
  background: #fff5df;
}

.group-type-card--coupon .group-type-card__icon {
  color: #ff6a2e;
  background: #fff2e8;
}

.group-type-card--signup .group-type-card__icon {
  color: #22a7f0;
  background: #edf5ff;
}

.group-type-card__copy {
  display: grid;
  gap: 4px;
  flex: 1;
  min-width: 0;
}

.group-type-card__title {
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
  font-weight: 900;
  line-height: 1.25;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.group-type-card__title small {
  border-radius: var(--radius-pill);
  background: var(--color-bg-surface);
  color: var(--color-text-secondary);
  padding: 3px 7px;
  font-size: var(--font-size-xs);
  line-height: 1;
  font-weight: 800;
}

.group-type-card__subtitle {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.35;
}

.group-type-card__arrow {
  color: var(--color-text-hint);
  flex-shrink: 0;
}

.open-group-checklist__grid {
  display: grid;
  gap: 10px;
}

.open-group-check {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  border-radius: 10px;
  background: var(--color-bg-surface);
  padding: 11px;
}

.open-group-check > .van-icon {
  color: var(--color-primary);
  font-size: 20px;
  margin-top: 1px;
}

.open-group-check div {
  display: grid;
  gap: 3px;
}

.open-group-check b {
  color: var(--color-text-primary);
  font-size: var(--font-size-md);
  line-height: 1.25;
}

.open-group-check span {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.35;
}

.open-group-rule {
  width: 100%;
  border: 0;
  background: transparent;
  min-height: 44px;
  margin-top: 10px;
  text-align: center;
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  font-weight: 800;
  cursor: pointer;
}

.open-group-bottom {
  background: var(--color-bg-card);
  border-top: 1px solid var(--color-border);
  padding: 10px 14px calc(10px + var(--safe-area-bottom));
}

.open-group-bottom__primary {
  width: 100%;
  min-height: 50px;
  font-size: var(--font-size-lg);
}

@media (max-width: 340px) {
  .open-group-page {
    padding-left: 12px;
    padding-right: 12px;
  }

  .open-group-hero {
    grid-template-columns: 1fr;
  }

  .open-group-hero__badge {
    display: none;
  }
}
</style>
