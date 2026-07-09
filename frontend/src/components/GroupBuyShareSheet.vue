<template>
  <van-popup
    v-model:show="visible"
    position="bottom"
    round
    class="h5-constrained-bottom-sheet"
    :style="{ maxHeight: '88vh', overflowY: 'auto' }"
  >
    <div class="share-sheet">
      <div class="share-sheet__handle" />
      <div class="share-sheet__header">
        <div>
          <p>团购分享</p>
          <h2>扫码查看本次团购</h2>
        </div>
        <button type="button" aria-label="关闭分享" @click="visible = false">
          <van-icon name="cross" />
        </button>
      </div>

      <div class="share-card">
        <ImageWithFallback
          :src="payload.coverImageUrl"
          width="100%"
          height="148px"
          fit="cover"
          radius="8px"
          :alt="payload.title"
        />
        <div class="share-card__body">
          <div class="share-card__title-row">
            <h3>{{ payload.title }}</h3>
            <span v-if="priceText">{{ priceText }}</span>
          </div>
          <p>{{ payload.storeName }} · {{ payload.leaderName }}</p>
          <div class="share-card__meta">
            <span>{{ deliveryText }}</span>
            <span v-if="payload.shippingTime">{{ formatTime(payload.shippingTime) }} 履约</span>
          </div>
        </div>

        <div class="qr-panel">
          <div class="qr-panel__code">
            <img v-if="qrCodeUrl" :src="qrCodeUrl" alt="团购分享二维码" />
            <van-loading v-else size="24px" />
          </div>
          <div class="qr-panel__copy">
            <b>微信扫码打开</b>
            <span>也可以复制链接发给团员</span>
          </div>
        </div>
      </div>

      <div class="share-link">
        <span>{{ shareUrl }}</span>
        <button type="button" @click="copyLink">复制</button>
      </div>

      <div class="share-sheet__actions">
        <AppButton variant="primary" icon="share-o" :loading="sharing" @click="shareBySystem">
          系统分享
        </AppButton>
        <AppButton variant="ghost" icon="link-o" @click="copyLink">
          复制链接
        </AppButton>
      </div>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { showToast } from 'vant'
import QRCode from 'qrcode'
import ImageWithFallback from './ImageWithFallback.vue'
import AppButton from './AppButton.vue'
import { formatAmount } from '@/utils/format'
import { getDeliveryTypeText } from '@/utils/status'
import { copyTextToClipboard, shareBySystem as invokeSystemShare } from '@/utils/share'

export interface GroupBuySharePayload {
  title: string
  coverImageUrl: string | null
  minPriceAmount?: number | null
  maxPriceAmount?: number | null
  storeName: string
  leaderName: string
  deliveryType?: string | null
  shippingTime?: string | null
}

const props = defineProps<{
  modelValue: boolean
  payload: GroupBuySharePayload
  shareUrl: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})
const qrCodeUrl = ref('')
const sharing = ref(false)

const priceText = computed(() => {
  const min = props.payload.minPriceAmount
  const max = props.payload.maxPriceAmount
  if (min === null || min === undefined) return ''
  if (max !== null && max !== undefined && max > min) {
    return `${formatAmount(min)}-${formatAmount(max)}`
  }
  return `${formatAmount(min)} 起`
})
const deliveryText = computed(() => (
  props.payload.deliveryType ? getDeliveryTypeText(props.payload.deliveryType) : '团长履约'
))

watch(
  () => [visible.value, props.shareUrl],
  async ([isVisible]) => {
    if (!isVisible || !props.shareUrl) return
    qrCodeUrl.value = ''
    try {
      qrCodeUrl.value = await QRCode.toDataURL(props.shareUrl, {
        width: 216,
        margin: 1,
        errorCorrectionLevel: 'M',
      })
    } catch {
      showToast('二维码生成失败')
    }
  },
  { immediate: true },
)

function formatTime(value: string): string {
  return value.slice(0, 16).replace('T', ' ')
}

async function copyLink(): Promise<void> {
  try {
    await copyTextToClipboard(props.shareUrl)
    showToast('链接已复制')
  } catch {
    showToast('复制失败，请手动复制链接')
  }
}

async function shareBySystem(): Promise<void> {
  sharing.value = true
  try {
    const result = await invokeSystemShare({
      title: props.payload.title,
      text: `${props.payload.storeName}的团购正在进行`,
      url: props.shareUrl,
    })
    if (result === 'shared' || result === 'aborted') {
      return
    }
    await copyTextToClipboard(props.shareUrl)
    showToast(result === 'unsupported' ? '当前浏览器不支持系统分享，已复制链接' : '系统分享调用失败，已复制链接')
  } catch {
    showToast('分享失败，请复制链接发送')
  } finally {
    sharing.value = false
  }
}
</script>

<style scoped>
.share-sheet {
  padding: 8px 16px calc(16px + var(--safe-area-bottom));
  background: var(--color-bg);
}

.share-sheet__handle {
  width: 38px;
  height: 4px;
  border-radius: 999px;
  background: #d8dde3;
  margin: 0 auto 12px;
}

.share-sheet__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.share-sheet__header p {
  margin: 0 0 4px;
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  font-weight: 800;
}

.share-sheet__header h2 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 22px;
  line-height: 1.25;
  font-weight: 900;
}

.share-sheet__header button {
  width: 44px;
  height: 44px;
  border: 0;
  border-radius: 999px;
  background: var(--color-bg-soft);
  color: var(--color-text-secondary);
}

.share-card {
  padding: 12px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-card);
}

.share-card__body {
  padding: 12px 0 10px;
}

.share-card__title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.share-card__title-row h3 {
  margin: 0;
  min-width: 0;
  color: var(--color-text-primary);
  font-size: 18px;
  line-height: 1.35;
  font-weight: 900;
}

.share-card__title-row span {
  color: var(--color-price);
  font-size: var(--font-size-sm);
  font-weight: 900;
  white-space: nowrap;
}

.share-card__body p {
  margin: 6px 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.share-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.share-card__meta span {
  border-radius: 999px;
  padding: 5px 9px;
  background: var(--color-bg-soft);
  color: var(--color-text-secondary);
  font-size: 12px;
  font-weight: 800;
}

.qr-panel {
  display: grid;
  grid-template-columns: 116px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  background: #f7fbf8;
}

.qr-panel__code {
  width: 116px;
  height: 116px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #fff;
}

.qr-panel__code img {
  width: 104px;
  height: 104px;
  display: block;
}

.qr-panel__copy b,
.qr-panel__copy span {
  display: block;
}

.qr-panel__copy b {
  color: var(--color-text-primary);
  font-size: 17px;
  line-height: 1.35;
}

.qr-panel__copy span {
  margin-top: 6px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.45;
}

.share-link {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  min-height: 44px;
  margin-top: 12px;
  padding: 8px 8px 8px 12px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid var(--color-border-light);
}

.share-link span {
  min-width: 0;
  color: var(--color-text-hint);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.share-link button {
  min-width: 54px;
  min-height: 36px;
  border: 0;
  border-radius: 8px;
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-weight: 900;
}

.share-sheet__actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 12px;
}

@media (max-width: 340px) {
  .qr-panel {
    grid-template-columns: 1fr;
    justify-items: center;
    text-align: center;
  }
}
</style>
