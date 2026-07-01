<template>
  <PageLayout title="我的团购详情" show-back @back="goBack">
    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" @retry="fetchDetail" />

    <template v-else-if="detail">
      <div class="detail-content">
        <!-- 1. 基本信息卡 -->
        <div class="card pad">
          <div v-if="!editing" class="between">
            <h2 style="margin:0;font-size:20px;font-weight:900">{{ detail.groupBuy.title }}</h2>
            <span class="status-chip">{{ getGroupBuyStatusText(detail.groupBuy.status) }}</span>
          </div>
          <p v-if="!editing" class="muted" style="margin:8px 0 0;font-size:13px">
            开始：{{ detail.groupBuy.startTime || '立即' }}｜结束：{{ detail.groupBuy.endTime || '长期' }}｜配送：{{ getDeliveryTypeText(detail.groupBuy.deliveryType) }}
          </p>

          <!-- 编辑模式 -->
          <div v-if="editing" class="edit-form">
            <div class="field"><label>标题</label><input v-model="editForm.title" class="input" /></div>
            <div class="field"><label>介绍</label><textarea v-model="editForm.introduction" class="textarea"></textarea></div>
            <div class="field"><label>封面URL</label><input v-model="editForm.coverImageUrl" class="input" /></div>
            <div class="field"><label>配送</label>
              <div style="display:flex;gap:8px">
                <label v-for="d in ['express','pickup','local_delivery']" :key="d" style="display:flex;align-items:center;gap:4px;font-size:13px">
                  <input type="radio" v-model="editForm.deliveryType" :value="d" /> {{ {express:'快递',pickup:'自提',local_delivery:'同城'}[d] }}
                </label>
              </div>
            </div>
            <div class="field"><label>发货时间</label><input v-model="editForm.shippingTime" class="input" placeholder="如：48小时内发货" /></div>
            <div class="field"><label>开始时间</label><input v-model="editForm.startTime" type="datetime-local" class="input" /></div>
            <div class="field"><label>结束时间</label><input v-model="editForm.endTime" type="datetime-local" class="input" /></div>
            <div style="display:flex;gap:12px;justify-content:flex-end;padding:14px">
              <button class="btn" @click="cancelEdit">取消</button>
              <button class="btn primary" :disabled="editLoading" @click="saveEdit">{{ editLoading ? '保存中...' : '保存' }}</button>
            </div>
          </div>

          <div v-if="detail.groupBuy.introduction && !editing" class="intro-text">{{ detail.groupBuy.introduction }}</div>
          <img v-if="detail.groupBuy.coverImageUrl && !editing" :src="detail.groupBuy.coverImageUrl" class="cover-img" />
        </div>

        <!-- 2. 商品列表 -->
        <div v-for="item in detail.items" :key="item.id" class="card pad">
          <div class="row" style="gap:12px">
            <div class="fake-img">{{ item.displayName.charAt(0) }}</div>
            <div class="grow">
              <b>{{ item.displayName }}</b>
              <p class="muted" style="margin:4px 0">团购价 {{ formatAmount(item.groupPriceAmount) }}｜库存 {{ item.groupStock }}</p>
              <p class="muted" style="margin:0">已团 {{ item.soldCount }}</p>
            </div>
          </div>
        </div>

        <!-- 3. 团购操作 -->
        <div class="form-card">
          <div class="form-title">团购操作</div>
          <div class="field" @click="editing = true">
            <label>编辑基础信息</label>
            <span class="value">标题、介绍、封面</span>
            <b class="muted">›</b>
          </div>
          <div v-if="detail.groupBuy.status === 'published'" class="field" @click="handleEndGroupBuy">
            <label>结束团购</label>
            <span class="value">需要二次确认</span>
            <b class="muted">›</b>
          </div>
        </div>
      </div>
    </template>

    <template #action>
      <div v-if="detail" class="fixed-actions">
        <button class="btn ghost" @click="editing = true">编辑基础信息</button>
        <button v-if="detail?.groupBuy.status === 'published'" class="btn danger" :disabled="endLoading" @click="handleEndGroupBuy">
          {{ endLoading ? '处理中...' : '结束团购' }}
        </button>
      </div>
    </template>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import { getMyGroupBuy, updateMyGroupBuy, endGroupBuy } from '@/api/leaderGroupBuys'
import { getGroupBuyStatusText, getDeliveryTypeText, formatAmount } from '@/utils'
import type { GroupBuyManageDetailData } from '@/types'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const error = ref<string | null>(null)
const detail = ref<GroupBuyManageDetailData | null>(null)
const editing = ref(false)
const editLoading = ref(false)
const endLoading = ref(false)
const editForm = reactive({ title: '', introduction: '', coverImageUrl: '', deliveryType: 'express', shippingTime: '', startTime: '', endTime: '' })

async function fetchDetail() {
  loading.value = true; error.value = null
  try { detail.value = await getMyGroupBuy(route.params.id as string) }
  catch (err) { error.value = (err as { message?: string }).message || '加载失败' }
  finally { loading.value = false }
}
function goBack() { router.back() }
function cancelEdit() { editing.value = false; resetEditForm() }
function resetEditForm() {
  if (!detail.value) return; const g = detail.value.groupBuy
  editForm.title = g.title; editForm.introduction = g.introduction || ''; editForm.coverImageUrl = g.coverImageUrl || ''
  editForm.deliveryType = g.deliveryType; editForm.shippingTime = g.shippingTime || ''
  editForm.startTime = g.startTime || ''; editForm.endTime = g.endTime || ''
}
async function saveEdit() {
  if (!detail.value) return; editLoading.value = true
  try {
    await updateMyGroupBuy(detail.value.groupBuy.id, {
      title: editForm.title || undefined, introduction: editForm.introduction || null,
      coverImageUrl: editForm.coverImageUrl || null, deliveryType: editForm.deliveryType || undefined,
      shippingTime: editForm.shippingTime || null, startTime: editForm.startTime || null, endTime: editForm.endTime || null,
    })
    showToast('保存成功'); editing.value = false; await fetchDetail()
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
onMounted(() => { fetchDetail() })
</script>

<style scoped>
.detail-content { padding: 14px 14px 80px; }
.card { background:#fff; border-radius:14px; margin-bottom:12px; box-shadow:0 1px 0 rgba(0,0,0,.03); }
.pad { padding:14px; }
.between { display:flex; align-items:center; justify-content:space-between; gap:8px; }
.row { display:flex; align-items:center; }
.grow { flex:1; }
.muted { color:#969ca5; }
.status-chip { background:#eafaf1; color:var(--color-primary); padding:4px 8px; border-radius:99px; font-size:12px; font-weight:900; white-space:nowrap; }
.intro-text { margin-top:12px; color:#666; font-size:14px; line-height:1.6; }
.cover-img { width:100%; margin-top:12px; border-radius:8px; }
.fake-img { width:90px; height:90px; border-radius:8px; background:linear-gradient(145deg,#dcefe0,#a1c49f); display:flex; align-items:center; justify-content:center; color:#fff; font-weight:900; flex:none; }
.form-card { background:#fff; border-radius:14px; margin-bottom:12px; overflow:hidden; }
.form-title { font-weight:900; font-size:18px; padding:14px; border-bottom:1px solid #edf0f2; }
.field { display:grid; grid-template-columns:94px 1fr auto; gap:8px; align-items:center; padding:13px 14px; border-bottom:1px solid #edf0f2; min-height:52px; cursor:pointer; }
.field label { color:#262b32; font-weight:700; font-size:14px; }
.field .value { color:#9aa0a6; font-size:14px; }
.fixed-actions { background:#fff; border-top:1px solid #eee; padding:10px 14px calc(10px + var(--safe-area-bottom,0px)); display:grid; grid-template-columns:1fr 1fr; gap:12px; }
.fixed-actions .btn { height:50px; font-size:18px; border-radius:8px; }
.btn { border:0; border-radius:9px; padding:8px 14px; font-weight:800; display:inline-flex; align-items:center; justify-content:center; font-size:14px; }
.btn.primary { background:var(--color-primary); color:#fff; }
.btn.ghost { background:#fff; color:#666; border:1px solid #e7eaee; }
.btn.danger { background:#fff; color:#f25541; border:1px solid #ffd3cc; }
.btn:disabled { opacity:0.5; }
.edit-form { margin-top:12px; }
.edit-form .field { padding:8px 0; border-bottom:1px solid #edf0f2; grid-template-columns:80px 1fr; }
.input { height:38px; background:#f7f8fa; border-radius:8px; border:1px solid #eef0f3; padding:0 10px; color:#555; width:100%; font-size:14px; outline:none; }
.textarea { min-height:80px; background:#f7f8fa; border-radius:8px; border:1px solid #eef0f3; padding:10px; color:#555; width:100%; font-size:14px; font-family:inherit; outline:none; resize:vertical; }
</style>
