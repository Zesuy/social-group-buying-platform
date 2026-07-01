<template>
  <PageLayout title="我的店铺资料" show-back @back="goBack">
    <LoadingView v-if="loading" />

    <ErrorView
      v-else-if="error"
      :message="error"
      show-retry
      @retry="fetchStore"
    />

    <template v-else-if="store">
      <!-- 展示模式 — demo form-card + field 布局 -->
      <template v-if="!editing">
        <div class="store-display">
          <div class="form-card">
            <div class="form-title">店铺信息</div>
            <div class="field">
              <label>店铺名称</label>
              <span class="value">{{ store.name }}</span>
              <b class="muted"></b>
            </div>
            <div class="field">
              <label>Logo URL</label>
              <span class="value">{{ store.logoUrl || '-' }}</span>
              <b class="muted"></b>
            </div>
            <div class="field">
              <label>店铺简介</label>
              <span class="value">{{ store.description || '暂无简介' }}</span>
              <b class="muted"></b>
            </div>
            <div class="field">
              <label>默认物流</label>
              <span class="value">{{ getDeliveryTypeText(store.defaultDeliveryType) }}</span>
              <b class="muted">›</b>
            </div>
          </div>

          <div class="page-note">控件：店铺名称、logo、简介、默认物流方式、保存按钮。</div>
        </div>

        <div class="fixed-actions single">
          <button class="btn primary" @click="startEdit">编辑资料</button>
        </div>
      </template>

      <!-- 编辑模式 — van-form -->
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

function goBack() { router.back() }

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
    const authStore = useAuthStore()
    await authStore.fetchMe()
    editing.value = false
    await fetchStore()
  } catch (err) {
    const apiErr = err as { message?: string }
    showToast(apiErr.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => { fetchStore() })
</script>

<style scoped>
/* 展示模式 — 使用全局 .form-card / .field / .page-note / .fixed-actions 类 */
.store-display {
  padding: 0 14px 84px;  /* 为底部固定操作栏留空间 */
}

/* 编辑模式 */
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
