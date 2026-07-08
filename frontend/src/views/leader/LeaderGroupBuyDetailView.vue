<template>
  <PageLayout title="我的团购详情" show-back @back="goBack">
    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" @retry="fetchDetail" />

    <template v-else-if="detail">
      <div class="detail-content">
        <!-- 1. 基本信息卡 -->
        <AppCard>
          <template v-if="!editing">
            <div class="gb-detail-header">
              <h2 class="gb-detail-title">{{ detail.groupBuy.title }}</h2>
              <AppStatusPill :label="getGroupBuyStatusText(detail.groupBuy.status)" variant="green" />
            </div>
            <p class="gb-detail-meta">
              开始：{{ detail.groupBuy.startTime || '立即' }}｜结束：{{ detail.groupBuy.endTime || '长期' }}｜配送：{{ getDeliveryTypeText(detail.groupBuy.deliveryType) }}
            </p>
          </template>

          <!-- 编辑模式 -->
          <div v-if="editing" class="gb-edit-section">
            <van-field v-model="editForm.title" label="标题" />
            <van-field v-model="editForm.introduction" label="介绍" type="textarea" autosize />
            <van-field v-model="editForm.coverImageUrl" label="封面URL" />
            <van-field label="配送">
              <template #input>
                <div class="gb-delivery-options">
                  <label v-for="d in ['express','pickup','local_delivery']" :key="d" class="gb-radio-label">
                    <input type="radio" v-model="editForm.deliveryType" :value="d" /> {{ {express:'快递',pickup:'自提',local_delivery:'同城'}[d] }}
                  </label>
                </div>
              </template>
            </van-field>
            <van-field v-model="editForm.shippingTime" label="发货时间" placeholder="如：48小时内发货" />
            <van-field v-model="editForm.startTime" label="开始时间" type="datetime-local" />
            <van-field v-model="editForm.endTime" label="结束时间" type="datetime-local" />
            <div class="gb-edit-actions">
              <AppButton variant="ghost" @click="cancelEdit">取消</AppButton>
              <AppButton variant="primary" :loading="editLoading" @click="saveEdit">{{ editLoading ? '保存中...' : '保存' }}</AppButton>
            </div>
          </div>

          <div v-if="detail.groupBuy.introduction && !editing" class="intro-text">{{ detail.groupBuy.introduction }}</div>
          <img v-if="displayCoverImageUrl && !editing" :src="displayCoverImageUrl" class="cover-img" />
        </AppCard>

        <!-- 2. 商品列表 -->
        <AppCard v-for="item in detail.items" :key="item.id">
          <div class="gb-item-row">
            <div class="fake-img">{{ item.displayName.charAt(0) }}</div>
            <div class="gb-item-info">
              <b>{{ item.displayName }}</b>
              <p class="gb-item-meta">团购价 <PriceText :amount="item.groupPriceAmount" size="sm" />｜库存 {{ item.groupStock }}</p>
              <p class="gb-item-meta gb-item-sold">已团 {{ item.soldCount }}</p>
            </div>
          </div>
        </AppCard>

        <!-- 3. 团购操作 -->
        <AppFormCard title="团购操作">
          <AppFormRow label="编辑基础信息" value="标题、介绍、封面" arrow clickable @click="editing = true" />
          <AppFormRow v-if="detail.groupBuy.status === 'published'" label="分享团购" value="生成二维码链接" arrow clickable @click="openShare" />
          <AppFormRow v-if="detail.groupBuy.status === 'published'" label="结束团购" value="需要二次确认" arrow clickable @click="handleEndGroupBuy" />
        </AppFormCard>
      </div>

      <AppFixedActions :single="detail?.groupBuy.status !== 'published'">
        <AppButton
          v-if="detail?.groupBuy.status === 'published'"
          variant="ghost"
          :loading="shareLoading"
          @click="openShare"
        >
          分享团购
        </AppButton>
        <AppButton v-else variant="ghost" @click="editing = true">编辑基础信息</AppButton>
        <AppButton v-if="detail?.groupBuy.status === 'published'" variant="danger" :loading="endLoading" @click="handleEndGroupBuy">
          {{ endLoading ? '处理中...' : '结束团购' }}
        </AppButton>
      </AppFixedActions>
      <GroupBuyShareSheet
        v-if="shareCard"
        v-model="shareSheetVisible"
        :payload="sharePayload"
        :share-url="shareUrl"
      />
    </template>
  </PageLayout>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import AppCard from '@/components/AppCard.vue'
import AppButton from '@/components/AppButton.vue'
import AppFixedActions from '@/components/AppFixedActions.vue'
import AppFormCard from '@/components/AppFormCard.vue'
import AppFormRow from '@/components/AppFormRow.vue'
import AppStatusPill from '@/components/AppStatusPill.vue'
import PriceText from '@/components/PriceText.vue'
import GroupBuyShareSheet, { type GroupBuySharePayload } from '@/components/GroupBuyShareSheet.vue'
import { getMyGroupBuy, updateMyGroupBuy, endGroupBuy, getMyGroupBuyShareCard } from '@/api/leaderGroupBuys'
import { useSmartNavigation, useUnsavedChangesGuard } from '@/composables'
import { buildShareTokenUrl, getGroupBuyStatusText, getDeliveryTypeText, resolveDisplayImageUrl } from '@/utils'
import type { GroupBuyManageDetailData, ShareCardData } from '@/types'

const route = useRoute()
const { goBack } = useSmartNavigation('/leader/group-buys')
const loading = ref(true)
const error = ref<string | null>(null)
const detail = ref<GroupBuyManageDetailData | null>(null)
const editing = ref(false)
const editLoading = ref(false)
const editSnapshot = ref('')
const endLoading = ref(false)
const shareLoading = ref(false)
const shareSheetVisible = ref(false)
const shareCard = ref<ShareCardData | null>(null)
const editForm = reactive({ title: '', introduction: '', coverImageUrl: '', deliveryType: 'express', shippingTime: '', startTime: '', endTime: '' })
useUnsavedChangesGuard({
  isDirty: () => editing.value && JSON.stringify(editForm) !== editSnapshot.value,
})
function markEditClean() { editSnapshot.value = JSON.stringify(editForm) }
const shareUrl = computed(() => shareCard.value ? buildShareTokenUrl(shareCard.value.shareToken) : '')
const displayCoverImageUrl = computed(() => resolveDisplayImageUrl(
  detail.value?.groupBuy.coverImageUrl,
  detail.value?.groupBuy.title || '团购封面',
  'cover',
))
const sharePayload = computed<GroupBuySharePayload>(() => ({
  title: shareCard.value?.title || detail.value?.groupBuy.title || '团购分享',
  coverImageUrl: shareCard.value?.coverImageUrl ?? detail.value?.groupBuy.coverImageUrl ?? null,
  minPriceAmount: shareCard.value?.minPriceAmount ?? null,
  maxPriceAmount: shareCard.value?.maxPriceAmount ?? null,
  storeName: shareCard.value?.storeName || '团长店铺',
  leaderName: shareCard.value?.leaderName || '团长',
  deliveryType: shareCard.value?.deliveryType ?? detail.value?.groupBuy.deliveryType ?? null,
  shippingTime: shareCard.value?.shippingTime ?? detail.value?.groupBuy.shippingTime ?? null,
}))

async function fetchDetail() {
  loading.value = true; error.value = null
  try { detail.value = await getMyGroupBuy(route.params.id as string); resetEditForm() }
  catch (err) { error.value = (err as { message?: string }).message || '加载失败' }
  finally { loading.value = false }
}
function cancelEdit() { editing.value = false; resetEditForm() }
function resetEditForm() {
  if (!detail.value) return; const g = detail.value.groupBuy
  editForm.title = g.title; editForm.introduction = g.introduction || ''; editForm.coverImageUrl = g.coverImageUrl || ''
  editForm.deliveryType = g.deliveryType; editForm.shippingTime = g.shippingTime || ''
  editForm.startTime = g.startTime || ''; editForm.endTime = g.endTime || ''
  markEditClean()
}
async function saveEdit() {
  if (!detail.value) return; editLoading.value = true
  try {
    await updateMyGroupBuy(detail.value.groupBuy.id, {
      title: editForm.title || undefined, introduction: editForm.introduction || null,
      coverImageUrl: editForm.coverImageUrl || null, deliveryType: editForm.deliveryType || undefined,
      shippingTime: editForm.shippingTime || null, startTime: editForm.startTime || null, endTime: editForm.endTime || null,
    })
    showToast('保存成功'); editing.value = false; markEditClean(); await fetchDetail()
  } catch (err) { showToast((err as { message?: string }).message || '保存失败') }
  finally { editLoading.value = false }
}
async function handleEndGroupBuy() {
  if (!detail.value) return
  try { await showConfirmDialog({ title: '确认结束', message: '确定要结束该团购吗？结束之后将无法继续购买。' }) } catch { return }
  endLoading.value = true
  try { await endGroupBuy(detail.value.groupBuy.id); showToast('团购已结束'); await fetchDetail() }
  catch (err) { showToast((err as { message?: string; code?: string }).message || '操作失败') }
  finally { endLoading.value = false }
}
async function openShare() {
  if (!detail.value) return
  shareLoading.value = true
  try {
    shareCard.value = await getMyGroupBuyShareCard(detail.value.groupBuy.id)
    shareSheetVisible.value = true
  } catch (err) {
    showToast((err as { message?: string }).message || '分享卡片生成失败')
  } finally {
    shareLoading.value = false
  }
}
onMounted(() => { fetchDetail() })
</script>

<style scoped>
.detail-content { padding: 14px 14px 80px; }
.intro-text { margin-top:12px; color:#666; font-size:14px; line-height:1.6; }
.cover-img { width:100%; margin-top:12px; border-radius:8px; }
.fake-img { width:90px; height:90px; border-radius:8px; background:linear-gradient(145deg,#dcefe0,#a1c49f); display:flex; align-items:center; justify-content:center; color:#fff; font-weight:900; flex:none; }

/* ── 替换内联 style 的类 ── */
.gb-detail-header { display:flex; align-items:center; justify-content:space-between; gap:8px; }
.gb-detail-title { margin:0; font-size:20px; font-weight:900; }
.gb-detail-meta { margin:8px 0 0; font-size:13px; color:#969ca5; }
.gb-edit-section { margin-top:12px; }
.gb-delivery-options { display:flex; gap:8px; }
.gb-radio-label { display:flex; align-items:center; gap:4px; font-size:13px; cursor:pointer; }
.gb-edit-actions { display:flex; gap:12px; justify-content:flex-end; padding:14px; }
.gb-item-row { display:flex; align-items:center; gap:12px; }
.gb-item-info { flex:1; min-width:0; }
.gb-item-meta { margin:4px 0; color:#969ca5; font-size:var(--font-size-sm); }
.gb-item-sold { margin:0; }
</style>
