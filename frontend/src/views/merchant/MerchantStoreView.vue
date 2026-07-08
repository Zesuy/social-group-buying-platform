<template>
  <div class="merchant-page">
    <div class="page-head">
      <div>
        <p>店铺设置</p>
        <h1>店铺资料</h1>
      </div>
      <button type="button" class="primary-button" :disabled="submitting" @click="handleSave">
        {{ submitting ? '保存中...' : '保存资料' }}
      </button>
    </div>

    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" show-retry @retry="fetchStore" />

    <form v-else-if="store" class="store-form" @submit.prevent="handleSave">
      <section class="form-panel">
        <h2>基础信息</h2>
        <label class="field">
          <span>店铺名称</span>
          <input v-model="form.name" placeholder="请输入店铺名称" />
        </label>
        <label class="field">
          <span>店铺简介</span>
          <textarea v-model="form.description" rows="6" maxlength="200" placeholder="介绍你的店铺、履约范围和社群服务特点" />
        </label>
        <div class="delivery-grid">
          <button
            v-for="option in deliveryOptions"
            :key="option.value"
            type="button"
            :class="{ active: form.defaultDeliveryType === option.value }"
            @click="form.defaultDeliveryType = option.value"
          >
            {{ option.label }}
          </button>
        </div>
        <section class="location-box" :class="{ ready: hasLocation }">
          <div>
            <van-icon name="location-o" />
            <span>
              <strong>{{ hasLocation ? '已保存店铺位置' : '未添加店铺位置' }}</strong>
              <small>{{ locationText }}</small>
            </span>
          </div>
          <button type="button" class="ghost-button" :disabled="locating || submitting" @click="locateStore">
            {{ locating ? '定位中' : '获取当前位置' }}
          </button>
        </section>
      </section>

      <aside class="form-panel">
        <h2>店铺 Logo</h2>
        <ImageUploader
          v-model="form.logoUrl"
          :disabled="submitting"
          :preview-alt="form.name || '店铺 Logo'"
          demo-kind="store"
          :show-url-input="false"
          button-label="更换 Logo"
        />
        <dl class="preview-list">
          <div>
            <dt>当前状态</dt>
            <dd>{{ store.status === 'active' ? '营业中' : store.status }}</dd>
          </div>
          <div>
            <dt>默认物流</dt>
            <dd>{{ getDeliveryTypeText(form.defaultDeliveryType) }}</dd>
          </div>
          <div>
            <dt>坐标</dt>
            <dd>{{ coordinateText }}</dd>
          </div>
        </dl>
      </aside>
    </form>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { showToast } from 'vant'
import ErrorView from '@/components/ErrorView.vue'
import ImageUploader from '@/components/ImageUploader.vue'
import LoadingView from '@/components/LoadingView.vue'
import { getMyStore, updateMyStore } from '@/api/stores'
import { useUnsavedChangesGuard } from '@/composables'
import { useAuthStore } from '@/stores/auth'
import { getDeliveryTypeText, requestCurrentLocation, resolveDisplayImageUrl } from '@/utils'
import type { StoreInfo } from '@/types'

const authStore = useAuthStore()
const loading = ref(true)
const error = ref('')
const submitting = ref(false)
const locating = ref(false)
const store = ref<StoreInfo | null>(null)
const initialSnapshot = ref('')

const deliveryOptions = [
  { value: 'express', label: '快递配送' },
  { value: 'pickup', label: '到店自提' },
  { value: 'local_delivery', label: '同城配送' },
]

const form = reactive({
  name: '',
  logoUrl: '',
  description: '',
  defaultDeliveryType: 'express',
  latitude: null as number | null,
  longitude: null as number | null,
})
useUnsavedChangesGuard({
  isDirty: () => !loading.value && JSON.stringify(form) !== initialSnapshot.value,
})

function markClean() {
  initialSnapshot.value = JSON.stringify(form)
}

const hasLocation = computed(() => form.latitude != null && form.longitude != null)
const locationText = computed(() => hasLocation.value
  ? '首页附近筛选和团购卡片距离展示会使用该坐标'
  : '添加后，用户开启定位时可看到店铺距离')
const coordinateText = computed(() => hasLocation.value
  ? `${form.latitude?.toFixed(5)}, ${form.longitude?.toFixed(5)}`
  : '未设置')

function fillForm(data: StoreInfo) {
  form.name = data.name
  form.logoUrl = data.logoUrl || ''
  form.description = data.description || ''
  form.defaultDeliveryType = data.defaultDeliveryType
  form.latitude = data.latitude
  form.longitude = data.longitude
  markClean()
}

async function locateStore() {
  locating.value = true
  try {
    const location = await requestCurrentLocation({ enableHighAccuracy: false, timeout: 8000, maximumAge: 5 * 60 * 1000 })
    form.latitude = location.latitude
    form.longitude = location.longitude
    showToast('已获取店铺位置')
  } catch (err) {
    showToast((err as { message?: string }).message || '定位失败')
  } finally {
    locating.value = false
  }
}

async function fetchStore() {
  loading.value = true
  error.value = ''
  try {
    const data = await getMyStore()
    store.value = data?.store ?? null
    if (store.value) fillForm(store.value)
  } catch (err) {
    error.value = (err as { message?: string }).message || '店铺资料加载失败'
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!form.name.trim()) {
    showToast('请输入店铺名称')
    return
  }

  submitting.value = true
  try {
    await updateMyStore({
      name: form.name.trim(),
      logoUrl: resolveDisplayImageUrl(form.logoUrl.trim(), form.name, 'store'),
      description: form.description.trim() || null,
      defaultDeliveryType: form.defaultDeliveryType,
      latitude: form.latitude,
      longitude: form.longitude,
    })
    showToast('保存成功')
    await fetchStore()
    await authStore.fetchMe()
    markClean()
  } catch (err) {
    showToast((err as { message?: string }).message || '保存失败')
  } finally {
    submitting.value = false
  }
}

onMounted(fetchStore)
</script>

<style scoped>
.merchant-page {
  display: grid;
  gap: 16px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.page-head p {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
}

.page-head h1 {
  margin: 4px 0 0;
  font-size: 26px;
}

.store-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 16px;
  align-items: start;
}

.form-panel {
  display: grid;
  gap: 14px;
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.form-panel h2 {
  margin: 0;
  font-size: 16px;
}

.field {
  display: grid;
  gap: 8px;
}

.field span {
  color: #374151;
  font-size: 13px;
  font-weight: 900;
}

.field input,
.field textarea {
  width: 100%;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  outline: 0;
}

.field textarea {
  resize: vertical;
  line-height: 1.55;
}

.delivery-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.delivery-grid button {
  min-height: 38px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #fff;
  color: #4b5563;
  font-weight: 900;
}

.delivery-grid button.active {
  border-color: #e9563f;
  background: #fff1ed;
  color: #d63f2b;
}

.location-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
}

.location-box.ready {
  border-color: #b7e4c7;
  background: #f0fdf4;
}

.location-box > div {
  display: flex;
  align-items: center;
  gap: 10px;
}

.location-box strong,
.location-box small {
  display: block;
}

.location-box small {
  margin-top: 3px;
  color: #6b7280;
}

.primary-button,
.ghost-button {
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 900;
}

.primary-button {
  border: 0;
  background: #e9563f;
  color: #fff;
}

.ghost-button {
  border: 1px solid #d1d5db;
  background: #fff;
  color: #374151;
}

.preview-list {
  display: grid;
  gap: 10px;
  margin: 0;
}

.preview-list div {
  padding: 12px;
  border-radius: 8px;
  background: #f9fafb;
}

.preview-list dt,
.preview-list dd {
  margin: 0;
}

.preview-list dt {
  color: #6b7280;
  font-size: 12px;
  font-weight: 900;
}

.preview-list dd {
  margin-top: 4px;
  color: #111827;
  font-weight: 900;
}

@media (max-width: 1100px) {
  .store-form {
    grid-template-columns: 1fr;
  }
}
</style>
