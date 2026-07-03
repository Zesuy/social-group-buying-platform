<template>
  <PageLayout title="我的店铺资料" show-back @back="goBack">
    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" show-retry @retry="fetchStore" />

    <template v-else-if="store">
      <!-- 展示模式 -->
      <div v-if="!editing" class="store-display">
        <AppFormCard title="店铺信息">
          <AppFormRow label="店铺名称">{{ store.name }}</AppFormRow>
          <AppFormRow label="Logo URL">{{ store.logoUrl || '-' }}</AppFormRow>
          <AppFormRow label="店铺简介">{{ store.description || '暂无简介' }}</AppFormRow>
          <AppFormRow label="默认物流" arrow>{{ getDeliveryTypeText(store.defaultDeliveryType) }}</AppFormRow>
        </AppFormCard>

        <AppPageNote variant="info" text="修改店铺资料会同步影响团长主页展示" />
      </div>

      <!-- 编辑模式 — AppFormCard + AppButton + AppFixedActions -->
      <div v-else class="store-edit">
        <AppFormCard title="编辑店铺信息">
          <AppFormRow label="店铺名称">
            <van-field v-model="editForm.name" placeholder="请输入店铺名称" :rules="nameRules" />
          </AppFormRow>
          <AppFormRow label="Logo 链接">
            <ImageUploader
              v-model="editForm.logoUrl"
              :disabled="submitting"
              :preview-alt="editForm.name || '店铺 Logo'"
              demo-kind="store"
              placeholder="Logo URL（选填，可上传）"
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
import type { StoreResponseData } from '@/types'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(true)
const error = ref<string | null>(null)
const storeData = ref<StoreResponseData | null>(null)
const store = computed(() => storeData.value?.store ?? null)
const editing = ref(false)
const submitting = ref(false)

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
})

function startEdit() {
  if (!store.value) return
  editForm.name = store.value.name
  editForm.logoUrl = store.value.logoUrl || ''
  editForm.description = store.value.description || ''
  editForm.defaultDeliveryType = store.value.defaultDeliveryType
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
