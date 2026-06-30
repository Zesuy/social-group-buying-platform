<template>
  <PageLayout title="我的店铺" show-back @back="goBack">
    <LoadingView v-if="loading" />

    <ErrorView
      v-else-if="error"
      :message="error"
      show-retry
      @retry="fetchStore"
    />

    <template v-else-if="store">
      <!-- 展示模式 -->
      <template v-if="!editing">
        <div class="store-header">
          <van-image
            v-if="store.logoUrl"
            :src="store.logoUrl"
            class="store-logo"
            round
            fit="cover"
          />
          <van-icon v-else name="shop" class="store-logo store-logo--placeholder" />
          <h1 class="store-name">{{ store.name }}</h1>
        </div>

        <div class="store-info-section">
          <div class="store-info-row">
            <span class="store-info-label">店铺简介</span>
            <span class="store-info-value">{{ store.description || '暂无简介' }}</span>
          </div>
          <div class="store-info-row">
            <span class="store-info-label">默认物流方式</span>
            <span class="store-info-value">{{ getDeliveryTypeText(store.defaultDeliveryType) }}</span>
          </div>
          <div class="store-info-row">
            <span class="store-info-label">店铺状态</span>
            <span class="store-info-value">{{ getStoreStatusText(store.status) }}</span>
          </div>
        </div>

        <div class="store-actions">
          <van-button type="primary" block round @click="startEdit">
            编辑资料
          </van-button>
        </div>
      </template>

      <!-- 编辑模式 -->
      <template v-else>
        <div class="edit-section">
          <van-form @submit="handleSave">
            <van-cell-group inset>
              <van-field
                v-model="editForm.name"
                name="name"
                label="店铺名称"
                placeholder="请输入店铺名称"
                :rules="[{ required: true, message: '请输入店铺名称' }]"
                clearable
              />
              <van-field
                v-model="editForm.logoUrl"
                name="logoUrl"
                label="Logo URL"
                placeholder="Logo URL（选填）"
                clearable
              />
              <van-field
                v-model="editForm.description"
                name="description"
                label="店铺简介"
                placeholder="请输入店铺简介（选填）"
                type="textarea"
                rows="3"
                autosize
                clearable
              />
            </van-cell-group>

            <div class="edit-delivery-section">
              <div class="edit-delivery-label">默认物流方式</div>
              <van-radio-group v-model="editForm.defaultDeliveryType" direction="horizontal">
                <van-radio name="express">快递配送</van-radio>
                <van-radio name="pickup">到店自提</van-radio>
                <van-radio name="local_delivery">同城配送</van-radio>
              </van-radio-group>
            </div>

            <div class="edit-actions">
              <van-button
                type="primary"
                block
                round
                native-type="submit"
                :loading="submitting"
                loading-text="保存中..."
              >
                保存
              </van-button>
              <van-button
                block
                round
                plain
                style="margin-top: 12px"
                :disabled="submitting"
                @click="cancelEdit"
              >
                取消
              </van-button>
            </div>
          </van-form>
        </div>
      </template>
    </template>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useAuthStore } from '@/stores/auth'
import { getMyStore, updateMyStore } from '@/api/stores'
import { getDeliveryTypeText, getStoreStatusText } from '@/utils/status'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import type { StoreInfo } from '@/types'

const router = useRouter()

function goBack() {
  router.back()
}

const loading = ref(true)
const error = ref<string | null>(null)
const editing = ref(false)
const submitting = ref(false)
const store = ref<StoreInfo | null>(null)

const editForm = ref({
  name: '',
  logoUrl: '',
  description: '',
  defaultDeliveryType: 'express',
})

async function fetchStore() {
  loading.value = true
  error.value = null
  try {
    const data = await getMyStore()
    if (data && data.store) {
      store.value = data.store
    } else {
      error.value = '暂无店铺信息'
    }
  } catch (err) {
    const apiErr = err as { message?: string }
    error.value = apiErr.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function startEdit() {
  if (!store.value) return
  editForm.value = {
    name: store.value.name,
    logoUrl: store.value.logoUrl || '',
    description: store.value.description || '',
    defaultDeliveryType: store.value.defaultDeliveryType,
  }
  editing.value = true
}

function cancelEdit() {
  editing.value = false
}

async function handleSave() {
  submitting.value = true
  try {
    const data = {
      name: editForm.value.name.trim() || undefined,
      logoUrl: editForm.value.logoUrl.trim() || null,
      description: editForm.value.description.trim() || null,
      defaultDeliveryType: editForm.value.defaultDeliveryType,
    }
    await updateMyStore(data)
    showToast('保存成功')
    // Refresh auth store
    const authStore = useAuthStore()
    await authStore.fetchMe()
    editing.value = false
    // Re-fetch store data
    await fetchStore()
  } catch (err) {
    const apiErr = err as { message?: string }
    showToast(apiErr.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchStore()
})
</script>

<style scoped>
.store-header {
  text-align: center;
  padding: var(--spacing-xl) var(--spacing-lg);
  background: var(--color-primary-deep);
  border-radius: var(--radius-card);
  margin: 12px 14px;
  color: #fff;
  box-shadow: var(--shadow-card);
}

.store-logo {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
}

.store-logo--placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  color: var(--color-text-hint);
  background-color: var(--color-bg-surface);
  border-radius: 50%;
}

.store-name {
  font-size: var(--font-size-xxl);
  font-weight: 900;
  margin-top: 12px;
  color: #fff;
}

.store-info-section {
  background: var(--color-bg-card);
  padding: 14px;
  margin: 0 14px 12px;
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  border: 1px solid rgba(237, 240, 242, 0.72);
}

.store-info-row {
  display: flex;
  padding: 8px 0;
  align-items: flex-start;
}

.store-info-row + .store-info-row {
  border-top: 1px solid var(--color-border);
}

.store-info-label {
  width: 100px;
  flex-shrink: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-md);
}

.store-info-value {
  flex: 1;
  color: var(--color-text-primary);
  font-size: var(--font-size-md);
  word-break: break-all;
}

.store-actions {
  padding: var(--spacing-xl) 14px;
}

.store-actions :deep(.van-button) {
  height: var(--button-capsule-height);
  font-weight: 900;
}

.edit-section {
  padding: 12px 0;
}

.edit-section :deep(.van-cell-group--inset) {
  margin: 0 14px;
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
}

.edit-delivery-section {
  padding: var(--spacing-lg) var(--spacing-lg) 0;
  margin-top: var(--spacing-sm);
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  margin-left: 14px;
  margin-right: 14px;
  box-shadow: var(--shadow-card);
}

.edit-delivery-label {
  font-size: var(--font-size-md);
  color: var(--color-text-secondary);
  margin-bottom: 12px;
}

.edit-actions {
  padding: var(--spacing-xl) 14px;
}

.edit-actions :deep(.van-button) {
  height: var(--button-capsule-height);
  font-weight: 900;
}
</style>
