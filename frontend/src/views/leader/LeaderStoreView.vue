<template>
  <PageLayout title="我的店铺资料" show-back @back="goBack">
    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" show-retry @retry="fetchStore" />

    <template v-else-if="store">
      <!-- 展示模式 -->
      <div v-if="!editing" class="store-display">
        <AppFormCard title="店铺信息">
          <AppFormRow label="店铺名称">{{ store.name }}</AppFormRow>
          <AppFormRow label="店铺 Logo">
            <div class="store-logo-preview">
              <ImageWithFallback
                :src="store.logoUrl"
                :alt="`${store.name} Logo`"
                demo-kind="store"
                width="72px"
                height="72px"
                radius="10px"
              />
              <span>{{ store.logoUrl ? '已设置店铺头像' : '暂未上传，当前使用默认图' }}</span>
            </div>
          </AppFormRow>
          <AppFormRow label="店铺简介">{{ store.description || '暂无简介' }}</AppFormRow>
          <AppFormRow label="默认物流" arrow>{{ getDeliveryTypeText(store.defaultDeliveryType) }}</AppFormRow>
          <AppFormRow label="店铺位置">
            <div class="store-location-preview" :class="{ 'store-location-preview--ready': hasStoreLocation }">
              <van-icon name="location-o" />
              <div>
                <strong>{{ hasStoreLocation ? '已添加位置' : '未添加位置' }}</strong>
                <span>{{ locationDisplayText }}</span>
              </div>
            </div>
          </AppFormRow>
        </AppFormCard>

        <AppPageNote variant="info" text="修改店铺资料会同步影响团长主页展示" />
      </div>

      <!-- 编辑模式 — AppFormCard + AppButton + AppFixedActions -->
      <div v-else class="store-edit">
        <AppFormCard title="编辑店铺信息">
          <AppFormRow label="店铺名称">
            <van-field v-model="editForm.name" placeholder="请输入店铺名称" :rules="nameRules" />
          </AppFormRow>
          <AppFormRow label="店铺 Logo">
            <ImageUploader
              v-model="editForm.logoUrl"
              :disabled="submitting"
              :preview-alt="editForm.name || '店铺 Logo'"
              demo-kind="store"
              :show-url-input="false"
              button-label="更换图片"
            />
          </AppFormRow>
          <AppFormRow label="店铺简介">
            <van-field v-model="editForm.description" placeholder="可选，介绍你的店铺" type="textarea" :maxlength="200" show-word-limit />
          </AppFormRow>
          <AppFormRow label="默认物流方式">
            <template #control>
              <div class="delivery-chips">
                <span
                  v-for="d in deliveryOptions"
                  :key="d.value"
                  :class="['chip', { active: editForm.defaultDeliveryType === d.value }]"
                  @click="editForm.defaultDeliveryType = d.value"
                >{{ d.label }}</span>
              </div>
            </template>
          </AppFormRow>
          <AppFormRow label="店铺位置">
            <div class="store-location-editor" :class="{ 'store-location-editor--ready': hasEditLocation }">
              <div class="store-location-editor__copy">
                <van-icon name="location-o" />
                <div>
                  <strong>{{ hasEditLocation ? '已选择店铺位置' : '添加店铺位置' }}</strong>
                  <span>{{ editLocationText }}</span>
                </div>
              </div>
              <button
                type="button"
                class="store-location-editor__button"
                :disabled="submitting || locating"
                @click="locateStore"
              >
                {{ locating ? '定位中' : (hasEditLocation ? '重新定位' : '获取当前位置') }}
              </button>
            </div>
          </AppFormRow>
        </AppFormCard>
      </div>
    </template>

    <!-- 固定底部操作栏 -->
    <template v-if="store" #action>
      <AppFixedActions v-if="!editing" single>
        <AppButton variant="primary" @click="startEdit">编辑资料</AppButton>
      </AppFixedActions>
      <AppFixedActions v-else>
        <AppButton variant="ghost" :disabled="submitting" @click="cancelEdit">取消</AppButton>
        <AppButton variant="primary" :loading="submitting" @click="handleSave">保存</AppButton>
      </AppFixedActions>
    </template>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useAuthStore } from '@/stores/auth'
import { getMyStore, updateMyStore } from '@/api/stores'
import { resolveDisplayImageUrl } from '@/utils'
import { getDeliveryTypeText } from '@/utils/status'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import AppFormCard from '@/components/AppFormCard.vue'
import AppFormRow from '@/components/AppFormRow.vue'
import AppPageNote from '@/components/AppPageNote.vue'
import AppFixedActions from '@/components/AppFixedActions.vue'
import AppButton from '@/components/AppButton.vue'
import ImageUploader from '@/components/ImageUploader.vue'
import ImageWithFallback from '@/components/ImageWithFallback.vue'
import type { StoreResponseData } from '@/types'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(true)
const error = ref<string | null>(null)
const storeData = ref<StoreResponseData | null>(null)
const store = computed(() => storeData.value?.store ?? null)
const editing = ref(false)
const submitting = ref(false)
const locating = ref(false)

const deliveryOptions = [
  { value: 'express', label: '快递配送' },
  { value: 'pickup', label: '到店自提' },
  { value: 'local_delivery', label: '同城配送' },
]

const nameRules = [
  { required: true, message: '请输入店铺名称' },
]

const editForm = reactive({
  name: '',
  logoUrl: '',
  description: '',
  defaultDeliveryType: 'express',
  latitude: null as number | null,
  longitude: null as number | null,
})

const hasStoreLocation = computed(() => store.value?.latitude != null && store.value?.longitude != null)
const hasEditLocation = computed(() => editForm.latitude != null && editForm.longitude != null)
const locationDisplayText = computed(() => (
  hasStoreLocation.value
    ? '首页可按附近筛选展示，团购卡片会显示距离'
    : '添加后，用户在首页开启定位时能看到距离标记'
))
const editLocationText = computed(() => (
  hasEditLocation.value
    ? '保存后同步到团长主页和首页距离筛选'
    : '使用浏览器定位写入店铺坐标，不展示给用户精确地址'
))

function startEdit() {
  if (!store.value) return
  editForm.name = store.value.name
  editForm.logoUrl = store.value.logoUrl || ''
  editForm.description = store.value.description || ''
  editForm.defaultDeliveryType = store.value.defaultDeliveryType
  editForm.latitude = store.value.latitude
  editForm.longitude = store.value.longitude
  editing.value = true
}

function cancelEdit() {
  editing.value = false
}

async function handleSave() {
  if (!editForm.name.trim()) {
    showToast('请输入店铺名称')
    return
  }
  submitting.value = true
  try {
    await updateMyStore({
      name: editForm.name.trim(),
      logoUrl: resolveDisplayImageUrl(editForm.logoUrl.trim(), editForm.name, 'store'),
      description: editForm.description.trim() || null,
      defaultDeliveryType: editForm.defaultDeliveryType,
      latitude: editForm.latitude,
      longitude: editForm.longitude,
    })
    showToast('保存成功')
    editing.value = false
    await fetchStore()
    await authStore.fetchMe()
  } catch (err) {
    const apiErr = err as { message?: string }
    showToast(apiErr.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

function isValidCoordinate(latitude: number, longitude: number) {
  return latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180
}

function requestBrowserLocation(): Promise<{ latitude: number; longitude: number }> {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('当前浏览器不支持定位'))
      return
    }
    navigator.geolocation.getCurrentPosition(
      position => {
        const location = {
          latitude: Number(position.coords.latitude.toFixed(7)),
          longitude: Number(position.coords.longitude.toFixed(7)),
        }
        if (!isValidCoordinate(location.latitude, location.longitude)) {
          reject(new Error('定位坐标异常，请稍后重试'))
          return
        }
        resolve(location)
      },
      () => reject(new Error('未获得定位权限，请检查浏览器设置')),
      { enableHighAccuracy: false, timeout: 8000, maximumAge: 5 * 60 * 1000 },
    )
  })
}

async function locateStore() {
  locating.value = true
  try {
    const location = await requestBrowserLocation()
    editForm.latitude = location.latitude
    editForm.longitude = location.longitude
    showToast('已获取店铺位置')
  } catch (err) {
    const apiErr = err as { message?: string }
    showToast(apiErr.message || '定位失败')
  } finally {
    locating.value = false
  }
}

async function fetchStore() {
  loading.value = true
  error.value = null
  try {
    storeData.value = await getMyStore()
  } catch (err) {
    const apiErr = err as { message?: string }
    error.value = apiErr.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.back()
}

onMounted(() => {
  fetchStore()
})
</script>

<style scoped>
.store-display,
.store-edit {
  padding: 14px 14px calc(var(--actionbar-height) + var(--safe-area-bottom) + 14px);
}

.store-logo-preview {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.store-logo-preview span {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.4;
}

.store-location-preview,
.store-location-editor {
  width: 100%;
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-card);
  background: var(--color-bg-surface);
}

.store-location-preview {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
}

.store-location-preview--ready,
.store-location-editor--ready {
  border-color: rgba(16, 196, 104, 0.28);
  background: var(--color-primary-light);
}

.store-location-preview > .van-icon,
.store-location-editor__copy > .van-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: #fff;
  color: var(--color-primary-dark);
}

.store-location-preview div,
.store-location-editor__copy div {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.store-location-preview strong,
.store-location-editor strong {
  color: var(--color-text-primary);
  font-size: var(--font-size-md);
  line-height: 1.3;
  font-weight: 800;
}

.store-location-preview span,
.store-location-editor span {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.4;
}

.store-location-editor {
  display: grid;
  gap: 10px;
  padding: 10px;
}

.store-location-editor__copy {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.store-location-editor__button {
  min-height: 44px;
  border: 1px solid rgba(16, 196, 104, 0.3);
  border-radius: var(--radius-pill);
  background: #fff;
  color: var(--color-primary-dark);
  font-size: var(--font-size-md);
  font-weight: 800;
}

.store-location-editor__button:disabled {
  opacity: 0.62;
}

/* ── 配送方式 chips（可换行，44px 触控） ── */
.delivery-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.delivery-chips .chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 0 16px;
  border-radius: 9px;
  background: #f2f3f5;
  color: var(--color-text-secondary);
  font-size: var(--font-size-md);
  font-weight: 700;
  cursor: pointer;
  user-select: none;
  border: 1px solid transparent;
}

.delivery-chips .chip.active {
  background: var(--color-primary-light);
  color: var(--color-primary);
  border-color: var(--color-primary);
}

/* ── van-field 在 AppFormRow 内的适配 ── */
.store-edit :deep(.van-cell) {
  padding: 0;
  min-height: 44px;
}

.store-edit :deep(.van-field__body textarea) {
  min-height: 60px;
}
</style>
